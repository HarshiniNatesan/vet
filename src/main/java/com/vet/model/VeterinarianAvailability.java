package com.vet.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "veterinarian_availability",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"veterinarian_id", "availability_date"}
    )
)
public class VeterinarianAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "veterinarian_id", nullable = false)
    private User veterinarian;

    @Column(name = "availability_date", nullable = false)
    private LocalDate availabilityDate;

    @Column(nullable = false, length = 50)
    private String status;

    /* No notes field: the existing MySQL table has only four columns. */

    public VeterinarianAvailability() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getVeterinarian() { return veterinarian; }
    public void setVeterinarian(User veterinarian) { this.veterinarian = veterinarian; }

    public LocalDate getAvailabilityDate() { return availabilityDate; }
    public void setAvailabilityDate(LocalDate availabilityDate) {
        this.availabilityDate = availabilityDate;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
