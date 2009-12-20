# 
# A Makefile for Electric, because the build.xml is a tangled,
# convoluted mess.
#

all: packaging/bsh-2.0b4.jar
	echo "Open "


packaging/bsh-2.0b4.jar:
	cd packaging; wget http://www.beanshell.org/bsh-2.0b4.jar

clean:
	rm -rf srcj/electric.jar
	cd srcj; ant clean
