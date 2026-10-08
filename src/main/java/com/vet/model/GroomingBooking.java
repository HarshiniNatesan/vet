package com.vet.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name="grooming_bookings")
public class GroomingBooking {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false)
    @JoinColumn(name="pet_id")
    private Pet pet;

    @ManyToOne(optional=false)
    @JoinColumn(name="owner_id")
    private User owner;

    @Column(nullable=false)
    private String serviceName;

    @Column(nullable=false)
    private LocalDate bookingDate;

    @Column(nullable=false)
    private LocalTime bookingTime;

    private String status;

    public GroomingBooking(){}

    public Long getId(){return id;}
    public void setId(Long v){id=v;}

    public Pet getPet(){return pet;}
    public void setPet(Pet v){pet=v;}

    public User getOwner(){return owner;}
    public void setOwner(User v){owner=v;}

    public String getServiceName(){return serviceName;}
    public void setServiceName(String v){serviceName=v;}

    public LocalDate getBookingDate(){return bookingDate;}
    public void setBookingDate(LocalDate v){bookingDate=v;}

    public LocalTime getBookingTime(){return bookingTime;}
    public void setBookingTime(LocalTime v){bookingTime=v;}

    public String getStatus(){return status;}
    public void setStatus(String v){status=v;}
}
