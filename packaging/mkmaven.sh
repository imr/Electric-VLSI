#!/bin/sh
#
# Adds jars to the local maven repository
# usage:
#    mkmaven

mvn install:install-file -Dfile=jmf.jar -DgroupId=javax.media -DartifactId=jmf -Dversion=2.1.1e -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=prefuse.jar -DgroupId=org.prefuse -DartifactId=prefuse -Dversion=beta-20060809 -Dpackaging=jar -DgeneratePom=true

