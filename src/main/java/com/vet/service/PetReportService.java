package com.vet.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vet.model.*;
import com.vet.repository.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PetReportService {
    private final PetRepository pets; private final MedicalReportRepository reports;
    private final VaccinationRepository vaccinations; private final PrescriptionRepository prescriptions;
    private final AppointmentRepository appointments;
    private final SecureRandom random = new SecureRandom();
    private final String baseUrl; private final long expiryDays;

    public PetReportService(PetRepository pets, MedicalReportRepository reports, VaccinationRepository vaccinations,
                            PrescriptionRepository prescriptions, AppointmentRepository appointments,
                            @Value("${app.public-base-url:http://localhost:8080}") String baseUrl,
                            @Value("${app.qr.token-expiry-days:365}") long expiryDays) {
        this.pets=pets; this.reports=reports; this.vaccinations=vaccinations; this.prescriptions=prescriptions;
        this.appointments=appointments; this.baseUrl=baseUrl.replaceAll("/$", ""); this.expiryDays=expiryDays;
    }

    @Transactional
    public Map<String,Object> generateQr(Long petId) {
        Pet pet = pets.findById(petId).orElseThrow(() -> new IllegalArgumentException("Pet not found."));
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now=LocalDateTime.now();
        pet.setReportTokenHash(sha256(token)); pet.setReportTokenIssuedAt(now);
        pet.setReportTokenExpiresAt(now.plusDays(Math.max(1, expiryDays))); pet.setReportTokenRevoked(false);
        pets.save(pet);
        String url = baseUrl + "/pet-report-view.html?token=" + token;
        try {
            BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 420, 420);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix, new MatrixToImageConfig());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "PNG", out);
            Map<String,Object> result=new LinkedHashMap<>(); result.put("petId",pet.getId()); result.put("petCode",pet.getPetCode());
            result.put("url",url); result.put("expiresAt",pet.getReportTokenExpiresAt());
            result.put("imageDataUrl","data:image/png;base64,"+Base64.getEncoder().encodeToString(out.toByteArray())); return result;
        } catch(Exception e){throw new IllegalStateException("QR generation failed.",e);}
    }

    @Transactional
    public void revokeQr(Long petId) { Pet p=pets.findById(petId).orElseThrow(()->new IllegalArgumentException("Pet not found.")); p.setReportTokenRevoked(true); pets.save(p); }

    public Pet validateToken(String token) {
        if(token==null || token.length()<40 || token.length()>100) throw new IllegalArgumentException("Invalid or expired report link.");
        String hash=sha256(token);
        Pet p=pets.findByReportTokenHash(hash).orElseThrow(()->new IllegalArgumentException("Invalid or expired report link."));
        if(Boolean.TRUE.equals(p.getReportTokenRevoked()) || p.getReportTokenExpiresAt()==null || p.getReportTokenExpiresAt().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("This report link is expired or revoked.");
        return p;
    }

    public byte[] pdfForToken(String token) { return pdfForPet(validateToken(token)); }
    public byte[] pdfForPetId(Long id) { return pdfForPet(pets.findById(id).orElseThrow(()->new IllegalArgumentException("Pet not found."))); }

    private byte[] pdfForPet(Pet p) {
        try (PDDocument doc=new PDDocument(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            PDPage page=new PDPage(PDRectangle.A4); doc.addPage(page);
            PDPageContentStream cs=new PDPageContentStream(doc,page);
            float y=790; float left=45;
            cs.setFont(PDType1Font.HELVETICA_BOLD,20); cs.beginText(); cs.newLineAtOffset(left,y); cs.showText("Veterinary Hospital - Pet Report"); cs.endText(); y-=30;
            cs.setFont(PDType1Font.HELVETICA,10); y=write(cs,left,y,"Generated: "+LocalDateTime.now()); y-=8;
            cs.setFont(PDType1Font.HELVETICA_BOLD,13); y=section(cs,left,y,"Pet Details");
            cs.setFont(PDType1Font.HELVETICA,10); y=write(cs,left,y,"Pet ID: "+safe(p.getPetCode())); y=write(cs,left,y,"Name: "+safe(p.getName()));
            y=write(cs,left,y,"Species: "+safe(p.getSpecies())+"   Breed: "+safe(p.getBreed())); y=write(cs,left,y,"Gender: "+safe(p.getGender())+"   Age: "+safe(p.getAge())+"   Weight: "+safe(p.getWeight()));
            y=write(cs,left,y,"Allergies: "+safe(p.getAllergies())); y=write(cs,left,y,"Existing conditions: "+safe(p.getMedicalConditions())); y-=8;
            cs.setFont(PDType1Font.HELVETICA_BOLD,13); y=section(cs,left,y,"Owner"); cs.setFont(PDType1Font.HELVETICA,10);
            if(p.getOwner()!=null){ y=write(cs,left,y,"Name: "+safe(p.getOwner().getFullName())); y=write(cs,left,y,"Phone: "+safe(p.getOwner().getPhone())+"   Email: "+safe(p.getOwner().getEmail())); }
            y-=8; cs.setFont(PDType1Font.HELVETICA_BOLD,13); y=section(cs,left,y,"Medical Records"); cs.setFont(PDType1Font.HELVETICA,9);
            List<MedicalReport> rs=reports.findByPetAndCompletedTrue(p); if(rs.isEmpty()) y=write(cs,left,y,"No completed medical records available.");
            for(MedicalReport r:rs){ y=writeWrapped(cs,left,y,"Visit "+safe(r.getVisitDate())+" - "+safe(r.getVeterinarian()==null?null:r.getVeterinarian().getFullName())); y=writeWrapped(cs,left,y,"Diagnosis: "+safe(r.getDiagnosis())); y=writeWrapped(cs,left,y,"Treatment: "+safe(r.getTreatment())); y-=3; if(y<100){cs.close(); page=new PDPage(PDRectangle.A4); doc.addPage(page); cs=new PDPageContentStream(doc,page); y=790; cs.setFont(PDType1Font.HELVETICA,9);} }
            cs.setFont(PDType1Font.HELVETICA_BOLD,13); y=section(cs,left,y,"Vaccinations"); cs.setFont(PDType1Font.HELVETICA,9);
            List<VaccinationRecord> vs=vaccinations.findByPet(p); if(vs.isEmpty()) y=write(cs,left,y,"No vaccination records available.");
            for(VaccinationRecord v:vs){ y=write(cs,left,y,safe(v.getVaccineName())+" | Administered: "+safe(v.getAdministeredDate())+" | Next due: "+safe(v.getNextDueDate())); }
            y-=8; cs.setFont(PDType1Font.HELVETICA_BOLD,13); y=section(cs,left,y,"Prescriptions"); cs.setFont(PDType1Font.HELVETICA,9);
            List<Prescription> ps=prescriptions.findByPetOrderByIdDesc(p); if(ps.isEmpty()) y=write(cs,left,y,"No prescriptions available.");
            for(Prescription r:ps){ y=writeWrapped(cs,left,y,safe(r.getMedicineName())+" | "+safe(r.getDosage())+" | "+safe(r.getFrequency())+" | "+safe(r.getDuration())); y=writeWrapped(cs,left,y,"Instructions: "+safe(r.getInstructions())); }
            y-=8; cs.setFont(PDType1Font.HELVETICA_BOLD,13); y=section(cs,left,y,"Appointments"); cs.setFont(PDType1Font.HELVETICA,9);
            List<Appointment> as=appointments.findByOwnerOrderByAppointmentDateAscAppointmentTimeAsc(p.getOwner());
            long petAppointments=as.stream().filter(a->a.getPet()!=null&&a.getPet().getId().equals(p.getId())).count(); y=write(cs,left,y,"Recorded appointments for this pet: "+petAppointments);
            cs.close(); doc.save(out); return out.toByteArray();
        } catch(Exception e){throw new IllegalStateException("PDF report generation failed.",e);}
    }

    private float section(PDPageContentStream cs,float x,float y,String s)throws Exception{cs.beginText();cs.newLineAtOffset(x,y);cs.showText(s);cs.endText();return y-18;}
    private float write(PDPageContentStream cs,float x,float y,String s)throws Exception{cs.beginText();cs.newLineAtOffset(x,y);cs.showText(safePdf(s));cs.endText();return y-14;}
    private float writeWrapped(PDPageContentStream cs,float x,float y,String s)throws Exception{String v=safePdf(s);int max=105;for(int i=0;i<v.length();i+=max)y=write(cs,x,y,v.substring(i,Math.min(v.length(),i+max)));return y;}
    private String safe(Object v){return v==null?"-":String.valueOf(v);}
    private String safePdf(String v){return v==null?"-":v.replaceAll("[^\\x20-\\x7E]","?");}
    private String sha256(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format("%02x",x));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
}
