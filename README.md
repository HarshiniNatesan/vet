# Veterinary Hospital Management System — Integrated Pharmacy & Admin Extension

## Stack
Java 11, Spring Boot 2.7.18, Spring MVC, Spring Data JPA/Hibernate, MySQL, HTML/CSS/JavaScript and Spring Security.

## Added modules
- Pharmacy Dashboard
- Medicine inventory with automatic stock status
- Low-stock and expiry indicators
- Veterinarian-created prescription retrieval
- Transactional medicine dispensing and stock reduction
- Pharmacy transaction history
- Pet/prescription search
- Administrator dashboard
- User, veterinarian and pet-owner management
- Hospital statistics
- Hospital records summary
- System configuration
- Administrative activity logs
- Backend role-based authorization

## Run
1. Create MySQL database using `database/schema.sql`.
2. Check `src/main/resources/application.properties` and set your MySQL username/password.
3. Use Java 11.
4. Run `mvn spring-boot:run` from the project directory, or package with `mvn clean package` and run the generated jar.
5. Open `http://localhost:8080/login.html`.

JPA is configured with `ddl-auto=update`, so the new pharmacy/admin tables are added without requiring destructive schema replacement.

## Demo credentials
- Pet Owner: `owner1` / `owner123`
- Veterinarian: `doctor1` / `doctor123`
- Receptionist: `reception` / `reception123`
- Pharmacy: `pharmacy` / `pharmacy123`
- Admin: `admin` / `admin123`

## Pharmacy workflow
Veterinarian creates prescription → prescription appears in Pharmacy → pharmacy opens it → system validates medicine, expiry and stock → stock is reduced transactionally → pharmacy transaction is recorded → prescription status changes to DISPENSED or PARTIALLY_DISPENSED → low-stock calculation and dashboard statistics update.

## Important
Do not expose passwords or secrets in frontend code. Change demo passwords before real deployment.
