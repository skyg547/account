#!/bin/sh
set -eu

usage() {
    cat <<'EOF'
Usage: sh tools/cleanup-ghost-containers.sh
       sh tools/cleanup-ghost-containers.sh --apply <full-container-id> [...]

The first form reports rootless Podman ghost containers and stale healthcheck
timers. The second form revalidates and cleans only the explicitly selected
64-character IDs. Images, pods, networks, and volumes are never pruned.
EOF
}

apply=false
case "${1:-}" in
    '') ;;
    --apply)
        apply=true
        shift
        if [ "$#" -eq 0 ]; then
            echo 'ERROR: --apply requires at least one full container ID.' >&2
            usage >&2
            exit 2
        fi
        for selected_id do
            case "$selected_id" in
                *[!0-9a-f]*|'')
                    echo "ERROR: invalid container ID: $selected_id" >&2
                    exit 2
                    ;;
            esac
            if [ "${#selected_id}" -ne 64 ]; then
                echo "ERROR: container ID must contain exactly 64 lowercase hex characters: $selected_id" >&2
                exit 2
            fi
        done
        ;;
    -h|--help) usage; exit 0 ;;
    *) usage >&2; exit 2 ;;
esac
if [ "$apply" = false ] && [ "$#" -gt 0 ]; then
    usage >&2
    exit 2
fi

for command_name in podman systemctl awk mktemp; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "ERROR: required command not found: $command_name" >&2
        exit 1
    fi
done

rootless=$(podman info --format '{{.Host.Security.Rootless}}' 2>/dev/null) || {
    echo 'ERROR: unable to query Podman; run this as the rootless Podman owner.' >&2
    exit 1
}
if [ "$rootless" != true ]; then
    echo 'ERROR: refusing to operate on a non-rootless Podman store.' >&2
    exit 1
fi

work_dir=$(mktemp -d "${TMPDIR:-/tmp}/account-podman-ghosts.XXXXXX")
before_file="$work_dir/before.tsv"
after_file="$work_dir/after.tsv"
ghost_file="$work_dir/ghosts.tsv"
timer_file="$work_dir/timers.tsv"
units_file="$work_dir/units.txt"

cleanup_work_dir() {
    rm -f -- "$before_file" "$after_file" "$ghost_file" "$timer_file" "$units_file"
    rmdir "$work_dir" 2>/dev/null || true
}
trap cleanup_work_dir EXIT HUP INT TERM

# --sync reconciles Podman's database with the OCI runtime. A running-to-stopped
# transition between snapshots is a ghost candidate, not proof of how it exited.
podman ps -a --no-trunc --format '{{.ID}}\t{{.Names}}\t{{.State}}' >"$before_file"
podman ps -a --sync --no-trunc --format '{{.ID}}\t{{.Names}}\t{{.State}}' >"$after_file"
awk -F '\t' '
    NR == FNR { previous[$1] = $3; next }
    $3 ~ /^(unknown|stopping|removing)$/ ||
        (previous[$1] == "running" && $3 ~ /^(exited|stopped)$/) {
        print $1 "\t" $2 "\t" previous[$1] "\t" $3
    }
' "$before_file" "$after_file" >"$ghost_file"

# Podman systemd healthcheck timers use the full container ID as the transient
# unit name. A timer is stale when its container is absent or no longer running.
if ! systemctl --user list-units --all --type=timer --plain --no-legend --no-pager \
    >"$units_file" 2>/dev/null; then
    echo 'ERROR: unable to query the user systemd manager.' >&2
    exit 1
fi
awk '{print $1}' "$units_file" \
    | awk '/^[0-9a-f]{64}\.timer$/ {print}' \
    | while IFS= read -r timer; do
        container_id=${timer%.timer}
        state=$(awk -F '\t' -v id="$container_id" '$1 == id {print $3; exit}' "$after_file")
        if [ "$state" != running ]; then
            printf '%s\t%s\n' "$timer" "${state:-missing}"
        fi
    done >"$timer_file"

ghost_count=$(awk 'END {print NR + 0}' "$ghost_file")
timer_count=$(awk 'END {print NR + 0}' "$timer_file")
echo "Ghost containers: $ghost_count"
while IFS="$(printf '\t')" read -r container_id container_name previous_state current_state; do
    [ -n "$container_id" ] || continue
    echo "  $container_id  $container_name  $previous_state -> $current_state"
done <"$ghost_file"
echo "Stale healthcheck timers: $timer_count"
while IFS="$(printf '\t')" read -r timer state; do
    [ -n "$timer" ] || continue
    echo "  $timer  container=$state"
done <"$timer_file"

if [ "$apply" != true ]; then
    if [ "$ghost_count" -gt 0 ] || [ "$timer_count" -gt 0 ]; then
        echo 'Report only. Re-run with --apply plus the reviewed full container IDs.'
    else
        echo 'No cleanup required.'
    fi
    exit 0
fi

handled_timer_count=0
removed_ghost_count=0
for container_id do
    timer=${container_id}.timer
    current_state=$(podman inspect --format '{{.State.Status}}' "$container_id" 2>/dev/null || true)
    if [ "$current_state" = running ]; then
        echo "Skipping running container and its timer: $container_id"
        continue
    fi

    if awk -F '\t' -v unit="$timer" '$1 == unit {found = 1} END {exit !found}' "$timer_file"; then
        service=${container_id}.service
        echo "Stopping selected stale user-systemd unit: $timer"
        systemctl --user stop "$timer"
        systemctl --user stop "$service" 2>/dev/null || true
        systemctl --user reset-failed "$timer" "$service" 2>/dev/null || true
        handled_timer_count=$((handled_timer_count + 1))
    fi

    case "$current_state" in
        exited|stopped|unknown|stopping|removing) ;;
        '')
            if ! awk -F '\t' -v unit="$timer" '$1 == unit {found = 1} END {exit !found}' "$timer_file"; then
                echo "Skipping ID with no container or stale timer: $container_id"
            fi
            continue
            ;;
        *)
            echo "Skipping container whose state is $current_state: $container_id"
            continue
            ;;
    esac
    echo "Removing selected non-running container: $container_id"
    podman rm --force --ignore "$container_id"
    removed_ghost_count=$((removed_ghost_count + 1))
done

echo "Cleanup complete: removed $removed_ghost_count ghost container(s), handled $handled_timer_count stale timer(s)."
