# Edge Agent V1 — Executable Implementation Specification

**Project:** Edge AI Model Deployment Platform  
**Primary target:** NVIDIA Jetson Orin Nano Super (Linux aarch64)  
**Control plane:** React + Spring Boot (Java 21) + PostgreSQL + AWS S3 + AWS SQS  
**Execution plane:** Python 3.12 Edge Agent on customer-owned or platform-owned Jetson  
**Status:** Implementation-ready specification for an AI coding agent  
**Scope:** Single-vendor NVIDIA TensorRT MVP; multi-vendor extension points only

## 0. Mission and definition of done

Build a reusable **Edge Agent** that can run on either the platform operator's Jetson or a customer's Jetson. The agent registers its hardware/software capabilities with the Spring Boot control plane, securely polls for assigned jobs, downloads ONNX model assets, performs TensorRT build, runtime validation, and GPU benchmarking, uploads artifacts and reports to S3, and reports reliable job state transitions. The agent must require **no inbound connection** to the customer's device.

**End-to-end acceptance:** A user uploads an ONNX model in the React app, selects a registered Jetson device, submits a job, sees accurate progress and errors, and downloads an engine, validation report, benchmark report, build log, and manifest. The same agent package must run on an operator-owned and a customer-owned Jetson with different device credentials.

**Do not claim hardware validation without actual inference on a compatible physical target. Do not claim a benchmark when only static analysis or cloud compilation occurred.**

## 1. Existing project context — inspect, do not overwrite

Repository: `edge-ai-platform/` with `frontend/` (React), `backend/` (Spring Boot), `model-worker/` (Python), `docs/`. Existing ONNX analysis flow: React -> Spring Boot -> S3 -> Python analysis worker -> JSON -> UI. Existing AWS deployment: backend on ECS Fargate, PostgreSQL planned/used, S3 and AWS region `us-west-2`. Preserve existing endpoints, schemas, authentication, CI/CD, and naming conventions wherever possible.

Known physical-device proof of concept:

- Device: NVIDIA Jetson Orin Nano Engineering Reference Developer Kit Super; GPU compute capability 8.7.
- Python 3.12; TensorRT Python 10.16.2.10; CUDA toolkit 13.2; `cuda.bindings.runtime` works; NumPy 1.26.4; OpenCV 4.8.0.
- `defect_detector.onnx` may use `defect_detector.onnx.data` external weights. Input `[1,3,224,224]`, output `[1,2]`.
- `trtexec --onnx=... --saveEngine=...` succeeds; Python TensorRT inference succeeds.
- Benchmark proof of concept: 20 warmups, 100 timed runs; mean GPU-only latency 2.8556 ms, P50 2.8579 ms, P95 2.8705 ms, estimated 350.19 FPS. These are **one test run**, not performance guarantees.
- **Known warning:** TensorRT emits `Using an engine plan file across different models of devices is not supported...` when loading the existing engine. Diagnose and document before claiming engine portability or broad compatibility. Rebuild on the same Jetson with a new filename and inspect the warning. Do not silently suppress.

Before editing, inspect the repo and identify existing models, job tables, S3 conventions, auth, and any worker scripts. Reuse working implementations. Never commit credentials or hardcode account IDs.

## 2. Product capabilities and trust levels

| Capability | Requires physical target? | Output | Truthful status |
|---|---|---|---|
| ONNX static analysis | No | Model metadata | `ANALYZED` |
| Rule-based compatibility assessment | No | Compatibility report with caveats | `COMPATIBILITY_ASSESSED` |
| TensorRT build | Usually build on matching GPU/environment for this MVP | `.engine`, build log, manifest | `BUILT` |
| Runtime validation | Yes, for hardware-verified claim | Validation JSON, optional output comparison | `HARDWARE_VERIFIED` only if checks pass |
| GPU benchmark | Yes, for measured target performance | Benchmark JSON | `BENCHMARKED` |

