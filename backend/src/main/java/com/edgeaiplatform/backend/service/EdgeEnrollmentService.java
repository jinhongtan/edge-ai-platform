package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.config.EdgeAgentProperties;
import com.edgeaiplatform.backend.dto.*;
import com.edgeaiplatform.backend.exception.AgentAccessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

/** File-backed, configuration-gated enrollment adapter for local development only. */
@Service
public class EdgeEnrollmentService {
    private final EdgeAgentProperties properties; private final ObjectMapper json;
    private final Map<String, Token> tokens = new HashMap<>(); private final Map<String, StoredDevice> devices = new HashMap<>();
    private final SecureRandom random = new SecureRandom(); private Path store;
    public EdgeEnrollmentService(EdgeAgentProperties properties, ObjectMapper json) { this.properties = properties; this.json = json; }
    @PostConstruct synchronized void load() throws IOException {
        store = properties.directory(); Files.createDirectories(store);
        Path file = store.resolve("devices.json");
        if (Files.isRegularFile(file)) devices.putAll(json.readValue(file.toFile(), new TypeReference<Map<String, StoredDevice>>() {}));
    }
    public synchronized EnrollmentTokenResponse issue(String adminKey, String tenantId) {
        requireEnabled(); requireAdmin(adminKey); String raw = secret(); Instant expiry = Instant.now().plus(properties.enrollmentTokenTtl());
        tokens.put(hash(raw), new Token(tenantId, expiry)); return new EnrollmentTokenResponse(raw, expiry);
    }
    public synchronized AgentEnrollmentResponse enroll(AgentEnrollmentRequest request) {
        requireEnabled(); Token token = tokens.remove(hash(request.token()));
        if (token == null || token.expiresAt().isBefore(Instant.now())) throw new AgentAccessException(401, "INVALID_ENROLLMENT_TOKEN", "Enrollment token is invalid or expired.");
        String id = UUID.randomUUID().toString(), credential = secret(); Instant now = Instant.now();
        devices.put(id, new StoredDevice(new EdgeDevice(id, token.tenantId(), request.displayName(), "PENDING_APPROVAL", request.agentVersion(), safeMap(request.capabilities()), null, now, now), hash(credential)));
        persist(); return new AgentEnrollmentResponse(id, credential, "PENDING_APPROVAL");
    }
    public synchronized EdgeDevice heartbeat(String credential, AgentHeartbeatRequest request) {
        StoredDevice stored = authenticate(credential); EdgeDevice d = stored.device();
        if (!"ONLINE".equals(d.status())) throw new AgentAccessException(403, "DEVICE_NOT_APPROVED", "Device is not approved for agent operations.");
        EdgeDevice updated = new EdgeDevice(d.id(), d.tenantId(), d.displayName(), d.status(), d.agentVersion(), safeMap(request.capabilities()), Instant.now(), d.createdAt(), Instant.now());
        devices.put(d.id(), new StoredDevice(updated, stored.credentialHash())); persist(); return updated;
    }
    public synchronized List<EdgeDevice> list(String adminKey, String tenantId) { requireEnabled(); requireAdmin(adminKey); return devices.values().stream().map(StoredDevice::device).filter(d -> d.tenantId().equals(tenantId)).toList(); }
    public synchronized EdgeDevice approve(String adminKey, String id) { requireEnabled(); requireAdmin(adminKey); StoredDevice old = Optional.ofNullable(devices.get(id)).orElseThrow(() -> new IllegalArgumentException("Device was not found.")); EdgeDevice d = old.device(); EdgeDevice approved = new EdgeDevice(d.id(), d.tenantId(), d.displayName(), "ONLINE", d.agentVersion(), d.capabilities(), d.lastHeartbeatAt(), d.createdAt(), Instant.now()); devices.put(id, new StoredDevice(approved, old.credentialHash())); persist(); return approved; }
    private StoredDevice authenticate(String credential) { requireEnabled(); return devices.values().stream().filter(d -> MessageDigest.isEqual(d.credentialHash().getBytes(StandardCharsets.US_ASCII), hash(credential).getBytes(StandardCharsets.US_ASCII))).findFirst().orElseThrow(() -> new AgentAccessException(401, "INVALID_DEVICE_CREDENTIAL", "Device credential is invalid.")); }
    private void requireEnabled() { if (!properties.enabled()) throw new AgentAccessException(404, "EDGE_AGENT_DISABLED", "Edge agent APIs are disabled."); }
    private void requireAdmin(String key) { if (properties.adminKey() == null || properties.adminKey().isBlank() || !MessageDigest.isEqual(properties.adminKey().getBytes(StandardCharsets.UTF_8), Objects.requireNonNullElse(key, "").getBytes(StandardCharsets.UTF_8))) throw new AgentAccessException(403, "ADMIN_AUTH_REQUIRED", "A valid edge-agent admin key is required."); }
    private void persist() { try { json.writeValue(store.resolve("devices.json").toFile(), devices); } catch (IOException e) { throw new IllegalStateException("Device registry could not be stored."); } }
    private String secret() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static Map<String, Object> safeMap(Map<String, Object> map) { return map == null ? Map.of() : Map.copyOf(map); }
    private record Token(String tenantId, Instant expiresAt) { }
    public record StoredDevice(EdgeDevice device, String credentialHash) { }
}
