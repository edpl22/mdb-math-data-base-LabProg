import random
from typing import Any

from app.clients.java_client import JavaApiClient
from app.core.config import Settings
from app.schemas.mock_exam import MockExamRequest
from app.services.selection_service import fetch_pool


class InsufficientProblemsError(Exception):
    def __init__(self, available: int, requested: int):
        self.available = available
        self.requested = requested
        super().__init__(
            f"Apenas {available} problema(s) disponíveis com os filtros informados, "
            f"mas {requested} foram solicitados."
        )


def build_mock_exam(client: JavaApiClient, settings: Settings, request: MockExamRequest) -> list[dict[str, Any]]:
    pool = fetch_pool(
        client, request.area, None, request.minDifficulty, request.maxDifficulty,
        settings.max_pool_size_for_selection,
    )
    if len(pool) < request.quantity:
        raise InsufficientProblemsError(available=len(pool), requested=request.quantity)

    return random.sample(pool, request.quantity)
