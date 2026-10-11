-- V4: Add reading goal columns to reading_list_items
-- These columns enable status/priority/notes for the full reading-list CRUD.
-- Previously attempted as V2 (conflict); moved to V4 for a clean migration history.

IF COL_LENGTH('dbo.reading_list_items','notes') IS NULL
    ALTER TABLE reading_list_items ADD notes NVARCHAR(500) NULL;
GO

IF COL_LENGTH('dbo.reading_list_items','priority') IS NULL
    ALTER TABLE reading_list_items ADD priority NVARCHAR(20) NULL;
GO

IF COL_LENGTH('dbo.reading_list_items','status') IS NULL
    ALTER TABLE reading_list_items ADD status NVARCHAR(30) NULL;
GO
