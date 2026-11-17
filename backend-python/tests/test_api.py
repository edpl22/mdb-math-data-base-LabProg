import io

import httpx
import pytest

from app.main import app
from tests.helpers import make_mock_client


@pytest.fixture
def client():
    app.config["TESTING"] = True
    with app.test_client() as test_client:
        yield test_client
    app.config.pop("JAVA_CLIENT", None)


def use_java(handler):
    app.config["JAVA_CLIENT"] = make_mock_client(handler)


def _empty_page(_request=None) -> httpx.Response:
    return httpx.Response(200, json={"content": [], "page": 0, "size": 100, "totalElements": 0, "totalPages": 0})


def _page(n: int) -> dict:
    content = [
        {"id": i, "title": f"P{i}", "area": "Algebra", "tags": ["t1"] if i % 2 else ["t2"],
         "initialDifficulty": (i % 5) + 1, "communityDifficulty": None, "ratingsCount": 0}
        for i in range(1, n + 1)
    ]
    return {"content": content, "page": 0, "size": 100, "totalElements": n, "totalPages": 1}


def _csv_upload(content: bytes, name: str = "problemas.csv") -> dict:
    return {"file": (io.BytesIO(content), name)}



def test_mock_exam_endpoint_returns_400_when_not_enough_problems(client):
    use_java(_empty_page)
    response = client.post("/tools/mock-exam", json={"quantity": 5})

    assert response.status_code == 400
    assert "disponíveis" in response.get_json()["detail"]


def test_random_problem_endpoint_returns_404_when_empty(client):
    use_java(_empty_page)
    response = client.get("/tools/random-problem")

    assert response.status_code == 404


def test_random_problem_rejects_invalid_difficulty(client):
    use_java(_empty_page)
    response = client.get("/tools/random-problem?minDifficulty=9")

    assert response.status_code == 422
    assert "minDifficulty" in response.get_json()["detail"]


def test_mock_exam_with_statements_fetches_details(client):
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/problems":
            return httpx.Response(200, json=_page(6))
        problem_id = int(request.url.path.rsplit("/", 1)[1])
        return httpx.Response(200, json={**_page(6)["content"][problem_id - 1],
                                         "statement": f"Enunciado {problem_id}", "hint": "h", "solution": "s"})

    use_java(handler)
    response = client.post("/tools/mock-exam", json={"quantity": 3, "includeStatements": True})

    assert response.status_code == 200
    problems = response.get_json()["problems"]
    assert len(problems) == 3
    assert all(p["statement"].startswith("Enunciado") for p in problems)


def test_validation_error_returns_readable_detail(client):
    use_java(lambda request: httpx.Response(200, json=_page(1)))
    response = client.post("/tools/mock-exam", json={"quantity": 0})

    assert response.status_code == 422
    assert isinstance(response.get_json()["detail"], str)
    assert "quantity" in response.get_json()["detail"]


def test_mock_exam_without_json_body_returns_400(client):
    response = client.post("/tools/mock-exam", data="não é json")

    assert response.status_code == 400



def test_stats_endpoint_aggregates_catalog(client):
    use_java(lambda request: httpx.Response(200, json=_page(10)))
    response = client.get("/tools/stats")

    assert response.status_code == 200
    body = response.get_json()
    assert body["totalProblems"] == 10
    assert sum(d["count"] for d in body["byDifficulty"]) == 10
    assert body["byArea"] == [{"area": "Algebra", "count": 10}]
    assert {t["tag"] for t in body["topTags"]} == {"t1", "t2"}


def test_returns_503_json_when_java_api_is_down(client):
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("connection refused", request=request)

    use_java(handler)
    response = client.get("/tools/random-problem")

    assert response.status_code == 503
    assert "indisponível" in response.get_json()["detail"]


def test_health_reports_java_status(client):
    use_java(lambda request: httpx.Response(200, json={"status": "ok"}))
    response = client.get("/health")

    assert response.get_json() == {"status": "ok", "service": "api-python", "framework": "Flask", "javaApi": "ok"}


def test_unknown_route_returns_json_404(client):
    response = client.get("/nao-existe")

    assert response.status_code == 404
    assert response.get_json() == {"detail": "Rota não encontrada."}



def test_csv_import_requires_authorization_header(client):
    response = client.post("/tools/csv/import", data=_csv_upload(b"id,titulo\n1,x\n"))

    assert response.status_code == 401


def test_csv_import_rejects_non_admin_user(client):
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/users/me":
            return httpx.Response(200, json={"id": 2, "name": "Demo", "role": "USER"})
        raise AssertionError("não deveria chamar outro endpoint")

    use_java(handler)
    response = client.post(
        "/tools/csv/import",
        headers={"Authorization": "Bearer fake-user-token"},
        data=_csv_upload(b"id,titulo\n1,x\n"),
    )

    assert response.status_code == 403


def test_csv_import_accepts_admin_user_and_imports_valid_rows(client):
    calls = {"created": 0}

    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/users/me":
            return httpx.Response(200, json={"id": 1, "name": "Admin", "role": "ADMIN"})
        if request.url.path == "/api/problems" and request.method == "POST":
            calls["created"] += 1
            return httpx.Response(201, json={"id": calls["created"]})
        raise AssertionError(f"unexpected {request.method} {request.url.path}")

    use_java(handler)
    csv_content = (
        "id,titulo,enunciado,area,dificuldade,dica,solucao,fonte\n"
        '1,"Título","Enunciado","Álgebra",2,"Dica","Solução",\n'
    ).encode("utf-8")

    response = client.post(
        "/tools/csv/import",
        headers={"Authorization": "Bearer fake-admin-token"},
        data=_csv_upload(csv_content),
    )

    assert response.status_code == 200
    body = response.get_json()
    assert body["imported"] == 1
    assert body["failed"] == 0
    assert calls["created"] == 1


def test_csv_import_rejects_non_csv_file(client):
    use_java(lambda request: httpx.Response(200, json={"id": 1, "name": "Admin", "role": "ADMIN"}))
    response = client.post(
        "/tools/csv/import",
        headers={"Authorization": "Bearer fake-admin-token"},
        data=_csv_upload(b"qualquer coisa", name="planilha.xlsx"),
    )

    assert response.status_code == 400


def test_csv_export_requires_admin(client):
    use_java(lambda request: httpx.Response(200, json={"id": 2, "name": "Demo", "role": "USER"}))
    response = client.get("/tools/csv/export", headers={"Authorization": "Bearer fake-user-token"})

    assert response.status_code == 403


def test_csv_export_returns_csv_file_for_admin(client):
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/users/me":
            return httpx.Response(200, json={"id": 1, "name": "Admin", "role": "ADMIN"})
        if request.url.path == "/api/problems":
            return httpx.Response(200, json=_page(2))
        problem_id = int(request.url.path.rsplit("/", 1)[1])
        return httpx.Response(200, json={**_page(2)["content"][problem_id - 1],
                                         "statement": "E", "hint": "D", "solution": "S"})

    use_java(handler)
    response = client.get("/tools/csv/export", headers={"Authorization": "Bearer fake-admin-token"})

    assert response.status_code == 200
    assert response.mimetype == "text/csv"
    assert response.get_data(as_text=True).startswith("id,titulo,enunciado")
