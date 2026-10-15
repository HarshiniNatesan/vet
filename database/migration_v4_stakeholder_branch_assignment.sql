-- v4: stakeholder branch-assignment audit and safe migration support.
-- users.branch_id already exists in the multibranch schema. Do NOT assign legacy
-- accounts to an arbitrary branch: review this result and assign each account
-- using Admin > User Management after confirming the correct branch.
START TRANSACTION;
SELECT id, username, role, full_name
FROM users
WHERE role IN ('PET_OWNER','VETERINARIAN','RECEPTIONIST','PHARMACY')
  AND branch_id IS NULL
ORDER BY role, username;
COMMIT;
