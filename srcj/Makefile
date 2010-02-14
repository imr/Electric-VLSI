# 
# A Makefile for Electric, because the build.xml is a tangled, convoluted mess.
#
#
# NOTE: fastjar must be available and it can be downloaded from http://savannah.nongnu.org/projects/fastjar.
#

#jars    = $(shell find packaging -name \*.jar  | tr ' \n' '::')
jars    = ../packaging/bsh-2.0b4.jar:../packaging/junit-4.5.jar:../packaging/jython.jar:../packaging/prefuse.jar:../packaging/AppleJavaExtensions.jar
javas   = $(shell find .         -name \*.java | grep -v com/sun/electric/plugins/JMF)
classes = $(javas:%.java=build/%.class)
junk    = $(shell find com -not -name \*.java -not -name \*.svn* -not -type d | grep -v manualRussian)
#junk    = $(shell find com -not -name \*.java -not -name \*.html -not -name \*.svn* -not -type d | grep -v manualRussian | grep -v helphtml)

electric.jar: $(javas) Makefile
	mkdir -p build
	@echo javac -J-Xmx900m -d build -cp $(jars) -source 1.5 -target 1.5 ...
	@javac -J-Xmx900m -d build -cp $(jars) -source 1.5 -target 1.5 $(javas)
	cd build; fastjar xvf ../../packaging/bsh-2.0b4.jar; rm -rf META-INF
	echo 'Main-Class: com.sun.electric.Main' > manifest
	fastjar cmf manifest $@ $(junk) -C build .
	rm -f manifest

clean:
	rm -rf electric.jar build

javadoc:
	rm -rf ../../electric-apidoc/
	mkdir ../../electric-apidoc/
	javadoc -J-Xmx400m -d ../../electric-apidoc/ `find . -name \*.java`
	open ../../electric-apidoc/index.html
