# Edge AI Platform — Core UI Design & Implementation Specification

**Version:** 1.0  
**Audience:** AI coding agents implementing the React frontend and necessary Spring Boot contracts  
**Goal:** Ship an accessible, guided deployment experience from ONNX upload to target hardware selection, build, validation, benchmark, and artifact download.

## 0. Agent instructions

1. Inspect the repository before changing code. Identify the existing React, TypeScript, Vite, routing, styling, API client, authentication, model-upload and job APIs. Reuse conventions; do not scaffold a second frontend.
2. Read existing backend API definitions and database migrations. Do not assume endpoints in this document already exist.
3. Implement in the phases below, in order. After each phase run typecheck, lint and tests, report changed files and unresolved integration gaps. Do not overwrite working model-analysis functionality.
4. Use **English** for all user-facing text. Do not present mock data as live results.
5. Prefer existing UI component library. If none exists, use a coherent accessible component system with CSS variables and responsive layouts. Avoid adding large dependencies without justification.
6. Do not call a build 'hardware verified' unless a real target device ran the validation job and the backend attests to that outcome.
7. If backend endpoints are unavailable, create typed API adapters and an explicitly labeled demo/mock mode; do not silently fake production responses.

## 1. Product definition

The platform helps users deploy ONNX models to edge AI hardware. It offers static analysis, hardware compatibility checks, target-specific builds where supported, real-device inference validation, benchmarking, and downloadable deployment bundles.

**Primary user journey:** `Upload Model → Select Hardware → Choose Execution → Configure Build → Review Results`.

**Execution choices:**
- **My Device:** Customer-owned compatible device running an authenticated Edge Agent.
- **Platform Hardware:** Platform-owned matching device, only when service and capacity are available.
- **No Device:** Analysis and compatibility only; build only if a supported cross-compiler is available; never promise target-device validation or benchmarks.

The frontend must show the difference between *compiled*, *runtime verified*, and *benchmarked*.

## 2. Information architecture and routes

Use the existing router and preserve existing routes where possible. Proposed canonical routes:

| Route | Purpose |
|---|---|
| `/dashboard` | Overview, recent deployments, next action |
| `/models` | Model list, upload, versions, analysis |
| `/devices` | Registered devices, connection state, setup |
| `/deployments/new` | Main five-step guided wizard |
| `/deployments/:id` | Persistent deployment details and progress |
| `/builds` | Build history and status filters |
| `/benchmarks` | Benchmark history and comparison |
| `/artifacts` | Available deployment bundles |

Global shell: logo and workspace, primary nav, main content, account menu. Primary CTA on dashboard and models page: **New Deployment**. Do not expose internal infrastructure names (SQS, workers, etc.) as top-level navigation.

## 3. Visual and UX system

- Professional, minimal, engineering-oriented UI. Neutral background, subtle borders, restrained accent color, generous spacing, clear typography.
- Desktop: compact left navigation, main content max-width ~1100 px. Mobile: collapsible navigation and single-column cards.
- Use consistent spacing (4/8/12/16/24/32 px), 8–12 px corner radius, visible focus rings, adequate contrast.
- Semantic HTML, keyboard navigation, labeled controls, live regions for asynchronous status, screen-reader descriptions for icons. Target WCAG 2.2 AA.
- Avoid unexplained acronyms. Explain ONNX, TensorRT, and Edge Agent in contextual help, not in the primary CTA.
- Every asynchronous state must support loading, empty, error, retry, and success. Never invent a percentage progress bar for stages without measurable progress.
- Responsive breakpoints: ensure wizard works at 360 px width, tablet, and desktop. No horizontal overflow.

### Reusable components

