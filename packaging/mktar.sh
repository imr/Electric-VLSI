#!/bin/sh
#
# Creates a source-only GNU tarball from the command line.
# usage:
#    mkdmg <volname> <srcdir>
#
# Where <volname> is the name to use for the mounted image including the version
# number of the volume and <srcdir> is where the contents to put on the dmg are.
#
# The result will be a file called <volname>.dmg

if [ $# != 1 ]; then
 echo "usage: mktar.sh version"
 echo "example: mktar.sh version8-10"
 echo "         The result will be ~/electricExport/version8-10.tar.gz"
 exit 0
fi
VERSION="$1"
echo Making $VERSION.tar.gz ...
mkdir ~/electricExport
rm -Rf ~/electricExport/$VERSION
svn export .. ~/electricExport/$VERSION
cd ~/electricExport/$VERSION
rm packaging/*.jar
rm -Rf srcj/com/sun/electric/plugins/*
rm -Rf srcj/com/sun/electric/tool/simulation/eventsim
rm -Rf srcj/com/sun/electric/tool/simulation/interval/BigBinary.java
rm -Rf srcj/com/sun/electric/tool/simulation/interval/UnivariateBigPolynomial.java
cd ~/electricExport
gtar -czf $VERSION.tar.gz $VERSION
rm -Rf $VERSION
