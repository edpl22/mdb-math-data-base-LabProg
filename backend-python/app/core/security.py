from typing import Optional

from app.clients.java_client import JavaApiClient, JavaApiError
from app.core.errors import ApiError


def extract_bearer_token(authorization: Optional[str]) -> str:
    if not authorization or not authorization.lower().startswith("bearer "):
        raise ApiError(401, "É necessário o header Authorization com um token Bearer (faça login).")
    return authorization.split(" ", 1)[1].strip()


def require_admin(token: str, client: JavaApiClient) -> dict:
    # o papel do usuário é conferido na própria API Java
    try:
        user = client.get_current_user(token)
    except JavaApiError as exc:
        raise ApiError(401, exc.message) from exc

    if user.get("role") != "ADMIN":
        raise ApiError(403, "Apenas administradores podem realizar esta operação.")
    return user
