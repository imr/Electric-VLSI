JARLOC = Jama-1.0.1.jar:ia_math.jar:nbsp.jar:../electric.jar
JAVACOMPILER = javac
JAVAFLAGS = -classpath $(JARLOC) -source 1.5
# -Xlint:unchecked

JAVAPURESRC = \
	IntervalMatrix.java \
	IntervalExp.java \
	Trick.java

#JAVA_SRCS = $(JAVAPURESRC:%=com/sun/electric/%)
JAVA_SRCS = $(JAVAPURESRC)
JAVA_OBJS = $(JAVA_SRCS:%.java=%.class)

.SUFFIXES: .java .class
.java.class:
	$(JAVACOMPILER) $(JAVAFLAGS) $<

all: isim.jar

isim.jar: $(JAVA_SRCS)
	$(JAVACOMPILER) $(JAVAFLAGS) $(JAVA_SRCS)
	jar -cf isim.jar $(JAVA_OBJS)

tests: wrapping trick diode hermite

wrapping: isim.jar
	java -classpath .:$(JARLOC):isim.jar IntervalExp

trick: isim.jar
	java -classpath .:$(JARLOC):isim.jar Trick

diode: isim.jar
	java -classpath $(JARLOC) com.sun.electric.tool.simulation.interval.Diode

hermite: isim.jar Hermite.class
	java -enableassertions -classpath .:$(JARLOC):isim.jar Hermite

ulp: Ulp.class
	java  -server -XX:+PrintCompilation  -classpath . Ulp
	java  -client -XX:+PrintCompilation  -classpath . Ulp

testint: TestIntervals.class
	java -enableassertions -classpath $(JARLOC):. TestIntervals

sum: Sum.class
	java -server -XX:+PrintCompilation -classpath $(JARLOC):. Sum

inflated: InflatedLinearMap.class
	java -server -classpath $(JARLOC):. InflatedLinearMap

floating: Floating.class
	java -classpath $(JARLOC):. Floating

floats: TestFloatingDecimal.class
	java -client -Xmx600m -enableassertions -classpath $(JARLOC):. TestFloatingDecimal
#	java -server -Xmx600m -enableassertions -classpath $(JARLOC):. TestFloatingDecimal

floatp: TestFloatingDecimal.class
	java -server -Xmx600m -classpath $(JARLOC):. TestFloatingDecimal -p

floatpc: TestFloatingDecimal.class
	java -client -Xmx600m -classpath $(JARLOC):. TestFloatingDecimal -p

strictfp: Strictfp.class
	java -XX:+PrintCompilation -classpath $(JARLOC):. Strictfp

clean:
	rm -rf *.class
