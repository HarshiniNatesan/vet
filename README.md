# Veterinary Hospital Management System

Existing Java 11 / Spring Boot 2.7 / MySQL veterinary hospital application extended additively for multi-branch management, secure pet-report QR access, PDF reports, and verified Razorpay payments.

## Important architecture notes

- Existing roles and routes are retained.
- Admin is global; branch staff are assigned to a Branch entity.
- Pet owners are constrained to their own records by backend authorization.
- Pet QR links use a cryptographically random token. Only a SHA-256 hash is stored in the database; tokens can expire and be revoked.
- QR links open `pet-report-view.html`, which validates the token through the backend before loading the PDF.
- PDFs are generated from the existing database entities; no dummy medical history is inserted.
- Payment status is server controlled. The frontend cannot set `SUCCESS` directly.
- Razorpay Orders + Checkout and provider UPI QR are used. Checkout signatures and webhooks are verified server-side, and payment amounts are compared against the server-side bill.
- The uploaded/static personal QR image is not used as proof of payment.

## Environment variables

Set these outside source control:

```text
DB_USERNAME=root
DB_PASSWORD=your_database_password
APP_PUBLIC_BASE_URL=https://your-public-domain.example
APP_QR_TOKEN_EXPIRY_DAYS=365
PAYMENT_UPI_ID=yourupi@bank
PAYMENT_PAYEE_NAME=PawsCare Veterinary Hospital
RAZORPAY_WEBHOOK_SECRET=xxxxxxxxxxxxxxxx
```

Never put real secrets in Java files, HTML, JavaScript, `application.properties`, or GitHub.

## Razorpay setup

1. Create/configure the merchant account in Razorpay Dashboard.
2. Use Test Mode first and generate test API keys.
3. Configure the webhook URL as:
   `https://your-public-domain.example/api/payments/webhook`
4. Configure a webhook secret and store it as `RAZORPAY_WEBHOOK_SECRET`.
5. Subscribe to payment events required by the application, especially `payment.captured`, `payment.failed`, and `order.paid`.
6. Test checkout, UPI, amount mismatch, invalid signatures, duplicate webhook delivery, and failed/cancelled payments.
7. Only after successful sandbox testing, configure live credentials.

The backend uses the official Razorpay Java SDK. UPI QR creation is provider-side and payment-specific; a QR scan alone never marks a payment as successful.

## Database migration

For local development the project currently keeps `spring.jpa.hibernate.ddl-auto=update`, so the new Branch, Payment, token, branch, and consultation-fee columns are added automatically on startup.

For a controlled production migration, review and apply `database/migration_v2.sql` only for columns/constraints that are not already present. After the migration is verified, use `spring.jpa.hibernate.ddl-auto=validate` rather than `update`.

Existing records are not deleted. Legacy rows with no branch remain identifiable as unassigned/legacy data rather than being silently moved to a branch.

## Build

The original archive did not contain Maven Wrapper and this environment did not have Maven installed. On a machine with Maven and Java 11:

```bash
mvn clean
mvn test
mvn package
```

The expected runtime is Java 11, matching the original project.

## Demo branch setup

The initializer creates:

- `TAMBARAM` — Tambaram Branch
- `ECR` — ECR Branch

The demo veterinarian, receptionist, and pharmacy staff are assigned to Tambaram. The demo veterinarian has a server-side consultation fee of INR 500.00 for project evaluation; production fees should be configured by an authorized administrator.

## UPI QR payments (updated)

Razorpay checkout and gateway credentials are no longer used. Configure `PAYMENT_UPI_ID` and `PAYMENT_PAYEE_NAME` in the environment (or `payment.upi-id` / `payment.payee-name` in application configuration). A QR is generated per payment and contains the exact amount, INR currency, payee and bill reference. A normal UPI QR does not notify this application that money arrived; customers can report a payment and optionally enter their UTR, which sets `USER_REPORTED`. Authorized staff/admin must manually set the payment to `CONFIRMED` or `FAILED` after checking their bank/UPI app.

Apply `database/migration_v3_upi_branch.sql` only after backing up the database and checking whether `reported_by` already exists. The project uses Hibernate `ddl-auto=update` for local development; for production, use reviewed migrations and change Hibernate to `validate` after schema rollout. Set `DB_PASSWORD`, `PAYMENT_UPI_ID`, and `PAYMENT_PAYEE_NAME` before running.


## UI and stakeholder updates (v4)
- The Pet Owner's My Pets list no longer exposes View Pet Records, Generate QR, or Download Report actions. Medical records remain available from the dedicated Medical Records module.
- The Pet Owner Payments page is now read-only and shows database-backed verified Total Money Spent and pending amounts; it no longer exposes UPI QR, payment initiation, or “I Have Paid” controls.
- Admin User Management displays stakeholder branch assignments and password handling status. Passwords are intentionally never shown: they are stored as one-way hashes, and admins should use an authorized password reset/change process instead of viewing credentials.
- Stakeholder edits support assigning an active branch. New non-admin stakeholder accounts created through the admin user API must have a valid branch. Legacy unassigned accounts are not silently assigned to a random branch.

### Migration / rollout
1. Back up the database before deploying.
2. Run `database/migration_v4_stakeholder_branch_assignment.sql` to list existing stakeholder accounts that still need branch review; it intentionally does not guess branch assignments.
3. Assign each listed account through Admin > User Management after verifying its correct branch.
4. Build and run with the project's existing Java/Maven setup and environment configuration.

### Security note
The admin UI cannot display existing passwords because the application stores password hashes, not recoverable plaintext. The password column is excluded from API serialization. Displaying or recovering plaintext passwords would undermine account security.

## Latest update: branch-wise stakeholders and manual billing

- Admin **User Management** now has an `All Branches` / individual branch filter and database-backed stakeholder counts by branch and role. The Password column was removed; stored credentials and login behavior are unchanged.
- Doctors can create a consultation bill from an appointment and enter the fee for that visit. The backend associates the amount with the appointment and prevents changing an already-paid/finalized amount through the ordinary edit flow.
- Pharmacy staff can generate a medicine invoice after dispensing. The invoice includes the actual dispensed medicine line items, quantities, unit prices, subtotals, and total.
- Doctor and pharmacy bills have separate payment types. `Mark as Paid` requires a confirmation prompt and is authorized server-side by role, branch, and bill association.
- Pet Owners can review consultation charges, medicine charges, paid totals, pending totals, and individual bill statuses without payment-initiation controls on the payment summary page.

Run the application using the existing setup steps above and verify the new workflows with a MySQL instance before production use. Automated Maven tests were not run in the packaging environment because Maven was unavailable; see `IMPLEMENTATION_STATUS.md` for the verification boundary and remaining audit-ledger limitation.

## Payment details update

- Staff (veterinarian for consultation bills, pharmacy for medicine bills) record payments with a payment method (Cash, UPI, Card, Bank Transfer, Other), an optional transaction reference, and a server-generated timestamp. Consultation and medicine bills are paid independently.
- Database: run `database/migration_v5_payment_method.sql` if you manage the schema manually (it is additive). With the default `spring.jpa.hibernate.ddl-auto=update`, the two new columns (`payment_method`, `payment_method_other`) are created automatically at startup; run the script's last `UPDATE` to give older bills an invoice number.
- Build and run: `mvn clean package` then `mvn spring-boot:run` (MySQL as configured in `application.properties`).
