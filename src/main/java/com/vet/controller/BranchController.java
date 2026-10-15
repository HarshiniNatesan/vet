package com.vet.controller;

import com.vet.service.BranchService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin/branches")
public class BranchController {
    private final BranchService service;
    public BranchController(BranchService service){this.service=service;}
    @GetMapping public List<Map<String,Object>> list(){return service.list();}
    @PostMapping public Map<String,Object> create(@RequestBody Map<String,Object> body){return service.save(null,body);}
    @PutMapping("/{id}") public Map<String,Object> edit(@PathVariable Long id,@RequestBody Map<String,Object> body){return service.save(id,body);}
    @PatchMapping("/{id}/status") public Map<String,Object> status(@PathVariable Long id,@RequestParam boolean active){return service.status(id,active);}
    @PostMapping("/{branchId}/users/{userId}") public Map<String,Object> assign(@PathVariable Long branchId,@PathVariable Long userId){return service.assignUser(branchId,userId);}
    @GetMapping("/statistics") public Map<String,Object> allStats(){return service.statistics(null);}
    @GetMapping("/{id}/statistics") public Map<String,Object> stats(@PathVariable Long id){return service.statistics(id);}
}
