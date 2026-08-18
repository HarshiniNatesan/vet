# Veterinary Hospital Management System - Receptionist + Veterinarian Fixed

Java 11 + Spring Boot 2.7.18 + MySQL.

## Important fixes in this version

1. `pets.veterinarian_id` is mapped correctly to `Pet.assignedVeterinarian`.
2. `veterinarian_availability` is mapped to the existing columns:
   - `id`
   - `veterinarian_id`
   - `availability_date`
   - `status`
3. Availability no longer tries to save/read a non-existent `notes` column.
4. Receptionist registration stores the owner in `users` and the patient in `pets` and can assign a veterinarian.
5. Pet IDs are displayed as `PET-000001`, `PET-000002`, etc.
6. Receptionist can view veterinarians, owners, pets, appointments, grooming, availability, operations and medical records.
7. Veterinarian profile shows doctor name, doctor ID, username, specialization, qualification, phone and availability.
8. Veterinarian patient list uses the actual `pets.veterinarian_id` assignment, with appointment fallback.
9. Veterinarians can open an assigned patient and create/update medical reports, vaccinations and prescriptions.
10. Older medical/vaccination records can be updated by the veterinarian currently assigned to the pet.
11. The initializer assigns the sample `Bruno` patient to `doctor1` so the veterinarian dashboard has a test patient.

## Database

The application uses the existing database:

`veterinary_hospital`

The application is configured for the same MySQL connection that was in the uploaded project.

## Login accounts

- Receptionist: `reception` / `reception123`
- Veterinarian: `doctor1` / `doctor123`
- Owner: `owner1` / `owner123`
- Admin: `admin` / `admin123`
- Pharmacy: `pharmacy` / `pharmacy123`

## Run

From the project folder containing `pom.xml`:

```powershell
mvn clean
mvn spring-boot:run
```

If port 8080 is already occupied, stop the old Java process before starting this project.
