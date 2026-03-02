package com.mindcarex.mindcarex.repository;

import com.mindcarex.mindcarex.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    Optional<Doctor> findByUser_Id(UUID userId);

    Optional<Doctor> findByLicenseNumber(String licenseNumber);
}