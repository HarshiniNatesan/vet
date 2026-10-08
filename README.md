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
