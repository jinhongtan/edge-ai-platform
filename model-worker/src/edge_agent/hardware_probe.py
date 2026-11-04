from __future__ import annotations

import platform
import shutil
import subprocess
from pathlib import Path
from typing import Any


def probe() -> dict[str, Any]:
    """Collect only detected facts; missing Jetson tooling is represented explicitly."""
    model = _read_text(Path("/proc/device-tree/model"))
    trtexec = shutil.which("trtexec")
    version = _command_version(trtexec) if trtexec else None
    capabilities = {
        "build": bool(trtexec),
        "validate": False,
        "benchmark": False,
    }
    try:
        import tensorrt as trt  # type: ignore[import-not-found]
        capabilities["validate"] = True
        capabilities["benchmark"] = _cuda_available()
        tensorrt_version: str | None = trt.__version__
    except ImportError:
        tensorrt_version = None
    return {"deviceModel": model, "architecture": platform.machine(), "pythonVersion": platform.python_version(),
            "trtexecPath": trtexec, "trtexecVersion": version, "tensorrtVersion": tensorrt_version,
            "capabilities": capabilities}


def _read_text(path: Path) -> str | None:
    try:
        return path.read_bytes().replace(b"\0", b"").decode("utf-8").strip() or None
    except OSError:
        return None


def _command_version(executable: str) -> str | None:
    try:
        output = subprocess.run([executable, "--version"], check=False, capture_output=True, text=True, timeout=5)
        return (output.stdout or output.stderr).strip()[:512] or None
    except (OSError, subprocess.TimeoutExpired):
        return None


def _cuda_available() -> bool:
    try:
        from cuda.bindings import runtime  # type: ignore[import-not-found]
        result, count = runtime.cudaGetDeviceCount()
        return int(result) == 0 and count > 0
    except (ImportError, AttributeError):
        return False