`AppShell`, `PageHeader`, `StepIndicator`, `WizardFooter`, `ModelUploadDropzone`, `ModelSummaryCard`, `HardwareCard`, `ExecutionOptionCard`, `CapabilityNotice`, `DeviceConnectionCard`, `BuildConfigurationForm`, `JobStageTimeline`, `JobLogsPanel`, `MetricCard`, `ArtifactDownloadList`, `EmptyState`, `ErrorState`, `StatusBadge`, `ConfirmDialog`.

Use consistent statuses: `ANALYZED`, `BUILT`, `HARDWARE_VERIFIED`, `BENCHMARKED`, `FAILED`, `CANCELLED`, and `UNVERIFIED` where applicable. Job lifecycle status and verification achievements are **different dimensions**; do not overload one enum to represent both.

## 4. Five-step deployment wizard (core feature)

Route: `/deployments/new`. Persist in-progress wizard selections in URL query or session storage if existing architecture supports it; do not persist credentials. Allow Back/Continue without losing inputs. Validate each step before continuing. Accessible stepper with current and completed steps.

### Step 1 — Model

**Heading:** “Select your model”  
**Helper:** “Upload an ONNX model or select one from your library.”

- Tabs: `Existing Models` / `Upload ONNX` (reuse existing upload/analysis flow).
- Drag-and-drop plus file picker; accept `.onnx`. Support ONNX external tensor data (`.onnx.data` and related referenced files) as a model bundle, preserving relative filenames.
- Show file sizes, upload progress, cancel/retry, server validation errors, and privacy notice.
- After analysis display model name, version, input/output shapes, operators summary, warnings.
- Block progression if upload or analysis has failed; provide retry.
- Data output: `modelId`, `modelVersionId`, `analysisId`.

### Step 2 — Hardware

**Heading:** “Choose target hardware”  
**Helper:** “Where do you want your model to run?”

- Selectable hardware cards with device name, vendor, accelerator, supported runtime, and support status.
- V1 enabled target: **NVIDIA Jetson Orin Nano Super**, provided the backend advertises it as supported.
- Hailo/Qualcomm and other planned targets should be labeled “Coming later” and disabled, not offered as functional choices.
- Show a concise compatibility summary based on backend assessment, including unknown or unsupported operators.
- Do not claim compatibility guarantees from static analysis.
- Data output: `hardwareTargetId`, optional `compatibilityAssessmentId`.

### Step 3 — Execution

**Heading:** “Where should we run your model?”

Three radio-card options:

1. **Use my own device** — list matching registered devices, online/offline status, runtime versions and capabilities. If none available, show “Connect your Jetson” with a setup link and enrollment flow. An offline device cannot be selected for immediate execution unless queued execution is explicitly supported.
2. **Use platform hardware** — only selectable when matching hosted worker capacity and policy are available. Otherwise show precise unavailable reason.
3. **I don't have a device** — allow analysis and compatible cross-compilation only. Show explicit notice: “Hardware validation and benchmarking require access to a matching device.”

Fetch execution options from backend for selected model and hardware. Disable unsupported actions with explanatory text. Never derive capability solely from frontend hardcoded assumptions.

Data output: `executionMode`, optional `deviceId`, `executionOptionId`.

### Step 4 — Build

**Heading:** “Configure your deployment”

- Runtime: TensorRT for supported Jetson target; options loaded from backend.
- Precision: FP32 default; FP16 only if backend says supported; INT8 hidden until calibration workflow exists.
- Operations: Analyze (if needed), Build, Validate on hardware, Benchmark on hardware. Present checkboxes/toggles only for allowed capabilities, with dependencies explained.
- Benchmark defaults: warm-up 20, measured iterations 100, batch 1; show advanced controls only when supported. Clarify GPU-only latency vs end-to-end latency.
- Review panel: model, hardware, execution location, runtime, precision, expected outputs, verification limitations.
- CTA: **Start Deployment**; show confirmation if device costs or long-running tasks apply (when billing exists).
- Submit idempotently (idempotency key per deliberate submission), prevent accidental duplicate clicks, and route to `/deployments/:id` on success.

### Step 5 — Results / progress

