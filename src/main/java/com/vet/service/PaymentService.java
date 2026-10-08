package com.vet.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vet.model.*;
import com.vet.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.Base64;

@Service
public class PaymentService {
    private final PaymentRepository payments; private final AppointmentRepository appointments;
    private final PrescriptionRepository prescriptions; private final PharmacyTransactionRepository pharmacyTransactions;
    private final UserRepository users;
    private final String upiId; private final String payeeName;
    private static final Pattern UTR = Pattern.compile("[A-Za-z0-9-]{6,35}");

    public PaymentService(PaymentRepository payments, AppointmentRepository appointments, PrescriptionRepository prescriptions,
                          PharmacyTransactionRepository pharmacyTransactions, UserRepository users,
                          @Value("${payment.upi-id:}") String upiId, @Value("${payment.payee-name:PawsCare Veterinary Hospital}") String payeeName) {
        this.payments=payments; this.appointments=appointments; this.prescriptions=prescriptions;
        this.pharmacyTransactions=pharmacyTransactions; this.users=users; this.upiId=upiId; this.payeeName=payeeName;
    }

    @Transactional
    public Map<String,Object> createConsultationPayment(Long appointmentId, String username) {
        User owner=requireOwner(username); Appointment a=appointments.findById(appointmentId).orElseThrow(()->notFound("Appointment not found."));
        if(a.getOwner()==null || !a.getOwner().getId().equals(owner.getId())) throw forbidden("You can pay only for your own appointment.");
        if(a.getVeterinarian()==null) throw bad("This appointment has no veterinarian assigned.");
        if(a.getStatus()==AppointmentStatus.CANCELLED || a.getStatus()==AppointmentStatus.REJECTED) throw bad("Cancelled or rejected appointments cannot be paid.");
        BigDecimal amount=a.getVeterinarian().getConsultationFee(); if(amount==null||amount.signum()<=0) throw bad("Payment amount is invalid.");
        Payment p=payments.findByAppointment(a).orElse(null);
        if(p==null){p=new Payment();p.setPet(a.getPet());p.setOwner(owner);p.setVeterinarian(a.getVeterinarian());p.setAppointment(a);p.setPaymentType(PaymentType.DOCTOR_CONSULTATION);p.setAmount(amount.setScale(2,RoundingMode.HALF_UP));p.setCurrency("INR");p.setBranch(a.getBranch()!=null?a.getBranch():a.getVeterinarian().getBranch());p.setProvider("UPI_QR");p.setProviderOrderId("VET-CONS-"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase());p=payments.saveAndFlush(p);}
        else if(p.getAmount().compareTo(amount.setScale(2,RoundingMode.HALF_UP))!=0) throw bad("The consultation amount changed. Please contact the hospital.");
        return dto(p);
    }

    @Transactional
    public Map<String,Object> createPharmacyPayment(Long prescriptionId,String username){
        User owner=requireOwner(username); Prescription prescription=prescriptions.findById(prescriptionId).orElseThrow(()->notFound("Prescription not found."));
        if(prescription.getPet()==null||prescription.getPet().getOwner()==null||!prescription.getPet().getOwner().getId().equals(owner.getId())) throw forbidden("You can pay only for your own prescription.");
        List<PharmacyTransaction> tx=pharmacyTransactions.findByPrescription(prescription); if(tx.isEmpty()) throw bad("No pharmacy bill has been generated for this prescription yet.");
        BigDecimal amount=tx.stream().map(PharmacyTransaction::getTotalAmount).filter(Objects::nonNull).map(BigDecimal::valueOf).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
        if(amount.signum()<=0) throw bad("Payment amount is invalid.");
        Payment p=payments.findByPrescription(prescription).orElse(null);
        if(p==null){p=new Payment();p.setPet(prescription.getPet());p.setOwner(owner);p.setVeterinarian(prescription.getVeterinarian());p.setPrescription(prescription);p.setPaymentType(PaymentType.PHARMACY);p.setAmount(amount);p.setCurrency("INR");p.setBranch(prescription.getBranch()!=null?prescription.getBranch():tx.stream().map(PharmacyTransaction::getBranch).filter(Objects::nonNull).findFirst().orElse(null));p.setProvider("UPI_QR");p.setProviderOrderId("VET-PH-"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase());p=payments.saveAndFlush(p);}
        else p.setAmount(amount);
        return dto(p);
    }

