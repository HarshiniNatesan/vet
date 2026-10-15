package com.vet.controller;

import com.vet.service.PaymentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service){this.service=service;}

    @PostMapping("/consultation/{appointmentId}")
    public Map<String,Object> consultation(@PathVariable Long appointmentId, Authentication auth){return service.createConsultationPayment(appointmentId,auth.getName());}
    @PostMapping("/pharmacy/{prescriptionId}")
    public Map<String,Object> pharmacy(@PathVariable Long prescriptionId, Authentication auth){return service.createPharmacyPayment(prescriptionId,auth.getName());}
    @PostMapping("/staff/consultation/{appointmentId}")
    public Map<String,Object> staffConsultation(@PathVariable Long appointmentId,@RequestBody(required=false) Map<String,Object> body,Authentication auth){java.math.BigDecimal amount=null;if(body!=null&&body.get("amount")!=null)try{amount=new java.math.BigDecimal(body.get("amount").toString());}catch(Exception e){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Invalid consultation fee.");}return service.createStaffConsultationBill(appointmentId,auth.getName(),amount);}
    @PostMapping("/staff/pharmacy/{prescriptionId}")
    public Map<String,Object> staffPharmacy(@PathVariable Long prescriptionId,Authentication auth){return service.createStaffPharmacyBill(prescriptionId,auth.getName());}
    @PostMapping("/{id}/mark-paid")
    public Map<String,Object> markPaid(@PathVariable Long id,@RequestBody(required=false) Map<String,Object> body,Authentication auth){return service.markPaidManually(id,auth.getName(),body);}
    @PostMapping("/{id}/qr")
    public Map<String,Object> qr(@PathVariable Long id, Authentication auth){return service.createDynamicQr(id,auth.getName());}
    @GetMapping("/{id}/qr")
    public Map<String,Object> qrGet(@PathVariable Long id, Authentication auth){return service.createDynamicQr(id,auth.getName());}
    @PostMapping("/{id}/report-paid")
    public Map<String,Object> reportPaid(@PathVariable Long id,@RequestBody(required=false) Map<String,String> body,Authentication auth){return service.reportPaid(id,auth.getName(),body==null?null:body.get("utr"));}
    @PostMapping("/{id}/review")
    public Map<String,Object> review(@PathVariable Long id,@RequestBody Map<String,String> body,Authentication auth){return service.review(id,auth.getName(),body.get("decision"));}
    @PostMapping("/verify")
    public Map<String,Object> verify(@RequestBody Map<String,String> body, Authentication auth){return service.verifyCheckout(body,auth.getName());}
    @GetMapping("/{id}")
    public Map<String,Object> status(@PathVariable Long id, Authentication auth){return service.status(id,auth.getName());}
    @GetMapping("/owner/{username}")
    public List<Map<String,Object>> owner(@PathVariable String username, Authentication auth){if(!auth.getName().equals(username))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"You can view only your own payments.");return service.ownerPayments(username);}
}
