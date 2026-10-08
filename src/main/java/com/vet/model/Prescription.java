package com.vet.model;

import javax.persistence.*;

@Entity
@Table(name="prescriptions")
public class Prescription {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="pet_id") private Pet pet;
    @ManyToOne(optional=false) @JoinColumn(name="veterinarian_id") private User veterinarian;
    @ManyToOne @JoinColumn(name="appointment_id") private Appointment appointment;
    private String medicineName, dosage, frequency, duration;
    @Column(nullable=false) private String status = "PENDING";
    @Column(name="required_quantity") private Integer requiredQuantity = 1;
    @ManyToOne @JoinColumn(name="branch_id") private Branch branch;
    @Column(length=2000) private String instructions;

    public Prescription(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Pet getPet(){return pet;} public void setPet(Pet v){pet=v;}
    public User getVeterinarian(){return veterinarian;} public void setVeterinarian(User v){veterinarian=v;}
    public Appointment getAppointment(){return appointment;} public void setAppointment(Appointment v){appointment=v;}
    public String getMedicineName(){return medicineName;} public void setMedicineName(String v){medicineName=v;}
    public String getDosage(){return dosage;} public void setDosage(String v){dosage=v;}
    public String getFrequency(){return frequency;} public void setFrequency(String v){frequency=v;}
    public String getDuration(){return duration;} public void setDuration(String v){duration=v;}
    public String getInstructions(){return instructions;} public void setInstructions(String v){instructions=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Integer getRequiredQuantity(){return requiredQuantity;} public void setRequiredQuantity(Integer v){requiredQuantity=v;}
    public Branch getBranch(){return branch;} public void setBranch(Branch v){branch=v;}
}
