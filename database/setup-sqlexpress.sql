/*
 Run this script in SSMS while connected to localhost\SQLEXPRESS with
 Windows Authentication. This creates a local classroom-development login.
 Change the sample password before using the system outside local development.
*/
USE master;
GO

IF DB_ID(N'lumina_library') IS NULL
BEGIN
    CREATE DATABASE lumina_library;
END;
GO

IF SUSER_ID(N'lumina_app') IS NULL
BEGIN
    CREATE LOGIN lumina_app
    WITH PASSWORD = 'Lumina_App_2026!',
         CHECK_POLICY = ON,
         CHECK_EXPIRATION = OFF;
END;
GO

USE lumina_library;
GO

IF USER_ID(N'lumina_app') IS NULL
BEGIN
    CREATE USER lumina_app FOR LOGIN lumina_app;
END;
GO

ALTER ROLE db_owner ADD MEMBER lumina_app;
GO

SELECT DB_NAME() AS database_name,
       SUSER_SNAME() AS configured_by,
       'Lumina SQL Server setup completed' AS status;
GO
