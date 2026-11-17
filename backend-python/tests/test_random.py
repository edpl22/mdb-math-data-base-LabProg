import httpx
import pytest

from app.services.random_service import NoProblemsFoundError, pick_random_problem
from tests.helpers import make_mock_client


def test_pick_random_problem_raises_when_no_matches(settings):
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"content": [], "page": 0, "size": 100, "totalElements": 0, "totalPages": 0})

    client = make_mock_client(handler)

    with pytest.raises(NoProblemsFoundError):
        pick_random_problem(client, settings, area=None, tag=None, min_difficulty=None, max_difficulty=None)


def test_pick_random_problem_returns_full_detail(settings):
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/problems":
            return httpx.Response(200, json={
                "content": [{"id": 7, "title": "Problema 7", "area": "Álgebra", "tags": [],
                             "initialDifficulty": 2, "communityDifficulty": None, "ratingsCount": 0}],
                "page": 0, "size": 100, "totalElements": 1, "totalPages": 1,
            })
        if request.url.path == "/api/problems/7":
            return httpx.Response(200, json={"id": 7, "title": "Problema 7", "statement": "...",
                                              "hint": "...", "solution": "...", "area": "Álgebra"})
        raise AssertionError(f"unexpected path {request.url.path}")

    client = make_mock_client(handler)
    result = pick_random_problem(client, settings, area="Álgebra", tag=None, min_difficulty=None, max_difficulty=None)

    assert result["id"] == 7
    assert "solution" in result
