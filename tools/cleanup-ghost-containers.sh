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
before_raw_file="$work_dir/before-raw.tsv"
after_raw_file="$work_dir/after-raw.tsv"
ghost_file="$work_dir/ghosts.tsv"
timer_file="$work_dir/timers.tsv"
units_file="$work_dir/units.txt"
eligible_file="$work_dir/eligible-ids.txt"

cleanup_work_dir() {
    rm -f -- "$before_file" "$after_file" "$before_raw_file" "$after_raw_file" \
        "$ghost_file" "$timer_file" "$units_file" "$eligible_file"
    rmdir "$work_dir" 2>/dev/null || true
}
trap cleanup_work_dir EXIT HUP INT TERM

effective_state() {
    reported_state=$1
    reported_pid=$2
    if [ "$reported_state" = running ]; then
        case "$reported_pid" in
            ''|0|*[!0-9]*) printf '%s\n' 'dead (pid missing)'; return ;;
        esac
        if [ ! -d "/proc/$reported_pid" ]; then
            printf '%s\n' 'dead (pid missing)'
            return
        fi
    fi
    printf '%s\n' "$reported_state"
}

write_snapshot() {
    snapshot_file=$1
    raw_snapshot_file=$2
    shift 2
    if ! podman ps -a "$@" --no-trunc \
        --format '{{.ID}}\t{{.Names}}\t{{.State}}\t{{.Pid}}' >"$raw_snapshot_file"; then
        echo 'ERROR: unable to query the Podman container snapshot.' >&2
        exit 1
    fi
    while IFS="$(printf '\t')" read -r container_id container_name state pid; do
        [ -n "$container_id" ] || continue
        state_for_report=$(effective_state "$state" "$pid")
        printf '%s\t%s\t%s\t%s\t%s\n' \
            "$container_id" "$container_name" "$state" "$pid" "$state_for_report"
    done <"$raw_snapshot_file" >"$snapshot_file"
}

# --sync cannot repair every orphaned conmon/runtime record. Both snapshots
# therefore turn a reported running state into a dead state when its host PID is
# absent, while retaining the raw state for the running-to-stopped comparison.
write_snapshot "$before_file" "$before_raw_file"
write_snapshot "$after_file" "$after_raw_file" --sync
awk -F '\t' '
    NR == FNR { previous_reported[$1] = $3; previous_effective[$1] = $5; next }
    $3 == "running" && $5 == "dead (pid missing)" {
        print $1 "\t" $2 "\t" $3 "\t" $5
        next
    }
    $5 ~ /^(unknown|stopping|removing)$/ ||
        (previous_reported[$1] == "running" && $5 ~ /^(exited|stopped)$/) {
        print $1 "\t" $2 "\t" previous_effective[$1] "\t" $5
    }
' "$before_file" "$after_file" >"$ghost_file"

# Podman systemd healthcheck timers use the full container ID as the transient
# unit name. A timer is stale when its container is absent or no longer running.
if ! systemctl --user list-units --all --plain --no-legend --no-pager \
    >"$units_file" 2>/dev/null; then
    echo 'ERROR: unable to query the user systemd manager.' >&2
    exit 1
fi
awk '{print $1}' "$units_file" \
    | awk '/^[0-9a-f]{64}\.timer$/ {print}' \
    | while IFS= read -r timer; do
        container_id=${timer%.timer}
        state=$(awk -F '\t' -v id="$container_id" '$1 == id {print $5; exit}' "$after_file")
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

timer_is_stale() {
    awk -F '\t' -v unit="$1" '$1 == unit {found = 1} END {exit !found}' "$timer_file"
}

stop_unit() {
    unit=$1
    if systemctl --user stop "$unit" 2>/dev/null; then
        return
    fi
    if ! unit_state_value=$(query_unit_state "$unit"); then
        return 1
    fi
    load_state=${unit_state_value%% *}
    if [ "$load_state" = not-found ]; then
        return
    fi
    echo "ERROR: unable to stop selected user-systemd unit: $unit" >&2
    return 1
}

