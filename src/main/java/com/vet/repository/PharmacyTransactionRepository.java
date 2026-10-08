package com.vet.repository;
import com.vet.model.*;
import org.springframework.data.jpa.repository.*;
import java.time.*;
import java.util.*;
public interface PharmacyTransactionRepository extends JpaRepository<PharmacyTransaction,Long>{
    List<PharmacyTransaction> findTop100ByOrderByDispensingDateDesc();
    long countByDispensingDateBetween(LocalDateTime start, LocalDateTime end);
    long countByDispensingDateGreaterThanEqual(LocalDateTime start);
}
