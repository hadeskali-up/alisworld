from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    api_key: str
    timezone: str = "Asia/Kuala_Lumpur"
    database_url: str = "sqlite:///./alisworld.db"
    firebase_credentials_path: Optional[str] = None

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
