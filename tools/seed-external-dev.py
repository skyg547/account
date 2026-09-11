#!/usr/bin/env python3
"""Load and verify deterministic synthetic fixtures in existing development DBs."""
import argparse
from pathlib import Path
import sys

from business_batch_support import (BatchError, PACKAGES, assert_engine_idle, assert_query, configure_transport, database_inputs,
                                    execution_lock, load_inputs, query, seed_path)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("verify", "seed"))
    parser.add_argument("--package", choices=(*PACKAGES, "all"), default="all")
    parser.add_argument("--engine", choices=("podman", "docker"), default="podman")
    parser.add_argument("--env-file", type=Path, required=True)
    parser.add_argument("--podman-socket", help="existing current-user Unix socket, for hosts under I/O pressure")
    args = parser.parse_args(argv)
    try:
        configure_transport(args.podman_socket)
        with execution_lock():
            assert_engine_idle(args.engine)
            values = load_inputs(args.env_file)
            contexts = [c for p, members in PACKAGES.items()
                        if args.package in (p, "all") for c in members]
            # Validate every selected context before the first database mutation.
            for context in contexts:
                database_inputs(values, context)
                for filename in ("seed.sql", "verify.sql"):
                    if not seed_path(context, filename).is_file():
                        raise BatchError(f"{context}: complete seed package is required")
            for context in contexts:
                if args.action == "seed":
                    sql = seed_path(context, "seed.sql").read_text(encoding="utf-8")
                    assertion = seed_path(context, "verify.sql").read_text(encoding="utf-8").strip().removesuffix(";")
                    query(args.engine, values, context,
                          "BEGIN; SELECT pg_advisory_xact_lock(690);\n" + sql
                          + "\nDO $gh690$ BEGIN IF NOT COALESCE((" + assertion
                          + "), FALSE) THEN RAISE EXCEPTION 'synthetic seed assertion failed'; END IF; END $gh690$;"
                          + "\nCOMMIT;", readonly=False)
                else:
                    assert_query(args.engine, values, context, seed_path(context, "verify.sql"))
                print(f"PASS seed {context}", flush=True)
        return 0
    except BatchError as error:
        print(f"FAIL {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
