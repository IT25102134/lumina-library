-- Circulation-only, READ-ONLY verification for SSMS.
-- Select your existing Lumina database in the SSMS dropdown before running.
-- This file does not CREATE, ALTER, INSERT, UPDATE or DELETE any table/record.

SELECT l.id AS loan_id, u.membership_number, u.first_name, u.last_name,
       b.title, c.barcode, l.status AS loan_status, c.status AS copy_status,
       l.requested_at, l.issued_at, l.due_at, l.returned_at, l.renewal_count
FROM dbo.loans l
JOIN dbo.user_accounts u ON u.id = l.member_id
JOIN dbo.book_copies c ON c.id = l.copy_id
JOIN dbo.books b ON b.id = c.book_id
ORDER BY l.id DESC;

-- Active registered members used by the New Loan selector.
SELECT id, first_name, last_name, membership_number, email
FROM dbo.user_accounts
WHERE role = 'MEMBER' AND active = 1;

-- Available copies whose catalogue record is active.
SELECT c.id AS copy_id, c.barcode, b.title, c.status
FROM dbo.book_copies c
JOIN dbo.books b ON b.id = c.book_id
WHERE c.status = 'AVAILABLE' AND b.active = 1;

-- Fine rows prevent physical deletion of their loans, including paid fines.
SELECT id, loan_id, member_id, amount, status
FROM dbo.fine_payments
ORDER BY id DESC;

-- Audit remains after a loan is deleted (entity_id is not a loan foreign key).
SELECT id, actor, action, entity_id, details, occurred_at
FROM dbo.audit_logs
WHERE entity_type = 'Loan'
ORDER BY id DESC;
