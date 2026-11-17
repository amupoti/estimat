#!/bin/sh
#
# Turns this template into a new app: renames the Kotlin package and
# application id, the display name, the Gradle project name, and the GitHub
# repository used by the in-app updater.
#
# Usage:
#   scripts/new-app.sh --package com.example.notes --name "Notes" \
#       [--name-ca "Notes"] [--repo owner/notes] [--project Notes]

set -eu

usage() {
    sed -n '3,10p' "$0" | sed 's/^# \{0,1\}//'
    exit "${1:-1}"
}

package=
name=
name_ca=
repo=
project=

while [ $# -gt 0 ]; do
    case "$1" in
        --package) package=$2; shift 2 ;;
        --name) name=$2; shift 2 ;;
        --name-ca) name_ca=$2; shift 2 ;;
        --repo) repo=$2; shift 2 ;;
        --project) project=$2; shift 2 ;;
        -h|--help) usage 0 ;;
        *) echo "Unknown option: $1" >&2; usage ;;
    esac
done

[ -n "$package" ] && [ -n "$name" ] || usage

if ! printf '%s' "$package" | grep -Eq '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$'; then
    echo "Invalid package name: $package" >&2
    exit 1
fi
if [ -n "$repo" ] && ! printf '%s' "$repo" | grep -Eq '^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$'; then
    echo "Repository must look like owner/name: $repo" >&2
    exit 1
fi

name_ca=${name_ca:-$name}
project=${project:-$(printf '%s' "$name" | tr -cd '[:alnum:]')}

root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$root"

old_package=$(sed -n 's/^ *namespace = "\(.*\)"$/\1/p' app/build.gradle.kts)
if [ -z "$old_package" ]; then
    echo "Could not read the current namespace from app/build.gradle.kts" >&2
    exit 1
fi

old_path=$(printf '%s' "$old_package" | tr . /)
new_path=$(printf '%s' "$package" | tr . /)

# Escapes a value for use as a sed replacement / regex.
escape_regex() { printf '%s' "$1" | sed 's/[.[\*^$/]/\\&/g'; }
escape_replacement() { printf '%s' "$1" | sed 's/[&/\]/\\&/g'; }

old_re=$(escape_regex "$old_package")
new_re=$(escape_replacement "$package")

if [ "$old_package" != "$package" ]; then
    for source_set in main test androidTest; do
        src="app/src/$source_set/java"
        [ -d "$src/$old_path" ] || continue
        mkdir -p "$src/$(dirname "$new_path")"
        mv "$src/$old_path" "$src/$new_path"
        # Remove now-empty parent directories of the old package.
        dir=$(dirname "$src/$old_path")
        while [ "$dir" != "$src" ] && rmdir "$dir" 2>/dev/null; do
            dir=$(dirname "$dir")
        done
    done

    grep -rl --exclude-dir=build "$old_package" app docs README.md |
        while IFS= read -r file; do
            sed -i.bak "s/$old_re/$new_re/g" "$file" && rm "$file.bak"
        done
    grep -rl "$old_path" docs README.md |
        while IFS= read -r file; do
            sed -i.bak "s/$(escape_regex "$old_path")/$(escape_replacement "$new_path")/g" "$file" &&
                rm "$file.bak"
        done
fi

# Escapes XML special characters and apostrophes for Android string resources.
android_string() {
    printf '%s' "$1" | sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' \
        -e "s/'/\\\\'/g"
}

set_string() {
    file=$1 key=$2 value=$(escape_replacement "$(android_string "$3")")
    sed -i.bak "s/\(<string name=\"$key\">\)[^<]*/\1$value/" "$file" && rm "$file.bak"
}
set_string app/src/main/res/values/strings.xml app_name "$name"
set_string app/src/main/res/values-ca/strings.xml app_name "$name_ca"

sed -i.bak "s/^rootProject.name = .*/rootProject.name = \"$(escape_replacement "$project")\"/" \
    settings.gradle.kts && rm settings.gradle.kts.bak

if [ -n "$repo" ]; then
    sed -i.bak "s/^appGithubRepository=.*/appGithubRepository=$(escape_replacement "$repo")/" \
        gradle.properties && rm gradle.properties.bak
fi

echo "Created $name ($package)."
echo "Next: update README.md, docs/project-map.md, and the changelog, then run ./build-apk.sh"
