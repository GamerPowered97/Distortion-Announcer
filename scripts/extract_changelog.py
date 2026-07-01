#!/usr/bin/env python3
import sys
import re

def extract_changelog(version):
    try:
        with open("CHANGELOG.md", "r", encoding="utf-8") as f:
            content = f.read()
    except FileNotFoundError:
        return f"Changelog file not found."

    # Look for ## [version] or ## version (case insensitive)
    # Match until the next ## (followed by space or [) or --- or end of file
    escaped_version = re.escape(version)
    pattern = rf"(?i)##\s*\[?{escaped_version}\]?.*?\n(.*?)(?=\n##\s|\n##\[|\n---\n|\Z)"
    match = re.search(pattern, content, re.DOTALL)
    
    if match:
        return match.group(1).strip()
    else:
        return f"Release notes for version {version} not found in CHANGELOG.md."

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: extract_changelog.py <version>")
        sys.exit(1)
    print(extract_changelog(sys.argv[1]))
