package dev.healthtech.deletion;

import java.util.ArrayList;
import java.util.List;

public final class PatientDeletionServiceTest {
    public static void main(String[] args) {
        List<String> events = new ArrayList<>();
        IdentityControl identity = new IdentityControl() {
            public List<String> listSessionIds(String userId) { events.add("list:" + userId); return List.of("s-1", "s-2"); }
            public void revokeSession(String id) { events.add("session:" + id); }
            public void revokeCredential(String id) { events.add("credential:" + id); }
        };
        PatientRecordStore records = new PatientRecordStore() {
            public List<Appointment> futureAppointments(String patientId) { return List.of(new Appointment("apt-7", "patient-contact")); }
            public void cancelAppointment(String id) { events.add("cancel:" + id); }
            public void deletePatient(String id) { events.add("delete:" + id); }
        };
        PatientNotifier notifier = (contact, id) -> events.add("notify:" + id + ":Appointment cancelled");

        PatientDeletionService.DeletionReceipt receipt = new PatientDeletionService(identity, records, notifier)
                .delete(new PatientDeletionService.DeletionCommand("patient-42", "user-42", "key-42"));

        List<String> expected = List.of("cancel:apt-7", "notify:apt-7:Appointment cancelled", "list:user-42",
                "session:s-1", "session:s-2", "credential:key-42", "delete:patient-42");
        if (!events.equals(expected)) throw new AssertionError("Unexpected order: " + events);
        if (!receipt.state().equals("DELETED") || receipt.sessionsRevoked() != 2 || receipt.appointmentsCancelled() != 1) {
            throw new AssertionError("Unexpected receipt: " + receipt);
        }
        System.out.println("PASS patient deletion revokes access before deleting the record");
    }
}
