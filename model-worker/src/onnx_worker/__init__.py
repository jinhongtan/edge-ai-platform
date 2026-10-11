"""ONNX validation and metadata extraction; no inference is executed."""

from .inspector import InspectionError, inspect_model

__all__ = ["InspectionError", "inspect_model"]
