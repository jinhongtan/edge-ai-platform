from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class BuildRequest:
    model_path: Path
    output_path: Path
    work_directory: Path | None = None
    precision: str = "FP32"
    workspace_mib: int = 1024
    timeout_seconds: int = 1800


class BuildError(RuntimeError):
    """A safe build error suitable for reporting without local paths."""


class BuildProvider(ABC):
    @abstractmethod
    def build(self, request: BuildRequest) -> Path: ...
