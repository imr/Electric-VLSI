JARLOC = Jama-1.0.1.jar:ia_math.jar:nbsp.jar:../electric.jar
JAVACOMPILER = javac
JAVAFLAGS = -classpath $(JARLOC) -source 1.5
# -Xlint:unchecked

JAVAPURESRC = \
	IntervalMatrix.java \
	IntervalExp.java \
	ExprEval.java \
	Trick.java \
	Diode.java

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
	java -classpath .:$(JARLOC):isim.jar Diode

hermite: isim.jar Hermite.class
	java -classpath .:$(JARLOC):isim.jar Hermite

ulp: Ulp.class
	java  -server -XX:+PrintCompilation  -classpath . Ulp
	java  -client -XX:+PrintCompilation  -classpath . Ulp

testint: TestIntervals.class
	java -enableassertions -classpath $(JARLOC):. TestIntervals

sum: Sum.class
	java -server -XX:+PrintCompilation -classpath $(JARLOC):. Sum

inflated: InflatedLinearMap.class
	java -server -classpath $(JARLOC):. InflatedLinearMap

floats: TestFloatingDecimal.class
	java -server -enableassertions -XX:+PrintCompilation -classpath $(JARLOC):. TestFloatingDecimal

# floatp: FloatingDecimal.class
#	java -server -XX:+PrintCompilation -classpath $(JARLOC):. FloatingDecimal

clean:
	rm -rf *.class
