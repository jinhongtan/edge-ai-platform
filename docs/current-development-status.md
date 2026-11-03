# Current Development Status

Last updated: 2026-10-22

## Implemented

### Model upload and analysis

- React UI uploads `.onnx` files to `POST /api/v1/models`.
- The Spring backend enforces the configured file-size limit, stores accepted uploads outside web roots, and computes a SHA-256 digest.
- The Python worker validates ONNX structure without executing the model.
- The worker reports file size, ONNX IR version, opsets, inputs, outputs, tensor types/shapes, operators, operator counts, and dense-initializer parameter count.
- Accepted uploads are retained even when validation fails. Their metadata records `VALID` or `INVALID` and a safe validation message.
- Local model metadata is persisted as `metadata.json` beside each uploaded model.

### Hardware compatibility V1

- The backend exposes one configurable, initial target: **NVIDIA Jetson Orin Nano Super Developer Kit**.
- Hardware profile APIs:
  - `GET /api/v1/hardware`
  - `GET /api/v1/hardware/{hardwareId}`
- Compatibility APIs:
  - `POST /api/v1/models/{modelId}/compatibility-check`
  - `GET /api/v1/models/{modelId}/compatibility`
- The synchronous compatibility check consumes the stored model analysis; it does not re-parse the ONNX file.
- Deterministic rules check the initial profile's operator allow-list, input/output precision, dynamic input shapes, and configured default-domain opset range.
- Results are `COMPATIBLE`, `WARNING`, or `INCOMPATIBLE`, using blocking-findings precedence over warnings.
- The latest result is persisted as `compatibility.json` beside its model.
- The React UI lets the user run the Jetson check and displays the target, operator coverage, precision support, shape and opset details, blocking findings, and warnings.

### Build request foundation

- `POST /api/v1/builds` creates a locally persisted `QUEUED` request for a valid, static-shape ONNX model whose latest Jetson compatibility result is `COMPATIBLE` or `WARNING`. Only FP32 and a bounded workspace are accepted.
- `GET /api/v1/builds/{buildId}` and `GET /api/v1/models/{modelId}/builds` expose these requests. The UI can create and display a queued FP32 build request after compatibility checking.
- The UI lets the user set the bounded workspace, explains compatibility-warning risk, submits an asynchronous request, and polls the build endpoint for stage/status changes.
- The worker package contains a tested TensorRT `trtexec` provider. It uses argument-safe subprocess invocation, an isolated per-build directory, and rejects missing, empty, or escaped artifacts.
- A disabled-by-default local dispatcher claims one job at a time and runs the TensorRT CLI on the same Jetson. When the configured Python environment has `onnx_worker` installed and `trtexec` is on `PATH`, it copies the ONNX input to the build directory, writes `model.engine` and `build.log` locally, then records `COMPLETED` or `FAILED`.
- A completed job means TensorRT produced a non-empty local engine. It does **not** prove inference executes correctly or meets a performance target.

### Edge agent foundation

- A disabled-by-default, file-backed local development adapter issues single-use enrollment tokens, enrolls device identities with hashed credentials, supports explicit approval, and accepts heartbeats from approved devices.
- The Python package now exposes `edge-agent doctor`, `edge-agent enroll --token-stdin`, and `edge-agent version`. Its hardware probe reports only detected TensorRT/CUDA capability facts and handles missing Jetson tooling.
- A PostgreSQL schema artifact for devices, jobs, events, and artifacts exists for the future persistence migration but is not active while datasource auto-configuration is disabled.

## Current storage and runtime model

- Model and compatibility metadata use local filesystem persistence under `backend/uploads/models/` by default.
- Build records use local JSON files under `backend/uploads/builds/`; each build's isolated directory contains its copied input and, when successful, its engine and log.
- The worker is a local Python CLI process, not an HTTP service.
- The project includes PostgreSQL dependencies/configuration, but JPA and datasource auto-configuration are currently disabled; no database-backed model, profile, or compatibility persistence is implemented.
- There is no S3 integration in the current codebase.
- The build-service direction is local filesystem storage and locally managed Jetson workers; cloud storage/queue configuration is deferred.

## Not implemented

- Jetson validation in this repository/environment. A real engine build requires a Jetson with JetPack/TensorRT and an enabled local dispatcher; this Windows development environment cannot produce a Jetson TensorRT engine.
- Engine download API, artifact retention policy, build cancellation, retry policy, or worker concurrency beyond one local build at a time.
- TensorRT inference validation or performance benchmarking.
- Benchmarking, deployment packages, or device deployment.
- Hardware compatibility beyond the initial Jetson profile.
- Authentication, authorization, model ownership, and multi-tenant access control.
- Asynchronous compatibility jobs.
- Database migrations/repositories for models, hardware profiles, or compatibility results.
- External ONNX-data artifact handling for builds.
- Edge job claim/lease APIs, agent run loop, artifact transfers, TensorRT runtime validation, GPU benchmarking, and frontend device/job workflows.

## Validation notes

- Worker tests and frontend TypeScript checks have been run successfully during the current development work.
- Backend Maven tests should be run from a terminal configured with Java 21: `cd backend; .\mvnw.cmd clean test`.
- The backend test suite compiles the local queue and dispatcher source, and the worker tests cover TensorRT command construction; TensorRT itself has not been exercised here because no Jetson/TensorRT installation is available.
- Local queue validation requires a real Jetson: install JetPack/TensorRT, make `trtexec` available on `PATH`, install `onnx_worker` in the configured Python environment, set `APP_BUILD_ENABLED=true`, and start the backend on that device.
- A `COMPATIBLE` result is static profile analysis only. It must not be represented as proof that TensorRT can build the model or that it executes correctly on Jetson hardware.
