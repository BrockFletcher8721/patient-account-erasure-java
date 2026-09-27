package dev.healthtech.deletion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class InfraiIdentityClient implements IdentityControl {
    private final DeletionConfig config;
    private final HttpClient http;

    public InfraiIdentityClient(DeletionConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build());
    }

    InfraiIdentityClient(DeletionConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    @Override
    public List<String> listSessionIds(String userId) {
        Map<String, Object> envelope = call("GET", "/v1/auth/session/list_for_user/" + segment(userId));
        Object data = envelope.get("data");
        List<?> sessions;
        if (data instanceof List<?> list) {
            sessions = list;
        } else if (data instanceof Map<?, ?> map && map.get("sessions") instanceof List<?> list) {
            sessions = list;
        } else {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (Object session : sessions) {
            if (session instanceof Map<?, ?> item && item.get("id") instanceof String id) ids.add(id);
        }
        return List.copyOf(ids);
    }

    @Override
    public void revokeSession(String sessionId) {
        call("POST", "/v1/auth/session/revoke/" + segment(sessionId));
    }

    @Override
    public void revokeCredential(String credentialId) {
        call("DELETE", "/v1/account/keys/revoke/" + segment(credentialId));
    }

    private Map<String, Object> call(String method, String path) {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpResponse<String> response = send(method, path);
            Map<String, Object> envelope = envelope(response.body());
            if (response.statusCode() == 429 && attempt < 3) {
                pause(retryDelay(response, attempt));
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) throw apiError(envelope, response.statusCode());
            if (response.statusCode() >= 500) throw new InfraiException("REMOTE_SERVICE_ERROR", "Remote service error", response.statusCode());
            return envelope;
        }
        throw new IllegalStateException("Retry loop exhausted");
    }

    private HttpResponse<String> send(String method, String path) {
        HttpRequest request = HttpRequest.newBuilder(config.baseUri().resolve(path))
                .timeout(config.requestTimeout())
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Accept", "application/json")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("Transport error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> envelope(String body) {
        Object decoded = Json.parse(body);
        if (!(decoded instanceof Map<?, ?> map)) throw new IllegalArgumentException("Expected JSON envelope");
        return (Map<String, Object>) map;
    }

    private static InfraiException apiError(Map<String, Object> envelope, int status) {
        Object raw = envelope.get("error");
        if (raw instanceof Map<?, ?> error) {
            Object rawCode = error.get("code");
            Object rawMessage = error.get("message");
            String code = rawCode == null ? "REQUEST_REJECTED" : String.valueOf(rawCode);
            String message = rawMessage == null ? "Request rejected" : String.valueOf(rawMessage);
            return new InfraiException(code, message, status);
        }
        return new InfraiException("REQUEST_REJECTED", "Request rejected", status);
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> {
                    try { return Duration.ofSeconds(Long.parseLong(value)); }
                    catch (NumberFormatException ignored) { return Duration.ofSeconds(1L << attempt); }
                })
                .orElse(Duration.ofSeconds(1L << attempt));
    }

    private static void pause(Duration delay) {
        try { Thread.sleep(delay.toMillis()); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Interrupted", e); }
    }

    private static String segment(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }
}
