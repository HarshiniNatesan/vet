package com.vet.controller;

import com.vet.model.*;
import com.vet.repository.PaymentRepository;
import com.vet.service.PaymentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {
    private final PaymentRepository payments; private final PaymentService service;
    public AdminPaymentController(PaymentRepository payments,PaymentService service){this.payments=payments;this.service=service;}
    @GetMapping
    public List<Map<String,Object>> list(@RequestParam(required=false) PaymentStatus status,@RequestParam(required=false) Long branchId){
        List<Payment> src=status==null?payments.findTop100ByOrderByCreatedAtDesc():payments.findByStatusOrderByCreatedAtDesc(status); List<Map<String,Object>> out=new ArrayList<>();
        for(Payment p:src){if(branchId!=null&&(p.getBranch()==null||!branchId.equals(p.getBranch().getId())))continue;out.add(dto(p));}return out;
    }
    @PostMapping("/{id}/review")
    public Map<String,Object> review(@PathVariable Long id,@RequestBody Map<String,String> body,Authentication auth){return service.review(id,auth.getName(),body.get("decision"));}
    @GetMapping("/summary") public Map<String,Object> summary(){
        BigDecimal revenue=payments.findAll().stream().filter(p->(p.getStatus()==PaymentStatus.CONFIRMED || p.getStatus()==PaymentStatus.SUCCESS)).map(Payment::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        Map<String,Object>x=new LinkedHashMap<>();x.put("total",payments.count());x.put("success",payments.findByStatusOrderByCreatedAtDesc(PaymentStatus.SUCCESS).size());x.put("pending",payments.findByStatusOrderByCreatedAtDesc(PaymentStatus.PENDING).size());x.put("failed",payments.findByStatusOrderByCreatedAtDesc(PaymentStatus.FAILED).size());x.put("revenue",revenue);return x;
    }
    private Map<String,Object> dto(Payment p){Map<String,Object>x=new LinkedHashMap<>();x.put("id",p.getId());x.put("paymentType",p.getPaymentType());x.put("amount",p.getAmount());x.put("currency",p.getCurrency());x.put("status",p.getStatus());x.put("providerOrderId",p.getProviderOrderId());x.put("providerPaymentId",p.getProviderPaymentId());x.put("transactionId",p.getTransactionId());x.put("pet",p.getPet()==null?null:p.getPet().getPetCode());x.put("owner",p.getOwner()==null?null:p.getOwner().getFullName());x.put("veterinarian",p.getVeterinarian()==null?null:p.getVeterinarian().getFullName());x.put("branch",p.getBranch()==null?null:p.getBranch().getBranchName());x.put("createdAt",p.getCreatedAt());x.put("paidAt",p.getPaidAt());return x;}
}
