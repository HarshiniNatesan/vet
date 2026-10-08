-- Additive migration for UPI QR payment reporting. Back up the database first.
-- This migration does not delete or recreate existing payment records.
ALTER TABLE payments MODIFY COLUMN provider VARCHAR(40) NOT NULL DEFAULT 'UPI_QR';
ALTER TABLE payments ADD COLUMN reported_by VARCHAR(150) NULL;
UPDATE payments SET provider='UPI_QR' WHERE provider='RAZORPAY';
-- Payment status column is VARCHAR(20), which accommodates USER_REPORTED and CONFIRMED.
-- Existing SUCCESS records remain readable; new manual confirmations use CONFIRMED.
ALTER TABLE payments MODIFY COLUMN qr_image_url MEDIUMTEXT NULL;
