USE veterinary_hospital;

CREATE TABLE IF NOT EXISTS medicines (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(255) NOT NULL,
 category VARCHAR(255),
 description VARCHAR(2000),
 manufacturer VARCHAR(255),
 batch_number VARCHAR(255),
 expiry_date DATE,
 quantity INT NOT NULL DEFAULT 0,
 reorder_level INT NOT NULL DEFAULT 0,
 unit_price DOUBLE NOT NULL DEFAULT 0,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 last_updated DATE,
 INDEX idx_medicine_name(name),
 INDEX idx_medicine_expiry(expiry_date)
);

CREATE TABLE IF NOT EXISTS pharmacy_transactions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 prescription_id BIGINT NOT NULL,
 pet_id BIGINT NOT NULL,
 medicine_id BIGINT NOT NULL,
 pharmacy_staff_id BIGINT NOT NULL,
 quantity_dispensed INT NOT NULL,
 batch_number VARCHAR(255),
 unit_price DOUBLE NOT NULL,
 total_amount DOUBLE NOT NULL,
 dispensing_date DATETIME NOT NULL,
 dispensing_status VARCHAR(50) NOT NULL,
 INDEX idx_pharmacy_txn_date(dispensing_date),
 INDEX idx_pharmacy_txn_prescription(prescription_id),
 CONSTRAINT fk_txn_prescription FOREIGN KEY(prescription_id) REFERENCES prescriptions(id),
 CONSTRAINT fk_txn_pet FOREIGN KEY(pet_id) REFERENCES pets(id),
 CONSTRAINT fk_txn_medicine FOREIGN KEY(medicine_id) REFERENCES medicines(id),
 CONSTRAINT fk_txn_staff FOREIGN KEY(pharmacy_staff_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS admin_activity_logs (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT,
 action VARCHAR(255) NOT NULL,
 description VARCHAR(2000),
 event_time DATETIME NOT NULL,
 CONSTRAINT fk_log_user FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS system_config (
 id BIGINT PRIMARY KEY,
 hospital_name VARCHAR(255),
 contact_information VARCHAR(255),
 address VARCHAR(255),
 operating_hours VARCHAR(255)
);

-- These columns are also created automatically by Hibernate ddl-auto=update.
-- If your existing schema already contains them, no manual action is required:
-- prescriptions.status, prescriptions.required_quantity
-- users.active, users.created_at, users.last_login
