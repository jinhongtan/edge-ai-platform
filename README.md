# Edge AI Platform

A monorepo scaffold for uploading ONNX models, validating their structure, inspecting tensor metadata, reporting evidence-based target compatibility, and tracking deployment preparation jobs.

## MVP scope

The current scaffold provides a dashboard shell, REST list/health endpoints, an ONNX inspection library/CLI, and architecture for incrementally delivering upload, compatibility and job workflows. It does **not** compile models for vendor hardware, deploy to physical devices, or benchmark inference. No compatibility is inferred solely from successful parsing.

## Stack

- Frontend: React, TypeScript, Vite, Vitest, React Testing Library.
- Backend: Java 21, Spring Boot 3, Spring Web/Validation/Data JPA, Maven.
- Worker: Python 3.12+, ONNX, ONNX Runtime, pytest.
- Persistence: PostgreSQL for deployment; local development can use PostgreSQL via the documented configuration.

## Repository

`frontend/` web UI | `backend/` API | `model-worker/` model inspection | `docs/` architecture and roadmap | `tests/` cross-component tests | `.github/workflows/ci.yml` CI.

## Prerequisites

Java 21, Node.js LTS and npm, Python 3.12+, PostgreSQL 15+. Maven 3.9+ can be used locally; CI runs the committed Maven Wrapper, which downloads Maven 3.9.9 on first use.

## Local development

Backend (set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` for PostgreSQL):

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
npm ci
npm run dev
```

Model worker:

```powershell
cd model-worker
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[test]"
python -m onnx_worker --help
```

The frontend currently labels its sample cards as mock data. The worker is a Python library/CLI, not an HTTP service. Backend endpoints are `GET /health`, `/api/v1/models`, `/api/v1/hardware`, and `/api/v1/deployments`.

## Checks

```powershell
cd backend; .\mvnw.cmd test
cd ..\model-worker; python -m pytest
cd ..\frontend; npm ci; npm test -- --run; npm run build
```

CI uses `backend/mvnw test`. The npm lockfile and Maven Wrapper scripts are committed for reproducible installs.

## Status and next steps

Implemented: health and empty list APIs, dashboard shell/mock state, Python ONNX validation and tensor metadata extraction, docs and component tests. Not yet implemented: upload persistence, API-to-worker invocation, hardware registry, compatibility evaluation, deployment job mutations/packages, real hardware support.

Recommended next: add bounded upload persistence and an isolated local-process worker adapter with size limits and contract tests.
