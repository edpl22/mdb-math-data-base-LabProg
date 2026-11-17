import pytest

from app.core.config import Settings


@pytest.fixture
def settings() -> Settings:
    return Settings(java_api_base_url="http://fake-java-api", max_pool_size_for_selection=500)
