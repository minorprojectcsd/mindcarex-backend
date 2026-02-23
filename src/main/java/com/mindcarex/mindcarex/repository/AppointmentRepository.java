package com.mindcarex.mindcarex.repository;
import java.util.*;
import com.mindcarex.mindcarex.entity.Appointment;
import com.mindcarex.mindcarex.entity.Doctor;
import com.mindcarex.mindcarex.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByDoctorId(UUID doctorId);

    List<Appointment> findByPatientId(UUID patientId);

    List<Appointment> findByPatient(Patient patient);

    List<Appointment> findByDoctor(Doctor doctor);


}
