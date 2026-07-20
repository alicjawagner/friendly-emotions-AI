# ADR-010: Room Schema Versioning and Migration Strategy

# Status
Accepted

# Context
The Friendly Words analysis (H-9 and L-1 findings) identified that the production database used `fallbackToDestructiveMigration`, which silently destroyed all user data on any schema change. For an educational therapy app where therapists invest significant time configuring learning steps and uploading custom images, data loss on an app update is a critical failure.

Friendly Emotions must handle schema evolution safely from the very first version, because retrofitting a migration strategy onto an existing database with user data is extremely difficult.

# Decision
The Room database is configured for safe, explicit schema management from version 1:

- **`exportSchema = true`** is permanent. Room exports a JSON schema file on every build. Schema JSON files are committed to source control alongside the migration code.
- **Database version starts at 1.** Every structural change increments the version number.
- **`fallbackToDestructiveMigration` is never enabled.** Any attempt to enable it is a prohibited change.
- **`AutoMigration` is used for simple additions** (adding a nullable column with a default value, adding a new table). Room generates the migration SQL automatically.
- **Manual migration scripts** (written in `MigrationX_Y.kt`) are used for structural changes: renaming columns, changing column types, splitting or merging tables, and any change that `AutoMigration` cannot handle automatically.
- **`DatabaseInitializer`** seeds example content on first launch only. It checks for the presence of any `LearningStepEntity` before seeding, ensuring it does not re-seed after an upgrade migration.

Every migration is tested with a Room migration test that applies it against the previous schema and verifies the resulting schema matches the exported JSON.

# Consequences
**Benefits:**
- User data (custom learning steps, uploaded images, folder configurations) is preserved across every app update.
- Schema history is fully auditable in source control — the exported JSON and migration scripts document every change.
- Developers receive a clear signal when a migration is required (Room build error on version mismatch) and a clear template for how to write it.

**Trade-offs:**
- Every schema change requires writing a migration, adding a test for it, and incrementing the version number. This is intentional overhead that prevents silent data loss.
- Exported schema JSON files add noise to git diffs but are mandatory for Room migration testing to work correctly.

**Limitations:**
- If a migration contains a bug that reaches production, fixing it requires another migration version. There is no rollback mechanism. This places a premium on testing migrations thoroughly before release.

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §2 Architectural Principle #10, §9.1 Room Database, §20 Architecture Decision Summary (schema migration row), §21 Top Architectural Principles for Implementation (#10)
- `docs/friendly-words/06-architecture-improvement-analysis.md` — H-9 finding (fallbackToDestructiveMigration), L-1 finding (no schema export)
