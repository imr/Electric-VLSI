BEANSHELLLOC = ../lib/java/bsh-1.2b3.jar
JARLOC = Jama-1.0.1.jar:ia_math.jar
JAVACOMPILER = javac
JAVAFLAGS = -classpath $(BEANSHELLLOC):$(JARLOC)

JAVAPURESRC = \
	IntervalMatrix.java \
	RawWriter.java \
	Trick.java \
	IntervalExp.java \
	ExprEval.java \
	Diode.java \
	Hermite.java

#JAVA_SRCS = $(JAVAPURESRC:%=com/sun/electric/%)
JAVA_SRCS = $(JAVAPURESRC)
JAVA_OBJS = $(JAVA_SRCS:%.java=%.class)

all: isim.jar

isim.jar: $(JAVA_SRCS)
	javac $(JAVAFLAGS) $(JAVA_SRCS)
	jar -cf isim.jar $(JAVA_OBJS)

tests: wrapping trick diode hermite

wrapping: isim.jar
	java -classpath .:$(BEANSHELLLOC):$(JARLOC):isim.jar IntervalExp

trick: isim.jar
	java -classpath .:$(BEANSHELLLOC):$(JARLOC):isim.jar Trick

diode: isim.jar
	java -classpath .:$(BEANSHELLLOC):$(JARLOC):isim.jar Diode

hermite: isim.jar
	java -classpath .:$(BEANSHELLLOC):$(JARLOC):isim.jar Hermite

clean:
	rm -rf $(JAVA_OBJS) *.class
