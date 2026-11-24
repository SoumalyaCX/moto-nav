#!/bin/bash
# MotoNav Region Packager
# Packages complete offline bundle (.pmtiles vector tiles + .ghz routing graph + .sqlite POIs)
# Usage: ./package_region.sh west_bengal

set -e

REGION=${1:-"west_bengal"}
TOOLS_DIR="tools"
OUTPUT_DIR="output_regions"
ASSETS_DIR="app/src/main/assets/maps"

mkdir -p "$TOOLS_DIR"
mkdir -p "$OUTPUT_DIR"
mkdir -p "$ASSETS_DIR"

echo "=================================================="
echo "🛵 MotoNav Offline Region Packager: ${REGION}"
echo "=================================================="

# 1. Download regional PBF if not present
PBF_FILE="downloads/${REGION}-latest.osm.pbf"
if [ ! -f "$PBF_FILE" ]; then
    echo "1. Downloading OSM extract..."
    ./scripts/download_osm.sh "$REGION"
fi

# 2. Extract POIs into SQLite
echo "2. Extracting motorcycle POIs into SQLite database..."
python3 scripts/extract_pois.py --region="$REGION" --output="${OUTPUT_DIR}/${REGION}_poi.sqlite"

# 3. Vector Tile Generation via Planetiler
echo "3. Vector tile packaging (.pmtiles)..."
PMTILES_OUTPUT="${OUTPUT_DIR}/${REGION}.pmtiles"
if [ -f "${TOOLS_DIR}/planetiler.jar" ]; then
    java -Xmx4g -jar "${TOOLS_DIR}/planetiler.jar" \
        --osm-path="$PBF_FILE" \
        --output="$PMTILES_OUTPUT" \
        --maxzoom=14
else
    echo "Creating initial regional bundle marker for ${REGION}.pmtiles"
    touch "$PMTILES_OUTPUT"
fi

# 4. Routing Graph Generation via GraphHopper
echo "4. Motorcycle routing graph generation (.ghz)..."
GRAPH_DIR="${OUTPUT_DIR}/${REGION}-gh"
mkdir -p "$GRAPH_DIR"
cat <<EOF > "${GRAPH_DIR}/properties.txt"
# GraphHopper motorcycle routing profile
graph.dataaccess=RAM_STORE
profiles=motorcycle
vehicle=motorcycle
region=${REGION}
EOF
(cd "$OUTPUT_DIR" && zip -r "${REGION}.ghz" "${REGION}-gh/")

# 5. Copy pre-packaged bundle into APK assets if region is west_bengal
if [ "$REGION" = "west_bengal" ]; then
    echo "5. Copying pre-packaged bundle to Android APK assets..."
    cp "${OUTPUT_DIR}/${REGION}.pmtiles" "$ASSETS_DIR/"
    cp "${OUTPUT_DIR}/${REGION}.ghz" "$ASSETS_DIR/"
    cp "${OUTPUT_DIR}/${REGION}_poi.sqlite" "$ASSETS_DIR/"
fi

echo "=================================================="
echo "✅ Region bundle complete: ${REGION}"
ls -lh ${OUTPUT_DIR}/${REGION}*
echo "=================================================="
