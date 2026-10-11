# Cross-component tests

Component-level tests currently live beside their components (`backend/src/test`, `model-worker/tests`, and `frontend/src`). Place future API/worker contract tests here when an executable cross-component boundary exists. The current worker boundary is a JSON CLI; the Java process adapter is not implemented yet.
