# Edge Agent V1 architecture

The edge agent is an outbound-only process running on a Jetson. It exchanges a short-lived, single-use enrollment token for a per-device credential, then reports detected capabilities by HTTPS. The current backend has neither tenant authentication nor PostgreSQL enabled, so its first implementation is intentionally a configuration-gated local adapter: it requires an `X-Edge-Admin-Key`, is disabled by default, stores only credential hashes, and leaves enrolled devices pending approval.

Set `APP_EDGE_AGENT_ENABLED=true`, `APP_EDGE_AGENT_ADMIN_KEY` to a nonempty secret, and optionally `APP_EDGE_AGENT_DIRECTORY` for controlled local development. These endpoints are not a substitute for the production tenant authorization layer. The included PostgreSQL schema is a future migration artifact and is not applied while the datasource is disabled.

Only an approved device can heartbeat. Job claiming, artifact URLs, TensorRT build execution, validation, and benchmarking remain unimplemented; consequently this milestone makes no hardware-verification or benchmark claim.
