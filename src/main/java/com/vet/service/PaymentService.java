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

    @Transactional
    public Map<String,Object> createStaffConsultationBill(Long appointmentId,String username,BigDecimal requestedAmount){
        User actor=users.findByUsername(username).orElseThrow(()->forbidden("User not found."));
        if(actor.getRole()!=Role.VETERINARIAN)throw forbidden("Only a veterinarian can create a consultation bill.");
        Appointment a=appointments.findById(appointmentId).orElseThrow(()->notFound("Appointment not found."));
        boolean permitted=(a.getVeterinarian()!=null&&a.getVeterinarian().getId().equals(actor.getId())) || (a.getVeterinarian()==null&&actor.getBranch()!=null&&a.getBranch()!=null&&actor.getBranch().getId().equals(a.getBranch().getId()));
        if(!permitted)throw forbidden("This appointment is outside your assigned scope.");
        if(actor.getBranch()==null || (a.getBranch()!=null && !actor.getBranch().getId().equals(a.getBranch().getId())))throw forbidden("This appointment is outside your assigned branch.");
        if(a.getOwner()==null||a.getPet()==null)throw bad("The appointment is missing pet-owner details.");
        BigDecimal amount=requestedAmount!=null?requestedAmount:actor.getConsultationFee();
        if(amount==null||amount.signum()<=0||amount.scale()>2||amount.precision()>12)throw bad("Enter a valid consultation fee greater than zero, using at most two decimal places.");
        amount=amount.setScale(2,RoundingMode.HALF_UP);
        Payment p=payments.findByAppointment(a).orElse(null);
        if(p==null){p=new Payment();p.setPet(a.getPet());p.setOwner(a.getOwner());p.setVeterinarian(a.getVeterinarian()!=null?a.getVeterinarian():actor);p.setAppointment(a);p.setPaymentType(PaymentType.DOCTOR_CONSULTATION);p.setAmount(amount.setScale(2,RoundingMode.HALF_UP));p.setCurrency("INR");p.setBranch(a.getBranch()!=null?a.getBranch():actor.getBranch());p.setProvider("MANUAL");p.setProviderOrderId("VET-CONS-"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase());p=payments.saveAndFlush(p);}
        if(p.getPaymentType()!=PaymentType.DOCTOR_CONSULTATION)throw bad("The appointment is already linked to a different bill type.");
        if(p.getStatus()==PaymentStatus.PENDING&&requestedAmount!=null){p.setAmount(amount.setScale(2,RoundingMode.HALF_UP));p=payments.save(p);}
        else if(p.getAmount().compareTo(amount.setScale(2,RoundingMode.HALF_UP))!=0)throw bad("This consultation bill is already finalized; use an authorized billing adjustment.");
        return dto(p);
    }

    @Transactional
    public Map<String,Object> createStaffPharmacyBill(Long prescriptionId,String username){
        User actor=users.findByUsername(username).orElseThrow(()->forbidden("User not found."));
        if(actor.getRole()!=Role.PHARMACY)throw forbidden("Only pharmacy staff can create a medicine bill.");
        Prescription prescription=prescriptions.findById(prescriptionId).orElseThrow(()->notFound("Prescription not found."));
        if(actor.getBranch()==null||prescription.getBranch()==null||!actor.getBranch().getId().equals(prescription.getBranch().getId()))throw forbidden("This prescription is outside your assigned branch.");
        List<PharmacyTransaction> tx=pharmacyTransactions.findByPrescription(prescription);if(tx.isEmpty())throw bad("Dispense medicine before generating the medicine bill.");
        BigDecimal amount=tx.stream().map(PharmacyTransaction::getTotalAmount).filter(Objects::nonNull).map(BigDecimal::valueOf).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
        if(amount.signum()<=0)throw bad("Medicine bill amount is invalid.");
        Payment p=payments.findByPrescription(prescription).orElse(null);
        if(p==null){p=new Payment();p.setPet(prescription.getPet());p.setOwner(prescription.getPet().getOwner());p.setVeterinarian(prescription.getVeterinarian());p.setPrescription(prescription);p.setPaymentType(PaymentType.PHARMACY);p.setAmount(amount);p.setCurrency("INR");p.setBranch(prescription.getBranch());p.setProvider("MANUAL");p.setProviderOrderId("VET-PH-"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase());p=payments.saveAndFlush(p);}
        else if(p.getStatus()!=PaymentStatus.PENDING&&p.getAmount().compareTo(amount)!=0)throw bad("This bill has already been processed and cannot be changed.");
        else if(p.getStatus()==PaymentStatus.PENDING){p.setAmount(amount);p=payments.save(p);}
        return dto(p);
    }

    @Transactional
    public Map<String,Object> markPaidManually(Long id,String username,Map<String,Object> body){
        User actor=users.findByUsername(username).orElseThrow(()->forbidden("User not found."));
        Payment p=payments.findById(id).orElseThrow(()->notFound("Bill was not found."));
        boolean typeAllowed=(p.getPaymentType()==PaymentType.DOCTOR_CONSULTATION&&actor.getRole()==Role.VETERINARIAN)||(p.getPaymentType()==PaymentType.PHARMACY&&actor.getRole()==Role.PHARMACY);
        boolean branchAllowed=actor.getBranch()!=null&&p.getBranch()!=null&&actor.getBranch().getId().equals(p.getBranch().getId());
        boolean doctorOwns=p.getPaymentType()!=PaymentType.DOCTOR_CONSULTATION||p.getVeterinarian()==null||p.getVeterinarian().getId().equals(actor.getId());
        if(!typeAllowed||!branchAllowed||!doctorOwns)throw forbidden("You are not authorized to confirm this bill.");
        if(p.getStatus()==PaymentStatus.CONFIRMED||p.getStatus()==PaymentStatus.SUCCESS)throw bad("This bill is already marked as paid.");
        if(p.getStatus()!=PaymentStatus.PENDING&&p.getStatus()!=PaymentStatus.USER_REPORTED)throw bad("This bill cannot be marked paid from its current status.");

        Map<String,Object> in=body==null?Collections.<String,Object>emptyMap():body;
        PaymentMethod method;
        try{method=PaymentMethod.valueOf(String.valueOf(in.get("paymentMethod")).trim().toUpperCase());}
        catch(Exception e){throw bad("Select the payment method.");}
        // Amount is always taken from the stored bill; a client-supplied amount may only confirm it.
        Object sent=in.get("amount");
        if(sent!=null&&!sent.toString().trim().isEmpty()){
            BigDecimal a;try{a=new BigDecimal(sent.toString().trim());}catch(Exception e){throw bad("Invalid payment amount.");}
            if(a.setScale(2,RoundingMode.HALF_UP).compareTo(p.getAmount().setScale(2,RoundingMode.HALF_UP))!=0)throw bad("Payment amount does not match the bill total.");
        }
        String reference=clean(in.get("transactionReference"));
        String other=clean(in.get("paymentMethodOther"));
        if(method==PaymentMethod.CASH)reference=null;
        else if(reference!=null){
            if(!UTR.matcher(reference).matches())throw bad("Transaction reference must be 6-35 letters, numbers or hyphens.");
            if(payments.existsByTransactionIdAndIdNot(reference,p.getId()))throw bad("This transaction reference is already recorded on another bill.");
        }
        if(method==PaymentMethod.OTHER){if(other!=null&&other.length()>60)throw bad("Payment method description is too long (max 60 characters).");}
        else other=null;

        p.setStatus(PaymentStatus.CONFIRMED);p.setPaidAt(LocalDateTime.now());p.setReportedBy(actor.getUsername());p.setProvider("MANUAL");
        p.setPaymentMethod(method);p.setPaymentMethodOther(other);
        if(reference!=null||method==PaymentMethod.CASH)p.setTransactionId(reference);
        try{return dto(payments.saveAndFlush(p));}
        catch(org.springframework.orm.ObjectOptimisticLockingFailureException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"This bill was just updated. Refresh and try again.");}
    }

    private String clean(Object v){if(v==null)return null;String t=v.toString().trim();return t.isEmpty()?null:t;}

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
        if("CONFIRMED".equalsIgnoreCase(decision)){p.setStatus(PaymentStatus.CONFIRMED);p.setPaidAt(LocalDateTime.now());if(p.getPaymentMethod()==null)p.setPaymentMethod(PaymentMethod.UPI);}
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
    public static String invoiceNumber(Payment p){return p.getProviderOrderId()!=null?p.getProviderOrderId():"INV-"+String.format("%06d",p.getId());}
    private Map<String,Object> dto(Payment p){Map<String,Object>x=new LinkedHashMap<>();x.put("id",p.getId());x.put("paymentType",p.getPaymentType());x.put("amount",p.getAmount());x.put("currency",p.getCurrency());x.put("status",p.getStatus());x.put("provider",p.getProvider()==null?"MANUAL":p.getProvider());x.put("paymentReference",p.getProviderOrderId());x.put("invoiceNumber",invoiceNumber(p));x.put("paymentMethod",p.getPaymentMethod());x.put("paymentMethodLabel",p.getPaymentMethod()==null?null:p.getPaymentMethod()==PaymentMethod.OTHER&&p.getPaymentMethodOther()!=null?p.getPaymentMethodOther():p.getPaymentMethod().getLabel());x.put("paymentMethodOther",p.getPaymentMethodOther());x.put("providerOrderId",p.getProviderOrderId());x.put("transactionId",p.getTransactionId());x.put("utr",p.getTransactionId());x.put("qrImageUrl",p.getQrImageUrl());x.put("failureReason",p.getFailureReason());x.put("createdAt",p.getCreatedAt());x.put("paidAt",p.getPaidAt());x.put("reportedBy",p.getReportedBy());x.put("petId",p.getPet()==null?null:p.getPet().getPetCode());x.put("petName",p.getPet()==null?null:p.getPet().getName());x.put("ownerName",p.getOwner()==null?null:p.getOwner().getFullName());x.put("veterinarian",p.getVeterinarian()==null?null:p.getVeterinarian().getFullName());x.put("appointmentId",p.getAppointment()==null?null:p.getAppointment().getId());x.put("prescriptionId",p.getPrescription()==null?null:p.getPrescription().getId());if(p.getPrescription()!=null){List<Map<String,Object>> items=new ArrayList<>();for(PharmacyTransaction t:pharmacyTransactions.findByPrescription(p.getPrescription())){Map<String,Object> item=new LinkedHashMap<>();item.put("medicineName",t.getMedicine()==null?null:t.getMedicine().getName());item.put("quantity",t.getQuantityDispensed());item.put("unitPrice",t.getUnitPrice());item.put("subtotal",t.getTotalAmount());items.add(item);}x.put("items",items);}else{x.put("items",Collections.emptyList());}x.put("branch",p.getBranch()==null?null:p.getBranch().getBranchName());return x;}
}
