package com.vet.controller;

import com.vet.model.Pet;
import com.vet.service.PetReportService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/pet-report")
public class PublicPetReportController {
    private final PetReportService service;
    public PublicPetReportController(PetReportService service){this.service=service;}
    @GetMapping("/{token}")
    public ResponseEntity<byte[]> report(@PathVariable String token){
        byte[] pdf=service.pdfForToken(token);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=pet-report.pdf").body(pdf);
    }
}