    public Map<String,Object> createDynamicQr(Long paymentId,String username){
        Payment p=payments.findById(paymentId).orElseThrow(()->notFound("Payment record was not found.")); authorizePayment(p,username);
        if(p.getStatus()==PaymentStatus.CONFIRMED || p.getStatus()==PaymentStatus.SUCCESS) return dto(p);
        if(p.getAmount()==null||p.getAmount().signum()<=0) throw bad("Payment amount is invalid.");
        if(upiId==null||upiId.trim().isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"UPI account is not configured.");
        try {
            String reference=p.getProviderOrderId()==null?"VET-"+p.getId():p.getProviderOrderId();
            String uri="upi://pay?pa="+enc(upiId.trim())+"&pn="+enc(payeeName)+"&am="+p.getAmount().setScale(2,RoundingMode.HALF_UP).toPlainString()+"&cu=INR&tn="+enc(reference);
            BitMatrix matrix=new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE,320,320);
            ByteArrayOutputStream out=new ByteArrayOutputStream(); MatrixToImageWriter.writeToStream(matrix,"PNG",out);
            p.setQrImageUrl("data:image/png;base64,"+Base64.getEncoder().encodeToString(out.toByteArray())); payments.save(p);
            Map<String,Object> result=dto(p); result.put("upiId",upiId); result.put("payeeName",payeeName); result.put("upiUri",uri); return result;
        } catch(Exception e){ if(e instanceof ResponseStatusException) throw (ResponseStatusException)e; throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Payment QR could not be generated."); }
    }

    @Transactional
    public Map<String,Object> reportPaid(Long id,String username,String utr){
        Payment p=payments.findById(id).orElseThrow(()->notFound("Payment record was not found.")); User u=users.findByUsername(username).orElseThrow(()->forbidden("User not found."));
        if(u.getRole()!=Role.PET_OWNER || p.getOwner()==null || !p.getOwner().getId().equals(u.getId())) throw forbidden("You are not authorized to report this payment.");
        if(p.getStatus()==PaymentStatus.USER_REPORTED) throw bad("Payment has already been reported.");
        if(p.getStatus()==PaymentStatus.CONFIRMED||p.getStatus()==PaymentStatus.SUCCESS) throw bad("Payment is already confirmed.");
        if(utr!=null&&!utr.trim().isEmpty()){String value=utr.trim();if(!UTR.matcher(value).matches())throw bad("Invalid UPI transaction reference.");p.setTransactionId(value);}
        p.setStatus(PaymentStatus.USER_REPORTED); p.setReportedBy(u.getUsername()); payments.save(p); return dto(p);
    }

    @Transactional
    public Map<String,Object> review(Long id,String username,String decision){
        User actor=users.findByUsername(username).orElseThrow(()->forbidden("User not found."));
        Payment p=payments.findById(id).orElseThrow(()->notFound("Payment record was not found."));
        if(actor.getRole()!=Role.ADMIN && !(actor.getBranch()!=null&&p.getBranch()!=null&&actor.getBranch().getId().equals(p.getBranch().getId())&&((p.getPaymentType()==PaymentType.PHARMACY&&actor.getRole()==Role.PHARMACY)||(p.getPaymentType()==PaymentType.DOCTOR_CONSULTATION&&actor.getRole()==Role.VETERINARIAN)))) throw forbidden("You are not authorized to verify this payment.");
        if(p.getStatus()!=PaymentStatus.USER_REPORTED) throw bad("Only user-reported payments can be reviewed.");
        if("CONFIRMED".equalsIgnoreCase(decision)){p.setStatus(PaymentStatus.CONFIRMED);p.setPaidAt(LocalDateTime.now());}
        else if("FAILED".equalsIgnoreCase(decision)){p.setStatus(PaymentStatus.FAILED);p.setFailureReason("Payment could not be verified by authorized staff.");}
        else throw bad("Decision must be CONFIRMED or FAILED.");
        return dto(payments.save(p));
    }

