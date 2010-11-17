---------------- This is Electric, Version ${project.version} ----------------

Electric is written in the Java programming language and is distributed in a
single ".jar" file.  There are two variations on the ".jar" file:
  With source code (called "electric-${project.version}.jar")
  Without source code (called, "electricBinary-${project.version}.jar").
Both of these files have the binary ".class" files needed to run Electric,
but the one with source-code is larger because it also has all of the Java code.
Latest source code can be downloaded from Electric Home Page:
http://java.net/projects/electric .

---------------- Requirements:

Electric requires Java version 1.5 or later from Sun Microsystems.  It can also run
with Apache Harmony.  However, it does not run properly on some open-source
implementations of Java, including the version shipped on Fedora Core systems.
You will have to reinstall Java from Sun or Apache in such cases. 

---------------- Running:

Running Electric varies with the different platforms.  Most systems allow you
to double-click on the .jar file. 

If double-clicking doesn't work, try running it from the command-line by typing: 
     java -jar electric.jar

An alternate command-line is: 
     java -classpath electric.jar com.sun.electric.Launcher

---------------- Adding Plug-Ins:

Electric plug-ins are additional pieces of code that can be downloaded separately
to enhance the system's functionality.  Currently, these plug-ins are available:
 
> IRSIM
  The IRSIM simulator is a gate-level simulator from Stanford University. Although
  originally written in C, it was translated to Java so that it could plug into
  Electric.  The Electric version is available from Static Free Software at:
    www.staticfreesoft.com/electricIRSIM-${project.version}.jar

> Java
  The Bean Shell is used to do scripting and parameter evaluation in Electric.  Advanced
  operations that make use of cell parameters will need this plug-in.  The Bean Shell is
  available from:
    www.beanshell.org

> Python
  Jython is used to do scripting in Electric.  Jython is available from:
    www.jython.org
  Build the "standalone" installation to get the JAR file.

> 3D
  The 3D facility lets you view an integrated circuit in three-dimensions. It requires
  the Java3D package, which is available from the Java Community Site, www.j3d.org.
  This is not a plugin, but rather an enhancement to your Java installation. 

> 3D Axis Controller
  Once the 3D facility is installed, there is one extra part that can be added to
  enhance the display: a 3D axis controller.  The 3D axis controller is available from
  Static Free Software at:
    www.staticfreesoft.com/electricJava3D-${project.version}.jar 

> Animation
  Another extra that can be added to the 3D facility is 3D animation.  This requires
  the Java Media Framework (JMF) and extra animation code.  The Java Media Framework is
  available from Sun Microsystems at java.sun.com/products/java-media/jmf (this is not
  a plugin: it is an enhancement to your Java installation).  The animation code is
  available from Static Free Software at:
    www.staticfreesoft.com/electricJMF-${project.version}.jar

> Russian User's Manual
  An earlier version of the user's manual (8.02) has been translated into Russian.
  This manual is available from Static Free Software at:
    www.staticfreesoft.com/electricRussianManual-${project.version}.jar

To attach a plugin, it must be in the CLASSPATH.  The simplest way to do that is to
invoked Electric from the command line, and specify the classpath.  For example, to
add the beanshell (a file named "bsh-2.0b1.jar"), type: 
    java -classpath electric.jar:bsh-2.0b1.jar com.sun.electric.Launcher

On Windows, you must use the ";" to separate jar files, and you might also have to
quote the collection since ";" separates commands:
    java -classpath "electric.jar;bsh-2.0b1.jar" com.sun.electric.Launcher

Note that you must explicitly mention the main Electric class (com.sun.electric.Launcher)
when using plug-ins since all of the jar files are grouped together as the "classpath".

---------------- Building from Sources:

Extract the source ".jar" file.  It will contain the subdirectory "com" with all
source code.  The file "build.xml" has the Ant scripts for compiling this code.

When rebuilding Electric, there are some Macintosh vs. non-Macintosh issues to consider:

> Build on a Macintosh
  The easiest thing to do is to remove references to "AppleJavaExtensions.jar"
  from the Ant script (build.xml).  This package is a collection of "stubs" to
  replace Macintosh functions that are unavailable elsewhere.  You can also build
  a native "App" by running the "mac-app" Ant script.  This script makes use of files
  in the "packaging" folder.  Macintosh computers must be running OS 10.3 or later. 

> Build on non-Macintosh
  If you are building Electric on and for a non-Macintosh platform, remove references
  to "AppleJavaExtensions.jar" from the Ant script (build.xml).  Also, remove the module
  "com.sun.electric.MacOSXInterface.java".  It is sufficient to delete this module,
  because Electric automatically detects its presence and is able to run without it.

> Build on non-Macintosh, to run on all platforms
  To build Electric so that it can run on all platforms, Macintosh and other, you will
  need to keep the module "com.sun.electric.MacOSXInterface.java".  However, in order
  to build it, you will need the stub package "AppleJavaExtensions.jar".  The package
  can be downloaded from Apple at
    http://developer.apple.com/samplecode/AppleJavaExtensions/index.html.

---------------- Building from Sources hosted on java.net in NetBeans IDE

1) Register on java.net . Let USERNAME is your username on java.net . 
2) Start NetBeans 6.9 or later.
3) Register java.net Team Server in NetBeans IDE :
   3.1) Choose Window > Services in the main menu to open the Services window.
   3.2) Right-click the Team Servers node and choose Add Team Server.
   3.3) Type the Name "java.net" and URL "https://java.net" of the Team Server. Click Add.
   3.4) Choose Team > Team Server > Login from the main menu to log in to the server.
        Alternatively, you can right-click the name of the server under the Team Servers node and choose Login.
