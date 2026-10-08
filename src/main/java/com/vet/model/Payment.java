
package com.vet.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_branch", columnList = "branch_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_created", columnList = "created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_order", columnNames = "provider_order_id")
})
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "veterinarian_id")
    private User veterinarian;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @ManyToOne
    @JoinColumn(name = "pharmacy_transaction_id")
    private PharmacyTransaction pharmacyTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 40)
    private PaymentType paymentType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(nullable = false, length = 40)
    private String provider = "UPI_QR";

    @Column(name = "provider_order_id", unique = true, length = 100)
    private String providerOrderId;

    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "qr_code_id", length = 100)
    private String qrCodeId;

    @Column(name = "qr_image_url", length = 1000)
    private String qrImageUrl;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "reported_by", length = 255)
    private String reportedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "last_webhook_event_id", length = 100)
    private String lastWebhookEventId;

    @Version
    private Long version;

    @PrePersist
    public void init() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch v) { branch = v; }

    public Pet getPet() { return pet; }
    public void setPet(Pet v) { pet = v; }

    public User getOwner() { return owner; }
    public void setOwner(User v) { owner = v; }

    public User getVeterinarian() { return veterinarian; }
    public void setVeterinarian(User v) { veterinarian = v; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment v) { appointment = v; }

    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription v) { prescription = v; }

    public PharmacyTransaction getPharmacyTransaction() { return pharmacyTransaction; }
    public void setPharmacyTransaction(PharmacyTransaction v) { pharmacyTransaction = v; }

    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType v) { paymentType = v; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { amount = v; }

    public String getCurrency() { return currency; }
    public void setCurrency(String v) { currency = v; }

    public String getProvider() { return provider; }
    public void setProvider(String v) { provider = v; }

    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String v) { providerOrderId = v; }

    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String v) { providerPaymentId = v; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String v) { transactionId = v; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus v) { status = v; }

    public String getQrCodeId() { return qrCodeId; }
    public void setQrCodeId(String v) { qrCodeId = v; }

    public String getQrImageUrl() { return qrImageUrl; }
    public void setQrImageUrl(String v) { qrImageUrl = v; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String v) { failureReason = v; }

    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String v) { reportedBy = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime v) { paidAt = v; }

    public String getLastWebhookEventId() { return lastWebhookEventId; }
    public void setLastWebhookEventId(String v) { lastWebhookEventId = v; }

    public Long getVersion() { return version; }
    public void setVersion(Long v) { version = v; }
}
