package dev.healthtech.deletion;

public final class AccountDeletionController {
    private final PatientDeletionService service;

    public AccountDeletionController(PatientDeletionService service) { this.service = service; }

    public Response delete(String patientId, String userId, String credentialId) {
        try {
            return new Response(200, service.delete(new PatientDeletionService.DeletionCommand(patientId, userId, credentialId)));
        } catch (InfraiException error) {
            int status = error.statusCode() >= 400 && error.statusCode() < 500 ? error.statusCode() : 502;
            return new Response(status, new ErrorBody(error.code(), error.getMessage()));
        } catch (IllegalArgumentException error) {
            return new Response(400, new ErrorBody("INVALID_REQUEST", error.getMessage()));
        }
    }

    public record Response(int status, Object body) {}
    public record ErrorBody(String code, String message) {}
}
