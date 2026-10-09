#!/usr/bin/env python3
"""Re-run observations against compiled repository code; keep frozen audit evidence unchanged."""
from pathlib import Path
import json
import subprocess

evidence = Path(__file__).resolve().parent
repository = evidence.parents[4]
scratch = Path('/tmp/account-tier2-audit-evidence')
scratch.mkdir(exist_ok=True)
init_script = scratch / 'classpath.init.gradle'
init_script.write_text((evidence / 'classpath.init.gradle').read_text())

# Resolve the actual projects' classpaths rather than guessing from every cached JAR.
subprocess.run([
    './gradlew', 'tier2AuditClasspath', '-I', str(init_script), '--offline',
    '--no-daemon', '--console=plain', '--max-workers=2'
], cwd=repository, check=True)
classpath = (scratch / 'classpath.txt').read_text()
subprocess.run([
    'javac', '-proc:none', '-cp', classpath, '-d', str(scratch),
    str(evidence / 'AuditProbes.java')
], cwd=repository, check=True)
subprocess.run([
    'java', '-cp', str(scratch) + ':' + classpath, 'AuditProbes'
], cwd=repository, check=True)
observations = json.loads((scratch / 'probe-results.json').read_text())
print(f"Observed {len(observations)} probes; saved audit evidence was not modified.")
