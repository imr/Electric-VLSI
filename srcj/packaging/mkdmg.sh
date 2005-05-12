#!/bin/sh
#
# Creates a disk image (dmg) on Mac OS X from the command line.
# usage:
#    mkdmg <volname> <srcdir>
#
# Where <volname> is the name to use for the mounted image including the version
# number of the volume and <srcdir> is where the contents to put on the dmg are.
#
# The result will be a file called <volname>.dmg

if [ $# != 2 ]; then
 echo "usage: mkdmg.sh volname srcdir"
 exit 0
fi

VOL="$1"
FILES="$2"

DMG="$VOL.dmg"
echo "DMG " $DMG $FILES
# create disk image and format, ejecting when done
hdiutil create "$DMG" -srcfolder ${FILES} -ov
#DISK=`hdiutil attach "$DMG" | sed -ne '/Apple_partition_scheme/s///p'`DISK="/dev/disk1"
#echo "DISK " $DISK
#hdiutil eject $DISK

# mount and copy files onto volume
#hdid "$DMG"
#cp -R "${FILES}"/* "/Volumes/$VOL"
#hdiutil eject $DISK

