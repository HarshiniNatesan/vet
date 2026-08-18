package com.vet.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="vaccination_records")
public class VaccinationRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="pet_id") private Pet pet;
    private String vaccineName;
    private LocalDate previousDate, nextDueDate;
    @ManyToOne @JoinColumn(name="veterinarian_id") private User veterinarian;
    public VaccinationRecord(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Pet getPet(){return pet;} public void setPet(Pet v){pet=v;}
    public String getVaccineName(){return vaccineName;} public void setVaccineName(String v){vaccineName=v;}
    public LocalDate getPreviousDate(){return previousDate;} public void setPreviousDate(LocalDate v){previousDate=v;}
    public LocalDate getNextDueDate(){return nextDueDate;} public void setNextDueDate(LocalDate v){nextDueDate=v;}
    public User getVeterinarian(){return veterinarian;} public void setVeterinarian(User v){veterinarian=v;}
}
