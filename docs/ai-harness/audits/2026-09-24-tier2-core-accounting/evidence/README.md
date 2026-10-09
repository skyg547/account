# Reproducing this audit's evidence

Run from the repository root at commit `a97d10ab6efc2570a88f83d630242cd748f8be57`. Required tools are the existing Gradle 8.7 cache/dependencies, the repository's installed Java 17 toolchain, a Java 21 shell runtime for these diagnostic probes, and Python 3. No package installation or network download is part of these commands.

The audit first ran the exact six-project offline test command. All 52 tasks were up to date, so a second run added `--rerun-tasks` to execute them freshly. The saved XML files and `../test-summary.json` describe the second run: 69 suites, 369 tests, zero failures/errors/skips. Probe observations are not added to these test counts.

```bash
./gradlew :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test :closing:core:test :closing:api:test :closing:batch:test --offline --no-daemon --console=plain --max-workers=2 --rerun-tasks
python3 docs/ai-harness/audits/2026-09-24-tier2-core-accounting/evidence/run-probes.py
```

The launcher exports the six projects' resolved test runtime classpaths using an external Gradle init script, compiles the saved `AuditProbes.java`, and executes it against the repository's compiled classes. It writes current diagnostics only to `/tmp/account-tier2-audit-evidence`; the saved audit evidence is preserved. It does not start a Spring Boot application or load production connection configuration. It creates only synthetic in-memory H2 tables for P12. Run the fresh module build first if class files are absent or changed.

Expected launcher result: exit 0 and 17 observation assertions marked PASS. One assertion group checks existing positive controls; the other 16 reproduce defects. **PASS means the described behavior was observed, not that the application is financially correct.** A remediation may deliberately make a defect assertion fail, at which point this audit probe must be replaced by a test of the intended behavior. The slip-collision sample is random: the recorded run found 197 duplicates among 5,000 allocations; future counts differ. Its theoretical key space is only 65,536 per day, independently of that sample.

Probe limitations:

- Most use actual domain/application classes with Mockito ports; they do not establish database isolation or HTTP authorization behavior.
- P03 proves in-memory mutability. Posted-detail foreign keys may prevent deletion; no successful database deletion is asserted.
- P06 exercises the default Jackson untyped mapper. A complete remediation must also test the real MVC/message binding configuration.
- P07 injects a status absent from today's master enum. Active locks and calendar IN_PROGRESS bypass are established by source tracing separately.
- P12 executes the actual FX-source SQL against minimal synthetic H2 tables; it does not test PostgreSQL migration, permissions, cursor streaming or load.
- P14 deliberately uses unequal currency units exposed by the current adapters; it is not a production balance lookup.
- P17 proves annual command generation with controlled query/posting ports, not account existence or persistence.

`classpath.log` records an initial temporary Gradle script error; `classpath-retry.log` records its correction. `probe-compile.log` records an initial temporary getter typo; `probe-compile-final.log` records successful compilation. Neither required editing repository build or product code. The final probe result is `probe-results.json`, with console output in `probes.log`.

The original working sources/logs remain in `/tmp/account-tier2-audit-evidence`. The copied evidence here, the source commit, exact issue references and source fingerprints make the package reviewable even when that temporary directory is unavailable.
