-- ==========================================
-- V4 - Update Feedback Schema
-- Lumina Library Management System
-- SQL Server / Flyway Migration
-- ==========================================

-- 1. Add archived column if missing

IF COL_LENGTH('dbo.feedback_items', 'archived') IS NULL
BEGIN
ALTER TABLE dbo.feedback_items
    ADD archived BIT NOT NULL DEFAULT 0;
END;


-- 2. Add category column if missing

IF COL_LENGTH('dbo.feedback_items', 'category') IS NULL
BEGIN
ALTER TABLE dbo.feedback_items
    ADD category NVARCHAR(60) NOT NULL
        DEFAULT 'GENERAL';
END;

-- 3. Add priority column if missing

IF COL_LENGTH('dbo.feedback_items', 'priority') IS NULL
BEGIN
ALTER TABLE dbo.feedback_items
    ADD priority NVARCHAR(20) NOT NULL
        DEFAULT 'MEDIUM';
END;


-- 4. Add updated_at column if missing

IF COL_LENGTH('dbo.feedback_items', 'updated_at') IS NULL
BEGIN
ALTER TABLE dbo.feedback_items
    ADD updated_at DATETIME2 NOT NULL
        DEFAULT SYSDATETIME();
END;


-- 5. Add reference_number column if missing

IF COL_LENGTH('dbo.feedback_items', 'reference_number') IS NULL
BEGIN
ALTER TABLE dbo.feedback_items
    ADD reference_number NVARCHAR(30) NULL;
END;


-- 6. Generate reference numbers for existing records

EXEC sp_executesql N'
    UPDATE dbo.feedback_items
    SET reference_number =
        CONCAT(''FCM-OLD-'', id)
    WHERE reference_number IS NULL;
';


-- 7. Make reference_number NOT NULL

IF EXISTS
(
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.feedback_items')
      AND name = 'reference_number'
      AND is_nullable = 1
)
BEGIN
ALTER TABLE dbo.feedback_items
ALTER COLUMN reference_number NVARCHAR(30) NOT NULL;
END;


-- 8. Add unique constraint if missing

IF NOT EXISTS
(
    SELECT 1
    FROM sys.key_constraints
    WHERE name = 'UQ_feedback_reference'
      AND parent_object_id = OBJECT_ID('dbo.feedback_items')
)
BEGIN
ALTER TABLE dbo.feedback_items
    ADD CONSTRAINT UQ_feedback_reference
        UNIQUE (reference_number);
END;


-- 9. Create feedback_replies table if missing

IF OBJECT_ID('dbo.feedback_replies', 'U') IS NULL
BEGIN
CREATE TABLE dbo.feedback_replies
(
    id BIGINT IDENTITY(1,1) PRIMARY KEY,

    feedback_id BIGINT NOT NULL
        REFERENCES dbo.feedback_items(id),

    author_id BIGINT NOT NULL
        REFERENCES dbo.user_accounts(id),

    message NVARCHAR(2500) NOT NULL,

    created_at DATETIME2 NOT NULL
);
END;