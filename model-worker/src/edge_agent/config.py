from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from urllib.parse import urlparse


@dataclass(frozen=True)
class AgentConfig:
    control_plane_url: str
    credential_file: Path
    work_dir: Path
    heartbeat_interval_seconds: int = 30
    poll_interval_seconds: int = 10
    max_parallel_jobs: int = 1
    max_model_bytes: int = 2_147_483_648
    job_timeout_seconds: int = 1800

    def validate(self) -> None:
        parsed = urlparse(self.control_plane_url)
        if parsed.scheme != "https" or not parsed.netloc:
            raise ValueError("control_plane_url must be an HTTPS URL")
        if self.heartbeat_interval_seconds < 1 or self.poll_interval_seconds < 1:
            raise ValueError("Polling and heartbeat intervals must be positive")
        if self.max_parallel_jobs != 1:
            raise ValueError("V1 supports exactly one parallel job")
        if self.max_model_bytes < 1 or self.job_timeout_seconds < 1:
            raise ValueError("Model limit and job timeout must be positive")
