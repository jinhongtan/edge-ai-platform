from pathlib import Path

import onnx
import pytest
from onnx import TensorProto, helper

from onnx_worker import InspectionError, inspect_model


@pytest.fixture
def tiny_model(tmp_path: Path) -> Path:
    x = helper.make_tensor_value_info("features", TensorProto.FLOAT, [1, 3])
    y = helper.make_tensor_value_info("scores", TensorProto.FLOAT, [1, 2])
    weights = helper.make_tensor("weights", TensorProto.FLOAT, [3, 2], [1, 0, 0, 1, 1, 1])
    node = helper.make_node("MatMul", ["features", "weights"], ["scores"])
    graph = helper.make_graph([node], "tiny", [x], [y], [weights])
    model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 13)])
    model.ir_version = 9
    path = tmp_path / "tiny.onnx"
    onnx.save(model, path)
    return path


def test_valid_model_metadata(tiny_model: Path) -> None:
    result = inspect_model(tiny_model)
    assert result["valid"] is True
    assert result["inputs"] == [{"name": "features", "element_type": "FLOAT", "shape": [1, 3]}]
    assert result["outputs"] == [{"name": "scores", "element_type": "FLOAT", "shape": [1, 2]}]
    assert result["opsets"] == [{"domain": "", "version": 13}]
    assert result["operators"] == ["MatMul"]
    assert result["operator_counts"] == {"MatMul": 1}
    assert result["parameter_count"] == 6


@pytest.mark.parametrize("name,content,code", [("broken.onnx", b"not an onnx model", "INVALID_ONNX"), ("empty.onnx", b"", "EMPTY_FILE")])
def test_invalid_files_are_reported(tmp_path: Path, name: str, content: bytes, code: str) -> None:
    path = tmp_path / name
    path.write_bytes(content)
    with pytest.raises(InspectionError) as error:
        inspect_model(path)
    assert error.value.code == code
    assert str(tmp_path) not in error.value.message


def test_missing_file_is_safe(tmp_path: Path) -> None:
    with pytest.raises(InspectionError, match="could not be read"):
        inspect_model(tmp_path / "missing.onnx")


def test_size_limit_precedes_parser(tiny_model: Path) -> None:
    with pytest.raises(InspectionError) as error:
        inspect_model(tiny_model, max_bytes=1)
    assert error.value.code == "FILE_TOO_LARGE"


def test_missing_onnx_dependency_is_actionable(tiny_model: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    import builtins

    real_import = builtins.__import__

    def without_onnx(name: str, *args: object, **kwargs: object) -> object:
        if name == "onnx":
            raise ImportError("test dependency unavailable")
        return real_import(name, *args, **kwargs)

    monkeypatch.setattr(builtins, "__import__", without_onnx)
    with pytest.raises(InspectionError) as error:
        inspect_model(tiny_model)
    assert error.value.code == "DEPENDENCY_MISSING"
    assert "pip install" in error.value.message