A deployment job is asynchronous; this step can route to `/deployments/:id` rather than hold the wizard open. The detail page must be deep-linkable and refresh-safe.

Stages (conditional): `Model Analysis → Compatibility → Engine Build → Inference Validation → Benchmark → Artifact Packaging`.

- Render backend-provided stage states: `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, `SKIPPED`, `CANCELLED`.
- Show stage timestamps, duration, error explanations, safe retry where supported.
- Live updates: polling every 3–5 seconds initially; stop when terminal, hidden-tab throttling as practical. SSE/WebSocket optional later.
- Logs panel with stage filter, timestamps, copy/download, bounded display, and no credential leakage.
- Results cards: build result, hardware validation result, benchmark metrics, target environment, artifact downloads.
- For benchmark, show mean, P50, P95, min/max, estimated FPS and explicit metric scope. Never label estimated FPS as measured throughput.
- Validation must report what was checked: smoke inference vs numeric comparison to reference outputs. A successful invocation alone does not establish accuracy equivalence.
- Display warnings (e.g. TensorRT engine portability/device mismatch) prominently in results and manifest.

## 5. Device setup and management

`/devices` list: name, hardware model, agent version, TensorRT/CUDA versions, last heartbeat, availability, supported capabilities, owner/workspace, and actions.

`/devices/connect` setup wizard:
1. Select hardware and operating system.
2. Show prerequisites (JetPack/TensorRT/Python/network and disk requirements) based on supported matrix.
3. Create a short-lived, single-use enrollment token using backend API. Display copyable installation command **only from trusted backend instructions**; never embed long-lived credentials in frontend source or logs.
4. User installs and starts Edge Agent on their Jetson.
5. Wait for device registration and heartbeat; show timeout, retry, and troubleshooting.
6. Confirm detected hardware and runtime; let user name the device.

Agents should initiate outbound connections; no inbound port forwarding should be required for MVP. Device revoke/disconnect controls must require confirmation.

## 6. Results and artifact UX

Summary header: deployment name, model version, target hardware, execution mode, job status, created/finished timestamps.

Separate indicators:
- **Built**: artifact generated.
- **Hardware Verified**: validation executed on identified matching target device with a recorded result.
- **Benchmarked**: real device benchmark executed and report attached.
- **Not Hardware Verified**: compilation only, or validation unavailable/skipped.

Artifacts: engine file, `manifest.json`, `validation.json` (if generated), `benchmark.json` (if generated), build logs, deployment instructions. Show file size, generation time, checksum if available, and expiring signed download links. Only show download controls for artifacts actually present.

## 7. API contracts (proposed; reconcile with existing backend)

Do not implement duplicate APIs if equivalents already exist. Use typed API clients and central error handling.

```http
GET  /api/models
POST /api/models/uploads/initiate
POST /api/models/{modelId}/analyze
GET  /api/hardware-targets
GET  /api/models/{modelId}/compatibility?hardwareTargetId=...
GET  /api/execution-options?modelId=...&hardwareTargetId=...
GET  /api/devices
POST /api/devices/enrollment-tokens
GET  /api/devices/{deviceId}
POST /api/deployments
GET  /api/deployments/{deploymentId}
GET  /api/deployments/{deploymentId}/stages
GET  /api/deployments/{deploymentId}/logs?cursor=...
GET  /api/deployments/{deploymentId}/artifacts
GET  /api/artifacts/{artifactId}/download-url
```

Example execution options response:

```json
{
  "options": [
    {"id":"own-001","type":"CUSTOMER_DEVICE","available":true,"deviceId":"device-001","capabilities":["BUILD","VALIDATE","BENCHMARK"]},
    {"id":"hosted","type":"PLATFORM_HOSTED","available":false,"reasonCode":"NO_AVAILABLE_WORKER","capabilities":[]},
    {"id":"cross","type":"CROSS_COMPILE","available":false,"reasonCode":"UNSUPPORTED_TARGET","capabilities":[]}
  ]
}
```

Example create deployment request:

```json
{
  "modelVersionId":"model-version-123",
  "hardwareTargetId":"jetson-orin-nano-super",
  "executionOptionId":"own-001",
  "runtime":"TENSORRT",
  "precision":"FP32",
  "operations":{"build":true,"validate":true,"benchmark":true},
  "benchmarkConfig":{"warmupIterations":20,"measuredIterations":100,"batchSize":1}
}
```

Use an `Idempotency-Key` request header for create operations. Backend must re-check execution capability, authorization and availability at submission time; UI capability data can become stale.

Suggested TypeScript models:

```ts
type ExecutionMode = 'CUSTOMER_DEVICE' | 'PLATFORM_HOSTED' | 'CROSS_COMPILE' | 'ANALYSIS_ONLY';
type StageStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED' | 'CANCELLED';
type Operation = 'BUILD' | 'VALIDATE' | 'BENCHMARK';
interface ExecutionOption {
  id: string;
  type: ExecutionMode;
  available: boolean;
  reasonCode?: string;
  deviceId?: string;
  capabilities: Operation[];
}
interface BenchmarkMetrics {
  metricScope: string;
  meanMs: number;
  p50Ms: number;
  p95Ms: number;
  minMs: number;
  maxMs: number;
  estimatedFps?: number;
  measuredIterations: number;
}
```

## 8. Implementation file organization

Adapt to the existing codebase rather than forcing these exact paths:

```text
frontend/src/
  app/                 # router, app shell, auth boundaries
  components/ui/       # shared primitives
  features/models/     # upload and analysis summary
  features/hardware/   # target selection and compatibility
  features/devices/    # device list and enrollment
  features/deployments/
    components/         # wizard steps, stage timeline, logs, artifacts
    pages/              # new deployment and details
    hooks/              # draft state, queries, mutations
    api/                # typed clients and DTO mappings
  features/benchmarks/ # history and metrics display
  styles/              # tokens and global styles
