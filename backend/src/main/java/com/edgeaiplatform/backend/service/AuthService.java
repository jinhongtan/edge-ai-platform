package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.config.AuthProperties;
import com.edgeaiplatform.backend.dto.*;
import com.edgeaiplatform.backend.exception.AuthException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.Instant;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Service;

/** File-backed authentication for local development. Tokens intentionally do not survive a restart. */
@Service
public class AuthService {
    private static final int ITERATIONS = 210_000;
    private final AuthProperties properties; private final ObjectMapper json; private final SecureRandom random = new SecureRandom();
    private final Map<String, Account> accounts = new HashMap<>(); private final Map<String, Session> sessions = new HashMap<>(); private Path file;
    public AuthService(AuthProperties properties, ObjectMapper json) { this.properties = properties; this.json = json; }
    @PostConstruct synchronized void load() throws IOException { Files.createDirectories(properties.directory()); file = properties.directory().resolve("accounts.json"); if (Files.isRegularFile(file)) accounts.putAll(json.readValue(file.toFile(), new TypeReference<Map<String, Account>>() {})); }
    public synchronized AuthSession register(RegisterRequest request) {
        String email = normalize(request.email());
        if (accounts.containsKey(email)) throw new AuthException(409, "EMAIL_ALREADY_REGISTERED", "An account with that email already exists.");
        byte[] salt = bytes(16); Account account = new Account(UUID.randomUUID().toString(), email, request.organization().trim(), base64(salt), base64(derive(request.password(), salt)), Instant.now());
        accounts.put(email, account); persist(); return createSession(account);
    }
    public synchronized AuthSession login(LoginRequest request) {
        Account account = accounts.get(normalize(request.email()));
        if (account == null || !MessageDigest.isEqual(decode(account.passwordHash()), derive(request.password(), decode(account.salt())))) throw new AuthException(401, "INVALID_CREDENTIALS", "Email or password is incorrect.");
        return createSession(account);
    }
    public synchronized UserIdentity currentUser(String token) {
        Session session = sessions.get(hash(token));
        if (session == null || session.expiresAt().isBefore(Instant.now())) { sessions.remove(hash(token)); throw new AuthException(401, "INVALID_SESSION", "Session is invalid or expired."); }
        return session.user();
    }
    private AuthSession createSession(Account account) { String token = base64(bytes(32)); Instant expires = Instant.now().plus(properties.sessionTtl()); UserIdentity user = new UserIdentity(account.id(), account.email(), account.organization()); sessions.put(hash(token), new Session(user, expires)); return new AuthSession(token, expires, user); }
    private void persist() { try { json.writeValue(file.toFile(), accounts); } catch (IOException exception) { throw new IllegalStateException("Account could not be stored."); } }
    private byte[] derive(String password, byte[] salt) { try { KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256); return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); } catch (Exception exception) { throw new IllegalStateException("Password hashing is unavailable.", exception); } }
    private byte[] bytes(int size) { byte[] value = new byte[size]; random.nextBytes(value); return value; }
    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static String base64(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private static byte[] decode(String value) { return Base64.getUrlDecoder().decode(value); }
    private static String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception exception) { throw new IllegalStateException(exception); } }
    public record Account(String id, String email, String organization, String salt, String passwordHash, Instant createdAt) { }
    private record Session(UserIdentity user, Instant expiresAt) { }
}
