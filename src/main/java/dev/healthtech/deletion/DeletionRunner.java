package dev.healthtech.deletion;

import java.util.ArrayList;
import java.util.List;

public final class DeletionRunner {
    private DeletionRunner() {}

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: DeletionRunner <patient-id> <user-id> <credential-id>");
            System.exit(2);
        }
        InMemoryRecords records = new InMemoryRecords(args[0]);
        PatientNotifier notifier = (contact, appointmentId) ->
                System.out.printf("notice contact=%s appointment=%s message=Appointment cancelled%n", contact, appointmentId);
        PatientDeletionService service = new PatientDeletionService(
                new InfraiIdentityClient(DeletionConfig.fromEnvironment()), records, notifier);
        AccountDeletionController.Response response = new AccountDeletionController(service).delete(args[0], args[1], args[2]);
        System.out.println(response);
        if (response.status() >= 400) System.exit(1);
    }

    private static final class InMemoryRecords implements PatientRecordStore {
        private final List<Appointment> appointments = new ArrayList<>();
        private boolean deleted;

        private InMemoryRecords(String patientId) {
            appointments.add(new Appointment("next-appointment", "patient:" + patientId));
        }

        public List<Appointment> futureAppointments(String patientId) { return List.copyOf(appointments); }
        public void cancelAppointment(String appointmentId) { appointments.removeIf(item -> item.id().equals(appointmentId)); }
        public void deletePatient(String patientId) { deleted = true; }
        @Override public String toString() { return "InMemoryRecords[deleted=" + deleted + "]"; }
    }
}