4) Download Electric Sources from java.net .
   4.1) Choose File > Open Team Project... from the main menu.
   4.2) Search for electric project
   4.3) Select Electric: VLSI Design System and click Open From Team Server
   4.4) Expand Electric: VLSI Design System node int the Team tab
   4.5) Expand Sources subnode
   4.6) Click on Source Code Repository (get)
   4.6) Either enter "Folder To Get" in "Get Sources From Team Server" dialog or click "Browse" button near it.
        The "Folder to Get" of Electric-9.00 is "tags/electric-9.00" .
        The "Folder to Get" of latest Electric sources is "trunk/electric" .
   4.7) Choose "Local Folder" in "Get Sources From Tram Server" where to download Electric Sources.
        The default is "~/NetBeansProjects/electric~svn".
   4.8) Click "Get From Team Server"
   4.9) The "Checkout Completed" dialog will say that 7 projects were checkout.
        It will suggest you to open a project.
   4.10) Click "Open Project..."
   4.11) Choose "electric" and click "Open".
5) Build Electric
   5.1) Right-click "electric" node in "Projects" tab.
   5.2) Choose "Build".
6) Open "electric-core" subproject and run Electric.
   6.1) Expand "electric" node in "Projects" tab.
   6.2) Expand "Modules" subnode.
   6.3) Double-click "electric-core" subnode.
   6.4) "electric-core" node will appear in "Projects" tab.
        Right-click on it and choose "Set as Main Project"
   6.5) Choose either "Run > Run Project (electric-core)" or "Debug > Debug Project (electric-core)" from the main menu.
7) Create a shortcut to start Electric from Desktop
   7.1) Create a shortcut to "Local Folder/electric/electric-distribution/electric-${project.version}-app/bin/electric" in Unux
        or to "Local Folder/electric/electric-distribution/electric-${project.version}-app/bin/electric.bat" in Windows
   7.2) Launch Electric by this shortcut.
8) Create electric distribution for your organization(optional).
   8.1) Open subproject "electric-distribution" by double-clicking node "electric|Modules|electric-distribution"
   8.2) Set electric-distribution as Main Project.
   8.3) Activate combo box in the tool bar that shows "<default-config>" and choose "distro" config.
   8.4) Build an "electric-distribution" project.
   8.5) Copy to a shared location in your file system any of 
         directory "Local Folder/electric/electric-distribution/electric-${project.version}-distro"
         jar file "Local Folder/electric/electric-distribution/electric-${project.version}-bin-jar"

---------------- Building from latest Sources in command-line:

1) Check that these tools are installed on your computer:
JDK 1.5 or later
Subversion
Apache Maven either version 3.0 or version 2.2.1  http://maven.apache.org

The following variable should be defined in environment
JAVA_PATH  -  path to JDK root directory

2) Obtain latest sources by Subversion

a) For the first time
$ cd WORK-DIRECTORY
$ svn --username USERNAME checkout https://svn.java.net/svn/electric~svn/trunk/electric
$ cd electric

b) Next time
$ cd WORK-DIRECTORY/electric
$ svn update

3) Compile sources and install jars with Electric binary components into maven local repository.
  The default location of maven local repository on Unix systems is ~/.m2/repository

$ mvn -DskipTests=true install

4) Run the Electric launcher

WORK-DIRECTORY/electric/electric-distribution/target/electric-${project.version}.app/bin/electric
or
WORK-DIRECTORY/electric/electric-distribution/target/electric-${project.version}.app/bin/electric.bat

You might execute Electric with larger heap size if your design is large.

set ELECTRIC_OPTS="-Xmx1024m -XX:MaxPermSize=128m"; WORK-DIRECTORY/electric/electric-distribution/target/electric-${project.version}.app/bin/electric
or
set ELECTRIC_OPTS="-Xmx1024m -XX:MaxPermSize=128m"
WORK-DIRECTORY/electric/electric-distribution/target/electric-${project.version}.app/bin/electric.bat

---------------- Building distribution from latest Sources:

The Electric launcher will load Electric components from the maven local repository.
If you want that other people from your organization can run Electric from your binaries,
you can prepare Electric distribution for them.

$ mvn -DskipTests=true -Pdistro install

The resulting distribution is generated in three formats:

a) directory
WORK-DIRECTORY/electric/electric-distribution/target/electric-${project.version}-distro/
   Its subdirectory
WORK-DIRECTORY/electric/electric-distribution/target/electric-${project.version}-distro/bin
 contains launch scripts
electric.bar - for Windows systems
electric     - for Unix systems

Add the Electric "bin" directory to your system PATH 
or link "electric" to your local bin directory
or create a shortcut for "electric.bat"

b) tarball with directory 
WORK-DIRECTORY/electric/electric-distribution/target/electric-${project.version}-distro.tar

c) jar archive with all dependencies
WORK-DIRECTORY/electric/electric-distribution/target/electric-${project.version}-bin.jar

---------------- Discussion:

There are three mailing lists devoted to Electric:

> google groups "electricvlsi"
  View at: http://groups.google.com/group/electricvlsi

> bug-gnu-electric
  Subscribe at http://mail.gnu.org/mailman/listinfo/bug-gnu-electric

> discuss-gnu-electric
  Subscribe at http://mail.gnu.org/mailman/listinfo/discuss-gnu-electric

In addition, you can send mail to:
    info@staticfreesoft.com
