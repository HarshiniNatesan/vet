package com.vet.controller;

import com.vet.model.User;
import com.vet.repository.PetRepository;
import com.vet.service.PetReportService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/pet-reports")
public class PetReportController {
    private final PetReportService service; private final PetRepository pets;
    public PetReportController(PetReportService service,PetRepository pets){this.service=service;this.pets=pets;}
    @PostMapping("/pets/{petId}/qr") public Map<String,Object> qr(@PathVariable Long petId,Authentication auth){authorize(petId,auth);return service.generateQr(petId);}
    @PostMapping("/pets/{petId}/revoke") public Map<String,String> revoke(@PathVariable Long petId,Authentication auth){authorize(petId,auth);service.revokeQr(petId);return Collections.singletonMap("message","QR access revoked.");}
    @GetMapping("/pets/{petId}/pdf") public ResponseEntity<byte[]> pdf(@PathVariable Long petId,Authentication auth){authorize(petId,auth);return pdfResponse(service.pdfForPetId(petId));}
    private void authorize(Long petId,Authentication auth){User u=(User)pets.findById(petId).map(p->p.getOwner()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Pet not found"));String name=auth.getName();if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")))return;if(u==null||!u.getUsername().equals(name))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not authorized to access this pet report.");}
    private ResponseEntity<byte[]> pdfResponse(byte[] bytes){return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=pet-report.pdf").body(bytes);}
}
