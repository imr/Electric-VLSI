# 
# A Makefile for Electric, because the build.xml is a tangled,
# convoluted mess.
#

#jars    = $(shell find packaging -name \*.jar  | tr ' \n' '::')
jars    = packaging/bsh-2.0b4.jar:packaging/junit-4.1.jar:packaging/jython.jar:packaging/prefuse.jar
javas   = $(shell find .         -name \*.java | grep -v com/sun/electric/plugins/JMF)
classes = $(javas:%.java=build/%.class)
junk    = $(shell find com -not -name \*.java -not -name \*.html -not -type d | grep -v manualRussian | grep -v helphtml)

electric.jar: $(javas) Makefile
	mkdir -p build
	@echo javac -J-Xmx900m -d build -cp $(jars) -target 1.5 ...
	@javac -J-Xmx900m -d build -cp $(jars) -target 1.5 $(javas)
	cd build; fastjar xvf ../packaging/bsh-2.0b4.jar; rm -rf META-INF
	echo 'Main-Class: com.sun.electric.Main' > manifest
	fastjar cmf manifest $@ $(junk) -C build .
	rm -f manifest

clean:
	rm -rf electric.jar build