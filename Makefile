# 
# This Makefile downloads bean schell for Electric Netbeans project.
#

all: packaging/bsh-2.0b4.jar
	echo "Open this project in Netbeans:"
	@pwd


packaging/bsh-2.0b4.jar:
	cd packaging; wget http://www.beanshell.org/bsh-2.0b4.jar

