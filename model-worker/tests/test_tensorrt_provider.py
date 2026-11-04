from pathlib import Path
from unittest.mock import Mock, patch

import pytest

from onnx_worker.build.provider import BuildError, BuildRequest
from onnx_worker.build.tensorrt_provider import TensorRTBuildProvider


def request(tmp_path: Path) -> BuildRequest:
    model = tmp_path / "model.onnx"
    model.write_bytes(b"onnx")
    return BuildRequest(model, tmp_path / "model.engine", tmp_path)


def test_constructs_argument_safe_fp32_command_and_requires_engine(tmp_path: Path) -> None:
    build = request(tmp_path)
    def produce_engine(*args: object, **kwargs: object) -> Mock:
        build.output_path.write_bytes(b"engine")
        return Mock(returncode=0)
    with patch("onnx_worker.build.tensorrt_provider.subprocess.run", side_effect=produce_engine) as run:
        assert TensorRTBuildProvider("/usr/bin/trtexec").build(build) == build.output_path.resolve()
    command = run.call_args.args[0]
    assert command == ["/usr/bin/trtexec", f"--onnx={build.model_path.resolve()}", f"--saveEngine={build.output_path.resolve()}", "--memPoolSize=workspace:1024"]
    assert run.call_args.kwargs["shell"] is False


def test_rejects_missing_trtexec_and_nonzero_exit(tmp_path: Path) -> None:
    with pytest.raises(BuildError, match="not found"):
        TensorRTBuildProvider(None).build(request(tmp_path))
    with patch("onnx_worker.build.tensorrt_provider.subprocess.run", return_value=Mock(returncode=7)):
        with pytest.raises(BuildError, match="exit code 7"):
            TensorRTBuildProvider("trtexec").build(request(tmp_path))


def test_rejects_unsupported_precision_and_output_escape(tmp_path: Path) -> None:
    build = request(tmp_path)
    with pytest.raises(BuildError, match="FP32"):
        TensorRTBuildProvider("trtexec").build(BuildRequest(build.model_path, build.output_path, tmp_path, precision="FP16"))
    with pytest.raises(BuildError, match="output"):
        TensorRTBuildProvider("trtexec").build(BuildRequest(build.model_path, tmp_path.parent / "escape.engine", tmp_path))
