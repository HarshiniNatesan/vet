package com.vet.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pets")
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String species;
    private String breed;
    private String gender;
    private String color;
    private String allergies;
    private String medicalConditions;
    private Integer age;
    private Double weight;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /* IMPORTANT: this matches the existing MySQL pets.veterinarian_id column. */
    @ManyToOne
    @JoinColumn(name = "veterinarian_id")
    private User assignedVeterinarian;

    @Column(name="report_token_hash", unique=true, length=64)
    private String reportTokenHash;
    @Column(name="report_token_issued_at") private LocalDateTime reportTokenIssuedAt;
    @Column(name="report_token_expires_at") private LocalDateTime reportTokenExpiresAt;
    @Column(name="report_token_revoked", nullable=false) private Boolean reportTokenRevoked = false;

    public Pet() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /** Human-friendly patient identifier, e.g. PET-000001. */
    @Transient
    public String getPetCode() {
        return id == null ? null : String.format("PET-%06d", id);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getMedicalConditions() { return medicalConditions; }
    public void setMedicalConditions(String medicalConditions) { this.medicalConditions = medicalConditions; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public User getAssignedVeterinarian() { return assignedVeterinarian; }
    public void setAssignedVeterinarian(User assignedVeterinarian) {
        this.assignedVeterinarian = assignedVeterinarian;
    }

    public String getReportTokenHash(){return reportTokenHash;}
    public void setReportTokenHash(String v){reportTokenHash=v;}
    public LocalDateTime getReportTokenIssuedAt(){return reportTokenIssuedAt;}
    public void setReportTokenIssuedAt(LocalDateTime v){reportTokenIssuedAt=v;}
    public LocalDateTime getReportTokenExpiresAt(){return reportTokenExpiresAt;}
    public void setReportTokenExpiresAt(LocalDateTime v){reportTokenExpiresAt=v;}
    public Boolean getReportTokenRevoked(){return reportTokenRevoked;}
    public void setReportTokenRevoked(Boolean v){reportTokenRevoked=v;}
}
