# Build Service V1 — Implementation Specification for AI Coding Agent

## Mission

> **Current implementation decision — local only:** Build Service V1 must run without AWS services. Store models, queued build records, engines, logs, and manifests on local controlled filesystem paths; run the TensorRT worker locally on the Jetson or on a directly managed local network. Do not add S3, SQS, ECS, cloud credentials, or presigned URLs in this phase. AWS object storage and queue adapters may be added later behind the same artifact-store and dispatch boundaries, with configuration documented at that time.

Implement an end-to-end **Build Service V1** in the existing Edge AI Model Deployment Platform. The application already has React + TypeScript, Spring Boot, PostgreSQL, Amazon S3, a Python model worker, ONNX model analysis, and a hardware compatibility-check workflow. Reuse these components and repository conventions. Do not replace the existing architecture.

The MVP target is **NVIDIA Jetson Orin Nano Super**, using TensorRT to compile an uploaded ONNX model into a `.engine` artifact. Spring Boot runs in AWS ECS Fargate; the build worker runs on a Jetson device with a compatible JetPack/TensorRT installation. Use **PostgreSQL**, not MySQL.

**Important distinction:** A successful TensorRT build is **not** proof of successful runtime inference or benchmark performance. Display `BUILD_SUCCEEDED` separately from `RUNTIME_VERIFIED`. TensorRT engine portability is constrained by target GPU, TensorRT version, plugins, and build options. Do not advertise universal portability.

## Agent operating instructions

1. Read `AGENTS.md` and existing project docs, if present. Inspect actual repository structure before writing code.
2. Inspect existing model entities, analysis JSON schema, compatibility result schema, auth/ownership checks, database migrations, S3 integration, API conventions, job queue, worker protocol, tests, and frontend UI components.
3. Follow established patterns and reuse existing services; do not create duplicate abstractions or a second authentication system.
4. Implement incrementally, with tests. Avoid unrelated refactors.
5. If a required integration is missing (e.g., Jetson is not online), implement a testable adapter and document the setup; do not fake a successful build or hardcode example artifacts.
6. Do not commit credentials, publish sensitive model contents to logs, or open the Jetson to public inbound access.
7. Do not automatically merge a PR or deploy to production. Present changes, tests, configuration, and manual validation steps.

## Scope

### In scope
- Build request and configuration validation
- PostgreSQL persistence and migration
- Asynchronous job creation, dispatch, retries, and status updates
- Worker-to-backend authenticated communication or an existing secure messaging mechanism
- S3 download of original ONNX and upload of engine, logs, and manifest
- TensorRT build provider on Jetson, initially **FP32 with static input shapes**
- Progress, status, errors, and download endpoints
- React Build panel/page integrated with the existing Model Detail/Compatibility UI
- Unit, integration, and mocked worker tests
- Runtime/toolchain metadata in the build manifest

### Out of scope
- TensorRT inference validation or real performance benchmarking
- FP16 and INT8 build support (design extension points only)
- Quantization/calibration datasets
- Automatic ONNX graph rewriting or optimization recommendations
- Supporting Hailo, Qualcomm, Rockchip, or other hardware
- User-submitted custom TensorRT plugins or arbitrary shell commands
- Billing, multi-region orchestration, Kubernetes, and autoscaling

## Architecture

```text
React Build UI
    |
    | POST /api/builds
    v
Spring Boot BuildController / BuildService
    |-- authorize model ownership
    |-- check analysis and compatibility readiness
    |-- persist build in PostgreSQL
    |-- enqueue build ID via existing queue, or Amazon SQS
    v
Build queue
    |
    v
Jetson Python Build Worker (outbound-only connectivity)
    |-- acquire/claim job safely
    |-- download ONNX from private S3
    |-- run TensorRT trtexec with validated arguments
    |-- capture logs, exit code, and toolchain metadata
    |-- upload engine, log, manifest to private S3
    |-- report terminal status
    v
Spring Boot / PostgreSQL
    |
    v
React polls build status and renders logs/download
```

Use a **pull worker** on the Jetson. Prefer the application's existing reliable queue; if none exists, use **Amazon SQS with a dead-letter queue**. SQS delivery is at-least-once: implement idempotent claiming and terminal updates. Keep AWS credentials on the Jetson in a dedicated least-privilege identity or short-lived credentials. Do not expose an unauthenticated worker callback endpoint.

## API contract

Use existing API naming/auth conventions if they differ; otherwise implement:

