package com.vet.controller;
import com.vet.service.PharmacyService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.*;
@RestController @RequestMapping("/api/pharmacy")
public class PharmacyController{
 private final PharmacyService service; public PharmacyController(PharmacyService s){service=s;}
 @GetMapping("/stats") public Map<String,Object> stats(Authentication auth){return service.stats(auth.getName());}
 @GetMapping("/medicines") public List<Map<String,Object>> medicines(@RequestParam(required=false) String q,Authentication auth){return service.medicines(q,auth.getName());}
 @PostMapping("/medicines") public Map<String,Object> add(@RequestBody Map<String,Object>b,Authentication auth){return map(service.saveMedicine(b,null,auth.getName()));}
 @PutMapping("/medicines/{id}") public Map<String,Object> edit(@PathVariable Long id,@RequestBody Map<String,Object>b,Authentication auth){return map(service.saveMedicine(b,id,auth.getName()));}
 @DeleteMapping("/medicines/{id}") public Map<String,String> del(@PathVariable Long id,Authentication auth){service.deactivate(id,auth.getName());return Collections.singletonMap("message","Medicine deactivated.");}
 @GetMapping("/low-stock") public List<Map<String,Object>> low(Authentication auth){return service.lowStock(auth.getName());}
 @GetMapping("/prescriptions") public List<Map<String,Object>> prescriptions(Authentication auth){return service.prescriptions(auth.getName());}
 @GetMapping("/prescriptions/{id}") public Map<String,Object> prescription(@PathVariable Long id,Authentication auth){return service.prescription(id,auth.getName());}
 @GetMapping("/patients/search") public List<Map<String,Object>> search(@RequestParam String q,Authentication auth){return service.searchPets(q,auth.getName());}
 @GetMapping("/patients/{code}") public Map<String,Object> patient(@PathVariable String code,Authentication auth){return service.petByCode(code,auth.getName());}
 @PostMapping("/dispense/{id}") public Map<String,Object> dispense(@PathVariable Long id,@RequestBody Map<String,Object>b,@RequestParam(required=false) String username,Authentication auth){int q=Integer.parseInt(Objects.toString(b.get("quantity"),"0"));return service.dispense(id,q,auth.getName());}
 @GetMapping("/records") public List<Map<String,Object>> records(Authentication auth){return service.records(auth.getName());}
 private Map<String,Object> map(com.vet.model.Medicine m){Map<String,Object>x=new LinkedHashMap<>();x.put("id",m.getId());x.put("name",m.getName());x.put("category",m.getCategory());x.put("description",m.getDescription());x.put("manufacturer",m.getManufacturer());x.put("batchNumber",m.getBatchNumber());x.put("expiryDate",m.getExpiryDate());x.put("quantity",m.getQuantity());x.put("reorderLevel",m.getReorderLevel());x.put("unitPrice",m.getUnitPrice());x.put("status",m.getStockStatus());x.put("active",m.getActive());x.put("lastUpdated",m.getLastUpdated());return x;}
}
