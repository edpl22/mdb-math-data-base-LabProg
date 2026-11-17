import httpx
import pytest
from pydantic import ValidationError

from app.schemas.mock_exam import MockExamRequest
from app.services.mock_exam_service import InsufficientProblemsError, build_mock_exam
from tests.helpers import make_mock_client


def _page_with(n: int) -> dict:
    content = [
        {"id": i, "title": f"Problema {i}", "area": "Álgebra", "tags": [],
         "initialDifficulty": 2, "communityDifficulty": None, "ratingsCount": 0}
        for i in range(1, n + 1)
    ]
    return {"content": content, "page": 0, "size": 100, "totalElements": n, "totalPages": 1}


def test_build_mock_exam_raises_when_pool_too_small(settings):
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=_page_with(3))

    client = make_mock_client(handler)
    request = MockExamRequest(quantity=10)

    with pytest.raises(InsufficientProblemsError) as exc_info:
        build_mock_exam(client, settings, request)

    assert exc_info.value.available == 3
    assert exc_info.value.requested == 10


def test_build_mock_exam_returns_requested_quantity(settings):
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=_page_with(20))

    client = make_mock_client(handler)
    request = MockExamRequest(quantity=5, minDifficulty=1, maxDifficulty=5)

    problems = build_mock_exam(client, settings, request)

    assert len(problems) == 5
    assert len({p["id"] for p in problems}) == 5  # sem repeticao


def test_mock_exam_request_rejects_max_lower_than_min():
    with pytest.raises(ValidationError):
        MockExamRequest(quantity=5, minDifficulty=4, maxDifficulty=2)


def test_mock_exam_request_rejects_zero_quantity():
    with pytest.raises(ValidationError):
        MockExamRequest(quantity=0)
