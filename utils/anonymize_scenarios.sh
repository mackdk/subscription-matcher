#!/bin/bash

set -euo pipefail

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "Error: 'jq' is not installed. Please install it to continue."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JQ_DIR="$SCRIPT_DIR/jq"

JQ_JSON_TRANSFORM="$(< "$JQ_DIR/json_transform.jq")"
JQ_CSV_REPLACE_SYSTEMS="$(< "$JQ_DIR/csv_replace_systems.jq")"
JQ_CSV_CLEAR_SYSTEM_COLUMN="$(< "$JQ_DIR/csv_clear_system_column.jq")"

# Function to print help
usage() {
    echo "Usage: $0 [options] [scenario_numbers...]"
    echo ""
    echo "Options:"
    echo "  -a, --all               Process all scenarios in the base directory."
    echo "  -d, --base-dir PATH     Specify the base directory."
    echo "  -s, --use-system-name   Anonymize system names instead of removing them."
    echo "  -h, --help              Print this help message."
    echo ""
    echo "Examples:"
    echo "  $0 1 5 10               # Process scenarios 1, 5, and 10"
    echo "  $0 -s 1 2               # Anonymize (sys-001...) in 1 and 2"
    echo "  $0 -a                   # Process all scenarios"
    echo "  $0 -d ./tmp 1 2         # Process scenarios 1 and 2 inside ./tmp"
    exit 0
}

# Parse arguments
SCENARIOS=()
BASE_DIR=""
PROCESS_ALL=false
USE_SYSTEM_NAME=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        -a|--all)
            PROCESS_ALL=true
            shift
            ;;
        -d|--base-dir)
            if [[ $# -lt 2 || -z "$2" || "$2" == -* ]]; then
                echo "Error: --base-dir requires a path argument."
                exit 1
            fi
            BASE_DIR="$2"
            shift 2
            ;;
        -s|--use-system-name)
            USE_SYSTEM_NAME=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            SCENARIOS+=("$1")
            shift
            ;;
    esac
done

if [[ -z "$BASE_DIR" ]]; then
    # Default relative path from git root
    REL_PATH="src/test/resources/com/suse/matcher/scenarios"
    if ! GIT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)"; then
        echo "Error: Not a git repository."
        echo "Please run this script from within subscription-matcher repository or specify the base directory with -d."
        exit 1
    fi

    BASE_DIR="$GIT_ROOT/$REL_PATH"
fi

if [[ ! -d "$BASE_DIR" ]]; then
    echo "Error: Base directory '$BASE_DIR' does not exist."
    exit 1
fi

# Determine which directories to process
DIRS_TO_PROCESS=()

