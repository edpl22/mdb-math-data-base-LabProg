from collections import Counter
from typing import Any

from app.clients.java_client import JavaApiClient
from app.services.selection_service import fetch_pool


def effective_difficulty(problem: dict[str, Any]) -> int:
    # média da comunidade se houver avaliações; senão, a dificuldade inicial
    community = problem.get("communityDifficulty")
    if community is not None:
        return max(1, min(5, round(community)))
    return int(problem.get("initialDifficulty") or 3)


def build_stats(client: JavaApiClient, max_pool_size: int) -> dict[str, Any]:
    pool = fetch_pool(client, None, None, None, None, max_pool_size)

    by_area = Counter(p.get("area", "—") for p in pool)
    by_difficulty = Counter(effective_difficulty(p) for p in pool)
    tag_counter = Counter(tag for p in pool for tag in p.get("tags", []))
    rated = [p for p in pool if p.get("communityDifficulty") is not None]
    total_ratings = sum(int(p.get("ratingsCount") or 0) for p in pool)

    average = (
        round(sum(effective_difficulty(p) for p in pool) / len(pool), 2) if pool else None
    )

    return {
        "totalProblems": len(pool),
        "ratedProblems": len(rated),
        "totalRatings": total_ratings,
        "averageDifficulty": average,
        "byArea": [{"area": area, "count": count} for area, count in by_area.most_common()],
        "byDifficulty": [{"level": level, "count": by_difficulty.get(level, 0)} for level in range(1, 6)],
        "topTags": [{"tag": tag, "count": count} for tag, count in tag_counter.most_common(8)],
    }
