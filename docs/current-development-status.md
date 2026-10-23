# Current Development Status

Last updated: 2026-10-21

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
- The UI lets the user set the bounded workspace, explains warning-result risk, and polls the build endpoint for stage/status changes. While local worker dispatch is absent, a request truthfully remains `QUEUED`.
- The worker package contains a tested TensorRT `trtexec` provider. It uses argument-safe subprocess invocation and rejects missing, empty, or escaped artifacts.
- A disabled-by-default local dispatcher can claim one request at a time and run the TensorRT CLI on the same Jetson. Enable it only when the configured Python environment has this worker package and `trtexec`. It writes `model.engine` and `build.log` locally, then reports `COMPLETED` or `FAILED`; completion still does not claim runtime inference validation. AWS S3/queue integration is a future optional deployment adapter, not a current dependency.

## Current storage and runtime model

- Model and compatibility metadata use local filesystem persistence under `backend/uploads/models/` by default.
- The worker is a local Python CLI process, not an HTTP service.
- The project includes PostgreSQL dependencies/configuration, but JPA and datasource auto-configuration are currently disabled; no database-backed model, profile, or compatibility persistence is implemented.
- There is no S3 integration in the current codebase.
- The build-service direction is local filesystem storage and locally managed Jetson workers; cloud storage/queue configuration is deferred.

## Not implemented

- End-to-end TensorRT engine building or vendor compilation (the isolated provider exists, but dispatch and artifact handling are not integrated).
- Execution on Jetson or any physical device.
- Benchmarking, deployment packages, or device deployment.
- Hardware compatibility beyond the initial Jetson profile.
- Authentication, authorization, model ownership, and multi-tenant access control.
- Asynchronous compatibility jobs.
- Database migrations/repositories for models, hardware profiles, or compatibility results.

## Validation notes

- Worker tests and frontend TypeScript checks have been run successfully during the current development work.
- Backend Maven tests should be run from a terminal configured with Java 21: `cd backend; .\mvnw.cmd clean test`.
- A `COMPATIBLE` result is static profile analysis only. It must not be represented as proof that TensorRT can build the model or that it executes correctly on Jetson hardware.
