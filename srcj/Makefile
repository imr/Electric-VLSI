BEANSHELLLOC = ../lib/java/bsh-1.2b3.jar
JAVACOMPILER = javac
JAVAFLAGS = -classpath $(BEANSHELLLOC)

JAVAPURESRC = \
	database/ArcInst.java \
	database/ArcProto.java \
	database/Cell.java \
	database/Connection.java \
	database/EdgeH.java \
	database/EdgeV.java \
	database/EGraphics.java \
	database/ElectricObject.java \
	database/Export.java \
	database/Geometric.java \
	database/JNetwork.java \
	database/Layer.java \
	database/Library.java \
	database/Networkable.java \
	database/NodeInst.java \
	database/NodeProto.java \
	database/Poly.java \
	database/PortInst.java \
	database/PortProto.java \
	database/PrimitiveNode.java \
	database/PrimitivePort.java \
	database/SizeOffset.java \
	database/Technology.java \
	database/Tool.java \
	database/VarContext.java \
	database/View.java \
	technologies/TecGeneric.java \
	technologies/TecMoCMOS.java \
	user/ClassReloader.java \
	user/EditWindow.java \
	user/Electric.java \
	user/MessagesWindow.java

JAVA_SRCS = $(JAVAPURESRC:%=com/sun/electric/%)
JAVA_OBJS = $(JAVA_SRCS:%.java=%.class)

electric.jar:
	javac $(JAVAFLAGS) $(JAVA_SRCS)
	jar -cfm electric.jar MANIFEST.MF $(JAVA_OBJS) $(BEANSHELLLOC)

all: electric.jar
	java -classpath .:$(BEANSHELLLOC) com/sun/electric/user/Electric

clean:
	rm -rf $(JAVA_OBJS)
