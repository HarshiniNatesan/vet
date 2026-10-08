package com.vet.repository;
import com.vet.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface PaymentRepository extends JpaRepository<Payment,Long> {
    Optional<Payment> findByProviderAndProviderOrderId(String provider, String providerOrderId);
    Optional<Payment> findByProviderOrderId(String providerOrderId);
    List<Payment> findByOwnerOrderByCreatedAtDesc(User owner);
    List<Payment> findTop100ByOrderByCreatedAtDesc();
    List<Payment> findByBranchOrderByCreatedAtDesc(Branch branch);
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
    Optional<Payment> findByAppointment(Appointment appointment);
    Optional<Payment> findByPrescription(Prescription prescription);
}
