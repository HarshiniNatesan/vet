package com.vet.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;

@Entity
@Table(name="users")
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String username;

    @JsonIgnore
    @Column(nullable=false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Role role;

    private String fullName;
    private String address;
    private String email;
    private String phone;
    private String specialization;
    private String qualification;
    private String availabilityStatus;
    @Column(nullable=false, columnDefinition="TINYINT(1) DEFAULT 1") private Boolean active = true;
    @Column(name="created_at") private java.time.LocalDateTime createdAt;
    @Column(name="last_login") private java.time.LocalDateTime lastLogin;

    @ManyToOne @JoinColumn(name="branch_id")
    private Branch branch;

    @Column(name="consultation_fee", precision=10, scale=2)
    private java.math.BigDecimal consultationFee;

    public User() {}

    @PrePersist public void initialize(){ if(createdAt==null) createdAt=java.time.LocalDateTime.now(); if(active==null) active=true; }

    public User(String username, String password, Role role, String fullName, String email, String phone) {
        this.username=username;
        this.password=password;
        this.role=role;
        this.fullName=fullName;
        this.email=email;
        this.phone=phone;
    }

    public Long getId(){return id;}
    public String getUsername(){return username;}
    public String getPassword(){return password;}
    public Role getRole(){return role;}
    public String getFullName(){return fullName;}
    public String getAddress(){return address;}
    public String getEmail(){return email;}
    public String getPhone(){return phone;}
    public String getSpecialization(){return specialization;}
    public String getQualification(){return qualification;}
    public String getAvailabilityStatus(){return availabilityStatus;}
    public Boolean getActive(){return active;}
    public java.time.LocalDateTime getCreatedAt(){return createdAt;}
    public java.time.LocalDateTime getLastLogin(){return lastLogin;}
    public Branch getBranch(){return branch;}
    public java.math.BigDecimal getConsultationFee(){return consultationFee;}

    public void setId(Long id){this.id=id;}
    public void setUsername(String v){username=v;}
    public void setPassword(String v){password=v;}
    public void setRole(Role v){role=v;}
    public void setFullName(String v){fullName=v;}
    public void setAddress(String v){address=v;}
    public void setEmail(String v){email=v;}
    public void setPhone(String v){phone=v;}
    public void setSpecialization(String v){specialization=v;}
    public void setQualification(String v){qualification=v;}
    public void setAvailabilityStatus(String v){availabilityStatus=v;}
    public void setActive(Boolean v){active=v;}
    public void setCreatedAt(java.time.LocalDateTime v){createdAt=v;}
    public void setLastLogin(java.time.LocalDateTime v){lastLogin=v;}
    public void setBranch(Branch v){branch=v;}
    public void setConsultationFee(java.math.BigDecimal v){consultationFee=v;}
}
