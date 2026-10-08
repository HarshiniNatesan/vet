package com.vet.repository;

import com.vet.model.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface OperationRepository extends JpaRepository<Operation,Long> {
    List<Operation> findByOperationDateBetweenOrderByOperationDateAscOperationTimeAsc(LocalDate start, LocalDate end);
}
