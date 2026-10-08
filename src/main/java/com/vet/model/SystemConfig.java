package com.vet.model;

import javax.persistence.*;

@Entity
@Table(name="system_config")
public class SystemConfig {
    @Id private Long id=1L;
    private String hospitalName;
    private String contactInformation;
    private String address;
    private String operatingHours;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getHospitalName(){return hospitalName;} public void setHospitalName(String v){hospitalName=v;}
    public String getContactInformation(){return contactInformation;} public void setContactInformation(String v){contactInformation=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getOperatingHours(){return operatingHours;} public void setOperatingHours(String v){operatingHours=v;}
}
