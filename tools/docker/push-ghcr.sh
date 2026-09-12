#!/usr/bin/env bash
# Keep the public entrypoint portable across Docker/Podman hosts; JSON parsing
# uses Python's standard library instead of a second image-target list or jq.
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec python3 - "$script_dir/../../deploy/image-targets.json" "$@" <<'PY'
import argparse
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path


DESTINATION_PREFIX = "ghcr.io/skyg547/account"
TAG_PATTERN = r"[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}"
NAME_PATTERN = r"[a-z0-9]+(?:-[a-z0-9]+)*"
# Source prefixes support account, localhost/account and host[:port]/path.
PREFIX_PATTERN = r"[a-z0-9]+(?:[.-][a-z0-9]+)*(?::[0-9]+)?(?:/[a-z0-9]+(?:[._-][a-z0-9]+)*)*"


def parse_options(argv):
    parser = argparse.ArgumentParser(
        description="Tag and push existing manifest images to ghcr.io/skyg547/account.",
        epilog="See docs/guides/ghcr-container-registry-guide.md. No build/login/pull is performed.",
    )
    parser.add_argument("--tag", required=True, help="destination version tag (no implicit latest)")
    parser.add_argument("--engine", choices=("docker", "podman"), default="docker")
    parser.add_argument("--source-prefix", default="account")
    parser.add_argument("--source-tag", default="local")
    parser.add_argument("--target", action="append", default=[], help="manifest name; repeat to select several")
    parser.add_argument("--dry-run", action="store_true", help="print mappings without invoking the engine")
    options = parser.parse_args(argv)
    for label, value in (("tag", options.tag), ("source-tag", options.source_tag)):
        if not re.fullmatch(TAG_PATTERN, value):
            parser.error(f"--{label} must be a valid image tag (1-128 ASCII characters)")
    if not re.fullmatch(PREFIX_PATTERN, options.source_prefix):
        parser.error("--source-prefix must be a lowercase image repository prefix, without a tag or scheme")
    if len(set(options.target)) != len(options.target):
        parser.error("duplicate --target selection")
    return options


def select_targets(manifest_path, requested):
    manifest = json.loads(Path(manifest_path).read_text(encoding="utf-8"))
    if not isinstance(manifest, dict) or type(manifest.get("schemaVersion")) is not int or manifest["schemaVersion"] != 1:
        raise ValueError("unsupported image manifest schema")
    targets = manifest.get("targets")
    if not isinstance(targets, list) or not targets:
        raise ValueError("image manifest must contain targets")
    names = set()
    enabled = []
    for target in targets:
        if not isinstance(target, dict):
            raise ValueError("invalid image manifest target")
        name = target.get("name")
        if not isinstance(name, str) or not re.fullmatch(NAME_PATTERN, name) or name in names:
            raise ValueError("invalid or duplicate image manifest target name")
        if type(target.get("enabled")) is not bool:
            raise ValueError("manifest target enabled must be boolean")
        names.add(name)
        if target["enabled"]:
            enabled.append(name)
    if set(requested) - set(enabled):
        raise ValueError("--target must name an enabled manifest target")
    selected = [name for name in enabled if not requested or name in requested]
    if not selected:
        raise ValueError("no enabled image targets selected")
    return selected


def image_id(engine, source):
    # Inspect only the ID, never image configuration/env. Pin all IDs before
    # mutation so missing images cannot leave a half-published release, and a
    # concurrent local rebuild cannot silently change the selected content.
    result = subprocess.run(
        [engine, "image", "inspect", "--format", "{{.Id}}", source],
        stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True,
    )
    value = result.stdout.strip()
    if result.returncode or not re.fullmatch(r"(?:sha256:)?[0-9a-f]{64}", value):
        raise RuntimeError(f"local image preflight failed: {source}; check engine access and build/tag it first")
    return value


def publish(engine, images):
    completed = 0
    for source, destination, pinned_id in images:
        for command in ([engine, "tag", pinned_id, destination], [engine, "push", destination]):
            # Engine diagnostics can contain credential helper details; emit
            # only the operation and known destination on failure.
            result = subprocess.run(command, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if result.returncode:
                raise RuntimeError(
                    f"{command[1]} failed: {destination}; pushed {completed}/{len(images)}. "
                    "Stopped; previous pushes remain. See runbook for authentication/retry."
                )
        completed += 1
        print(f"PUSHED {destination}", flush=True)
    print(f"SUCCESS: pushed {completed}/{len(images)} images", flush=True)


def main():
    options = parse_options(sys.argv[2:])
    names = select_targets(sys.argv[1], options.target)
    mappings = [
        (f"{options.source_prefix}/{name}:{options.source_tag}", f"{DESTINATION_PREFIX}/{name}:{options.tag}")
        for name in names
    ]
    print(f"{'DRY-RUN' if options.dry_run else 'PUBLISH'}: {len(mappings)} images; engine={options.engine}", flush=True)
    for source, destination in mappings:
        print(f"{source} -> {destination}", flush=True)
    if options.dry_run:
        return
    engine = shutil.which(options.engine)
    if engine is None:
        raise RuntimeError(f"engine not found: {options.engine}")
    images = [(source, destination, image_id(engine, source)) for source, destination in mappings]
    publish(engine, images)


try:
    main()
except (OSError, ValueError, RuntimeError) as error:
    print(f"ERROR: {error}", file=sys.stderr)
    sys.exit(1)
except KeyboardInterrupt:
    print("ERROR: interrupted; earlier pushes may remain. Check the release before retrying.", file=sys.stderr)
    sys.exit(130)
PY
