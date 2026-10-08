-- Veterinary Hospital Management System - additive v2 migration.
-- The application also supports Hibernate ddl-auto=update for local development.
-- In production, apply only the ALTER statements whose columns are not already present,
-- then change spring.jpa.hibernate.ddl-auto to validate.

CREATE TABLE IF NOT EXISTS branches (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  branch_code VARCHAR(40) NOT NULL UNIQUE,
  branch_name VARCHAR(150) NOT NULL,
  address VARCHAR(500), phone VARCHAR(255), email VARCHAR(255),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  branch_id BIGINT, pet_id BIGINT NOT NULL, owner_id BIGINT NOT NULL,
  veterinarian_id BIGINT, appointment_id BIGINT, prescription_id BIGINT,
  pharmacy_transaction_id BIGINT, payment_type VARCHAR(40) NOT NULL,
  amount DECIMAL(12,2) NOT NULL, currency VARCHAR(3) NOT NULL DEFAULT 'INR',
  provider VARCHAR(40) NOT NULL DEFAULT 'RAZORPAY', provider_order_id VARCHAR(100) NOT NULL UNIQUE,
  provider_payment_id VARCHAR(100), transaction_id VARCHAR(100), status VARCHAR(20) NOT NULL,
  qr_code_id VARCHAR(100), qr_image_url VARCHAR(1000), failure_reason VARCHAR(1000),
  created_at DATETIME NOT NULL, paid_at DATETIME, last_webhook_event_id VARCHAR(100), version BIGINT,
  INDEX idx_payment_branch(branch_id), INDEX idx_payment_status(status), INDEX idx_payment_created(created_at)
);

-- Add these only if they do not already exist in the current schema:
-- ALTER TABLE users ADD COLUMN branch_id BIGINT NULL, ADD COLUMN consultation_fee DECIMAL(10,2) NULL;
-- ALTER TABLE medicines ADD COLUMN branch_id BIGINT NULL;
-- ALTER TABLE pets ADD COLUMN report_token_hash VARCHAR(64) NULL UNIQUE, ADD COLUMN report_token_issued_at DATETIME NULL,
--   ADD COLUMN report_token_expires_at DATETIME NULL, ADD COLUMN report_token_revoked BOOLEAN NOT NULL DEFAULT FALSE;
-- ALTER TABLE appointments ADD COLUMN branch_id BIGINT NULL;
-- ALTER TABLE medical_reports ADD COLUMN branch_id BIGINT NULL;
-- ALTER TABLE prescriptions ADD COLUMN branch_id BIGINT NULL;
-- ALTER TABLE pharmacy_transactions ADD COLUMN branch_id BIGINT NULL;

-- After columns exist, add foreign keys if they are not already present:
-- ALTER TABLE users ADD CONSTRAINT fk_users_branch FOREIGN KEY (branch_id) REFERENCES branches(id);
-- ALTER TABLE appointments ADD CONSTRAINT fk_appointments_branch FOREIGN KEY (branch_id) REFERENCES branches(id);
-- ALTER TABLE medical_reports ADD CONSTRAINT fk_reports_branch FOREIGN KEY (branch_id) REFERENCES branches(id);
-- ALTER TABLE prescriptions ADD CONSTRAINT fk_prescriptions_branch FOREIGN KEY (branch_id) REFERENCES branches(id);
-- ALTER TABLE pharmacy_transactions ADD CONSTRAINT fk_pharmacy_tx_branch FOREIGN KEY (branch_id) REFERENCES branches(id);
-- ALTER TABLE medicines ADD CONSTRAINT fk_medicines_branch FOREIGN KEY (branch_id) REFERENCES branches(id);
-- ALTER TABLE payments ADD CONSTRAINT fk_payment_branch FOREIGN KEY (branch_id) REFERENCES branches(id);

-- UPI QR payment additions. Apply after checking whether columns already exist.
-- ALTER TABLE payments ADD COLUMN reported_by VARCHAR(150) NULL;
-- UPDATE payments SET provider='UPI_QR' WHERE provider='RAZORPAY';
-- Existing SUCCESS rows are preserved for backward compatibility; new manual confirmations use CONFIRMED.
