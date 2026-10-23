from __future__ import annotations

import shutil
import subprocess
from pathlib import Path

from .provider import BuildError, BuildProvider, BuildRequest


class TensorRTBuildProvider(BuildProvider):
    """FP32 static-shape builder for a Jetson with an installed ``trtexec`` binary."""

    def __init__(self, trtexec: str | None = None) -> None:
        self._trtexec = trtexec or shutil.which("trtexec")

    def build(self, request: BuildRequest) -> Path:
        if request.precision != "FP32":
            raise BuildError("Only FP32 TensorRT builds are supported in V1.")
        if request.workspace_mib < 1 or request.workspace_mib > 4096:
            raise BuildError("Workspace size is outside the allowed range.")
        if request.timeout_seconds < 1:
            raise BuildError("Build timeout must be positive.")
        if not self._trtexec:
            raise BuildError("TensorRT trtexec was not found on this worker.")
        model = request.model_path.resolve()
        output = request.output_path.resolve()
        work_dir = (request.work_directory or model.parent).resolve()
        if not model.is_file() or not _within(model, work_dir):
            raise BuildError("The build input is unavailable in the isolated work directory.")
        if not _within(output, work_dir):
            raise BuildError("The engine output must remain in the isolated work directory.")
        output.parent.mkdir(parents=True, exist_ok=True)
        command = [self._trtexec, f"--onnx={model}", f"--saveEngine={output}", f"--memPoolSize=workspace:{request.workspace_mib}"]
        try:
            completed = subprocess.run(command, shell=False, check=False, capture_output=True, text=True, timeout=request.timeout_seconds)
        except subprocess.TimeoutExpired as error:
            raise BuildError("TensorRT build timed out.") from error
        except OSError as error:
            raise BuildError("TensorRT build process could not be started.") from error
        if completed.returncode != 0:
            raise BuildError(f"TensorRT build failed with exit code {completed.returncode}.")
        if not output.is_file() or output.stat().st_size == 0:
            raise BuildError("TensorRT did not produce a non-empty engine artifact.")
        return output


def _within(path: Path, parent: Path) -> bool:
    try:
        path.relative_to(parent)
        return True
    except ValueError:
        return False
