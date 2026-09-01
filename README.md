# Lumina Public Library

A complete role-based public library management website built with Java 21, Spring Boot, Thymeleaf, Spring Security, JPA, Flyway, and Microsoft SQL Server. It includes six independent major modules and the shared services from the project brief.

## Major modules

| Role | Primary workspace | Main URL |
|---|---|---|
| Head Librarian | Catalog Management | `/catalog` |
| Circulation Desk Staff | Circulation Management | `/circulation` |
| Library Member | Member Services | `/my-library` |
| Library Assistant | Inventory & Asset Management | `/inventory` |
| Event Coordinator | Event & Program Management | `/events` |
| Library Manager | Feedback & Complaint Management | `/feedback/manage` |

Shared capabilities include authentication, user/role administration, fines and payments, e-book links, dashboards, reporting, notifications, profiles, reading lists, and audit logs.

## Quick preview (no SQL Server required)

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

The demo profile stores records in `data/lumina-demo.mv.db`. Open <http://localhost:8081> if the browser does not open automatically.

## Run with SQL Server and SSMS

1. Install Java 21+, IntelliJ IDEA, SQL Server, and SQL Server Management Studio.
2. Connect to `localhost\SQLEXPRESS` in SSMS with Windows Authentication and run [`database/setup-sqlexpress.sql`](database/setup-sqlexpress.sql).
3. Set the connection values in the IntelliJ run configuration (`Run` → `Edit Configurations` → `Environment variables`):

```text
DB_HOST=localhost
DB_PORT=1433
DB_NAME=lumina_library
DB_USERNAME=lumina_app
DB_PASSWORD=your-strong-password
OPEN_BROWSER=true
```

4. Add `SPRING_PROFILES_ACTIVE=sqlserver` to the same environment-variable list.
5. Open this folder in IntelliJ, select JDK 21, allow Maven import, then run `LibraryApplication`.

Flyway creates and upgrades all tables automatically. Each successful website action is committed through JPA to SQL Server, so refreshing SSMS immediately shows the new record.

If using SQL Server Express, enable TCP/IP in SQL Server Configuration Manager, configure TCP port `1433`, and restart the SQL Server service.

## Demo users

All seeded demo accounts use password `Library@123`.

| Account | Email |
|---|---|
| Head Librarian | `head@lumina.lk` |
| Circulation Staff | `circulation@lumina.lk` |
| Member | `member@lumina.lk` |
| Library Assistant | `inventory@lumina.lk` |
| Event Coordinator | `events@lumina.lk` |
| Library Manager | `manager@lumina.lk` |

Change or disable demo accounts before a real deployment.

## GitHub team workflow

This project is initialized on the `common` branch. After creating an empty GitHub repository:

```powershell
git remote add origin https://github.com/YOUR-ORGANIZATION/lumina-library.git
git add .
git commit -m "Initial Lumina library system"
git push -u origin common
```

Each member clones once and checks out the shared branch:

```powershell
git clone https://github.com/YOUR-ORGANIZATION/lumina-library.git
cd lumina-library
git checkout common
```

For fewer merge conflicts, work on `feature/catalog`, `feature/circulation`, `feature/member-services`, `feature/inventory`, `feature/events`, or `feature/feedback`, then merge a pull request into `common`. See [`docs/TEAM_SETUP.md`](docs/TEAM_SETUP.md) for the complete daily workflow and shared-database topology.

## Verification

```powershell
.\mvnw.cmd clean test
```

The integration suite verifies public routes, security, all six role workspaces, and a real member borrow request transaction.

> Payment is a safe academic simulation that records a payment reference; it is not connected to a real payment gateway.
