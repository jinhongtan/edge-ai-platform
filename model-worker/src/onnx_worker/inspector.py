"""Bounded, non-executing ONNX file validation and metadata inspection."""

from __future__ import annotations

from pathlib import Path
from typing import Any

MAX_MODEL_BYTES = 100 * 1024 * 1024


class InspectionError(Exception):
    """An actionable, safe-to-display model inspection failure."""

    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code
        self.message = message


def _tensor(value_info: Any, onnx: Any) -> dict[str, Any]:
    tensor_type = value_info.type.tensor_type
    dimensions = []
    for dimension in tensor_type.shape.dim:
        if dimension.dim_value > 0:
            dimensions.append(dimension.dim_value)
        elif dimension.dim_param:
            dimensions.append(dimension.dim_param)
        else:
            dimensions.append(None)
    return {
        "name": value_info.name,
        "element_type": onnx.TensorProto.DataType.Name(tensor_type.elem_type),
        "shape": dimensions,
    }


def _operator_name(node: Any) -> str:
    """Return a stable operator identifier, including a non-default domain."""
    return f"{node.domain}::{node.op_type}" if node.domain else node.op_type


def _operator_counts(model: Any) -> dict[str, int]:
    counts: dict[str, int] = {}
    for node in model.graph.node:
        name = _operator_name(node)
        counts[name] = counts.get(name, 0) + 1
    return dict(sorted(counts.items()))


def _parameter_count(model: Any) -> int:
    """Count elements in dense graph initializers without reading tensor data."""
    total = 0
    for initializer in model.graph.initializer:
        elements = 1
        for dimension in initializer.dims:
            elements *= dimension
        total += elements
    return total


def inspect_model(path: str | Path, max_bytes: int = MAX_MODEL_BYTES) -> dict[str, Any]:
    """Inspect an ONNX model without running its graph or exposing local paths."""
    file_path = Path(path)
    try:
        size = file_path.stat().st_size
    except (OSError, ValueError):
        raise InspectionError("FILE_UNREADABLE", "Model file could not be read.") from None
    if not file_path.is_file():
        raise InspectionError("FILE_UNREADABLE", "Model file could not be read.")
    if size == 0:
        raise InspectionError("EMPTY_FILE", "Model file is empty.")
    if size > max_bytes:
        raise InspectionError("FILE_TOO_LARGE", f"Model exceeds the {max_bytes}-byte size limit.")
    if file_path.suffix.lower() != ".onnx":
        raise InspectionError("UNSUPPORTED_FILE_TYPE", "Only ONNX model files are supported.")
    try:
        import onnx
    except ImportError:
        raise InspectionError("DEPENDENCY_MISSING", "Install the worker dependencies with: python -m pip install -e '.[test]'.") from None
    try:
        model = onnx.load_model(str(file_path), load_external_data=False)
        onnx.checker.check_model(model, full_check=False)
    except Exception:
        raise InspectionError("INVALID_ONNX", "File is not a valid ONNX model. Verify the file and export it again.") from None

    initializers = {value.name for value in model.graph.initializer}
    inputs = [_tensor(value, onnx) for value in model.graph.input if value.name not in initializers]
    outputs = [_tensor(value, onnx) for value in model.graph.output]
    opsets = [{"domain": item.domain, "version": item.version} for item in model.opset_import]
    operator_counts = _operator_counts(model)
    return {
        "valid": True,
        "file_size_bytes": size,
        "ir_version": model.ir_version,
        "opsets": opsets,
        "inputs": inputs,
        "outputs": outputs,
        "operators": list(operator_counts),
        "operator_counts": operator_counts,
        "parameter_count": _parameter_count(model),
    }
