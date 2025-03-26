package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.model.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    boolean existsByEmail(String email);
}