    public Map<String,Object> verifyCheckout(Map<String,String> body,String username){throw new ResponseStatusException(HttpStatus.GONE,"Razorpay checkout has been replaced by UPI QR payments.");}
    public void webhook(String rawBody,String signature){throw new ResponseStatusException(HttpStatus.GONE,"Razorpay webhooks are disabled; UPI payments require manual verification.");}
    public Map<String,Object> status(Long id,String username){Payment p=payments.findById(id).orElseThrow(()->notFound("Payment record was not found."));authorizePayment(p,username);return dto(p);}
    public List<Map<String,Object>> ownerPayments(String username){User u=requireOwner(username);List<Map<String,Object>>out=new ArrayList<>();for(Payment p:payments.findByOwnerOrderByCreatedAtDesc(u))out.add(dto(p));return out;}

    private void authorizePayment(Payment p,String username){User u=users.findByUsername(username).orElseThrow(()->forbidden("User not found."));if(u.getRole()==Role.ADMIN)return;if(u.getRole()==Role.PET_OWNER&&p.getOwner()!=null&&p.getOwner().getId().equals(u.getId()))return;if(u.getBranch()!=null&&p.getBranch()!=null&&u.getBranch().getId().equals(p.getBranch().getId())&&(u.getRole()==Role.RECEPTIONIST||u.getRole()==Role.PHARMACY||u.getRole()==Role.VETERINARIAN))return;throw forbidden("You are not authorized to access this payment.");}
    private User requireOwner(String username){User u=users.findByUsername(username).orElseThrow(()->forbidden("User not found."));if(u.getRole()!=Role.PET_OWNER)throw forbidden("Pet owner access required.");return u;}
    private String enc(String s)throws java.io.UnsupportedEncodingException{return java.net.URLEncoder.encode(s==null?"":s,"UTF-8");}
    private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);} private ResponseStatusException forbidden(String m){return new ResponseStatusException(HttpStatus.FORBIDDEN,m);} private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
    private Map<String,Object> dto(Payment p){Map<String,Object>x=new LinkedHashMap<>();x.put("id",p.getId());x.put("paymentType",p.getPaymentType());x.put("amount",p.getAmount());x.put("currency",p.getCurrency());x.put("status",p.getStatus());x.put("provider","UPI_QR");x.put("paymentReference",p.getProviderOrderId());x.put("providerOrderId",p.getProviderOrderId());x.put("transactionId",p.getTransactionId());x.put("utr",p.getTransactionId());x.put("qrImageUrl",p.getQrImageUrl());x.put("failureReason",p.getFailureReason());x.put("createdAt",p.getCreatedAt());x.put("paidAt",p.getPaidAt());x.put("reportedBy",p.getReportedBy());x.put("petId",p.getPet()==null?null:p.getPet().getPetCode());x.put("petName",p.getPet()==null?null:p.getPet().getName());x.put("ownerName",p.getOwner()==null?null:p.getOwner().getFullName());x.put("veterinarian",p.getVeterinarian()==null?null:p.getVeterinarian().getFullName());x.put("appointmentId",p.getAppointment()==null?null:p.getAppointment().getId());x.put("prescriptionId",p.getPrescription()==null?null:p.getPrescription().getId());x.put("branch",p.getBranch()==null?null:p.getBranch().getBranchName());return x;}
}
