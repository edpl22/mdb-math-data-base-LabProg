import csv
import io
from typing import Any

from app.clients.java_client import JavaApiClient, JavaApiError
from app.schemas.csv_import import CsvImportResult, CsvRowError

REQUIRED_FIELDS = ["titulo", "enunciado", "area", "dificuldade", "dica", "solucao"]
EXPECTED_HEADER = ["id", "titulo", "enunciado", "area", "dificuldade", "dica", "solucao", "fonte"]


def parse_csv(content: bytes) -> list[dict[str, str]]:
    text = content.decode("utf-8-sig")
    reader = csv.DictReader(io.StringIO(text))
    return list(reader)


def _validate_row(row: dict[str, str]) -> tuple[dict[str, Any] | None, str | None]:
    missing = [field for field in REQUIRED_FIELDS if not (row.get(field) or "").strip()]
    if missing:
        return None, f"Campos obrigatórios ausentes ou vazios: {', '.join(missing)}"

    raw_difficulty = row["dificuldade"].strip()
    try:
        difficulty = int(raw_difficulty)
    except ValueError:
        return None, f"Dificuldade '{raw_difficulty}' não é um número inteiro"

    if not (1 <= difficulty <= 5):
        return None, f"Dificuldade {difficulty} fora do intervalo permitido (1 a 5)"

    payload = {
        "title": row["titulo"].strip(),
        "statement": row["enunciado"].strip(),
        "area": row["area"].strip(),
        "hint": row["dica"].strip(),
        "solution": row["solucao"].strip(),
        "difficulty": difficulty,
        "tags": [],
        "source": (row.get("fonte") or "").strip() or None,
    }
    return payload, None


def import_rows(rows: list[dict[str, str]], token: str, client: JavaApiClient) -> CsvImportResult:
    errors: list[CsvRowError] = []
    imported = 0

    for index, row in enumerate(rows, start=2):  # linha 1 é o cabeçalho
        payload, error = _validate_row(row)
        if error:
            errors.append(CsvRowError(line=index, reason=error, raw=row))
            continue

        try:
            client.create_problem(payload, token)
            imported += 1
        except JavaApiError as exc:
            errors.append(CsvRowError(line=index, reason=f"Rejeitada pela API Java: {exc.message}", raw=row))

    return CsvImportResult(
        totalRows=len(rows),
        imported=imported,
        failed=len(errors),
        errors=errors,
    )


def export_rows(problems: list[dict[str, Any]]) -> str:
    buffer = io.StringIO()
    writer = csv.writer(buffer, quoting=csv.QUOTE_MINIMAL)
    writer.writerow(EXPECTED_HEADER)
    for problem in problems:
        writer.writerow([
            problem.get("id", ""),
            problem.get("title", ""),
            problem.get("statement", ""),
            problem.get("area", ""),
            problem.get("initialDifficulty", ""),
            problem.get("hint", ""),
            problem.get("solution", ""),
            problem.get("source") or "",
        ])
    return buffer.getvalue()
