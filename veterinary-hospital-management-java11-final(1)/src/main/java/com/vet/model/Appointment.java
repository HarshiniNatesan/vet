package com.vet.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name="appointments")
public class Appointment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="pet_id") private Pet pet;
    @ManyToOne(optional=false) @JoinColumn(name="owner_id") private User owner;
    @ManyToOne @JoinColumn(name="veterinarian_id") private User veterinarian;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    @Column(length=2000) private String reason;
    @Enumerated(EnumType.STRING) private AppointmentPriority priority;
    @Enumerated(EnumType.STRING) private AppointmentStatus status;

    public Appointment(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Pet getPet(){return pet;} public void setPet(Pet v){pet=v;}
    public User getOwner(){return owner;} public void setOwner(User v){owner=v;}
    public User getVeterinarian(){return veterinarian;} public void setVeterinarian(User v){veterinarian=v;}
    public LocalDate getAppointmentDate(){return appointmentDate;} public void setAppointmentDate(LocalDate v){appointmentDate=v;}
    public LocalTime getAppointmentTime(){return appointmentTime;} public void setAppointmentTime(LocalTime v){appointmentTime=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
    public AppointmentPriority getPriority(){return priority;} public void setPriority(AppointmentPriority v){priority=v;}
    public AppointmentStatus getStatus(){return status;} public void setStatus(AppointmentStatus v){status=v;}
}
