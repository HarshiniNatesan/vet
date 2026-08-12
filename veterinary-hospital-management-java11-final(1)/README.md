# Veterinary Hospital Management System — Java 11

## Technology
- Java 11
- Spring Boot 2.7.18
- Spring Security
- Spring Data JPA / Hibernate
- MySQL
- HTML/CSS/JavaScript

## Project structure
src/main/java/com/vet
  config
  controller
  model
  repository
src/main/resources
  static
    css
    js
    *.html
  application.properties
database/schema.sql

## Setup
1. Install Java 11.
2. Install MySQL 8.x.
3. Create the database:
   `CREATE DATABASE veterinary_hospital;`
4. Edit `src/main/resources/application.properties`.
5. Replace `YOUR_MYSQL_PASSWORD` with your MySQL password.
6. Open a terminal in the folder containing pom.xml.
7. Run:
   `mvn spring-boot:run`
8. Open:
   http://localhost:8080/

## Demo accounts
Pet Owner: owner1 / owner123
Veterinarian: doctor1 / doctor123
Admin: admin / admin123
Receptionist: reception / reception123
Pharmacy: pharmacy / pharmacy123

## Important
This version is intentionally Java 11 compatible. It uses Spring Boot 2.7.18 and javax.persistence rather than the Jakarta APIs used by Spring Boot 3.
The Pet Owner and Veterinarian dashboards are functional. Admin, Receptionist, and Pharmacy have login access and extension-ready dashboard pages.
