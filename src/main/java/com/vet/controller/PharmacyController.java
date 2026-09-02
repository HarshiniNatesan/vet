package com.vet.controller;
import com.vet.service.PharmacyService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/pharmacy")
public class PharmacyController{
 private final PharmacyService service; public PharmacyController(PharmacyService s){service=s;}
 @GetMapping("/stats") public Map<String,Object> stats(){return service.stats();}
 @GetMapping("/medicines") public List<Map<String,Object>> medicines(@RequestParam(required=false) String q){return service.medicines(q);}
 @PostMapping("/medicines") public Map<String,Object> add(@RequestBody Map<String,Object>b){return map(service.saveMedicine(b,null));}
 @PutMapping("/medicines/{id}") public Map<String,Object> edit(@PathVariable Long id,@RequestBody Map<String,Object>b){return map(service.saveMedicine(b,id));}
 @DeleteMapping("/medicines/{id}") public Map<String,String> del(@PathVariable Long id){service.deactivate(id);return Collections.singletonMap("message","Medicine deactivated.");}
 @GetMapping("/low-stock") public List<Map<String,Object>> low(){return service.lowStock();}
 @GetMapping("/prescriptions") public List<Map<String,Object>> prescriptions(){return service.prescriptions();}
 @GetMapping("/prescriptions/{id}") public Map<String,Object> prescription(@PathVariable Long id){return service.prescription(id);}
 @GetMapping("/patients/search") public List<Map<String,Object>> search(@RequestParam String q){return service.searchPets(q);}
 @GetMapping("/patients/{code}") public Map<String,Object> patient(@PathVariable String code){return service.petByCode(code);}
 @PostMapping("/dispense/{id}") public Map<String,Object> dispense(@PathVariable Long id,@RequestBody Map<String,Object>b,@RequestParam String username){int q=Integer.parseInt(Objects.toString(b.get("quantity"),"0"));return service.dispense(id,q,username);}
 @GetMapping("/records") public List<Map<String,Object>> records(){return service.records();}
 private Map<String,Object> map(com.vet.model.Medicine m){Map<String,Object>x=new LinkedHashMap<>();x.put("id",m.getId());x.put("name",m.getName());x.put("category",m.getCategory());x.put("description",m.getDescription());x.put("manufacturer",m.getManufacturer());x.put("batchNumber",m.getBatchNumber());x.put("expiryDate",m.getExpiryDate());x.put("quantity",m.getQuantity());x.put("reorderLevel",m.getReorderLevel());x.put("unitPrice",m.getUnitPrice());x.put("status",m.getStockStatus());x.put("active",m.getActive());x.put("lastUpdated",m.getLastUpdated());return x;}
}