### Create build
`POST /api/builds` — returns `202 Accepted`

Request:
```json
{
  "modelId": "<existing-model-id>",
  "hardwareId": "jetson-orin-nano-super",
  "precision": "FP32",
  "options": { "workspaceMiB": 1024 }
}
```

Response:
```json
{
  "buildId": "<uuid>",
  "status": "QUEUED",
  "createdAt": "<ISO-8601 timestamp>"
}
```

Validation:
- Authenticated user must own or have access to the model.
- Model must exist, have completed ONNX analysis, and reference a valid private S3 object.
- Hardware profile must exist and be enabled.
- Compatibility result must exist and have no known blocking incompatibility; static `COMPATIBLE` or `WARNING` is **not** a build-success guarantee. Define and document policy for `WARNING` (allow with user-visible caution).
- V1 accepts only `FP32`; reject FP16/INT8 with an actionable validation error.
- V1 requires known static input shapes; reject dynamic inputs with a clear unsupported-for-V1 error (not a claim that the model is incompatible with TensorRT in general).
- Bound workspace size and other user-controlled inputs.
- Use consistent HTTP status codes (`400` invalid request, `401/403` unauthorized, `404` missing, `409` invalid state) and existing error schema.

### Read builds
- `GET /api/builds/{buildId}` — status, progress stage, timestamps, target, precision, summary, error details safe for the user.
- `GET /api/models/{modelId}/builds` — paginated build history, authorized.
- `GET /api/builds/{buildId}/logs` — bounded/paginated or presigned download; never return unlimited log data.
- `GET /api/builds/{buildId}/download` — only after successful build; return short-lived presigned S3 download URL or redirect according to existing API conventions.
- Optional `POST /api/builds/{buildId}/cancel` if existing jobs support cancellation; otherwise document as future work.

### Worker protocol
Reuse existing secure worker APIs if present. Otherwise implement authenticated job-claim/status/report operations or use SQS for dispatch plus signed worker callbacks. Verify worker identity, authorize it for the job and target, validate allowed state transitions, and make repeated updates idempotent. Never accept arbitrary artifact S3 keys from an untrusted caller without validation.

## PostgreSQL schema

Prefer existing Flyway/Liquibase conventions and the project's existing ID type. The following is a **reference schema**, not a demand to rename existing tables:

```sql
CREATE TABLE build_jobs (
    id UUID PRIMARY KEY,
    model_id UUID NOT NULL REFERENCES models(id),
    hardware_profile_id VARCHAR(100) NOT NULL,
    requested_by UUID,
    precision VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    stage VARCHAR(32),
    configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
    artifact_s3_key TEXT,
    log_s3_key TEXT,
    manifest_s3_key TEXT,
    worker_id VARCHAR(128),
    toolchain_version VARCHAR(128),
    error_code VARCHAR(80),
    error_message TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT build_jobs_attempt_count_nonnegative CHECK (attempt_count >= 0)
);
CREATE INDEX idx_build_jobs_model_created ON build_jobs(model_id, created_at DESC);
CREATE INDEX idx_build_jobs_status_created ON build_jobs(status, created_at);
```

Adapt FK types and table names to the real repository. Add an FK for `hardware_profile_id` and `requested_by` when their actual tables/types are known. Do not create an invalid migration just to match this sample. Use JSONB for configuration and structured diagnostic details as appropriate. Prefer a stable job ID and immutable build input snapshot (model S3 key/version or checksum, profile ID/version, precision, configuration) to support reproducibility.

Status enum:

```text
QUEUED -> DOWNLOADING -> BUILDING -> UPLOADING -> COMPLETED
                         \-> FAILED
QUEUED/RUNNING -> CANCELLED (if supported)
```

`FAILED` may be retried according to policy by incrementing `attempt_count` and moving through a controlled retry transition. Do not allow a late worker callback to overwrite a terminal success/failure. Use optimistic locking or atomic conditional updates to enforce transitions.

## Queue and reliability

- Prefer transactionally reliable dispatch. If writing PostgreSQL and publishing to SQS separately, implement an outbox or equivalent recovery/reconciliation so a committed job cannot be permanently stranded by a queue publish failure.
- Messages should contain only opaque identifiers (e.g., build ID), not model bytes or secrets.
- SQS visibility timeout must cover processing or be extended via heartbeat. Configure DLQ and bounded retries.
- Acquire each job atomically; duplicate messages must not trigger concurrent builds for the same attempt.
- Worker crash/timeout should leave a recoverable job, not a permanently stuck `BUILDING` record.
- Bound concurrency on Jetson (start with one build at a time).
- Implement structured logging and correlation by build ID.

