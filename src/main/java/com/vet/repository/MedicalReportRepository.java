package com.vet.repository;
import com.vet.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MedicalReportRepository extends JpaRepository<MedicalReport,Long> {
    List<MedicalReport> findByPetAndCompletedTrue(Pet pet);
    List<MedicalReport> findByVeterinarian(User vet);
}