```

Recommended: React Router (if existing), TanStack Query (if already installed or justified), React Hook Form + Zod (optional), and a consistent accessible component library. Keep server state separate from wizard draft state. Never use client-only state as the source of truth for completed jobs.

## 9. Phased execution plan for AI coding agent

### Phase 0 — Audit and plan

- Inspect repository, existing routes, styles, APIs, authentication, upload flow, and tests.
- Produce a concise mapping from this specification to current implementation.
- Identify missing backend contracts and decide adapter/mock strategy.
- **Exit criteria:** written change plan and no destructive migrations.

### Phase 1 — Shell and shared components

- Implement navigation, page header, buttons, badges, cards, skeletons, empty/error states and responsive layout.
- Add `/deployments/new` and `/deployments/:id` routes behind existing auth.
- **Exit criteria:** responsive navigation, keyboard-accessible shell, existing pages still work.

### Phase 2 — Wizard model + hardware

- Reuse model library/upload/analysis.
- Add hardware target cards, compatibility summary, disabled future targets.
- Implement stepper, validation, draft persistence, Back/Continue.
- **Exit criteria:** user can select a real model and target; invalid steps cannot advance.

### Phase 3 — Execution and device connection

- Integrate execution-options API; implement radio cards and capability notices.
- Build `/devices` list and `/devices/connect` enrollment screens.
- Support offline/unavailable/unsupported states.
- **Exit criteria:** no-device path never promises validation/benchmark; connect-device path is discoverable.

### Phase 4 — Build configuration and submission

- Implement runtime/precision/operations form and review summary.
- Enforce capability-dependent UI, submit idempotently, handle backend validation errors.
- **Exit criteria:** one deliberate submission creates one deployment; redirects to detail page.

### Phase 5 — Live deployment details

- Add stage timeline, polling, logs, failures/retries and conditional steps.
- Persist job view across refresh and deep links.
- **Exit criteria:** states reflect backend; no fake progress or fake success.

### Phase 6 — Results and downloads

- Render validation type, benchmark metrics with metric-scope labels, hardware environment, warnings and artifacts.
- Add signed download URL flow and missing-artifact handling.
- **Exit criteria:** user can distinguish compiled-only from hardware-verified and download only real artifacts.

### Phase 7 — Dashboard, history and quality

- Add dashboard recent jobs, models/devices summary, build/benchmark history, search and filters.
- Implement automated tests and run accessibility/responsiveness checks.
- **Exit criteria:** all acceptance scenarios below pass and build pipeline is green.

## 10. Test matrix and acceptance scenarios

Automated tests: unit tests for capability mapping, step validation, request construction, status mapping; component tests for disabled states and error handling; integration tests with mocked API; E2E with Playwright if present (or add with approval).

Required E2E scenarios:

1. Existing ONNX model + online matching customer Jetson → select Build/Validate/Benchmark → submit → stage progress → real result/artifact links.
2. User has no device, no hosted capacity, no cross-compiler → analysis-only path with clear explanation; Build/Validate/Benchmark disabled.
3. Hosted device unavailable → option disabled with backend reason; user can choose own device or analysis-only.
4. Device offline between selection and submission → backend rejects safely; UI refreshes options and preserves form.
5. Build succeeds, validation fails → display Built but **not** Hardware Verified; do not show benchmark as completed.
6. Build-only cross-compile → mark Not Hardware Verified; no real benchmark metrics.
7. Model with external ONNX weights → upload bundle is complete and analyzed correctly.
8. Network interruption during job polling → show reconnecting/retry; no fabricated progress.
9. Duplicate click or retry → idempotency prevents duplicate jobs.
10. Signed download link expires → request fresh link or show clear recovery path.
11. Mobile 360 px and keyboard-only navigation → all steps usable and focus visible.
12. User cannot view another workspace's device, deployment, logs or artifacts.

## 11. Security and operational guardrails

- All APIs authenticated and workspace-scoped; backend enforces ownership and permissions.
- Enrollment tokens short-lived and single-use; never log tokens or embed persistent secrets in the frontend.
- Use presigned S3 URLs only for authorized upload/download; avoid exposing bucket credentials.
- Sanitize displayed logs, escape user-controlled text, and apply content security policy as supported.
- Do not expose raw shell command execution to users. Edge Agent executes only allowlisted job types.
- Build artifacts can be device/software-specific; include provenance and compatibility warnings.
- Avoid public-facing claims of accuracy, FPS, or compatibility without supporting test context.

## 12. Definition of Done

The MVP is complete when an authenticated user can, entirely through the UI:

1. Upload/select an ONNX model and view analysis.
2. Choose a supported target and see a truthful compatibility summary.
3. Choose an available customer-owned Agent or other genuinely available execution option.
4. Configure supported Build/Validation/Benchmark operations.
5. Submit once and track actual job progress/logs.
6. View build result, verification scope, hardware benchmark and environment information.
7. Download available artifacts and understand unsupported/unverified states.

All existing flows must remain functional. Tests, typecheck and production build must pass. Any incomplete backend integration must be called out explicitly, never disguised with live-looking mock data.

## 13. Starter prompt for an AI coding agent

> Read `docs/EDGE_AI_CORE_UI_DESIGN_SPEC.md` completely. Inspect the current frontend and backend APIs first. Implement **Phase 0 and Phase 1 only**, following existing project conventions. Preserve the existing ONNX analysis feature. Create a concise plan, implement responsive app shell and deployment routes, add tests, run typecheck/lint/build, and report changed files and missing API contracts. Do not fabricate live data or implement later phases until Phase 1 is validated.

For subsequent phases, instruct the agent to implement **one phase at a time**, run tests, and provide a short completion checklist before proceeding.
