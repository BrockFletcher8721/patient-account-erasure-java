package dev.healthtech.deletion;

import java.util.List;

public final class PatientDeletionService {
    private final IdentityControl identityControl;
    private final PatientRecordStore records;
    private final PatientNotifier notifier;

    public PatientDeletionService(IdentityControl identityControl, PatientRecordStore records, PatientNotifier notifier) {
        this.identityControl = identityControl;
        this.records = records;
        this.notifier = notifier;
    }

    public DeletionReceipt delete(DeletionCommand command) {
        List<PatientRecordStore.Appointment> appointments = records.futureAppointments(command.patientId());
        for (PatientRecordStore.Appointment appointment : appointments) {
            records.cancelAppointment(appointment.id());
            notifier.appointmentCancelled(appointment.patientContact(), appointment.id());
        }

        List<String> sessions = identityControl.listSessionIds(command.userId());
        sessions.forEach(identityControl::revokeSession);
        identityControl.revokeCredential(command.credentialId());
        records.deletePatient(command.patientId());
        return new DeletionReceipt(command.patientId(), appointments.size(), sessions.size(), "DELETED");
    }

    public record DeletionCommand(String patientId, String userId, String credentialId) {
        public DeletionCommand {
            if (patientId.isBlank() || userId.isBlank() || credentialId.isBlank()) {
                throw new IllegalArgumentException("patientId, userId and credentialId are required");
            }
        }
    }

    public record DeletionReceipt(String patientId, int appointmentsCancelled, int sessionsRevoked, String state) {}
}