## Python Build Worker

Suggested structure, adapted to the existing repository:

```text
model-worker/
  build/
    provider.py
    tensorrt_provider.py
    worker.py
    artifact_store.py
    job_client.py
    manifest.py
  tests/
    test_tensorrt_provider.py
    test_build_worker.py
```

Build-provider contract:

```python
from abc import ABC, abstractmethod
from dataclasses import dataclass
from pathlib import Path

@dataclass(frozen=True)
class BuildRequest:
    model_path: Path
    output_path: Path
    precision: str = "FP32"
    workspace_mib: int = 1024

class BuildProvider(ABC):
    @abstractmethod
    def build(self, request: BuildRequest) -> Path:
        ...
```

### TensorRT provider requirements

- Discover the installed `trtexec` executable and detect TensorRT version on the **Jetson**; validate CLI flags against the installed version. Do not assume a universal TensorRT CLI version.
- For TensorRT 10 where supported, a baseline invocation resembles:

```bash
trtexec --onnx=/work/model.onnx \
  --saveEngine=/work/model.engine \
  --memPoolSize=workspace:1024
```

- Do not include `--fp16` or `--int8` in V1. Treat FP32 as the requested build configuration; do not promise every internal layer executes in FP32 without verifying the runtime's actual precision semantics.
- Use `subprocess.run([...], shell=False, timeout=...)` or `Popen` for streamed logs; never interpolate client input into a shell command.
- Run in a per-job isolated working directory. Restrict paths to this directory and validate input and output paths.
- Preserve ONNX external-data files and relative references when present; download the full model artifact set, not only a small `.onnx` descriptor.
- Bound ONNX file size, extracted artifact size, disk use, build duration, and concurrency.
- Record exit code, stdout/stderr, and a bounded user-safe error summary. Save complete logs privately with retention controls.
- Verify that the engine file exists, is non-empty, and has a SHA-256 checksum before marking the build completed.
- Do not load untrusted custom plugins or execute arbitrary user code.
- Build failures must be surfaced honestly; never create placeholder `.engine` files.

### Worker sequence

```text
1. Receive job ID and claim attempt.
2. Fetch build metadata and short-lived S3 download permission.
3. Create per-build work directory.
4. Download ONNX and any external weights.
5. Verify input integrity/checksum.
6. Update status to DOWNLOADING/BUILDING.
7. Invoke TensorRTBuildProvider.
8. Capture logs and build environment metadata.
9. Upload engine, logs, manifest to private S3.
10. Verify artifact metadata, report COMPLETED atomically.
11. Acknowledge queue message.
12. On failure: report FAILED/retryable state, clean up, and apply retry policy.
```

## S3 artifact layout

Use the existing bucket/configuration. Recommended key pattern:

```text
models/{modelId}/...                   # existing model storage
builds/{buildId}/model.engine
builds/{buildId}/build.log
builds/{buildId}/manifest.json
```

Do not invent a new public bucket. Enable server-side encryption and block public access. Use short-lived presigned URLs, verify object ownership, and scope worker access to required prefixes.

Example manifest (placeholder fields must be populated from actual environment):

```json
{
  "schemaVersion": 1,
  "buildId": "<uuid>",
  "modelId": "<id>",
  "hardwareId": "jetson-orin-nano-super",
  "precisionRequested": "FP32",
  "builder": "TensorRT",
  "tensorRtVersion": "<detected>",
  "jetPackVersion": "<detected>",
  "deviceModel": "<detected>",
  "modelSha256": "<sha256>",
  "engineSha256": "<sha256>",
  "buildStatus": "COMPLETED",
  "runtimeVerified": false,
  "benchmarkVerified": false
}
```

## React implementation

Integrate into the existing model detail page, preferably after compatibility checking:

- Show selected model and supported target hardware.
- Show `FP32` as the only enabled precision; label FP16/INT8 as future features or omit them.
- Show build configuration with validated workspace limit.
- Disable Build while analysis is incomplete, a blocking compatibility result exists, or a request is being submitted.
- Display a caution when static compatibility status is `WARNING`.
- On submit, show build ID and stage, poll `GET /api/builds/{id}` with cleanup/backoff, and render terminal status.
- Show safe error summary, bounded logs, toolchain version, and engine download when completed.
- Clearly label **Build successful — runtime not yet verified**.
- Handle unauthorized access, network failures, stale jobs, and empty states.
- Reuse existing components, hooks, API client, design system, and tests.

