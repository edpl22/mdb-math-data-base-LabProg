import logging
import os
from typing import Optional

import httpx
from flask import Flask, Response, current_app, jsonify, request
from flask_cors import CORS
from pydantic import ValidationError
from werkzeug.exceptions import HTTPException

from app.clients.java_client import JavaApiClient, JavaApiError, get_java_client
from app.core.config import get_settings
from app.core.errors import ApiError
from app.core.security import extract_bearer_token, require_admin
from app.schemas.mock_exam import MockExamRequest, MockExamResponse
from app.schemas.stats import CatalogStats
from app.services.csv_service import export_rows, import_rows, parse_csv
from app.services.mock_exam_service import InsufficientProblemsError, build_mock_exam
from app.services.random_service import NoProblemsFoundError, pick_random_problem
from app.services.selection_service import fetch_pool
from app.services.stats_service import build_stats

logger = logging.getLogger("mdb.tools")

app = Flask(__name__)
app.json.sort_keys = False
app.json.ensure_ascii = False

allowed_origins = os.getenv(
    "CORS_ALLOWED_ORIGINS", "http://localhost:5500,http://localhost:8081,http://localhost:3000"
).split(",")
CORS(app, origins=allowed_origins, supports_credentials=True)


def java_client() -> JavaApiClient:
    # nos testes, app.config["JAVA_CLIENT"] recebe um cliente falso
    return current_app.config.get("JAVA_CLIENT") or get_java_client()


def query_int(name: str, minimum: int = 1, maximum: int = 5) -> Optional[int]:
    raw = request.args.get(name)
    if raw is None or raw == "":
        return None
    try:
        value = int(raw)
    except ValueError as exc:
        raise ApiError(422, f"Dados inválidos: {name} deve ser um número inteiro") from exc
    if not minimum <= value <= maximum:
        raise ApiError(422, f"Dados inválidos: {name} deve estar entre {minimum} e {maximum}")
    return value


def problem_filters() -> dict:
    return {
        "area": request.args.get("area") or None,
        "tag": request.args.get("tag") or None,
        "min_difficulty": query_int("minDifficulty"),
        "max_difficulty": query_int("maxDifficulty"),
    }


@app.get("/health")
def health():
    try:
        java_status = "ok" if java_client().ping() else "error"
    except httpx.HTTPError:
        java_status = "unreachable"
    return jsonify(status="ok", service="api-python", framework="Flask", javaApi=java_status)


@app.get("/tools/random-problem")
def random_problem():
    filters = problem_filters()
    try:
        problem = pick_random_problem(java_client(), get_settings(), **filters)
    except NoProblemsFoundError as exc:
        raise ApiError(404, str(exc)) from exc
    return jsonify(problem)


@app.post("/tools/mock-exam")
def mock_exam():
    body = request.get_json(silent=True)
    if not isinstance(body, dict):
        raise ApiError(400, "Envie um corpo JSON, por exemplo {\"quantity\": 5}.")
    exam_request = MockExamRequest.model_validate(body)

    client = java_client()
    try:
        problems = build_mock_exam(client, get_settings(), exam_request)
    except InsufficientProblemsError as exc:
        raise ApiError(400, str(exc)) from exc

    if exam_request.includeStatements:
        problems = [client.get_problem(p["id"]) for p in problems]

    return jsonify(MockExamResponse(problems=problems).model_dump())


@app.get("/tools/stats")
def catalog_stats():
    stats = build_stats(java_client(), get_settings().max_pool_size_for_selection)
    return jsonify(CatalogStats.model_validate(stats).model_dump())


@app.post("/tools/csv/import")
def import_csv():
    token = extract_bearer_token(request.headers.get("Authorization"))
    client = java_client()
    require_admin(token, client)

    file = request.files.get("file")
    if file is None or not file.filename:
        raise ApiError(400, "Envie o arquivo no campo \"file\" (multipart/form-data).")
    if not file.filename.lower().endswith(".csv"):
        raise ApiError(400, "O arquivo enviado deve ser um .csv")

    content = file.read()
    if not content:
        raise ApiError(400, "Arquivo CSV vazio.")
    try:
        rows = parse_csv(content)
    except UnicodeDecodeError as exc:
        raise ApiError(400, "Não foi possível ler o arquivo como texto UTF-8.") from exc
    if not rows:
        raise ApiError(400, "O CSV não contém linhas de dados.")

    return jsonify(import_rows(rows, token, client).model_dump())


@app.get("/tools/csv/export")
def export_csv():
    token = extract_bearer_token(request.headers.get("Authorization"))
    client = java_client()
    require_admin(token, client)

    summaries = fetch_pool(client, max_pool_size=get_settings().max_pool_size_for_selection, **problem_filters())
    details = [client.get_problem(summary["id"], token) for summary in summaries]

    return Response(
        export_rows(details),
        mimetype="text/csv",
        headers={"Content-Disposition": "attachment; filename=mdb_problemas_export.csv"},
    )


# Tratamento de erros: toda falha vira {"detail": "..."} com o status adequado.

@app.errorhandler(ApiError)
def handle_api_error(exc: ApiError):
    return jsonify(detail=exc.detail), exc.status_code


@app.errorhandler(ValidationError)
def handle_validation_error(exc: ValidationError):
    errors = []
    for err in exc.errors():
        field = ".".join(str(part) for part in err.get("loc", []))
        message = str(err.get("msg", "valor inválido")).removeprefix("Value error, ")
        errors.append(f"{field}: {message}" if field else message)
    return jsonify(detail="Dados inválidos: " + "; ".join(errors), errors=errors), 422


@app.errorhandler(JavaApiError)
def handle_java_api_error(exc: JavaApiError):
    return jsonify(detail=exc.message), exc.status_code


@app.errorhandler(httpx.HTTPError)
def handle_java_unreachable(exc: httpx.HTTPError):
    logger.warning("Falha ao comunicar com a API Java: %s", exc)
    return jsonify(detail="A API Java está indisponível no momento. Verifique se ela está rodando na porta 8080."), 503


@app.errorhandler(HTTPException)
def handle_http_exception(exc: HTTPException):
    messages = {404: "Rota não encontrada.", 405: "Método não suportado nesta rota."}
    return jsonify(detail=messages.get(exc.code, exc.description)), exc.code


@app.errorhandler(Exception)
def handle_unexpected(exc: Exception):
    logger.exception("Erro inesperado", exc_info=exc)
    return jsonify(detail="Ocorreu um erro inesperado na API Python."), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "8000")))
