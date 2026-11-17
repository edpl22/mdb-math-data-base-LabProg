from typing import Optional

from pydantic import BaseModel


class ProblemSummary(BaseModel):
    id: int
    title: str
    area: str
    tags: list[str] = []
    initialDifficulty: int
    communityDifficulty: Optional[float] = None
    ratingsCount: int = 0


class ProblemDetail(BaseModel):
    id: int
    title: str
    statement: str
    hint: str
    solution: str
    source: Optional[str] = None
    area: str
    tags: list[str] = []
    initialDifficulty: int
    communityDifficulty: Optional[float] = None
    ratingsCount: int = 0
    createdByName: Optional[str] = None
    createdAt: Optional[str] = None
    updatedAt: Optional[str] = None


class ProblemPage(BaseModel):
    content: list[ProblemSummary]
    page: int
    size: int
    totalElements: int
    totalPages: int
