package com.vet.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="vaccination_records")
public class VaccinationRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) @JoinColumn(name="pet_id")
    private Pet pet;

    private String vaccineName;
    private String vaccineType;
    private LocalDate administeredDate;

    /* Kept for compatibility with the original schema/application. */
    private LocalDate previousDate;
    private LocalDate nextDueDate;

    @ManyToOne @JoinColumn(name="veterinarian_id")
    private User veterinarian;

    @Column(length=2000)
    private String notes;

    private String status;

    public VaccinationRecord(){}

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Pet getPet(){return pet;} public void setPet(Pet v){pet=v;}
    public String getVaccineName(){return vaccineName;} public void setVaccineName(String v){vaccineName=v;}
    public String getVaccineType(){return vaccineType;} public void setVaccineType(String v){vaccineType=v;}
    public LocalDate getAdministeredDate(){return administeredDate;} public void setAdministeredDate(LocalDate v){administeredDate=v;}
    public LocalDate getPreviousDate(){return previousDate;} public void setPreviousDate(LocalDate v){previousDate=v;}
    public LocalDate getNextDueDate(){return nextDueDate;} public void setNextDueDate(LocalDate v){nextDueDate=v;}
    public User getVeterinarian(){return veterinarian;} public void setVeterinarian(User v){veterinarian=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
