import random
from typing import Any, Optional

from app.clients.java_client import JavaApiClient
from app.core.config import Settings
from app.services.selection_service import fetch_pool


class NoProblemsFoundError(Exception):
    pass


def pick_random_problem(
    client: JavaApiClient,
    settings: Settings,
    area: Optional[str],
    tag: Optional[str],
    min_difficulty: Optional[int],
    max_difficulty: Optional[int],
) -> dict[str, Any]:
    pool = fetch_pool(client, area, tag, min_difficulty, max_difficulty, settings.max_pool_size_for_selection)
    if not pool:
        raise NoProblemsFoundError("Nenhum problema encontrado com os filtros informados.")

    chosen_summary = random.choice(pool)
    return client.get_problem(chosen_summary["id"])
