#!/bin/sh
#
# Builds the picture that sits behind the icons of the macOS disk image.
#
# Finder never scales a background picture: the artwork has to be exactly the size of the window it
# sits behind, and a Retina display needs a second copy at twice that. One TIFF can carry both
# representations, which is why the background ships as a TIFF and not as the PNG it is drawn as.
#
# Usage: make-dmg-background.sh <source.png> <output.tiff> <width> <height>
#
# The source is the artwork at any size, as long as its proportions are <width>:<height>. Anything
# else is rejected rather than squashed: the reserved regions the icons are placed on are part of the
# drawing, and a stretched picture moves them out from under the icons.

set -eu

if [ $# -ne 4 ]; then
    echo "usage: $(basename "$0") <source.png> <output.tiff> <width> <height>" >&2
    exit 2
fi

source_image=$1
output_image=$2
width=$3
height=$4

if [ ! -f "$source_image" ]; then
    cat >&2 <<EOF
The disk image background is missing: $source_image

It is drawn outside this repository and committed as a PNG; app/dmg/README.md holds the grid it has
to follow and the prompt it was generated from.
EOF
    exit 1
fi

if ! command -v tiffutil > /dev/null 2>&1; then
    echo "tiffutil is not on PATH; it ships with macOS and is what merges the two representations." >&2
    exit 1
fi

read_dimension() {
    sips -g "$1" "$source_image" | tail -n 1 | awk '{ print $2 }'
}

source_width=$(read_dimension pixelWidth)
source_height=$(read_dimension pixelHeight)

# Integer arithmetic only: sh has no floating point, and a percentage is precise enough to catch a
# picture delivered in the wrong aspect ratio while tolerating a generator that rounds by a pixel.
ratio=$((source_width * height * 100 / (source_height * width)))
if [ "$ratio" -lt 98 ] || [ "$ratio" -gt 102 ]; then
    cat >&2 <<EOF
The disk image background is ${source_width}x${source_height}, which is not ${width}:${height}.

Redraw or re-crop it to those proportions. Resizing it here would move the regions the icons are
placed on, and the icons would land on top of the artwork instead of the space left for them.
EOF
    exit 1
fi

output_directory=$(dirname "$output_image")
mkdir -p "$output_directory"

standard="$output_directory/background.png"
retina="$output_directory/background@2x.png"

# sips takes the height first.
sips --resampleHeightWidth "$height" "$width" "$source_image" --out "$standard" > /dev/null
sips --resampleHeightWidth $((height * 2)) $((width * 2)) "$source_image" --out "$retina" > /dev/null

# -cathidpicheck refuses the pair unless the second is exactly twice the first, so a mistake in the
# sizes above fails here rather than shipping a disk image that is blurry on every Retina display.
tiffutil -cathidpicheck "$standard" "$retina" -out "$output_image" > /dev/null

echo "Disk image background written to $output_image (${width}x${height} and @2x)"
