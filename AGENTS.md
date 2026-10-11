# Repository guidance

## Purpose and boundaries
Build a platform that inspects ONNX models and reports evidence-based compatibility with registered edge targets. MVP covers upload, validation, metadata, compatibility reports, and deployment job tracking. Hardware compilation, device deployment, and benchmarking are future work; never represent them as implemented.

## Areas
- `frontend/`: React UI and API client.
- `backend/`: Java/Spring REST API, application services, persistence, and worker adapter.
- `model-worker/`: isolated Python ONNX inspection package and CLI contract. Keep model processing out of Java.
- `docs/`: architecture, API contracts, and roadmap.
- `tests/`: cross-component or contract-level checks.

Inspect existing code and repository instructions before editing. Preserve user work, avoid unrelated refactors and unnecessary dependencies. Update docs when architecture or API contracts change.

## Conventions
- Java: Java 21, small Spring services, constructor injection, records for DTOs, Jakarta validation, explicit error responses, no business logic in controllers.
- Python: typed, small functions, deterministic tests, explicit errors; never execute uploaded files or use unsafe deserialization.
- TypeScript: strict mode, functional React components, accessible markup, typed API boundaries.
- REST: version under `/api/v1`, JSON, stable resource names, consistent errors, appropriate HTTP statuses, validate inputs.

## Safety and claims
Treat model uploads as untrusted. Enforce size/type limits, store outside web roots, avoid logging secrets or internal paths, and inspect using ONNX tooling without executing models. Compatibility requires explicit target/runtime capability evidence; ONNX parsing or file extension alone never proves hardware support. Do not add vendor compilation without its SDK.

## Validation
Add or update focused tests for changes. Run backend Maven tests, worker pytest, frontend Vitest and production build as applicable. Document unavailable toolchains or failed checks accurately. Keep environment-specific configuration out of source and never commit credentials or large model binaries.
