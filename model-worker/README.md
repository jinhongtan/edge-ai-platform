# Model worker

Python library and CLI for validating ONNX structure and reading input/output metadata. It does not execute the graph, expose an HTTP service, or compile for hardware.

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[test]"
python -m pytest
onnx-worker path\to\model.onnx
```

The JSON CLI returns `{success, data}` or `{success: false, error: {code, message}}`. A successful inspection includes file size, ONNX IR version, opsets, graph inputs and outputs, distinct operators, per-operator node counts, and the total elements in dense graph initializers as `parameter_count`. The Java `LocalProcessModelInspector` invokes this CLI with argument-safe process creation, a bounded timeout, and capped output.
