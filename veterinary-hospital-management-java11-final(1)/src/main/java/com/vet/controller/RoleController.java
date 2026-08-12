package com.vet.controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api")
public class RoleController {
    @GetMapping("/admin/dashboard") public Map<String,String> admin(){return Collections.singletonMap("message","Admin dashboard ready for extension");}
    @GetMapping("/reception/dashboard") public Map<String,String> reception(){return Collections.singletonMap("message","Receptionist dashboard ready for extension");}
    @GetMapping("/pharmacy/dashboard") public Map<String,String> pharmacy(){return Collections.singletonMap("message","Pharmacy dashboard ready for extension");}
}
