from typing import Optional

from pydantic import BaseModel


class AreaCount(BaseModel):
    area: str
    count: int


class DifficultyCount(BaseModel):
    level: int
    count: int


class TagCount(BaseModel):
    tag: str
    count: int


class CatalogStats(BaseModel):
    totalProblems: int
    ratedProblems: int
    totalRatings: int
    averageDifficulty: Optional[float] = None
    byArea: list[AreaCount]
    byDifficulty: list[DifficultyCount]
    topTags: list[TagCount]
