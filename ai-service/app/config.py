import os
import re
from dataclasses import dataclass, field


@dataclass(frozen=True)
class Settings:
    api_key: str = field(default="", repr=False)
    service_token: str = field(default="", repr=False)
    model: str = ""
    api_base: str = "https://api.commandcode.ai/provider/v1"
    zdr: bool = False
    deadline_ms: int = 30000
    concurrency: int = 4
    daily_tokens: int = 1000000
    max_input_bytes: int = 48000
    max_candidates: int = 192
    max_jobs: int = 10
    budget_path: str = "jev-budget.sqlite3"
    retries: int = 1
    accept: float = 0.9
    reject: float = 0.1
    quality_approved: bool = False

    def __post_init__(self):
        if self.api_base.rstrip("/") not in {
            "https://api.commandcode.ai/provider", "https://api.commandcode.ai/provider/v1"
        }:
            raise ValueError("unsupported provider endpoint")
        if self.model and not re.fullmatch(r"typesafe/jev-\d+\.\d+\.\d+", self.model):
            raise ValueError("Jev model required")
        if not (0 <= self.reject < self.accept <= 1):
            raise ValueError("invalid policy thresholds")
        if not all(v > 0 for v in (self.deadline_ms, self.concurrency, self.daily_tokens,
                                  self.max_input_bytes, self.max_candidates, self.max_jobs)):
            raise ValueError("limits must be positive")
        if self.retries not in (0, 1):
            raise ValueError("at most one retry")

    @property
    def ready(self):
        return bool(self.api_key and self.service_token and self.model)

    @classmethod
    def from_env(cls):
        return cls(api_key=os.getenv("CMD_API_KEY", ""),
                   service_token=os.getenv("AI_SERVICE_TOKEN", ""),
                   model=os.getenv("JEV_MODEL", ""),
                   api_base=os.getenv("CMD_API_BASE", cls.api_base),
                   zdr=os.getenv("CMD_ZDR", "0") == "1",
                   deadline_ms=int(os.getenv("AI_DEADLINE_MS", "30000")),
                   concurrency=int(os.getenv("AI_CONCURRENCY", "4")),
                   daily_tokens=int(os.getenv("AI_DAILY_TOKENS", "1000000")),
                   budget_path=os.getenv("AI_BUDGET_PATH", "jev-budget.sqlite3"),
                   max_input_bytes=int(os.getenv("AI_MAX_INPUT_BYTES", "48000")),
                   max_candidates=int(os.getenv("AI_MAX_CANDIDATES", "192")),
                   max_jobs=int(os.getenv("AI_MAX_JOBS", "10")),
                   retries=int(os.getenv("AI_RETRIES", "1")),
                   quality_approved=os.getenv("AI_QUALITY_APPROVED", "0") == "1")
