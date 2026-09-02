package com.vet.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name="operations")
public class Operation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) @JoinColumn(name="pet_id")
    private Pet pet;

    @ManyToOne(optional=false) @JoinColumn(name="owner_id")
    private User owner;

    @ManyToOne @JoinColumn(name="veterinarian_id")
    private User veterinarian;

    @Column(nullable=false)
    private String operationType;
    @Column(nullable=false)
    private LocalDate operationDate;
    private LocalTime operationTime;
    private String status;
    @Column(length=2000)
    private String notes;

    public Operation() {}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Pet getPet(){return pet;} public void setPet(Pet v){pet=v;}
    public User getOwner(){return owner;} public void setOwner(User v){owner=v;}
    public User getVeterinarian(){return veterinarian;} public void setVeterinarian(User v){veterinarian=v;}
    public String getOperationType(){return operationType;} public void setOperationType(String v){operationType=v;}
    public LocalDate getOperationDate(){return operationDate;} public void setOperationDate(LocalDate v){operationDate=v;}
    public LocalTime getOperationTime(){return operationTime;} public void setOperationTime(LocalTime v){operationTime=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
