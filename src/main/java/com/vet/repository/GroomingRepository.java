package com.vet.repository;

import com.vet.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface GroomingRepository extends JpaRepository<GroomingBooking,Long> {
    List<GroomingBooking> findByOwnerOrderByBookingDateAscBookingTimeAsc(User owner);
    List<GroomingBooking> findByBookingDateBetweenOrderByBookingDateAscBookingTimeAsc(java.time.LocalDate start, java.time.LocalDate end);
}