Separate **job lifecycle** (`QUEUED`, `ASSIGNED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `TIMED_OUT`) from **artifact assurance level** (`BUILT`, `HARDWARE_VERIFIED`, `BENCHMARKED`). Never use one field for both.

## 3. Architecture and security boundaries

```text
React UI
  | REST + existing user authentication
Spring Boot Control Plane (ECS)
  |-- PostgreSQL: devices, jobs, attempts, events, artifacts
  |-- S3: models, logs, engine, reports
  |-- SQS: job notifications (internal control plane)
  |
  | HTTPS outbound-only agent API (mTLS or per-device scoped credentials)
  v
Jetson Edge Agent (operator-owned OR customer-owned)
  |-- capability probe / heartbeat
  |-- job polling + claim/lease/renew
  |-- S3 pre-signed URL download/upload
  |-- TensorRT build provider (trtexec)
  |-- inference validation provider
  |-- CUDA-event benchmark provider
  `-- manifest + logs + result upload
```

**Important design choice:** Customer-owned agents must **not** receive general AWS SQS credentials or cross-tenant S3 credentials. SQS is for internal backend scheduling/notifications. The agent calls a tenant-scoped Spring Boot API over HTTPS to obtain authorized jobs and short-lived, scoped pre-signed S3 URLs. For MVP, backend can assign jobs in PostgreSQL and agents long-poll `GET /api/agent/v1/jobs/next`; SQS may wake an internal scheduler but is not required for device access. This prevents a shared queue from exposing another customer's work.

Agent initiates every connection; no inbound port forwarding, SSH, or public device IP is required. All artifacts and logs are tenant-isolated. Device enrollment must be explicitly approved by a tenant admin.

## 4. Repository layout to implement

Adapt to existing conventions; suggested structure:

```text
edge-ai-platform/
  backend/
    src/main/java/.../agent/
      AgentController.java
      AgentEnrollmentService.java
      DeviceService.java
      JobDispatchService.java
      JobLeaseService.java
      AgentAuthFilter.java
      dto/
    src/main/java/.../build/
      BuildJobController.java
      BuildJobService.java
      dto/
    src/main/resources/db/migration/
      Vxxx__edge_agents_and_jobs.sql
  model-worker/
    edge-agent/
      pyproject.toml
      README.md
      config.example.yaml
      edge_agent/
        __init__.py
        main.py
        config.py
        api_client.py
        hardware_probe.py
        runner.py
        sandbox.py
        artifact_io.py
        manifest.py
        providers/
          __init__.py
          base.py
          tensorrt_build.py
          tensorrt_validate.py
          tensorrt_benchmark.py
      tests/
        test_api_client.py
        test_runner.py
        test_tensorrt_build.py
        test_manifest.py
      deploy/
        edge-agent.service
  frontend/src/  # integrate into existing routes/components
  docs/
    edge-agent-architecture.md
    edge-agent-operations.md
    edge-agent-security.md
    edge-agent-api.md
```

Do not rename or move the existing working `model-worker` unless needed. Provide backward-compatible wrappers if integrating existing `inference.py` and `benchmark.py`.

## 5. Data model and migration

Use Flyway if already present; otherwise follow the project's migration tool. Use UUID primary keys, `timestamptz`, optimistic versioning, foreign keys, and tenant-scoped queries.

### `edge_devices`

- `id uuid pk`, `tenant_id uuid not null`, `display_name text not null`
- `hardware_vendor text`, `hardware_model text`, `gpu_name text`, `compute_capability text`
- `os_arch text`, `jetpack_version text null`, `cuda_version text`, `tensorrt_version text`
- `capabilities jsonb not null`, `status text not null` (`PENDING_APPROVAL`, `ONLINE`, `OFFLINE`, `DISABLED`)
- `credential_hash text null` (if token-based enrollment), `credential_version integer not null default 1`
- `last_heartbeat_at timestamptz`, `created_at`, `updated_at`

### `edge_jobs`

- `id uuid pk`, `tenant_id uuid not null`, `model_id uuid not null`, `device_id uuid null references edge_devices(id)`
- `job_type text not null` (`BUILD`, `VALIDATE`, `BENCHMARK`, `PIPELINE`)
- `status text not null`, `assurance_level text null`
- `requested_precision text not null` (`FP32` initially), `requested_config jsonb not null`
- `attempt integer not null default 0`, `max_attempts integer not null default 2`
- `lease_token_hash text null`, `lease_expires_at timestamptz null`, `version bigint not null default 0`
- `error_code text null`, `error_message text null`, `created_at`, `started_at`, `finished_at`, `updated_at`
- `idempotency_key text null`; unique `(tenant_id, idempotency_key)` when non-null

### `edge_job_events`

- `id uuid pk`, `job_id uuid fk`, `device_id uuid null`, `event_type text`, `message text`, `metadata jsonb`, `created_at`

### `edge_artifacts`

- `id uuid pk`, `tenant_id uuid`, `job_id uuid fk`, `kind text` (`ENGINE`, `BUILD_LOG`, `VALIDATION_REPORT`, `BENCHMARK_REPORT`, `MANIFEST`)
- `s3_bucket text`, `s3_key text`, `sha256 text`, `size_bytes bigint`, `created_at`

**Concurrency:** Claim a job with a transactional conditional update or `SELECT ... FOR UPDATE SKIP LOCKED`, binding it to a specific `device_id`. Reject updates from stale leases. A late `RUNNING`/`SUCCEEDED` callback must never overwrite a terminal `FAILED`/`CANCELLED` or a newer attempt. Store event history.

## 6. API contract

All paths are examples; adapt prefix to existing API conventions. Return JSON with stable error codes. Backend enforces tenant authorization on user APIs and device identity on agent APIs.

### User-facing APIs

**`POST /api/v1/edge-devices/enrollment-tokens`** (tenant admin): issue short-lived, single-use enrollment token. Return token once. Store only its hash and expiry.

**`GET /api/v1/edge-devices`**: list current tenant's devices, capabilities, status, heartbeat freshness.

**`POST /api/v1/edge-jobs`**: create job.

Example:

```json
{
  "modelId": "<existing-model-uuid>",
  "deviceId": "<approved-device-uuid>",
  "jobType": "PIPELINE",
  "precision": "FP32",
  "options": {
    "warmupIterations": 20,
    "measuredIterations": 100,
    "validateWithReferenceOutputs": false
  },
  "idempotencyKey": "<client-generated-uuid>"
}
```

Response `202 Accepted`: `jobId`, `status=QUEUED`, links to job status. Reject unsupported device/precision combinations before queueing.

**`GET /api/v1/edge-jobs/{jobId}`**: current state, stage (`BUILD`, `VALIDATE`, `BENCHMARK`), progress, device, warnings, artifacts, error code.

**`GET /api/v1/edge-jobs/{jobId}/events`**: ordered events.

**`GET /api/v1/edge-jobs/{jobId}/artifacts/{artifactId}/download`**: authorized short-lived download URL; never expose raw S3 bucket access.

**`POST /api/v1/edge-jobs/{jobId}/cancel`**: mark cancellation requested and propagate on next agent poll/heartbeat.

### Agent-facing APIs

**`POST /api/agent/v1/enroll`**: exchange short-lived enrollment token for device identity and scoped credential; approval may be required before job claims. Credential must be stored root-readable only (`0600`), rotated/revocable. Prefer mTLS or short-lived signed device tokens for production.

**`POST /api/agent/v1/heartbeat`**: device identity inferred from credential; report hardware, SDK versions, available disk, busy/idle, and agent version. Backend updates online status.

**`POST /api/agent/v1/jobs/claim`**: request one eligible job for authenticated device; backend atomically leases one job and returns `jobId`, `attempt`, opaque `leaseToken`, expiry, input asset descriptors with short-lived pre-signed GET URLs, and constrained job config. If none, return `204 No Content`. Do not allow agent to choose arbitrary tenant IDs.

**`POST /api/agent/v1/jobs/{jobId}/lease/renew`**: renew only for matching device, attempt, and lease token.

**`POST /api/agent/v1/jobs/{jobId}/events`**: stage/progress events, bounded and rate-limited.

**`POST /api/agent/v1/jobs/{jobId}/artifacts/prepare`**: request short-lived pre-signed PUT URLs for expected artifacts with server-generated tenant-scoped S3 keys and size/type constraints. Server must validate job ownership and lease.

**`POST /api/agent/v1/jobs/{jobId}/complete`**: submit manifest, artifact hashes, stage outcomes, warnings, and success/failure. Backend verifies uploaded objects/metadata and atomically finalizes only the active lease attempt.

**`GET /api/agent/v1/jobs/{jobId}/control`**: cancellation/lease status.

**Authentication and errors:** `401` invalid credential, `403` disabled/unapproved device, `404` inaccessible job, `409` stale attempt/lease or illegal transition, `422` unsupported model/config, `429` rate limited, `503` temporary control-plane issue. Use bounded exponential backoff with jitter.

## 7. Job lifecycle, retries, and failure semantics

State flow:

```text
QUEUED -> ASSIGNED -> RUNNING -> SUCCEEDED
                          |-> FAILED
                          |-> CANCELLED
                          |-> TIMED_OUT -> QUEUED (new attempt, if retryable)
```

Pipeline stages are separate: `BUILD -> VALIDATE -> BENCHMARK -> PACKAGE`. Each stage records `STARTED`, `SUCCEEDED`, `FAILED`, `SKIPPED`, timestamps and error codes. A build can succeed even when validation fails; retain the engine but mark its assurance as `BUILT`, not `HARDWARE_VERIFIED`.

- Lease TTL: initial 2 minutes; renew every 30 seconds; use DB server time, not device wall clock.
- On lease expiry, reconcile job and allow retry with incremented attempt. Old attempts cannot finalize.
- Agent stores minimal local checkpoint (`jobId`, `attempt`, stage, artifact paths) to resume uploads after transient network failures. Re-running build is permitted but must not overwrite artifacts of a different attempt.
- Use S3 prefixes such as `tenants/{tenantId}/jobs/{jobId}/attempt-{attempt}/...`; tenant ID and keys are generated by backend, not trusted from agent input.
- Do not retry deterministic model parser errors indefinitely. Retry network/transient AWS errors with capped backoff.
- On shutdown, stop claiming jobs, attempt to cancel or finish active subprocess, and report status if connected.

## 8. Edge Agent Python implementation

### 8.1 Packaging and configuration

Use Python 3.12 and a `pyproject.toml` CLI entry point `edge-agent`. Prefer `venv --system-site-packages` on Jetson so system TensorRT bindings remain visible; document dependency compatibility. Pin/test supported Python packages. Do not install or replace system CUDA/TensorRT automatically.

`config.example.yaml`:

```yaml
control_plane_url: "https://api.example.com"
credential_file: "/etc/edge-agent/device-credential"
work_dir: "/var/lib/edge-agent/jobs"
heartbeat_interval_seconds: 30
poll_interval_seconds: 10
max_parallel_jobs: 1
max_model_bytes: 2147483648
job_timeout_seconds: 1800
log_level: "INFO"
```

No credentials in YAML or logs. Validate config on startup. Provide `edge-agent enroll --token-stdin`, `edge-agent doctor`, `edge-agent run`, and `edge-agent version` commands.

### 8.2 Hardware probe

Collect via available system tools / TensorRT APIs:

- `cat /proc/device-tree/model` (strip null terminators)
- CPU architecture, OS release, Python version
- `trtexec --version` or TensorRT Python version
- CUDA runtime version, GPU name, compute capability
- JetPack/L4T version where discoverable
- free disk space and available memory

Handle missing tools gracefully. Capabilities must be **detected**, not blindly declared. `BUILD` only if `trtexec` executable and TensorRT runtime available; `VALIDATE`/`BENCHMARK` only if required Python bindings and CUDA device work.

### 8.3 Secure asset download

- Only HTTPS pre-signed URLs returned by the authorized backend; restrict redirects and destinations; optionally allowlist configured S3 host patterns.
- Download to per-job directory with restrictive permissions, stream in chunks, enforce content-length and actual-byte limits, verify SHA-256.
- ONNX external data: support an explicit asset manifest containing ONNX and its `.data` files. Preserve safe relative paths. Reject absolute paths, `..`, symlinks, and paths escaping the job directory.
- Verify `onnx.load(..., load_external_data=True)` where ONNX is installed, or otherwise ensure assets exist before compilation. No arbitrary shell command or URL from job payload.

### 8.4 TensorRT build provider

Implement interface:

```python
class BuildProvider(Protocol):
    def supports(self, device: dict, job: dict) -> bool: ...
    def build(self, work_dir: Path, model_path: Path, options: dict) -> dict: ...
```

For V1 use NVIDIA `trtexec` subprocess with argument **list**, never `shell=True`. Allowlist precision `FP32` only at first; `FP16` can be a follow-up after validation. Use safe output filenames, timeout, disk budget, and streamed bounded logs.

Example generated command:

```bash
/usr/src/tensorrt/bin/trtexec \
  --onnx=/var/lib/edge-agent/jobs/<job-id>/input/model.onnx \
  --saveEngine=/var/lib/edge-agent/jobs/<job-id>/output/model.engine \
  --skipInference
```

Capture exit code, elapsed build time, TensorRT warnings, engine SHA-256 and size, GPU/SDK fingerprint. Never mark `BUILT` if process fails or engine missing/empty. Surface device compatibility warnings as structured warnings and preserve build log.

### 8.5 Inference validation provider

Use TensorRT 10.x tensor API (`num_io_tensors`, `get_tensor_mode`, `set_tensor_address`, `execute_async_v3`) and `cuda.bindings.runtime`. Use a non-default CUDA stream with correctly ordered H2D, enqueue, D2H, and synchronization. Allocate buffers based on actual engine tensor shapes and dtypes; reject unsupported dynamic shapes until explicitly implemented.

Two validation levels:

1. **Smoke test:** deserialize engine, execute with a valid synthetic input, verify no runtime error, expected output shape, finite outputs. Label result `RUNTIME_SMOKE_TEST_PASSED` only; this does **not** prove model accuracy.
2. **Reference comparison (optional):** customer provides approved test inputs and reference outputs (or backend produces reference outputs using ONNX Runtime). Compare with explicit absolute/relative tolerances, report per-output max errors and pass/fail. Only mark `HARDWARE_VERIFIED` when the configured validation policy succeeds; record which policy was used. Never infer class labels or preprocessing; use provided metadata.

For initial MVP, `HARDWARE_VERIFIED` may mean successful real-hardware smoke test **if and only if** the UI/report explicitly states `smoke-test-only` and does not imply numerical equivalence or accuracy validation. Prefer separate `validation_level` values.

### 8.6 Benchmark provider

Reuse the working CUDA-event benchmark implementation rather than rewriting it unnecessarily. Default 20 warmups + 100 measured iterations, batch size 1. Use non-default CUDA stream and synchronize CUDA events correctly. Validate `iterations` bounds (e.g. 10–10000), input shape, and allocation sizes.

Required report fields:

```json
{
  "schema_version": "1.0",
  "job_id": "<uuid>",
  "device_id": "<uuid>",
  "timestamp_utc": "<ISO-8601>",
  "engine_sha256": "<sha256>",
  "hardware": {
    "model": "Jetson Orin Nano Super",
    "gpu": "Orin",
    "compute_capability": "8.7"
  },
  "software": {"tensorrt": "10.16.2", "cuda": "13.2"},
  "input_shape": [1, 3, 224, 224],
  "warmup_iterations": 20,
  "measured_iterations": 100,
  "metric_scope": "CUDA-event GPU-only inference; excludes transfers and preprocessing",
  "latency_ms": {
    "mean": 0.0, "median_p50": 0.0, "p95": 0.0,
    "min": 0.0, "max": 0.0
  },
  "throughput_fps_estimate": 0.0,
  "validation_level": "RUNTIME_SMOKE_TEST_PASSED",
  "warnings": [],
  "notes": ["Synthetic input; not accuracy validation", "FPS derived from mean GPU latency"]
}
```

Do not fill fake zeroes in real reports: the example above is a schema illustration. Include precision and power mode if reliably detected; use `null` if unavailable. Distinguish estimated FPS from observed end-to-end throughput. Do not promise stable benchmarks without recording thermal/power conditions.

### 8.7 Manifest and artifact package

Produce:

```text
output/
  model.engine
  build.log
  validation.json
  benchmark.json
  manifest.json
```

Manifest includes source ONNX hash(es), engine hash, build options, device fingerprint, software versions, build time, validation level, benchmark scope, warnings, and artifact paths. Keep engine portability caveats explicit. Upload artifacts using per-object pre-signed PUT URLs, verify response, and call backend completion endpoint. Optionally produce a ZIP with a `README` explaining required runtime dependencies; do not bundle proprietary SDK libraries without license review.

## 9. Backend implementation sequence

**Task B1 — Discovery:** Inspect existing Spring Boot code, model entities, auth, AWS S3 integrations, and migrations. Write short `docs/edge-agent-architecture.md` listing integration points before modifying code.

**Task B2 — Database:** Add migrations and JPA repositories for devices/jobs/events/artifacts; add tenant indexes, job status indexes, uniqueness and concurrency tests.

**Task B3 — Enrollment:** Implement one-time enrollment token and device credential issuance/revocation; tenant admin approval and audit events; never log raw secrets.

**Task B4 — Device API:** Implement heartbeat and online/offline detection; device capability schema validation; restrict tenant visibility.

**Task B5 — User job API:** Validate model ownership, device ownership, supported capabilities, job type, requested precision, idempotency key. Create `QUEUED` job.

**Task B6 — Claim/lease:** Atomic eligible-job claim for authenticated device, attempt and lease token; renewal, timeout reconciliation, stale callback rejection. Add race-condition tests with concurrent claimers.

**Task B7 — Artifact transfer:** Generate narrowly scoped, short-lived S3 pre-signed GET/PUT URLs with server-owned keys. Verify object size and hash metadata before final completion. Apply S3 encryption and lifecycle retention policy.

**Task B8 — Result API:** Expose stages, errors, warnings, artifacts and reports to React. Never expose device credentials or internal S3 object keys to unauthorized users.

**Task B9 — SQS integration (optional for MVP):** Use SQS internally for dispatch/scheduler wakeups, not as the device's direct cross-tenant queue. If already present, preserve existing patterns. Make DB the authoritative state source.

## 10. Frontend implementation sequence

**F1 Device page:** list enrolled devices, model, SDK version, capabilities, status, last seen, approval state. Provide enrollment instructions and token issuance for tenant admins.

**F2 Build form:** select existing ONNX model, eligible target device, FP32, optional benchmark iterations. Disable unsupported combinations with explanation.

**F3 Job details:** poll job API every 2–5 seconds while active; show stages, progress, warnings, failure reasons, elapsed time, and cancel action.

**F4 Results:** display engine download, validation level, benchmark metrics (mean/P50/P95/FPS estimate), device/software fingerprint and clear benchmark scope. If only `BUILT`, do not show a green hardware-verified badge.

**F5 Failure UX:** handle offline device, expired enrollment, build timeout, unsupported ONNX operator, missing external data, disk full, and TensorRT device warning.

## 11. Security and isolation requirements

- Device credentials are scoped to one tenant/device, revocable and rotated; require HTTPS. Never share AWS access keys with customer devices.
- Use least-privilege IAM roles on backend ECS; S3 pre-signed URLs limited to specific objects and short expiry.
- Prevent path traversal, symlink escape, unsafe archive extraction, arbitrary command execution, and unrestricted subprocess flags.
- Treat ONNX models as untrusted input. Use dedicated non-root service user, job timeouts, memory/disk quotas, bounded logs, and isolated per-job directories. Container isolation is desirable but GPU/Jetson compatibility must be validated; do not claim full sandboxing solely from a subprocess.
- Reject oversized uploads and unreasonable tensor shapes. Avoid running user-supplied custom plugins or executable code in V1.
- Enforce tenant ownership at every endpoint and S3 key derivation. Audit enrollment, claims, artifact uploads and job completions.
- Never accept arbitrary callback URLs or shell commands in job payloads.
- Document that hardware agent installation grants the service ability to run model workloads on that device; require informed customer admin consent.

## 12. Tests and acceptance criteria

### Unit tests

- Capability probe handles missing `trtexec`/CUDA cleanly.
- Build command uses safe argument list; rejects unsupported flags and path traversal.
- External-data asset manifest rejects `../`, absolute paths and symlinks.
- Benchmark report computes mean/P50/P95 correctly from known samples.
- API client retries transient `503` and stops on `401`/`403`.
- Lease state machine rejects stale completion and illegal terminal-state regression.
- Idempotent user job creation returns same job for repeated key.

### Backend integration tests

- Two concurrent agents cannot claim the same job.
- Agent A cannot claim or complete tenant B's job.
- Expired lease causes a new attempt; old attempt cannot overwrite its results.
- Failed S3 upload does not mark job `SUCCEEDED`.
- Offline/disabled device cannot receive jobs.
- Cancellation and timeouts reach consistent terminal states.

### Jetson hardware tests

1. `edge-agent doctor` identifies Jetson, TensorRT, CUDA, GPU and supported capabilities.
2. Enroll a platform-owned Jetson and confirm it appears in UI.
3. Upload known ONNX and external `.data` weights, create `PIPELINE` job.
4. Agent claims job, downloads assets, builds a **new** engine on this device.
5. Load engine and execute smoke-test inference; optionally compare reference outputs.
6. Run 20 warmups + 100 timed GPU-only inferences; emit real report.
7. Upload engine/log/reports/manifest; verify hashes and download links.
8. Repeat with second agent identity to demonstrate the same package works for customer-owned devices.
9. Disconnect device mid-job; verify lease expiry/retry and no false success.
10. Inspect and document any TensorRT engine device mismatch warning. Do not mark engine as portable by default.

**MVP passes only if:** no manual SSH is needed during a submitted job; no inbound port is opened; device and tenant isolation tests pass; all artifacts are downloadable; failures are observable; reported benchmark scope is accurate.

## 13. Suggested development order and commits

1. `docs: describe edge-agent v1 architecture and API contracts`
2. `feat(backend): device enrollment, device registry, heartbeat`
3. `feat(backend): jobs schema, idempotent creation, lease claim and renewal`
4. `feat(backend): secure artifact presigned URLs and completion API`
5. `feat(agent): CLI, configuration, hardware probe, enrollment and heartbeat`
6. `feat(agent): claim loop, lease renewal, cancellation and local job workspace`
7. `feat(agent): TensorRT build provider and S3 artifact transfer`
8. `feat(agent): TensorRT runtime validation and benchmark integration`
9. `feat(frontend): devices, submit job, progress, artifacts and benchmark report`
10. `test: concurrency, tenancy, failure recovery and Jetson E2E`
11. `docs: installation, operations, troubleshooting and security`

Implement in small reviewable commits. Run tests after each stage; do not claim unexecuted tests passed.

## 14. Local development and Jetson deployment commands

Commands below are templates; adjust paths after inspecting repository.

```bash
# Jetson: install Python venv support if needed
sudo apt update
sudo apt install -y python3.12-venv

cd ~/edge-ai-platform/model-worker/edge-agent
python3 -m venv --system-site-packages .venv
source .venv/bin/activate
python -m pip install -e '.[dev]'

# Verify GPU, SDK, agent configuration
python -c 'import tensorrt as trt; print(trt.__version__)'
python -c 'from cuda.bindings import runtime as cudart; print(cudart.cudaGetDeviceCount())'
edge-agent doctor

# Agent enrollment (token entered via stdin, not command line history)
edge-agent enroll --token-stdin

# Foreground debug mode
edge-agent run

# Run unit tests
pytest -q
```

Systemd unit (adjust paths and user; do not run as root):

```ini
[Unit]
Description=Edge AI Agent
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=edgeagent
Group=edgeagent
WorkingDirectory=/opt/edge-agent
ExecStart=/opt/edge-agent/.venv/bin/edge-agent run
Restart=on-failure
RestartSec=10
NoNewPrivileges=true
UMask=0077

[Install]
WantedBy=multi-user.target
```

If access to GPU libraries/devices requires group membership, document the minimum required permissions. Ensure `/etc/edge-agent/device-credential` and `/var/lib/edge-agent` are readable/writable only by the service identity. Test the actual service after installation.

## 15. AI coding agent instructions (copy into implementation task)

You are implementing **Edge Agent V1** in the existing `edge-ai-platform` repository. Follow this specification in order. First inspect existing code and write an integration plan. Do not invent nonexistent APIs or overwrite working code. Implement backend migration/auth/job APIs, a Python Jetson agent, provider abstractions, frontend workflows, tests, and operational documentation in small commits. Use the existing Spring Boot, React, S3 and AWS conventions. For customer-owned devices, use outbound HTTPS polling and scoped device credentials; never distribute AWS SQS or cross-tenant S3 credentials. Support ONNX external weight files, safe TensorRT compilation, actual device inference, GPU-only benchmark, artifact integrity checks, lease-based concurrency, and truthful result labels. At each milestone report changed files, commands run, test results, blockers, and next steps. Do not mark hardware-dependent tests as passed unless executed on the Jetson. Do not suppress the existing TensorRT engine compatibility warning. Start with Phase 1: repository inspection, architecture document, migration, and device enrollment API.

## 16. Out of scope for V1

- Hailo, Qualcomm QNN, OpenVINO compilation providers (only interfaces/placeholders).
- INT8 calibration and automated quantization.
- Multi-device distributed benchmarking and sophisticated fleet scheduling.
- Billing, subscriptions, Kubernetes, and production-grade multi-region failover.
- Guaranteed model correctness from synthetic-input smoke tests.
- Guaranteed engine portability between Jetson models, software versions, or GPU architectures.

## 17. Follow-up roadmap

**V1.1:** FP16 build with numerical reference validation, richer thermal/power monitoring (`tegrastats`), more accurate end-to-end throughput tests.  
**V1.2:** Customer-facing installer, credential rotation UX, device policy controls, offline recovery and artifact retention controls.  
**V2:** Additional hardware/compiler providers, hosted hardware pool scheduling, build compatibility matrix, cross-compiler support where vendor SDK permits.
