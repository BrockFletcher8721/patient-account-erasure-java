package dev.healthtech.deletion;

public interface PatientNotifier {
    void appointmentCancelled(String contact, String appointmentId);
}
