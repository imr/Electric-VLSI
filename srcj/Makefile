all: electric.jar
	java -classpath .:../lib/java/bsh-1.2b3.jar com/sun/electric/user/Electric

electric.jar:
	javac -classpath ../lib/java/bsh-1.2b3.jar com/sun/electric/*/*.java
	jar -cf electric.jar com/sun/electric/*/*.class


