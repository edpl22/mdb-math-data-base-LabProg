from pydantic import BaseModel


class CsvRowError(BaseModel):
    line: int
    reason: str
    raw: dict[str, str] = {}


class CsvImportResult(BaseModel):
    totalRows: int
    imported: int
    failed: int
    errors: list[CsvRowError]
