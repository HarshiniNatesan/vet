package com.vet.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="medicines", indexes={
    @Index(name="idx_medicine_name", columnList="name"),
    @Index(name="idx_medicine_expiry", columnList="expiry_date")
})
public class Medicine {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String name;
    private String category;
    @Column(length=2000) private String description;
    private String manufacturer;
    @Column(name="batch_number") private String batchNumber;
    @Column(name="expiry_date") private LocalDate expiryDate;
    @Column(nullable=false) private Integer quantity=0;
    @Column(name="reorder_level", nullable=false) private Integer reorderLevel=0;
    @Column(name="unit_price", nullable=false) private Double unitPrice=0.0;
    @Column(nullable=false) private Boolean active=true;
    @Column(name="last_updated") private LocalDate lastUpdated;
    @ManyToOne @JoinColumn(name="branch_id") private Branch branch;

    @PrePersist @PreUpdate public void touch(){ lastUpdated=LocalDate.now(); }

    @Transient public String getStockStatus(){
        if(expiryDate!=null && expiryDate.isBefore(LocalDate.now())) return "EXPIRED";
        if(quantity==null || quantity<=0) return "OUT_OF_STOCK";
        if(reorderLevel!=null && quantity<=reorderLevel) return "LOW_STOCK";
        return "IN_STOCK";
    }
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getManufacturer(){return manufacturer;} public void setManufacturer(String v){manufacturer=v;}
    public String getBatchNumber(){return batchNumber;} public void setBatchNumber(String v){batchNumber=v;}
    public LocalDate getExpiryDate(){return expiryDate;} public void setExpiryDate(LocalDate v){expiryDate=v;}
    public Integer getQuantity(){return quantity;} public void setQuantity(Integer v){quantity=v;}
    public Integer getReorderLevel(){return reorderLevel;} public void setReorderLevel(Integer v){reorderLevel=v;}
    public Double getUnitPrice(){return unitPrice;} public void setUnitPrice(Double v){unitPrice=v;}
    public Boolean getActive(){return active;} public void setActive(Boolean v){active=v;}
    public LocalDate getLastUpdated(){return lastUpdated;} public void setLastUpdated(LocalDate v){lastUpdated=v;}
    public Branch getBranch(){return branch;} public void setBranch(Branch v){branch=v;}
}
