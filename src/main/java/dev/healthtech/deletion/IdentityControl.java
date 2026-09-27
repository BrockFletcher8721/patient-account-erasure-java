package dev.healthtech.deletion;

import java.util.List;

public interface IdentityControl {
    List<String> listSessionIds(String userId);
    void revokeSession(String sessionId);
    void revokeCredential(String credentialId);
}
