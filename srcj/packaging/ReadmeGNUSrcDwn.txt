---------------- This is Electric, Version 8.04 ----------------

Electric is written in the Java programming language and is distributed in a
single ".jar" file.  There are two variations on the ".jar" file:
  With source code (called "electric-8.04.jar")
  Without source code (called, "electricBinary-8.04.jar").
Both of these files have the binary ".class" files needed to run Electric,
but the one with source-code is larger because it also has all of the Java code. 

---------------- Requirements:

Electric requires Java version 1.5 or later from Sun Microsystems.  It does not
run properly on open-source implementations of Java, including the version shipped
on Fedora Core systems.  You will have to reinstall Java from Sun in such cases. 

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
    www.staticfreesoft.com/electricIRSIM-8.04.jar

> PIE (Port Interchange Experiment)
  This is an experimental version of NCC (see Section 9-7-1).  Because it is
  ever-evolving, it is left as a plug-in so that frequent updates can be made.
  The latest version is available from Static Free Software at
    www.staticfreesoft.com/electricPIE-8.04.jar

> Bean Shell
  The Bean Shell is used to do parameter evaluation in Electric.  Advanced operation
  that make use of cell parameters will need this plug-in.  The Bean Shell is available from
    www.beanshell.org.

> 3D
  The 3D facility lets you view an integrated circuit in three-dimensions. It requires
  the Java3D package, which is available from the Java Community Site, www.j3d.org.
  This is not a plugin, but rather an enhancement to your Java installation. 

> 3D Axis Controller
  Once the 3D facility is installed, there is one extra part that can be added to
  enhance the display: a 3D axis controller.  The 3D axis controller is available from
  Static Free Software at:
    www.staticfreesoft.com/electricJava3D-8.04.jar 

> Animation
  Another extra that can be added to the 3D facility is 3D animation.  This requires
  the Java Media Framework (JMF) and extra animation code.  The Java Media Framework is
  available from Sun Microsystems at java.sun.com/products/java-media/jmf (this is not
  a plugin: it is an enhancement to your Java installation).  The animation code is
  available from Static Free Software at:
    www.staticfreesoft.com/electricJMF-8.04.jar

> Russian User's Manual
  An earlier version of the user's manual (8.02) has been translated into Russian.
  This manual is available from Static Free Software at:
    www.staticfreesoft.com/electricRussianManual-8.04.jar

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

---------------- Discussion:

There are two GNU mailing lists devoted to Electric:

> bug-gnu-electric
  Subscribe at http://mail.gnu.org/mailman/listinfo/bug-gnu-electric

> discuss-gnu-electric
  Subscribe at http://mail.gnu.org/mailman/listinfo/discuss-gnu-electric

In addition, you can send mail to:
    info@staticfreesoft.com


