from typing import Any, Optional

from app.clients.java_client import JavaApiClient

PAGE_SIZE = 100


def fetch_pool(
    client: JavaApiClient,
    area: Optional[str],
    tag: Optional[str],
    min_difficulty: Optional[int],
    max_difficulty: Optional[int],
    max_pool_size: int,
) -> list[dict[str, Any]]:
    pool: list[dict[str, Any]] = []
    page = 0
    while len(pool) < max_pool_size:
        result = client.search_problems(
            area=area, tag=tag, min_difficulty=min_difficulty, max_difficulty=max_difficulty,
            page=page, size=PAGE_SIZE,
        )
        content = result.get("content", [])
        pool.extend(content)
        if page >= result.get("totalPages", 1) - 1 or not content:
            break
        page += 1
    return pool[:max_pool_size]
