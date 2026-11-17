import httpx

from app.services.csv_service import export_rows, import_rows, parse_csv
from tests.helpers import make_mock_client

VALID_CSV = (
    "id,titulo,enunciado,area,dificuldade,dica,solucao,fonte\n"
    '1,"Equação quadrática","Resolva x^2 - 5x + 6 = 0","Álgebra",2,"Fatore","x=2 ou x=3","Lista 1"\n'
)

INVALID_CSV = (
    "id,titulo,enunciado,area,dificuldade,dica,solucao,fonte\n"
    '1,"","Enunciado","Álgebra",2,"Dica","Solução",\n'  # titulo vazio
    '2,"Dificuldade fora do range","Enunciado","Álgebra",8,"Dica","Solução",\n'  # dificuldade invalida
    '3,"Dificuldade não numérica","Enunciado","Álgebra",abc,"Dica","Solução",\n'
)


def test_parse_csv_reads_rows_as_dicts():
    rows = parse_csv(VALID_CSV.encode("utf-8"))
    assert len(rows) == 1
    assert rows[0]["titulo"] == "Equação quadrática"
    assert rows[0]["dificuldade"] == "2"


def test_import_rows_inserts_valid_and_reports_none_when_all_valid():
    rows = parse_csv(VALID_CSV.encode("utf-8"))

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/api/problems"
        return httpx.Response(201, json={"id": 100, "title": "Equação quadrática"})

    client = make_mock_client(handler)
    result = import_rows(rows, token="fake-token", client=client)

    assert result.totalRows == 1
    assert result.imported == 1
    assert result.failed == 0
    assert result.errors == []


def test_import_rows_reports_errors_without_aborting_batch():
    rows = parse_csv(INVALID_CSV.encode("utf-8"))

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(201, json={"id": 1})

    client = make_mock_client(handler)
    result = import_rows(rows, token="fake-token", client=client)

    assert result.totalRows == 3
    assert result.imported == 0
    assert result.failed == 3
    reasons = " ".join(e.reason for e in result.errors)
    assert "obrigatórios" in reasons
    assert "fora do intervalo" in reasons
    assert "não é um número inteiro" in reasons


def test_import_rows_reports_java_rejection_as_row_error():
    rows = parse_csv(VALID_CSV.encode("utf-8"))

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(404, json={"message": "Area 'Álgebra' not found."})

    client = make_mock_client(handler)
    result = import_rows(rows, token="fake-token", client=client)

    assert result.imported == 0
    assert result.failed == 1
    assert "not found" in result.errors[0].reason


def test_export_rows_produces_expected_header_and_values():
    problems = [{
        "id": 1, "title": "Título", "statement": "Enunciado", "area": "Álgebra",
        "initialDifficulty": 2, "hint": "Dica", "solution": "Solução", "source": "Fonte X",
    }]
    csv_text = export_rows(problems)
    lines = csv_text.strip().splitlines()
    assert lines[0] == "id,titulo,enunciado,area,dificuldade,dica,solucao,fonte"
    assert "Título" in lines[1] and "Fonte X" in lines[1]
