IF DB_ID(N'lumina_library') IS NULL
BEGIN
    CREATE DATABASE lumina_library;
END;
GO

USE lumina_library;
GO

SELECT DB_NAME() AS database_name, 'Ready for Lumina Flyway migrations' AS status;
GO
