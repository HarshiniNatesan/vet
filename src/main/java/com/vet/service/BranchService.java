package com.vet.service;

import com.vet.model.*;
import com.vet.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BranchService {
    private final BranchRepository branches;
    private final UserRepository users;
    private final AppointmentRepository appointments;
    private final PetRepository pets;
    private final PaymentRepository payments;
    private final PharmacyTransactionRepository pharmacyTransactions;

    public BranchService(BranchRepository branches, UserRepository users,
                         AppointmentRepository appointments, PetRepository pets,
                         PaymentRepository payments, PharmacyTransactionRepository pharmacyTransactions) {
        this.branches = branches; this.users = users; this.appointments = appointments;
        this.pets = pets; this.payments = payments; this.pharmacyTransactions = pharmacyTransactions;
    }

    public List<Map<String,Object>> list() {
        List<Map<String,Object>> out = new ArrayList<>();
        for (Branch b : branches.findAllByOrderByBranchNameAsc()) out.add(dto(b));
        return out;
    }

    @Transactional
    public Map<String,Object> save(Long id, Map<String,Object> body) {
        Branch b = id == null ? new Branch() : branches.findById(id).orElseThrow(() -> new IllegalArgumentException("Branch not found."));
        String code = Objects.toString(body.get("branchCode"), "").trim().toUpperCase();
        String name = Objects.toString(body.get("branchName"), "").trim();
        if (code.isEmpty() || name.isEmpty()) throw new IllegalArgumentException("Branch code and branch name are required.");
        branches.findByBranchCodeIgnoreCase(code).ifPresent(existing -> {
            if (!existing.getId().equals(b.getId())) throw new IllegalArgumentException("Branch code already exists.");
        });
        b.setBranchCode(code); b.setBranchName(name);
        b.setAddress(Objects.toString(body.get("address"), ""));
        b.setPhone(Objects.toString(body.get("phone"), "")); b.setEmail(Objects.toString(body.get("email"), ""));
        b.setActive(body.get("active") == null || Boolean.parseBoolean(body.get("active").toString()));
        return dto(branches.save(b));
    }

    @Transactional
    public Map<String,Object> status(Long id, boolean active) {
        Branch b = branches.findById(id).orElseThrow(() -> new IllegalArgumentException("Branch not found."));
        b.setActive(active); return dto(branches.save(b));
    }

    @Transactional
    public Map<String,Object> assignUser(Long branchId, Long userId) {
        Branch b = branches.findById(branchId).orElseThrow(() -> new IllegalArgumentException("Branch not found."));
        User u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (u.getRole() == Role.ADMIN) throw new IllegalArgumentException("Global administrators do not belong to a branch.");
        if (u.getRole() == Role.PET_OWNER) throw new IllegalArgumentException("Pet owners are not branch staff.");
        if (!Boolean.TRUE.equals(b.getActive())) throw new IllegalArgumentException("Cannot assign staff to an inactive branch.");
        u.setBranch(b); users.save(u); return userDto(u);
    }

    public Map<String,Object> statistics(Long branchId) {
        Branch b = branchId == null ? null : branches.findById(branchId).orElseThrow(() -> new IllegalArgumentException("Branch not found."));
        long staff = users.findAll().stream().filter(u -> u.getBranch()!=null && (b==null || u.getBranch().getId().equals(b.getId()))).count();
        long appts = appointments.findAll().stream().filter(a -> a.getBranch()!=null && (b==null || a.getBranch().getId().equals(b.getId()))).count();
        long paymentCount = payments.findAll().stream().filter(p -> p.getBranch()!=null && (b==null || p.getBranch().getId().equals(b.getId()))).count();
        java.math.BigDecimal revenue = payments.findAll().stream()
                .filter(p -> p.getStatus()==PaymentStatus.SUCCESS && p.getBranch()!=null && (b==null || p.getBranch().getId().equals(b.getId())))
                .map(Payment::getAmount).filter(Objects::nonNull).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        long pharmacy = pharmacyTransactions.findAll().stream().filter(t -> t.getBranch()!=null && (b==null || t.getBranch().getId().equals(b.getId()))).count();
        Map<String,Object> x=new LinkedHashMap<>(); x.put("branch", b==null?"ALL":b.getBranchName()); x.put("staff",staff); x.put("appointments",appts); x.put("payments",paymentCount); x.put("revenue",revenue); x.put("pharmacyTransactions",pharmacy); return x;
    }

    private Map<String,Object> dto(Branch b){
        Map<String,Object>x=new LinkedHashMap<>(); x.put("id",b.getId()); x.put("branchCode",b.getBranchCode()); x.put("branchName",b.getBranchName());
        x.put("address",b.getAddress()); x.put("phone",b.getPhone()); x.put("email",b.getEmail()); x.put("active",b.getActive());
        long usersCount=users.findAll().stream().filter(u->u.getBranch()!=null&&u.getBranch().getId().equals(b.getId())).count();
        long petsCount=pets.findAll().stream().filter(p->p.getAssignedVeterinarian()!=null&&p.getAssignedVeterinarian().getBranch()!=null&&p.getAssignedVeterinarian().getBranch().getId().equals(b.getId())).count();
        x.put("staffCount",usersCount); x.put("patientCount",petsCount); return x;
    }
    private Map<String,Object> userDto(User u){Map<String,Object>x=new LinkedHashMap<>();x.put("id",u.getId());x.put("username",u.getUsername());x.put("fullName",u.getFullName());x.put("role",u.getRole());x.put("branch",u.getBranch()==null?null:dto(u.getBranch()));return x;}
}
