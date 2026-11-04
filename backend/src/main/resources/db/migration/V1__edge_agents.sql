-- PostgreSQL production schema. This repository currently disables datasource/Flyway at runtime;
-- apply through the future database persistence rollout, not as a filesystem-store substitute.
CREATE TABLE edge_devices (
  id UUID PRIMARY KEY, tenant_id UUID NOT NULL, display_name TEXT NOT NULL,
  hardware_vendor TEXT, hardware_model TEXT, gpu_name TEXT, compute_capability TEXT,
  os_arch TEXT, jetpack_version TEXT, cuda_version TEXT, tensorrt_version TEXT,
  capabilities JSONB NOT NULL DEFAULT '{}'::jsonb, status TEXT NOT NULL,
  credential_hash TEXT, credential_version INTEGER NOT NULL DEFAULT 1,
  last_heartbeat_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_edge_devices_tenant_status ON edge_devices(tenant_id, status);
CREATE TABLE edge_jobs (
  id UUID PRIMARY KEY, tenant_id UUID NOT NULL, model_id UUID NOT NULL, device_id UUID REFERENCES edge_devices(id),
  job_type TEXT NOT NULL, status TEXT NOT NULL, assurance_level TEXT, requested_precision TEXT NOT NULL,
  requested_config JSONB NOT NULL DEFAULT '{}'::jsonb, attempt INTEGER NOT NULL DEFAULT 0, max_attempts INTEGER NOT NULL DEFAULT 2,
  lease_token_hash TEXT, lease_expires_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0,
  error_code TEXT, error_message TEXT, idempotency_key TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), started_at TIMESTAMPTZ, finished_at TIMESTAMPTZ, updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, idempotency_key)
);
CREATE TABLE edge_job_events (id UUID PRIMARY KEY, job_id UUID NOT NULL REFERENCES edge_jobs(id), device_id UUID REFERENCES edge_devices(id), event_type TEXT NOT NULL, message TEXT, metadata JSONB NOT NULL DEFAULT '{}'::jsonb, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE edge_artifacts (id UUID PRIMARY KEY, tenant_id UUID NOT NULL, job_id UUID NOT NULL REFERENCES edge_jobs(id), kind TEXT NOT NULL, s3_bucket TEXT NOT NULL, s3_key TEXT NOT NULL, sha256 TEXT NOT NULL, size_bytes BIGINT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
