"""JSON command-line interface suitable for a future bounded local process adapter."""

from __future__ import annotations

import argparse
import json
import sys

from .inspector import InspectionError, inspect_model


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate an ONNX model and print its metadata as JSON.")
    parser.add_argument("model_path", help="Path to the ONNX file to inspect")
    args = parser.parse_args()
    try:
        result = {"success": True, "data": inspect_model(args.model_path)}
        code = 0
    except InspectionError as error:
        result = {"success": False, "error": {"code": error.code, "message": error.message}}
        code = 2
    json.dump(result, sys.stdout)
    sys.stdout.write("\n")
    return code


if __name__ == "__main__":
    raise SystemExit(main())