reset_unit_if_failed() {
    unit=$1
    if ! unit_state_value=$(query_unit_state "$unit"); then
        return 1
    fi
    active_state=${unit_state_value#* }
    if [ "$active_state" != failed ]; then
        return
    fi
    if ! systemctl --user reset-failed "$unit" 2>/dev/null; then
        echo "ERROR: unable to reset failed user-systemd unit: $unit" >&2
        return 1
    fi
    if ! unit_state_value=$(query_unit_state "$unit"); then
        return 1
    fi
    active_state=${unit_state_value#* }
    if [ "$active_state" = failed ]; then
        echo "ERROR: selected user-systemd unit remains failed: $unit" >&2
        return 1
    fi
}

query_unit_state() {
    queried_unit=$1
    if ! unit_properties=$(systemctl --user show "$queried_unit" \
        --property=LoadState --property=ActiveState 2>/dev/null); then
        echo "ERROR: unable to query selected user-systemd unit: $queried_unit" >&2
        return 1
    fi
    queried_load_state=$(printf '%s\n' "$unit_properties" \
        | awk -F '=' '$1 == "LoadState" {print $2; exit}')
    queried_active_state=$(printf '%s\n' "$unit_properties" \
        | awk -F '=' '$1 == "ActiveState" {print $2; exit}')
    if [ -z "$queried_load_state" ] || [ -z "$queried_active_state" ]; then
        echo "ERROR: incomplete state for selected user-systemd unit: $queried_unit" >&2
        return 1
    fi
    printf '%s %s\n' "$queried_load_state" "$queried_active_state"
}

inspect_current_record() {
    inspected_id=$1
    if podman container exists "$inspected_id" 2>/dev/null; then
        if podman inspect --format '{{.State.Status}} {{.State.Pid}}' \
            "$inspected_id" 2>/dev/null; then
            return
        fi
        # A concurrent successful removal is the only accepted inspect failure.
        if podman container exists "$inspected_id" 2>/dev/null; then
            echo "ERROR: unable to inspect selected container: $inspected_id" >&2
            return 1
        else
            exists_status=$?
            if [ "$exists_status" -eq 1 ]; then
                return 2
            fi
            echo "ERROR: unable to recheck selected container: $inspected_id" >&2
            return 1
        fi
    else
        exists_status=$?
        if [ "$exists_status" -eq 1 ]; then
            return 2
        fi
        echo "ERROR: unable to query selected container: $inspected_id" >&2
        return 1
    fi
}

# Revalidate every selection before touching units. Unit cleanup happens for the
# complete eligible set before the first removal so concurrent healthchecks
# cannot keep taking Podman's runtime lock while dead containers are removed.
: >"$eligible_file"
for container_id do
    if awk -F '\t' -v id="$container_id" '$1 == id {found = 1} END {exit !found}' \
        "$eligible_file"; then
        echo "Skipping duplicate selected container ID: $container_id"
        continue
    fi
    timer=${container_id}.timer
    service=${container_id}.service
    if current_record=$(inspect_current_record "$container_id"); then
        current_state=${current_record%% *}
        current_pid=${current_record#* }
        current_effective_state=$(effective_state "$current_state" "$current_pid")
    else
        inspect_status=$?
        if [ "$inspect_status" -ne 2 ]; then
            exit "$inspect_status"
        fi
        current_state=
        current_effective_state=
    fi

    case "$current_effective_state" in
        running)
            echo "Skipping running container and its timer: $container_id"
            continue
            ;;
        'dead (pid missing)'|exited|stopped|unknown|stopping|removing) ;;
        '')
            if ! timer_unit_state=$(query_unit_state "$timer"); then
                exit 1
            fi
            if ! service_unit_state=$(query_unit_state "$service"); then
                exit 1
            fi
            timer_active_state=${timer_unit_state#* }
            service_active_state=${service_unit_state#* }
            if ! timer_is_stale "$timer" \
                && [ "$timer_active_state" != active ] \
                && [ "$service_active_state" != active ] \
                && [ "$timer_active_state" != failed ] \
                && [ "$service_active_state" != failed ]; then
                echo "Skipping ID with no container or stale timer: $container_id"
                continue
            fi
            ;;
        *)
            echo "Skipping container whose state is $current_effective_state: $container_id"
            continue
            ;;
    esac
    if [ -n "$current_state" ]; then
        container_was_present=true
    else
        container_was_present=false
    fi
    printf '%s\t%s\n' "$container_id" "$container_was_present" >>"$eligible_file"
done

while IFS="$(printf '\t')" read -r container_id _container_was_present; do
    [ -n "$container_id" ] || continue
    timer=${container_id}.timer
    service=${container_id}.service
    timer_should_stop=false
    timer_is_active=false
    service_is_active=false
    if timer_is_stale "$timer"; then
        timer_should_stop=true
    fi
    if ! timer_unit_state=$(query_unit_state "$timer"); then
        exit 1
    fi
    if ! service_unit_state=$(query_unit_state "$service"); then
        exit 1
    fi
    timer_active_state=${timer_unit_state#* }
    service_active_state=${service_unit_state#* }
    if [ "$timer_active_state" = active ]; then
        timer_is_active=true
    fi
    if [ "$service_active_state" = active ]; then
        service_is_active=true
    fi

    if [ "$timer_should_stop" = true ] || [ "$timer_is_active" = true ]; then
        echo "Stopping selected stale user-systemd unit: $timer"
        stop_unit "$timer"
        handled_timer_count=$((handled_timer_count + 1))
    fi
    if [ "$service_is_active" = true ]; then
        echo "Stopping selected active user-systemd unit: $service"
        stop_unit "$service"
    fi
    reset_unit_if_failed "$timer"
    reset_unit_if_failed "$service"
done <"$eligible_file"

while IFS="$(printf '\t')" read -r container_id container_was_present; do
    [ -n "$container_id" ] || continue
    if current_record=$(inspect_current_record "$container_id"); then
        current_state=${current_record%% *}
        current_pid=${current_record#* }
        current_effective_state=$(effective_state "$current_state" "$current_pid")
        case "$current_effective_state" in
            running)
                echo "Skipping container that restarted before removal: $container_id"
                continue
                ;;
            'dead (pid missing)'|exited|stopped|unknown|stopping|removing) ;;
            *)
                echo "Skipping container whose state changed to $current_effective_state: $container_id"
                continue
                ;;
        esac
    else
        inspect_status=$?
        if [ "$inspect_status" -ne 2 ]; then
            exit "$inspect_status"
        fi
    fi

    echo "Removing selected ghost container: $container_id"
    podman rm --force --ignore "$container_id"
    if [ "$container_was_present" = true ]; then
        removed_ghost_count=$((removed_ghost_count + 1))
    fi
done <"$eligible_file"

echo "Cleanup complete: removed $removed_ghost_count ghost container(s), handled $handled_timer_count stale timer(s)."
