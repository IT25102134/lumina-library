-- Add missing feedback columns
ALTER TABLE dbo.feedback_items
    ADD
        archived BIT NOT NULL DEFAULT 0,
    category NVARCHAR(60) NOT NULL DEFAULT 'GENERAL',
    priority NVARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    reference_number NVARCHAR(30) NULL;

-- Generate reference numbers for existing records
UPDATE dbo.feedback_items
SET reference_number = CONCAT('FCM-OLD-', id)
WHERE reference_number IS NULL;

-- Make reference number required and unique
ALTER TABLE dbo.feedback_items
ALTER COLUMN reference_number NVARCHAR(30) NOT NULL;

ALTER TABLE dbo.feedback_items
    ADD CONSTRAINT UQ_feedback_reference
        UNIQUE (reference_number);

-- Create the missing replies table
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