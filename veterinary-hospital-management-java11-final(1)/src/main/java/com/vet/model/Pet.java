package com.vet.model;

import javax.persistence.*;

@Entity
@Table(name="pets")
public class Pet {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false) private String name;
    private String species, breed, gender, color, allergies, medicalConditions;
    private Integer age;
    private Double weight;
    @ManyToOne(optional=false) @JoinColumn(name="owner_id")
    private User owner;

    public Pet(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getSpecies(){return species;} public void setSpecies(String v){species=v;}
    public String getBreed(){return breed;} public void setBreed(String v){breed=v;}
    public String getGender(){return gender;} public void setGender(String v){gender=v;}
    public String getColor(){return color;} public void setColor(String v){color=v;}
    public String getAllergies(){return allergies;} public void setAllergies(String v){allergies=v;}
    public String getMedicalConditions(){return medicalConditions;} public void setMedicalConditions(String v){medicalConditions=v;}
    public Integer getAge(){return age;} public void setAge(Integer v){age=v;}
    public Double getWeight(){return weight;} public void setWeight(Double v){weight=v;}
    public User getOwner(){return owner;} public void setOwner(User v){owner=v;}
}
