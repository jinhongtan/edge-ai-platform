# Edge AI Platform

A monorepo scaffold for uploading ONNX models, validating their structure, inspecting tensor metadata, reporting evidence-based target compatibility, and tracking deployment preparation jobs.

## MVP scope

The current scaffold provides a dashboard, bounded local ONNX upload, REST list/health endpoints, an ONNX inspection library/CLI, and a Java process adapter for inspection. It does **not** compile models for vendor hardware, deploy to physical devices, or benchmark inference. No compatibility is inferred solely from successful parsing.

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

Install the model worker first so uploads can be inspected. Then run the API and UI.

Model worker:

```powershell
cd model-worker
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[test]"
python -m onnx_worker.cli --help
```

Backend (from a new terminal; PostgreSQL is not required for local uploads):

```powershell
cd backend
$env:APP_INSPECTOR_EXECUTABLE = "..\model-worker\.venv\Scripts\python.exe"
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
npm ci
npm run dev
```

Open http://localhost:5173 and use **Upload a model**. Files are stored under `backend/uploads/models/` when the API is started from `backend/`. The Vite dev server proxies `/api` to port 8080.

If `python` is already on PATH and the worker is installed into that interpreter, you can omit `APP_INSPECTOR_EXECUTABLE`. Override storage with `APP_STORAGE_DIRECTORY` and the 100 MiB limit with `APP_STORAGE_MAX_BYTES`.

The worker is a Python library/CLI, not an HTTP service. Backend endpoints include `GET /health`, `GET|POST /api/v1/models`, `/api/v1/hardware`, `/api/v1/compatibility-reports`, and `/api/v1/deployments`. Hardware, compatibility, and deployment lists currently return empty arrays; compatibility evaluation is not implemented.

## Checks

```powershell
cd backend; .\mvnw.cmd test
cd ..\model-worker; python -m pytest
cd ..\frontend; npm ci; npm test -- --run; npm run build
```

CI uses `backend/mvnw test`. The npm lockfile and Maven Wrapper scripts are committed for reproducible installs.

## Status and next steps

Implemented: health API, local ONNX upload to disk, model list/metadata API, dashboard upload UI, Python ONNX validation and tensor metadata extraction, Java CLI process adapter, docs and component tests. Not yet implemented: PostgreSQL metadata persistence, hardware registry, compatibility evaluation, deployment job mutations/packages, real hardware support.

Recommended next: persist model records in PostgreSQL and add evidence-based hardware compatibility reports.
