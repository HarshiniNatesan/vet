package com.vet.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="pharmacy_transactions", indexes={
    @Index(name="idx_pharmacy_txn_date", columnList="dispensing_date"),
    @Index(name="idx_pharmacy_txn_prescription", columnList="prescription_id")
})
public class PharmacyTransaction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="prescription_id", nullable=false) private Prescription prescription;
    @ManyToOne(optional=false) @JoinColumn(name="pet_id", nullable=false) private Pet pet;
    @ManyToOne(optional=false) @JoinColumn(name="medicine_id", nullable=false) private Medicine medicine;
    @ManyToOne(optional=false) @JoinColumn(name="pharmacy_staff_id", nullable=false) private User pharmacyStaff;
    @Column(name="quantity_dispensed", nullable=false) private Integer quantityDispensed;
    @Column(name="batch_number") private String batchNumber;
    @Column(name="unit_price", nullable=false) private Double unitPrice;
    @Column(name="total_amount", nullable=false) private Double totalAmount;
    @Column(name="dispensing_date", nullable=false) private LocalDateTime dispensingDate;
    @Column(name="dispensing_status", nullable=false) private String dispensingStatus;
    @ManyToOne @JoinColumn(name="branch_id") private Branch branch;

    public Long getId(){return id;} public Prescription getPrescription(){return prescription;} public void setPrescription(Prescription v){prescription=v;}
    public void setId(Long v){id=v;} public Pet getPet(){return pet;} public void setPet(Pet v){pet=v;}
    public Medicine getMedicine(){return medicine;} public void setMedicine(Medicine v){medicine=v;}
    public User getPharmacyStaff(){return pharmacyStaff;} public void setPharmacyStaff(User v){pharmacyStaff=v;}
    public Integer getQuantityDispensed(){return quantityDispensed;} public void setQuantityDispensed(Integer v){quantityDispensed=v;}
    public String getBatchNumber(){return batchNumber;} public void setBatchNumber(String v){batchNumber=v;}
    public Double getUnitPrice(){return unitPrice;} public void setUnitPrice(Double v){unitPrice=v;}
    public Double getTotalAmount(){return totalAmount;} public void setTotalAmount(Double v){totalAmount=v;}
    public LocalDateTime getDispensingDate(){return dispensingDate;} public void setDispensingDate(LocalDateTime v){dispensingDate=v;}
    public String getDispensingStatus(){return dispensingStatus;} public void setDispensingStatus(String v){dispensingStatus=v;}
    public Branch getBranch(){return branch;} public void setBranch(Branch v){branch=v;}
}
