package com.vet.repository;

import com.vet.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PrescriptionRepository extends JpaRepository<Prescription,Long> {
    List<Prescription> findByVeterinarian(User vet);
    List<Prescription> findByPetOrderByIdDesc(Pet pet);
}
