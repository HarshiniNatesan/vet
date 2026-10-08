package com.vet.service;

import com.vet.model.*;
import com.vet.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class PharmacyService {
    private final MedicineRepository medicines; private final PrescriptionRepository prescriptions;
    private final PetRepository pets; private final UserRepository users; private final PharmacyTransactionRepository transactions;
    public PharmacyService(MedicineRepository m, PrescriptionRepository p, PetRepository pets, UserRepository users, PharmacyTransactionRepository t){
        this.medicines=m;this.prescriptions=p;this.pets=pets;this.users=users;this.transactions=t;
    }
    private User staff(String username){User u=users.findByUsername(username).orElseThrow(()->new IllegalArgumentException("Pharmacy staff account not found."));if(u.getRole()!=Role.PHARMACY)throw new IllegalArgumentException("Pharmacy access required.");return u;}
    private boolean branchAllowed(Branch staffBranch, Branch dataBranch){return staffBranch==null || dataBranch==null || staffBranch.getId().equals(dataBranch.getId());}
    private Map<String,Object> medicine(Medicine m){
        Map<String,Object>x=new LinkedHashMap<>(); x.put("id",m.getId());x.put("name",m.getName());x.put("category",m.getCategory());x.put("description",m.getDescription());
        x.put("manufacturer",m.getManufacturer());x.put("batchNumber",m.getBatchNumber());x.put("expiryDate",m.getExpiryDate());x.put("quantity",m.getQuantity());x.put("reorderLevel",m.getReorderLevel());
        x.put("unitPrice",m.getUnitPrice());x.put("status",m.getStockStatus());x.put("active",m.getActive());x.put("lastUpdated",m.getLastUpdated()); return x;
    }
    public List<Map<String,Object>> medicines(String q,String username){
        User s=staff(username); List<Medicine> list=q==null||q.trim().isEmpty()?medicines.findByActiveTrueOrderByNameAsc():medicines.findByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(q.trim());
        List<Map<String,Object>> out=new ArrayList<>(); for(Medicine m:list)if(branchAllowed(s.getBranch(),m.getBranch()))out.add(medicine(m)); return out;
    }
    public List<Map<String,Object>> lowStock(String username){User s=staff(username);List<Map<String,Object>>o=new ArrayList<>();for(Medicine m:medicines.findLowStock())if(branchAllowed(s.getBranch(),m.getBranch()))o.add(medicine(m));return o;}
    public Medicine saveMedicine(Map<String,Object>b, Long id,String username){
        User s=staff(username); Medicine m=id==null?new Medicine():medicines.findById(id).orElseThrow(()->new IllegalArgumentException("Medicine not found.")); if(m.getBranch()!=null&&!branchAllowed(s.getBranch(),m.getBranch()))throw new IllegalArgumentException("Medicine belongs to another branch."); if(m.getBranch()==null)m.setBranch(s.getBranch());
        String name=Objects.toString(b.get("name"),"").trim(); if(name.isEmpty())throw new IllegalArgumentException("Medicine name cannot be empty.");
        m.setName(name);m.setCategory(Objects.toString(b.get("category"),""));m.setDescription(Objects.toString(b.get("description"),""));m.setManufacturer(Objects.toString(b.get("manufacturer"),""));
        m.setBatchNumber(Objects.toString(b.get("batchNumber"),""));m.setExpiryDate(date(b.get("expiryDate")));m.setQuantity(integer(b.get("quantity"),"Quantity",false));m.setReorderLevel(integer(b.get("reorderLevel"),"Reorder level",false));
        m.setUnitPrice(number(b.get("unitPrice"),"Unit price"));m.setActive(true);return medicines.save(m);
    }
    public void deactivate(Long id,String username){User s=staff(username);Medicine m=medicines.findById(id).orElseThrow(()->new IllegalArgumentException("Medicine not found."));if(!branchAllowed(s.getBranch(),m.getBranch()))throw new IllegalArgumentException("Medicine belongs to another branch.");m.setActive(false);medicines.save(m);}
    private int integer(Object v,String label,boolean positive){int n;try{n=Integer.parseInt(Objects.toString(v,"0"));}catch(Exception e){throw new IllegalArgumentException(label+" must be a valid number.");}if(n<0||(positive&&n==0))throw new IllegalArgumentException(label+" cannot be negative.");return n;}
    private double number(Object v,String label){double n;try{n=Double.parseDouble(Objects.toString(v,"0"));}catch(Exception e){throw new IllegalArgumentException(label+" must be a valid number.");}if(n<0)throw new IllegalArgumentException(label+" cannot be negative.");return n;}
    private LocalDate date(Object v){if(v==null||Objects.toString(v,"").isEmpty())return null;try{return LocalDate.parse(v.toString());}catch(Exception e){throw new IllegalArgumentException("Expiry date is invalid.");}}
    public List<Map<String,Object>> prescriptions(String username){User s=staff(username);List<Map<String,Object>>o=new ArrayList<>();for(Prescription p:prescriptions.findAll()){if(!branchAllowed(s.getBranch(),p.getBranch()))continue;Map<String,Object>x=new LinkedHashMap<>();x.put("id",p.getId());x.put("pet",pet(p.getPet()));x.put("veterinarian",user(p.getVeterinarian()));x.put("appointmentId",p.getAppointment()==null?null:p.getAppointment().getId());x.put("medicineName",p.getMedicineName());x.put("dosage",p.getDosage());x.put("frequency",p.getFrequency());x.put("duration",p.getDuration());x.put("instructions",p.getInstructions());x.put("requiredQuantity",p.getRequiredQuantity());x.put("status",p.getStatus());o.add(x);}o.sort((a,b)->Long.compare((Long)b.get("id"),(Long)a.get("id")));return o;}
    public List<Map<String,Object>> searchPets(String q,String username){
        User staffUser=staff(username); List<Map<String,Object>> out=new ArrayList<>();
        String s=q==null?"":q.trim();
        if(s.isEmpty()) return out;

        LinkedHashMap<Long,Pet> matches=new LinkedHashMap<>();
        String normalized=s.toUpperCase().replace("PET-","").trim();

        try{
            if(normalized.matches("\\d+")) pets.findById(Long.parseLong(normalized)).ifPresent(p->matches.put(p.getId(),p));
        }catch(Exception ignored){}

        for(Pet p:pets.findByNameContainingIgnoreCase(s)) matches.putIfAbsent(p.getId(),p);
        for(Pet p:pets.findAll()){
            if(p.getPetCode()!=null && p.getPetCode().equalsIgnoreCase(s)) matches.putIfAbsent(p.getId(),p);
            if(p.getOwner()!=null && p.getOwner().getFullName()!=null && p.getOwner().getFullName().toLowerCase().contains(s.toLowerCase())) matches.putIfAbsent(p.getId(),p);
        }

        for(Pet p:matches.values()){if(staffUser.getBranch()!=null && p.getAssignedVeterinarian()!=null && p.getAssignedVeterinarian().getBranch()!=null && !staffUser.getBranch().getId().equals(p.getAssignedVeterinarian().getBranch().getId()))continue;out.add(pet(p));}
        return out;
    }
    public Map<String,Object> petByCode(String code,String username){User s=staff(username);Long id=parsePetCode(code);Pet p=pets.findById(id).orElseThrow(()->new IllegalArgumentException("Pet not found."));if(s.getBranch()!=null&&p.getAssignedVeterinarian()!=null&&p.getAssignedVeterinarian().getBranch()!=null&&!s.getBranch().getId().equals(p.getAssignedVeterinarian().getBranch().getId()))throw new IllegalArgumentException("Patient belongs to another branch.");return petWithHistory(p);}
    private Long parsePetCode(String code){String s=code.toUpperCase().replace("PET-","").trim();try{return Long.valueOf(s);}catch(Exception e){throw new IllegalArgumentException("Invalid Pet ID. Use PET-000001.");}}
    public Map<String,Object> prescription(Long id,String username){User s=staff(username);Prescription p=prescriptions.findById(id).orElseThrow(()->new IllegalArgumentException("Prescription not found."));if(!branchAllowed(s.getBranch(),p.getBranch()))throw new IllegalArgumentException("Prescription belongs to another branch.");return prescriptionMap(p);}
    @Transactional public Map<String,Object> dispense(Long id,int qty,String username){
        if(qty<=0)throw new IllegalArgumentException("Dispensed quantity must be greater than zero.");
        User staffUser=staff(username); Prescription p=prescriptions.findById(id).orElseThrow(()->new IllegalArgumentException("Prescription not found.")); if(!branchAllowed(staffUser.getBranch(),p.getBranch()))throw new IllegalArgumentException("Prescription belongs to another branch.");
        if("DISPENSED".equals(p.getStatus())||"CANCELLED".equals(p.getStatus()))throw new IllegalArgumentException("This prescription is not available for dispensing.");
        Medicine m=medicines.findAllByNameIgnoreCaseAndActiveTrue(p.getMedicineName()).stream().filter(x->branchAllowed(staffUser.getBranch(),x.getBranch())).findFirst().orElseThrow(()->new IllegalArgumentException("Prescribed medicine is not available in this branch."));
        if(m.getExpiryDate()!=null&&m.getExpiryDate().isBefore(LocalDate.now()))throw new IllegalArgumentException("Expired medicines cannot be dispensed.");
        if(m.getQuantity()<qty)throw new IllegalArgumentException("Insufficient medicine stock.");
        m.setQuantity(m.getQuantity()-qty);medicines.save(m);
        PharmacyTransaction t=new PharmacyTransaction();t.setPrescription(p);t.setPet(p.getPet());t.setMedicine(m);t.setPharmacyStaff(staff(username));t.setBranch(staff(username).getBranch());t.setQuantityDispensed(qty);t.setBatchNumber(m.getBatchNumber());t.setUnitPrice(m.getUnitPrice());t.setTotalAmount(qty*m.getUnitPrice());t.setDispensingDate(LocalDateTime.now());t.setDispensingStatus("DISPENSED");transactions.save(t);
        int required=p.getRequiredQuantity()==null?qty:p.getRequiredQuantity();p.setStatus(qty>=required?"DISPENSED":"PARTIALLY_DISPENSED");prescriptions.save(p);
        return transactionMap(t);
    }
    public List<Map<String,Object>> records(String username){User s=staff(username);List<Map<String,Object>>o=new ArrayList<>();for(PharmacyTransaction t:transactions.findTop100ByOrderByDispensingDateDesc())if(branchAllowed(s.getBranch(),t.getBranch()))o.add(transactionMap(t));return o;}
    public Map<String,Object> stats(String username){
        User s=staff(username); List<Medicine> visible=medicines.findByActiveTrueOrderByNameAsc(); visible.removeIf(m->!branchAllowed(s.getBranch(),m.getBranch())); List<PharmacyTransaction> visibleTx=transactions.findTop100ByOrderByDispensingDateDesc(); visibleTx.removeIf(t->!branchAllowed(s.getBranch(),t.getBranch()));
        Map<String,Object>x=new LinkedHashMap<>();
        x.put("totalMedicines",visible.size());
        x.put("totalStockUnits",visible.stream().mapToLong(m->m.getQuantity()==null?0:m.getQuantity()).sum());
        x.put("lowStockMedicines",visible.stream().filter(m->m.getQuantity()!=null&&m.getReorderLevel()!=null&&m.getQuantity()<=m.getReorderLevel()).count());
        LocalDateTime start=LocalDate.now().atStartOfDay();
        x.put("medicinesDispensedToday",visibleTx.stream().filter(t->t.getDispensingDate()!=null&&!t.getDispensingDate().isBefore(start)).count());
        x.put("pendingPrescriptions",prescriptions.findAll().stream().filter(p->branchAllowed(s.getBranch(),p.getBranch())).filter(p->"PENDING".equals(p.getStatus())||"PARTIALLY_DISPENSED".equals(p.getStatus())).count());
        x.put("totalPharmacyTransactions",visibleTx.size());
        Map<Long,Map<String,Object>> grouped=new LinkedHashMap<>();
        for(PharmacyTransaction t:visibleTx){
            Medicine m=t.getMedicine();
            if(m==null) continue;
            Map<String,Object> row=grouped.computeIfAbsent(m.getId(),k->{Map<String,Object> z=new LinkedHashMap<>();z.put("medicineId",m.getId());z.put("medicineName",m.getName());z.put("quantityDispensed",0L);z.put("transactionCount",0L);z.put("availableStock",m.getQuantity()==null?0:m.getQuantity());return z;});
            row.put("quantityDispensed",((Long)row.get("quantityDispensed"))+Long.valueOf(t.getQuantityDispensed()==null?0:t.getQuantityDispensed()));
            row.put("transactionCount",((Long)row.get("transactionCount"))+1L);
        }
        List<Map<String,Object>> fast=new ArrayList<>(grouped.values());
        fast.sort((a,b)->Long.compare((Long)b.get("quantityDispensed"),(Long)a.get("quantityDispensed")));
        x.put("fastMovingMedicines",fast);
        return x;
    }
    private Map<String,Object> pet(Pet p){Map<String,Object>x=new LinkedHashMap<>();x.put("id",p.getId());x.put("petCode",p.getPetCode());x.put("name",p.getName());x.put("species",p.getSpecies());x.put("breed",p.getBreed());x.put("age",p.getAge());x.put("gender",p.getGender());x.put("owner",user(p.getOwner()));return x;}
    private Map<String,Object> petWithHistory(Pet p){Map<String,Object>x=pet(p);List<Map<String,Object>>h=new ArrayList<>();for(Prescription r:prescriptions.findByPetOrderByIdDesc(p))h.add(prescriptionMap(r));x.put("prescriptions",h);return x;}
    private Map<String,Object> user(User u){if(u==null)return null;Map<String,Object>x=new LinkedHashMap<>();x.put("id",u.getId());x.put("username",u.getUsername());x.put("fullName",u.getFullName());x.put("email",u.getEmail());x.put("phone",u.getPhone());x.put("role",u.getRole());return x;}
    private Map<String,Object> prescriptionMap(Prescription p){Map<String,Object>x=new LinkedHashMap<>();x.put("id",p.getId());x.put("pet",pet(p.getPet()));x.put("veterinarian",user(p.getVeterinarian()));x.put("appointmentId",p.getAppointment()==null?null:p.getAppointment().getId());x.put("medicineName",p.getMedicineName());x.put("dosage",p.getDosage());x.put("frequency",p.getFrequency());x.put("duration",p.getDuration());x.put("instructions",p.getInstructions());x.put("requiredQuantity",p.getRequiredQuantity());x.put("status",p.getStatus());return x;}
    private Map<String,Object> transactionMap(PharmacyTransaction t){Map<String,Object>x=new LinkedHashMap<>();x.put("id",t.getId());x.put("prescriptionId",t.getPrescription().getId());x.put("pet",pet(t.getPet()));x.put("ownerName",t.getPet().getOwner().getFullName());x.put("medicineName",t.getMedicine().getName());x.put("quantityDispensed",t.getQuantityDispensed());x.put("veterinarian",user(t.getPrescription().getVeterinarian()));x.put("pharmacyStaff",user(t.getPharmacyStaff()));x.put("dispensingDate",t.getDispensingDate());x.put("batchNumber",t.getBatchNumber());x.put("unitPrice",t.getUnitPrice());x.put("totalAmount",t.getTotalAmount());x.put("dispensingStatus",t.getDispensingStatus());return x;}
}
