#!/bin/sh
#
# Adds jars to the local maven repository
# usage:
#    mkmaven

REPO=.m2/repository/
mvn install:install-file -Dfile=${REPO}javax/media/2.1.1e/jmf-2.1.1e.jar -DgroupId=javax.media -DartifactId=jmf -Dversion=2.1.1e -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=${REPO}org/prefuse/prefuse/beta-20060809/prefuse-beta-20060809.jar -DgroupId=org.prefuse -DartifactId=prefuse -Dversion=beta-20060809 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=${REPO}com/apple/AppleJavaExtensions/1.4/AppleJavaExtensions-1.4.jar -DgroupId=com.apple -DartifactId=AppleJavaExtensions -Dversion=1.4 -Dpackaging=jar -DgeneratePom=true

