package com.vet.controller;
import com.vet.service.AdminService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/admin")
public class AdminController{
 private final AdminService service; public AdminController(AdminService s){service=s;}
 @GetMapping("/stats") public Map<String,Object> stats(){return service.stats();}
 @GetMapping("/users") public List<Map<String,Object>> users(@RequestParam(required=false)String q,@RequestParam(required=false)String role,@RequestParam(required=false)String status){return service.userList(q,role,status);}
 @PostMapping("/users") public Map<String,Object> add(@RequestBody Map<String,Object>b,@RequestParam String actor){return service.saveUser(b,null,actor);}
 @PutMapping("/users/{id}") public Map<String,Object> edit(@PathVariable Long id,@RequestBody Map<String,Object>b,@RequestParam String actor){return service.saveUser(b,id,actor);}
 @PatchMapping("/users/{id}/status") public Map<String,Object> status(@PathVariable Long id,@RequestParam boolean active,@RequestParam String actor){return service.toggleUser(id,active,actor);}
 @GetMapping("/veterinarians") public List<Map<String,Object>> vets(){return service.veterinarians();}
 @PostMapping("/veterinarians") public Map<String,Object> addVet(@RequestBody Map<String,Object>b,@RequestParam String actor){b.put("role","VETERINARIAN");return service.saveUser(b,null,actor);}
 @PutMapping("/veterinarians/{id}") public Map<String,Object> editVet(@PathVariable Long id,@RequestBody Map<String,Object>b,@RequestParam String actor){b.put("role","VETERINARIAN");return service.saveUser(b,id,actor);}
 @GetMapping("/receptionists") public List<Map<String,Object>> receptionists(){return service.userList(null,"RECEPTIONIST",null);}
 @PostMapping("/receptionists") public Map<String,Object> addReceptionist(@RequestBody Map<String,Object>b,@RequestParam String actor){b.put("role","RECEPTIONIST");return service.saveUser(b,null,actor);}
 @PutMapping("/receptionists/{id}") public Map<String,Object> editReceptionist(@PathVariable Long id,@RequestBody Map<String,Object>b,@RequestParam String actor){b.put("role","RECEPTIONIST");return service.saveUser(b,id,actor);}
 @GetMapping("/owners") public List<Map<String,Object>> owners(){return service.owners();}
 @GetMapping("/records") public Map<String,Object> records(){return service.records();}
 @GetMapping("/logs") public List<Map<String,Object>> logs(){return service.logs();}
 @GetMapping("/config") public Map<String,Object> config(){return service.config();}
 @PutMapping("/config") public Map<String,Object> saveConfig(@RequestBody Map<String,Object>b,@RequestParam String actor){return service.saveConfig(b,actor);}
}
