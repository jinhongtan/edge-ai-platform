"""Small local-only TensorRT build command used by the Spring local dispatcher."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from .provider import BuildError, BuildRequest
from .tensorrt_provider import TensorRTBuildProvider


def main() -> int:
    parser = argparse.ArgumentParser(description="Build a static-shape FP32 ONNX engine with local TensorRT.")
    parser.add_argument("model_path")
    parser.add_argument("output_path")
    parser.add_argument("workspace_mib", type=int)
    args = parser.parse_args()
    model = Path(args.model_path)
    output = Path(args.output_path)
    try:
        result = TensorRTBuildProvider().build(BuildRequest(model, output, output.parent, workspace_mib=args.workspace_mib))
        json.dump({"success": True, "engine": result.name}, sys.stdout)
        return 0
    except BuildError as error:
        json.dump({"success": False, "error": str(error)}, sys.stdout)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
