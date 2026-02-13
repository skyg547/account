---
description: role-based feature development workflow
---
# Feature Development Workflow

This workflow follows the roles defined in `.clinerules`.

1. **[PM] Requirement Analysis**
   - Review project requirements.
   - Update `docs/requirements.md`.

2. **[DA] Schema Design**
   - Create or update `docs/db/schema.sql`.
   - Update `docs/db/table_spec.md`.

3. **[Backend] Implementation**
   - Create JPA Entities based on schema.
   - Implement Service and Controller logic.

4. **[QA] Verification**
   - Run `./gradlew test`.
   - Verify API endpoints.

5. **[SRE] Deployment Prep**
   - Update `docker-compose.yml` if necessary.
