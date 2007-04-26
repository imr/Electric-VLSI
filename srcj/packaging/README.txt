This README file describes the Electric VLSI Design System, a state-of-the-art computer-aided design system for
VLSI circuit design. 

Electric designs MOS and bipolar integrated circuits, printed-circuit-boards, or any type of circuit you choose.
It has many editing styles including layout, schematics, artwork, and architectural specifications. 

A large set of tools is available including design-rule checkers, simulators, routers, layout generators, and more. 

Electric interfaces to most popular CAD specifications including EDIF, LEF/DEF, VHDL, CIF and GDS. 

The most valuable aspect of Electric is its layout-constraint system, which enables top-down design by enforcing
consistency of connections. 

Electric is written in the Java programming language and is distributed as a single ".jar" file.
This file is executable and also has source code in it. 

Electric requires Java version 1.5 from Sun Microsystems.  It does not run properly on open-source
implementations of Java, including the version shipped on Fedora Core systems.  You will have to
reinstall Java from Sun in such cases. 

-------------------- Running Electric

Running Electric varies with the different platforms.  Most systems also allow you to double-click on the .jar file. 

If double-clicking doesn't work, try running it from the command-line by typing: 
     java -jar electric.jar

An alternate command-line is: 
     java -classpath electric.jar com.sun.electric.Launcher

There are a number of options that can be given at the end of the command line: 
  -mdi        force a multiple document interface style (where Electric is one big window with smaller edit windows in it).
  -sdi        force a single document interface style (where each Electric window is separate).
  -s <script> run the <script> file through the Bean shell.
  -batch      run in batch mode (no windows or other user interface are shown).
  -version    provides full version information including build date.
  -v          provides brief version information.
  -help       prints a list of available command options.

-------------------- Memory Control

One problem with Java is that the Java Virtual Machine has a memory limit. This limit prevents programs
from growing too large. However, it prevents large circuits from being edited. 

If Electric runs out of memory, you can request that more be used. To do this, use "General" Preferences
(in menu File / Preferences..., "General" section, "General" tab).  At the bottom of the dialog are two
memory limit fields, for heap space and permanent space. 

The heap space limit is the most important because increasing it will offer much more circuitry capacity.
Note that 32-bit JVMs can only grow so far.  On 32-bit Windows systems you should not set it above 1500
(1.5 Gigabytes).  On 32-bit Linux or Macintosh system, you should not set it above 3600 (3.6 Gigabytes). 

Permanent space is an additional section of memory that can be insufficiently small.  For very large chips,
a value of 200 or larger may enhance performance. 

After changing these values, you will have to quit Electric and restart it for the new memory limits to take effect.

-------------------- Plugins

Electric plug-ins are additional pieces of code that can be downloaded separately to enhance the system's functionality.
Currently, these plug-ins are available: 

  IRSIM The IRSIM simulator is a gate-level simulator from Stanford University. Although originally written in C,
    it was translated to Java so that it could plug into Electric. The Electric version is available from
    Static Free Software at www.staticfreesoft.com/electricIRSIM-8.04.jar. 
  PIE Port Interchange Experiment (PIE) is an experimental version of NCC (see Section 9-7-1). Because it is
    ever-evolving, it is left as a plug-in so that frequent updates can be made. The latest version is available
    from Static Free Software at www.staticfreesoft.com/electricPIE-8.04.jar. 
  Bean Shell The Bean Shell is used to do parameter evaluation in Electric. Advanced operations that make use of
    cell parameters will need this plug-in. The Bean Shell is available from www.beanshell.org. 
  3D The 3D facility lets you view an integrated circuit in three-dimensions. It requires the Java3D package,
    which is available from the Java Community Site, www.j3d.org. This is not a plugin, but rather an enhancement
    to your Java installation. 
  3D Axis Controller Once the 3D facility is installed, there is one extra part that can be added to enhance the
    display: a 3D axis controller. The 3D axis controller is available from Static Free Software at
    www.staticfreesoft.com/electricJava3D-8.04.jar 
  Animation Another extra that can be added to the 3D facility is 3D animation. This requires the
    Java Media Framework (JMF) and extra animation code. The Java Media Framework is available from Sun Microsystems
    at java.sun.com/products/java-media/jmf (this is not a plugin: it is an enhancement to your Java installation).
    The animation code is available from Static Free Software at www.staticfreesoft.com/electricJMF-8.04.jar. 

To attach a plugin, it must be in the CLASSPATH.  The simplest way to do that is to invoked Electric from the command line,
and specify the classpath. For example, to add the beanshell (a file named "bsh-2.0b1.jar"), type: 
     java -classpath electric.jar:bsh-2.0b1.jar com.sun.electric.Launcher

On Windows, you must use the ";" to separate jar files, and you might also have to quote the collection since ";"
separates commands: 
     java -classpath "electric.jar;bsh-2.0b1.jar" com.sun.electric.Launcher

Note that you must explicitly mention the main Electric class (com.sun.electric.Launcher) when using plug-ins
since all of the jar files are grouped together as the "classpath".
