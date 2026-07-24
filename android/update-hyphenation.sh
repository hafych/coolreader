#!/bin/sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
hyph_dir="${script_dir}/../cr3gui/data/hyph"
res_dir="${script_dir}/res/raw"
mode=${1:-update}

if [ "${mode}" != "update" ] && [ "${mode}" != "--check" ]; then
    echo "usage: $0 [--check]" >&2
    exit 2
fi

status=0
for source_pattern in "${hyph_dir}"/*.pattern
do
    source_name=$(basename "${source_pattern}")
    android_name=$(printf '%s\n' "${source_name}" | sed 's/[-,]/_/g')
    android_pattern="${res_dir}/${android_name}"
    if [ "${mode}" = "--check" ]; then
        if [ ! -f "${android_pattern}" ] \
                || ! cmp -s "${source_pattern}" "${android_pattern}"; then
            echo "out of sync: ${source_name}" >&2
            status=1
        fi
    else
        echo "updating ${source_name}..."
        cp -p "${source_pattern}" "${android_pattern}"
    fi
done
exit "${status}"
