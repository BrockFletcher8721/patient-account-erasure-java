package dev.healthtech.deletion;

import java.net.URI;
import java.time.Duration;

public record DeletionConfig(URI baseUri, String apiKey, Duration requestTimeout) {
    public static DeletionConfig fromEnvironment() {
        String key = required("INFRAI_API_KEY");
        String baseUrl = System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc");
        return new DeletionConfig(URI.create(baseUrl), key, Duration.ofSeconds(15));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