## Test plan

### Spring Boot / PostgreSQL
- Create build with authorized model and ready analysis -> `202` and persisted `QUEUED` job.
- Missing model/hardware -> `404`.
- Unauthorized model access -> existing authorization response.
- Missing/incomplete analysis -> `409`.
- Blocking compatibility -> reject with clear reason.
- Compatibility `WARNING` -> apply documented allow-with-warning policy.
- Unsupported precision/dynamic shape -> validation failure.
- List/read/download only user's authorized builds.
- Valid and invalid state transitions; duplicate callbacks; late callbacks after terminal state.
- Queue publishing failure recovery/outbox if implemented.
- Migrations run cleanly on PostgreSQL (prefer Testcontainers if already used).

### Python worker
- Mock `subprocess` and S3 to test command construction, output validation, errors, timeout, and cleanup.
- Test missing `trtexec`, non-zero exit, empty engine, missing external weights, S3 upload failure, and retries.
- Ensure user-supplied paths/options cannot inject shell commands.
- Test duplicate queue deliveries and crash recovery behavior.

### React
- Build disabled when prerequisites are unmet.
- Submission displays queued job.
- Status progression, success/download, warning, failure/logs, and authorization errors.
- Polling stops on unmount and terminal state.

### Manual end-to-end acceptance
On a real Jetson with TensorRT installed:

1. Upload a known-good ONNX model using existing flow.
2. Wait for analysis and run compatibility check.
3. Click Build with FP32.
4. Verify the job moves through statuses without blocking the HTTP request.
5. Confirm a genuine `.engine` file and manifest are uploaded to private S3.
6. Confirm download works for the owner but not another user.
7. Verify logs and recorded TensorRT/JetPack versions.
8. Intentionally build an unsupported ONNX model and confirm a truthful FAILED result.
9. Confirm UI says runtime validation is **not yet performed**.

## Configuration and deployment

- Add environment-driven settings for queue URL, bucket/prefix, worker API URL, job timeout, max model size, workspace limit, log size, concurrency, and credentials.
- Reuse existing AWS region and deployment patterns. Backend may remain on ECS Fargate; do **not** attempt to run Jetson TensorRT compilation inside an ordinary CPU-only Fargate task.
- Document Jetson setup: Python version, JetPack/TensorRT, `trtexec` path, outbound network, queue/S3 permissions, service management (e.g., systemd), and log location.
- Keep production secrets in existing secret management; never commit them.
- If hardware is unavailable in the development environment, provide a mock worker for automated tests, explicitly distinct from real builds.

## Deliverables

1. PostgreSQL migration(s) and backend entities/repositories/services/controllers.
2. Secure async job dispatch and worker protocol integration.
3. Python TensorRT BuildProvider and worker orchestration.
4. Private S3 artifact upload/download integration.
5. React Build UI with progress, errors, logs, and download.
6. Automated tests and documented manual Jetson validation.
7. README/setup changes describing architecture, configuration, and how to run locally and on Jetson.
8. A concise final implementation report: files changed, migrations, tests executed/results, manual steps still needed, and known limitations.

## Definition of Done

- [ ] Existing upload, analysis, and compatibility flows still work.
- [ ] PostgreSQL stores real build jobs and reproducible configuration.
- [ ] `POST /api/builds` creates a nonblocking asynchronous job.
- [ ] A Jetson worker can claim and compile a supported static-shape ONNX model.
- [ ] A real nonempty TensorRT engine is stored privately in S3.
- [ ] Build logs and environment manifest are stored and retrievable securely.
- [ ] UI shows accurate statuses, failures, and engine download.
- [ ] Duplicate jobs/messages and late callbacks do not corrupt state.
- [ ] Authorization is enforced on all model/build/artifact access.
- [ ] Automated tests pass and PostgreSQL migrations are validated.
- [ ] Real Jetson validation is either completed and documented or explicitly marked pending.
- [ ] No fake benchmark or runtime-verification claims appear.

**Implementation priority:** Deliver a narrow, reliable ONNX → Jetson TensorRT FP32 engine build first. Keep provider interfaces extensible, but do not implement additional vendors or precisions in this PR.
