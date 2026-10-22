# MVP roadmap

## Phase 1: Repository foundation and development environment
**Deliverables:** component scaffolds, CI, docs, repeatable local setup. **Acceptance:** clean checkout can install/build/test each component. **Tests:** backend, worker, frontend checks in CI.

## Phase 2: ONNX upload, validation, and metadata inspection
**Deliverables:** bounded upload API, controlled file storage, worker adapter, model metadata API. **Acceptance:** valid small ONNX is stored and inspected; invalid/oversized files return safe actionable errors. **Tests:** upload boundaries, generated ONNX fixture, worker missing dependency, adapter timeout/error.

Status: local upload, disk catalog, metadata API, and process adapter are implemented. Compatibility evaluation is not.

## Phase 3: Hardware capability registry and compatibility reports
**Deliverables:** explicit target/runtime capability records and evidence-based report API. **Acceptance:** supported/unsupported/unknown capabilities are distinguished and rationale shown. **Tests:** capability matching, unknown cases, API serialization.

## Phase 4: Deployment package generation and job tracking
**Deliverables:** preparation package manifest and persisted job state. **Acceptance:** users can create and inspect preparation jobs without implying device deployment. **Tests:** lifecycle transitions, failures, duplicate requests, API validation.

## Phase 5: Integration with one real edge AI hardware target
**Deliverables:** one selected vendor adapter using its supported SDK and documented prerequisites. **Acceptance:** compile and deploy on explicitly supported hardware with reproducible artifacts. **Tests:** adapter contract/unit tests plus hardware-gated integration test.

## Phase 6: Benchmarking, additional targets, and deployment improvements
**Deliverables:** repeatable benchmark records, more adapters, deployment improvements. **Acceptance:** metrics include device/runtime/configuration and are comparable. **Tests:** benchmark parsing, adapter regression, hardware-gated end-to-end checks.

Keep each phase small and working end to end. Never label compatibility or deployment as supported without direct capability evidence and exercised adapters.
