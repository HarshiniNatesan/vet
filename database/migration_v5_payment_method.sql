-- Additive migration: record the transaction method on payments.
-- Back up the database first. No existing rows are deleted or rewritten except the invoice-number backfill below.
-- (spring.jpa.hibernate.ddl-auto=update adds the two columns automatically; run this script if you manage the schema manually.)
ALTER TABLE payments ADD COLUMN payment_method VARCHAR(20) NULL;
ALTER TABLE payments ADD COLUMN payment_method_other VARCHAR(60) NULL;

-- Give older staff-created bills (which had no order/invoice number) a stable invoice number.
UPDATE payments SET provider_order_id = CONCAT('INV-', LPAD(id, 6, '0')) WHERE provider_order_id IS NULL;

-- Historical paid bills have no recorded method; leave payment_method NULL (shown as blank in the UI).
