package com.vet.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="medical_reports")
public class MedicalReport {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="pet_id") private Pet pet;
    @ManyToOne(optional=false) @JoinColumn(name="veterinarian_id") private User veterinarian;
    @ManyToOne @JoinColumn(name="appointment_id") private Appointment appointment;
    private LocalDate visitDate;

    @Lob private String symptoms;
    @Lob private String observations;
    @Lob private String diagnosis;
    @Lob private String treatment;
    private String followUpDate;
    private Boolean surgeryRequired;
    @Lob private String surgeryDetails;
    @Lob private String notes;
    private boolean completed;

    public MedicalReport(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Pet getPet(){return pet;} public void setPet(Pet v){pet=v;}
    public User getVeterinarian(){return veterinarian;} public void setVeterinarian(User v){veterinarian=v;}
    public Appointment getAppointment(){return appointment;} public void setAppointment(Appointment v){appointment=v;}
    public LocalDate getVisitDate(){return visitDate;} public void setVisitDate(LocalDate v){visitDate=v;}
    public String getSymptoms(){return symptoms;} public void setSymptoms(String v){symptoms=v;}
    public String getObservations(){return observations;} public void setObservations(String v){observations=v;}
    public String getDiagnosis(){return diagnosis;} public void setDiagnosis(String v){diagnosis=v;}
    public String getTreatment(){return treatment;} public void setTreatment(String v){treatment=v;}
    public String getFollowUpDate(){return followUpDate;} public void setFollowUpDate(String v){followUpDate=v;}
    public Boolean getSurgeryRequired(){return surgeryRequired;} public void setSurgeryRequired(Boolean v){surgeryRequired=v;}
    public String getSurgeryDetails(){return surgeryDetails;} public void setSurgeryDetails(String v){surgeryDetails=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public boolean isCompleted(){return completed;} public void setCompleted(boolean v){completed=v;}
}
