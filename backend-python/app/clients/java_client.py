from typing import Any, Optional

import httpx

from app.core.config import Settings, get_settings


class JavaApiError(Exception):
    def __init__(self, status_code: int, message: str):
        self.status_code = status_code
        self.message = message
        super().__init__(message)


class JavaApiClient:
    """Acesso à API Java. O serviço Python não acessa o banco diretamente."""

    def __init__(self, client: Optional[httpx.Client] = None, settings: Optional[Settings] = None):
        self._settings = settings or get_settings()
        self._client = client or httpx.Client(
            base_url=self._settings.java_api_base_url,
            timeout=self._settings.request_timeout_seconds,
        )

    def _headers(self, token: Optional[str]) -> dict[str, str]:
        return {"Authorization": f"Bearer {token}"} if token else {}

    def _raise_for_status(self, response: httpx.Response) -> None:
        if response.status_code >= 400:
            try:
                message = response.json().get("message", response.text)
            except Exception:
                message = response.text
            raise JavaApiError(response.status_code, message)

    def search_problems(
        self,
        text: Optional[str] = None,
        area: Optional[str] = None,
        tag: Optional[str] = None,
        min_difficulty: Optional[int] = None,
        max_difficulty: Optional[int] = None,
        page: int = 0,
        size: int = 20,
    ) -> dict[str, Any]:
        params = {
            "text": text,
            "area": area,
            "tag": tag,
            "minDifficulty": min_difficulty,
            "maxDifficulty": max_difficulty,
            "page": page,
            "size": size,
        }
        params = {k: v for k, v in params.items() if v is not None}
        response = self._client.get("/api/problems", params=params)
        self._raise_for_status(response)
        return response.json()

    def get_problem(self, problem_id: int, token: Optional[str] = None) -> dict[str, Any]:
        response = self._client.get(f"/api/problems/{problem_id}", headers=self._headers(token))
        self._raise_for_status(response)
        return response.json()

    def create_problem(self, payload: dict[str, Any], token: str) -> dict[str, Any]:
        response = self._client.post("/api/problems", json=payload, headers=self._headers(token))
        self._raise_for_status(response)
        return response.json()

    def get_current_user(self, token: str) -> dict[str, Any]:
        response = self._client.get("/api/users/me", headers=self._headers(token))
        self._raise_for_status(response)
        return response.json()

    def ping(self) -> bool:
        response = self._client.get("/api/health", timeout=2.0)
        return response.status_code == 200

    def close(self) -> None:
        self._client.close()


_default_client: Optional[JavaApiClient] = None


def get_java_client() -> JavaApiClient:
    global _default_client
    if _default_client is None:
        _default_client = JavaApiClient()
    return _default_client
