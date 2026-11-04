# Model worker

Python library and CLI for validating ONNX structure and reading input/output metadata. It does not execute the graph, expose an HTTP service, or compile for hardware.

The `onnx_worker.build.TensorRTBuildProvider` is a Jetson-only FP32/static-shape primitive for the local dispatcher. It invokes the locally installed `trtexec` with an argument vector, verifies a non-empty engine, and never creates a placeholder artifact. It does not provide remote dispatch, artifact transfer, callback authentication, or runtime inference validation. An AWS artifact-store or queue adapter is explicitly deferred.

To activate the backend's single-concurrency local queue on a Jetson, set `APP_BUILD_ENABLED=true` and point `APP_BUILD_EXECUTABLE` at the Python interpreter where this package is installed. The backend and worker must share the controlled local model/build directories. Engines and build logs are written below `APP_BUILD_DIRECTORY/<build-id>/`.

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[test]"
python -m pytest
onnx-worker path\to\model.onnx
```

The JSON CLI returns `{success, data}` or `{success: false, error: {code, message}}`. A successful inspection includes file size, ONNX IR version, opsets, graph inputs and outputs, distinct operators, per-operator node counts, and the total elements in dense graph initializers as `parameter_count`. The Java `LocalProcessModelInspector` invokes this CLI with argument-safe process creation, a bounded timeout, and capped output.
