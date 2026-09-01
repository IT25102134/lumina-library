# Team setup and collaboration

## The two things being shared

GitHub and SQL Server solve different problems:

- **GitHub shares source code.** A push does not copy database rows.
- **One shared SQL Server instance shares live records.** Every laptop must point `DB_URL` to that same host for records to appear for the whole team.

For classroom development, nominate one reachable PC as the database host or use Azure SQL. On a LAN, enable SQL Server TCP/IP, use a fixed port, allow inbound TCP 1433 in the host firewall, and use a database login with only the permissions this application needs. Do not commit passwords.

Example team connection:

```text
DB_HOST=192.168.1.25
DB_PORT=1433
DB_NAME=lumina_library
DB_USERNAME=lumina_app
DB_PASSWORD=<stored only in each member's IntelliJ configuration>
```

If members use `localhost`, each person has a separate database and records will not synchronize.

## IntelliJ setup for every member

1. Install Java 21 and IntelliJ IDEA.
2. Clone the repository and open the repository folder, not only `src`.
3. Trust the Maven project and set Project SDK to 21.
4. Open `LibraryApplication.java`, choose **Run LibraryApplication**.
5. Add `SPRING_PROFILES_ACTIVE=sqlserver`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` to that run configuration. Omit these variables to use the automatic demo database.
6. Keep `OPEN_BROWSER=true` to open the website automatically after startup.
7. Confirm the console says `Tomcat started on port 8080`.

## Recommended branch ownership

| Member | Feature branch | Main files |
|---|---|---|
| 1 | `feature/catalog` | `CatalogController`, `catalog.html`, `book-form.html` |
| 2 | `feature/circulation` | `CirculationController`, `circulation.html` |
| 3 | `feature/member-services` | `MemberController`, `my-library.html` |
| 4 | `feature/inventory` | `InventoryController`, `inventory.html` |
| 5 | `feature/events` | `EventController`, `events*.html` |
| 6 | `feature/feedback` | `FeedbackController`, `feedback*.html` |

Daily work:

```powershell
git checkout common
git pull origin common
git checkout feature/catalog
git merge common
# edit and test
.\mvnw.cmd test
git add .
git commit -m "Catalog: describe the change"
git push -u origin feature/catalog
```

Open a pull request into `common`; another member reviews it before merging. If your course requires direct pushes, run `git pull --rebase origin common` before `git push origin common` and resolve conflicts locally.

## Database change rules

- Never edit `V1__initial_schema.sql` after it has run on a shared database.
- Add the next migration, for example `V2__add_member_address.sql`.
- Flyway applies pending migrations automatically when the application starts.
- Back up the shared database before a risky migration.
- Do not commit `.env`, database backups, `.mdf` files, or passwords.

## Troubleshooting

- **Login failed for user:** verify SQL authentication is enabled and credentials are correct.
- **TCP/IP connection failed:** verify SQL Server is running, TCP/IP and port 1433 are enabled, and the firewall permits the port.
- **Port 8080 already used:** set `SERVER_PORT=8081`.
- **Schema validation failed:** pull the newest migration and restart; do not manually change production tables.
- **Website does not open:** visit `http://localhost:8080`; headless environments cannot launch a browser automatically.
