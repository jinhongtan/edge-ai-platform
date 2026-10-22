# Cross-component tests

Component-level tests currently live beside their components (`backend/src/test`, `model-worker/tests`, and `frontend/src`). Backend tests cover upload boundaries, inspector JSON parsing, missing inspector executables, and adapter timeouts. Worker tests cover generated ONNX fixtures and missing-dependency errors. Place additional cross-component contract tests here when a shared fixture runner is added.