if [[ "$PROCESS_ALL" == true ]]; then
    # Find all subdirectories that contain an input.json
    for d in "$BASE_DIR"/*/; do
        if [[ -f "${d}input.json" ]]; then
            DIRS_TO_PROCESS+=("${d%/}")
        fi
    done
elif [[ ${#SCENARIOS[@]} -gt 0 ]]; then
    for s in "${SCENARIOS[@]}"; do
        dir_path="$BASE_DIR/$s"
        if [[ -d "$dir_path" ]]; then
            DIRS_TO_PROCESS+=("$dir_path")
        else
            echo "Warning: Scenario directory '$dir_path' not found. Skipping."
        fi
    done
else
    echo "Error: No scenarios specified. Use -a for all, or provide numbers."
    exit 1
fi

# Sort directories by numeric scenario name (1,2,3,...,10,11,...).
mapfile -t DIRS_TO_PROCESS < <(
    printf '%s\n' "${DIRS_TO_PROCESS[@]}" \
        | awk -F/ '{print $NF "\t" $0}' \
        | sort -n -k1,1 \
        | cut -f2-
)

for scenario_dir in "${DIRS_TO_PROCESS[@]}"; do
    echo "Processing Scenario: $(basename "$scenario_dir")"

    if [[ ! -f "$scenario_dir/input.json" ]]; then
        continue
    fi

    # Extract and map SCC Usernames
    mapfile -t scc_users < <(
        jq -r '(.subscriptions // [])[] | .scc_username? // empty' "$scenario_dir/input.json" \
            | sort -f -u
    )

    # Extract IDs to anonymize
    mapfile -t subscription_ids < <(
        jq -r '(.subscriptions // [])[] | .id? | numbers' "$scenario_dir/input.json" \
            | sort -n -u
    )

    # 2. System Names Logic
    mapfile -t systems < <(
        jq -r '(.systems // [])[] | .name? // empty' "$scenario_dir/input.json" \
            | sort -f -u
    )

    # Build SCC mapping as JSON object for jq
    scc_map='{}'
    for old_scc in "${scc_users[@]}"; do
        if [[ ! "$old_scc" =~ ^test-user-[a-zA-Z0-9]{6}$ ]]; then
            rand_suffix=""
            while [[ ${#rand_suffix} -lt 6 ]]; do
                rand_suffix+=$(dd if=/dev/urandom bs=16 count=1 2>/dev/null | LC_ALL=C tr -dc 'a-zA-Z0-9')
            done
            new_scc="test-user-${rand_suffix:0:6}"
        else
            new_scc="$old_scc"
        fi
        scc_map="$(jq -cn --argjson map "$scc_map" --arg k "$old_scc" --arg v "$new_scc" '$map + {($k): $v}')"
    done

    # Build subscription ID mapping as JSON object for jq
    sub_id_map='{}'
    # Count the negative ids as they map to OEM subscriptions and we want to keep them negative
    oem_id_count=$(printf '%s\n' "${subscription_ids[@]}" | grep -c '^-' || true)

    subs_counter=$((oem_id_count == 0 ? 1 : -oem_id_count))
    for old_sub_id in "${subscription_ids[@]}"; do
        sub_id_map="$(jq -cn --argjson map "$sub_id_map" --arg k "$old_sub_id" --argjson v "$subs_counter" '$map + {($k): $v}')"
        # Skip the zero since we want ids for non oem to start from 1
        subs_counter=$((subs_counter + 1 == 0 ? 1 : subs_counter + 1))
    done

    # Build system mapping as JSON object for jq
    system_map='{}'
    sys_counter=0
    for old_name in "${systems[@]}"; do
        new_name="$(printf "sys-%03d.test.local" "$sys_counter")"
        system_map="$(jq -cn --argjson map "$system_map" --arg k "$old_name" --arg v "$new_name" '$map + {($k): $v}')"
        ((++sys_counter))
    done

    # --- PROCESS ALL FILES IN SCENARIO ---
    for file_path in "$scenario_dir"/*.{json,csv}; do
        [[ -f "$file_path" ]] || continue
        tmp_file=""

        # Process JSON Files with jq-only transformations
        if [[ "$file_path" == *.json ]]; then
            tmp_file="$(mktemp)"
            indent_size=$(sed -n '2p' "$file_path" | grep -o '^[ ]*' | wc -L)
            [[ "$indent_size" -eq 0 ]] && indent_size=4

            jq --indent "$indent_size" \
                --argjson sccMap "$scc_map" \
                --argjson subIdMap "$sub_id_map" \
                --argjson systemMap "$system_map" \
                --argjson useSystemName "$USE_SYSTEM_NAME" \
                "$JQ_JSON_TRANSFORM" "$file_path" > "$tmp_file" && \
                mv "$tmp_file" "$file_path"

        # Process unmatched_product_report.csv with jq-only transformations
        elif [[ "$(basename "$file_path")" == "unmatched_product_report.csv" ]]; then
            tmp_file="$(mktemp)"
            if [[ "$USE_SYSTEM_NAME" == true ]]; then
                jq -Rsr --argjson systemMap "$system_map" "$JQ_CSV_REPLACE_SYSTEMS" "$file_path" > "$tmp_file" && \
                    mv "$tmp_file" "$file_path"
            else
                jq -Rsr "$JQ_CSV_CLEAR_SYSTEM_COLUMN" "$file_path" > "$tmp_file" && \
                    mv "$tmp_file" "$file_path"
            fi
        fi

        # Remove the temporary file if it still exists for any reason
        [[ -n "$tmp_file" && -f "$tmp_file" ]] && rm -f "$tmp_file"
    done
done

echo "Done."
