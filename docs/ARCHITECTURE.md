# Architecture

The application follows a conventional Spring MVC structure:

```text
Browser / Thymeleaf views
        ↓ HTTP + role authorization
Spring MVC controllers
        ↓ transactions
Spring Data JPA repositories
        ↓ JDBC
Microsoft SQL Server
```

`SecurityConfig` controls route access. `CurrentUserService` maps the signed-in email to the persisted account. Major actions create an `AuditLog` row and relevant workflows create `Notification` rows. Flyway owns schema evolution; `SeedData` creates demonstration users and records only when the user table is empty.

The generated hero asset is stored locally at `src/main/resources/static/images/lumina-library-hero.png`; the application does not depend on an image-generation service at runtime.
