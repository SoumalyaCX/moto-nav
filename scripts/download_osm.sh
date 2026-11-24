#!/bin/bash
# MotoNav - Download regional OpenStreetMap extract for an Indian state
# Example: ./download_osm.sh west-bengal

set -e

STATE_NAME=${1:-"west-bengal"}
GEOFABRIK_BASE_URL="https://download.geofabrik.de/asia/india"
OUTPUT_DIR="downloads"

mkdir -p "$OUTPUT_DIR"
TARGET_FILE="${OUTPUT_DIR}/${STATE_NAME}-latest.osm.pbf"

echo "=== MotoNav OSM Downloader ==="
echo "Downloading OpenStreetMap extract for: ${STATE_NAME}"
echo "Source: ${GEOFABRIK_BASE_URL}/${STATE_NAME}-latest.osm.pbf"

curl -L -C - "${GEOFABRIK_BASE_URL}/${STATE_NAME}-latest.osm.pbf" -o "$TARGET_FILE"

echo "Download completed successfully: $TARGET_FILE"
ls -lh "$TARGET_FILE"
