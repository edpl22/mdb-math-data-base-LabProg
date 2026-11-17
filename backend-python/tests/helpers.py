import httpx

from app.clients.java_client import JavaApiClient
from app.core.config import Settings


def make_mock_client(handler) -> JavaApiClient:
    transport = httpx.MockTransport(handler)
    http_client = httpx.Client(base_url="http://fake-java-api", transport=transport)
    return JavaApiClient(client=http_client, settings=Settings(java_api_base_url="http://fake-java-api"))
