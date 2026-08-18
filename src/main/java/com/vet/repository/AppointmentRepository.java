package com.vet.repository;

import com.vet.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AppointmentRepository extends JpaRepository<Appointment,Long> {

    List<Appointment> findByOwnerOrderByAppointmentDateAscAppointmentTimeAsc(User owner);

    List<Appointment> findByVeterinarianOrderByAppointmentDateAscAppointmentTimeAsc(User vet);
    List<Appointment> findByAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(java.time.LocalDate start, java.time.LocalDate end);

    @Query("select distinct a.pet from Appointment a where a.veterinarian = :vet order by a.pet.name")
    List<Pet> findDistinctPetsByVeterinarian(@Param("vet") User vet);

    @Query("select a from Appointment a order by case a.priority " +
           "when 'EMERGENCY' then 1 when 'HIGH' then 2 when 'NORMAL' then 3 else 4 end, " +
           "a.appointmentDate, a.appointmentTime")
    List<Appointment> findAllPrioritySorted();
}
