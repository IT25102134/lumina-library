-- V5: Validate and constrain reading_list_items status/priority columns.
-- Previously attempted as V3 (conflict); moved to V5 for a clean migration history.
-- Safe on SQL Server: sets defaults then adds NOT NULL constraints.

UPDATE reading_list_items
SET priority = 'NORMAL'
WHERE priority IS NULL OR priority NOT IN ('HIGH', 'NORMAL', 'LOW');
GO

UPDATE reading_list_items
SET status = 'PLAN_TO_READ'
WHERE status IS NULL OR status NOT IN ('PLAN_TO_READ', 'READING', 'COMPLETED');
GO

ALTER TABLE reading_list_items ALTER COLUMN priority NVARCHAR(20) NOT NULL;
GO

ALTER TABLE reading_list_items ALTER COLUMN status NVARCHAR(30) NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_reading_list_priority' AND parent_object_id = OBJECT_ID('dbo.reading_list_items'))
    ALTER TABLE reading_list_items ADD CONSTRAINT ck_reading_list_priority
        CHECK (priority IN ('HIGH', 'NORMAL', 'LOW'));
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_reading_list_status' AND parent_object_id = OBJECT_ID('dbo.reading_list_items'))
    ALTER TABLE reading_list_items ADD CONSTRAINT ck_reading_list_status
        CHECK (status IN ('PLAN_TO_READ', 'READING', 'COMPLETED'));
GO
