package dev.healthtech.deletion;

import java.util.List;

public interface PatientRecordStore {
    List<Appointment> futureAppointments(String patientId);
    void cancelAppointment(String appointmentId);
    void deletePatient(String patientId);

    record Appointment(String id, String patientContact) {}
}
