package com.vet.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="facility_records")
public class FacilityRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) @JoinColumn(name="pet_id")
    private Pet pet;

    @ManyToOne(optional=false) @JoinColumn(name="owner_id")
    private User owner;

    @ManyToOne(optional=false) @JoinColumn(name="veterinarian_id")
    private User veterinarian;

    @ManyToOne @JoinColumn(name="appointment_id")
    private Appointment appointment;

    private String serviceName;
    private LocalDate serviceDate;

    @Column(length=2000)
    private String description;

    private String status;

    public FacilityRecord(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Pet getPet(){return pet;} public void setPet(Pet v){pet=v;}
    public User getOwner(){return owner;} public void setOwner(User v){owner=v;}
    public User getVeterinarian(){return veterinarian;} public void setVeterinarian(User v){veterinarian=v;}
    public Appointment getAppointment(){return appointment;} public void setAppointment(Appointment v){appointment=v;}
    public String getServiceName(){return serviceName;} public void setServiceName(String v){serviceName=v;}
    public LocalDate getServiceDate(){return serviceDate;} public void setServiceDate(LocalDate v){serviceDate=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
