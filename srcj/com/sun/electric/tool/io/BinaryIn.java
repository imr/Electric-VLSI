package com.sun.electric.tool.io;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.technology.Technology;

import java.io.*;
import java.util.Iterator;

public class BinaryIn extends Input
{
	// ------------------------- private data ----------------------------
	/** true if bytes are swapped on disk */								private boolean bytesSwapped;
	/** the size of a "big" integer on disk (4 or more bytes) */			private int sizeOfBig;
	/** the size of a "small" integer on disk (2 or more bytes) */			private int sizeOfSmall;
	/** the size of a character on disk (1 or 2 bytes) */					private int sizeOfChar;
	/** the number of integers on disk that got clipped during input */		private int clippedIntegers;
	/** the number of bytes of data read so far */							private int byteCount;
	/** the number of tools in the file */									private int toolCount;
	/** the number of tools in older files */								private int toolBCount;
	/** the number of technologies in the file */							private int techCount;
	/** the number of primitive NodeProtos in the file */					private int primNodeProtoCount;
	/** the number of primitive PortProtos in the file */					private int primPortProtoCount;
	/** the number of ArcProtos in the file */								private int arcProtoCount;
	/** the number of NodeProtos in the file */								private int nodeProtoCount;
	/** the number of Cells in the file */									private int cellCount;
	/** the number of NodeInsts in the file */								private int nodeCount;
	/** the number of Exports in the file */								private int portProtoCount;
	/** the number of ArcInsts in the file */								private int arcCount;
	/** the number of Geometrics in the file */								private int geomCount;
	/** the index of the current cell */									private int curCell;
	/** true to convert all text descriptor values */						private boolean convertTextDescriptors;
	/** true to require text descriptor values */							private boolean alwaysTextDescriptors;
	/** list of all XXXX in the library */									private NodeInst [] nodeList;
	/** list of all XXXX in the library */									private int [] nodeCounts;
	/** list of all XXXX in the library */									private NodeProto [] nodeProtoList;
	/** list of all XXXX in the library */									private PortProto [] portProtoList;
	/** list of all XXXX in the library */									private int [] portCount;
	/** list of all XXXX in the library */									private PortProto [] portpProtoList;
	/** list of all XXXX in the library */									private String [] portpProtoError;
//	/** list of all XXXX in the library */									private Export [] portExpInstList;
//	/** list of all XXXX in the library */									private Connection [] portArcInstList;
	/** list of all XXXX in the library */									private ArcInst [] arcList;
	/** list of all XXXX in the library */									private int [] arcCounts;
	/** list of all XXXX in the library */									private NodeProto [] nodePrimProtoList;
	/** list of all XXXX in the library */									private int [] nodePrimProtoTech;
	/** list of all XXXX in the library */									private boolean [] nodePrimProtoError;
	/** list of all XXXX in the library */									private String [] nodePrimProtoOrig;
	/** list of all XXXX in the library */									private ArcProto [] arcProtoList;
	/** list of all XXXX in the library */									private String [] arcProtoError;
	/** list of all XXXX in the library */									private Technology [] techList;
	/** list of all XXXX in the library */									private String [] techError;
	/** list of all XXXX in the library */									private int [] toolList;
	/** list of all XXXX in the library */									private String [] toolError;

	// ---------------------- private and protected methods -----------------
	/** current magic number: version 12 */		private static final int MAGIC12=              -1595;
	/** older magic number: version 11 */		private static final int MAGIC11=              -1593;
	/** older magic number: version 10 */		private static final int MAGIC10=              -1591;
	/** older magic number: version 9 */		private static final int MAGIC9=               -1589;
	/** older magic number: version 8 */		private static final int MAGIC8=               -1587;
	/** older magic number: version 7 */		private static final int MAGIC7=               -1585;
	/** older magic number: version 6 */		private static final int MAGIC6=               -1583;
	/** older magic number: version 5 */		private static final int MAGIC5=               -1581;
	/** older magic number: version 4 */		private static final int MAGIC4=               -1579;
	/** older magic number: version 3 */		private static final int MAGIC3=               -1577;
	/** older magic number: version 2 */		private static final int MAGIC2=               -1575;
	/** oldest magic number: version 1 */		private static final int MAGIC1=               -1573;

	BinaryIn()
	{
	}

	// ----------------------- public methods -------------------------------

	public Library ReadLib()
	{
		try
		{
			return readTheLibrary();
		} catch (IOException e)
		{
			System.out.println("End of file reached while reading " + filePath);
			return null;
		}
	}
	
	private Library readTheLibrary() throws IOException
	{
		Library returnLib = null;

		// initialize
		clippedIntegers = 0;
		byteCount = 0;

		// read the magic number and determine whether bytes are swapped
		bytesSwapped = false;
		byte byte1 = readByte();
		byte byte2 = readByte();
		byte byte3 = readByte();
		byte byte4 = readByte();
		int magic = ((byte4&0xFF) << 24) | ((byte3&0xFF) << 16) | ((byte2&0xFF) << 8) | (byte1&0xFF);
		if (magic != MAGIC1 && magic != MAGIC2 && magic != MAGIC3 && magic != MAGIC4 &&
			magic != MAGIC5 && magic != MAGIC6 && magic != MAGIC7 && magic != MAGIC8 &&
			magic != MAGIC9 && magic != MAGIC10 && magic != MAGIC11 && magic != MAGIC12)
		{
			magic = ((byte1&0xFF) << 24) | ((byte2&0xFF) << 16) | ((byte3&0xFF) << 8) | (byte4&0xFF);
			if (magic != MAGIC1 && magic != MAGIC2 && magic != MAGIC3 && magic != MAGIC4 &&
				magic != MAGIC5 && magic != MAGIC6 && magic != MAGIC7 && magic != MAGIC8 &&
				magic != MAGIC9 && magic != MAGIC10 && magic != MAGIC11 && magic != MAGIC12)
			{
				System.out.println("Bad file format: does not start with proper magic number");
				return null;
			}
			bytesSwapped = true;
		}
		
		// determine the size of "big" and "small" integers as well as characters on disk
		if (magic <= MAGIC10)
		{
			sizeOfSmall = readByte();
			sizeOfBig = readByte();
		} else
		{
			sizeOfSmall = 2;
			sizeOfBig = 4;
		}
		if (magic <= MAGIC11)
		{
			sizeOfChar = readByte();
		} else
		{
			sizeOfChar = 1;
		}
		
		// get count of objects in the file
		toolCount = readBigInteger();
		techCount = readBigInteger();
		primNodeProtoCount = readBigInteger();
		primPortProtoCount = readBigInteger();
		arcProtoCount = readBigInteger();
		nodeProtoCount = readBigInteger();
		nodeCount = readBigInteger();
		portProtoCount = readBigInteger();
		arcCount = readBigInteger();
		geomCount = readBigInteger();
		if (magic <= MAGIC9 && magic >= MAGIC11)
		{
			/* versions 9 through 11 stored a "cell count" */
			cellCount = readBigInteger();
		} else
		{
			cellCount = nodeProtoCount;
		}
		curCell = readBigInteger();

		// get the Electric version (version 8 and later)
		String versionString;
		if (magic <= MAGIC8) versionString = readString(); else
			versionString = "3.35";
		Version version = Version.parseVersion(versionString);

		// for versions before 6.03q, convert MOSIS CMOS technology names
//		boolean convertMosisCmosTechnologies = false;
//		if (version.getMajor() < 6 ||
//			(version.getMajor() == 6 && version.getMinor() < 3) ||
//			(version.getMajor() == 6 && version.getMinor() == 3 && version.getDetail() < 17))
//		{
//			if ((asktech(mocmossub_tech, x_("get-state"))&MOCMOSSUBNOCONV) == 0)
//				convertMosisCmosTechnologies = true;
//		}

		// for versions before 6.04c, convert text descriptor values
		convertTextDescriptors = false;
		if (version.getMajor() < 6 ||
			(version.getMajor() == 6 && version.getMinor() < 4) ||
			(version.getMajor() == 6 && version.getMinor() == 4 && version.getDetail() < 3))
		{
			convertTextDescriptors = true;
		}

		// for versions 6.05x and later, always have text descriptor values
		alwaysTextDescriptors = true;
		if (version.getMajor() < 6 ||
			(version.getMajor() == 6 && version.getMinor() < 5) ||
			(version.getMajor() == 6 && version.getMinor() == 5 && version.getDetail() < 24))
		{
			alwaysTextDescriptors = false;
		}

		// get the newly created views (version 9 and later)
		for (Iterator it = View.getViews(); it.hasNext();)
		{
			View v = (View) it.next();
			v.setTemp1(0);
		}
		View.unknown.setTemp1(-1);
		View.layout.setTemp1(-2);
		View.schematic.setTemp1(-3);
		View.icon.setTemp1(-4);
		View.simsnap.setTemp1(-5);
		View.skeleton.setTemp1(-6);
		View.vhdl.setTemp1(-7);
		View.netlist.setTemp1(-8);
		View.doc.setTemp1(-9);
		View.netlistNetlisp.setTemp1(-10);
		View.netlistAls.setTemp1(-11);
		View.netlistQuisc.setTemp1(-12);
		View.netlistRsim.setTemp1(-13);
		View.netlistSilos.setTemp1(-14);
		View.verilog.setTemp1(-15);
		View.comp.setTemp1(-16);
		if (magic <= MAGIC9)
		{
			int numExtraViews = readBigInteger();
			for(int i=0; i<numExtraViews; i++)
			{
				String viewName = readString();
				String viewShortName = readString();
				View v = View.getView(viewName);
				if (v == null)
				{
					v = View.makeInstance(viewName, viewShortName, 0);
					if (v == null) return null;
				}
				v.setTemp1(i + 1);
			}
		}

		// get the number of toolbits to ignore
		if (magic <= MAGIC3 && magic >= MAGIC6)
		{
			// versions 3, 4, 5, and 6 find this in the file
			toolBCount = readBigInteger();
		} else
		{
			// versions 1 and 2 compute this (versions 7 and later ignore it)
			 toolBCount = toolCount;
		}

		// erase the current database
//		eraselibrary(lib);

		// allocate pointers
		nodeList = new NodeInst[nodeCount];
		nodeCounts = new int[nodeProtoCount];
		nodeProtoList = new NodeProto [nodeProtoCount];
		portProtoList = new PortProto [portProtoCount];
		portCount = new int[nodeProtoCount];
		portpProtoList = new PortProto[primPortProtoCount];
		portpProtoError = new String[primPortProtoCount];
		Export [] portExpInstList = new Export[portProtoCount];
		Connection [] portArcInstList = new Connection[arcCount * 2];
		arcList = new ArcInst[arcCount];
		arcCounts = new int[nodeProtoCount];
		nodePrimProtoList = new NodeProto[primNodeProtoCount];
		nodePrimProtoTech = new int[primNodeProtoCount];
		nodePrimProtoError = new boolean[primNodeProtoCount];
		nodePrimProtoOrig = new String[primNodeProtoCount];
		arcProtoList = new ArcProto[arcProtoCount];
		arcProtoError = new String[arcProtoCount];
		techList = new Technology[techCount];
		techError = new String[techCount];
		toolList = new int[toolCount];
		toolError = new String[toolCount];

		return returnLib;
	}

	byte readByte()
		throws IOException
	{
		int value = fileInputStream.read();
		if (value == -1) throw new IOException();
		return (byte)value;
	}

	int readBigInteger()
		throws IOException
	{
		byte [] data = new byte[4];
		readBytes(data, sizeOfBig, 4, true);
		int intValue = ((data[3]&0xFF) << 24) | ((data[2]&0xFF) << 16) |
			((data[1]&0xFF) << 8) | (data[0]&0xFF);
		return intValue;
	}

	String readString()
		throws IOException
	{
		if (sizeOfChar != 1)
		{
			/* disk and memory don't match: read into temporary string */
			System.out.println("Cannot handle library files with unicode strings");
//			tempstr = io_gettempstring();
//			if (allocstring(&name, tempstr, cluster)) return(0);
			return null;
		} else
		{
			/* disk and memory match: read the data */
			int len = readBigInteger();
			byte [] stringBytes = new byte[len];
			int ret = fileInputStream.read(stringBytes, 0, len);
			if (ret != len) throw new IOException();
			String theString = new String(stringBytes);
			return theString;
		}
	}

	/** used to swap bytes and adjust when disk size differs from memory size */
	private static byte [] swapBuf = new byte[128];

	void readBytes(byte [] data, int diskSize, int memorySize, boolean signExtend)
		throws IOException
	{
		/* check for direct transfer */
		if (diskSize == memorySize && !bytesSwapped)
		{
			/* just peel it off the disk */
			int ret = fileInputStream.read(data, 0, diskSize);
			if (ret != diskSize) throw new IOException();
		} else
		{
			/* not a simple read, use a buffer */
			int ret = fileInputStream.read(swapBuf, 0, diskSize);
			if (ret != diskSize) throw new IOException();
			if (bytesSwapped)
			{
				byte swapbyte;
				switch (diskSize)
				{
					case 2:
						swapbyte = swapBuf[0]; swapBuf[0] = swapBuf[1]; swapBuf[1] = swapbyte;
						break;
					case 4:
						swapbyte = swapBuf[3]; swapBuf[3] = swapBuf[0]; swapBuf[0] = swapbyte;
						swapbyte = swapBuf[2]; swapBuf[2] = swapBuf[1]; swapBuf[1] = swapbyte;
						break;
					case 8:
						swapbyte = swapBuf[7]; swapBuf[7] = swapBuf[0]; swapBuf[0] = swapbyte;
						swapbyte = swapBuf[6]; swapBuf[6] = swapBuf[1]; swapBuf[1] = swapbyte;
						swapbyte = swapBuf[5]; swapBuf[5] = swapBuf[2]; swapBuf[2] = swapbyte;
						swapbyte = swapBuf[4]; swapBuf[4] = swapBuf[3]; swapBuf[3] = swapbyte;
						break;
				}
			}
			if (diskSize == memorySize)
			{
				for(int i=0; i<memorySize; i++) data[i] = swapBuf[i];
			} else
			{
				if (diskSize > memorySize)
				{
					/* trouble! disk has more bits than memory.  check for clipping */
					for(int i=0; i<memorySize; i++) data[i] = swapBuf[i];
					for(int i=memorySize; i<diskSize; i++)
						if (swapBuf[i] != 0 && swapBuf[i] != 0xFF)
							clippedIntegers++;
				} else
				{
					/* disk has smaller integer */
					if (!signExtend || (swapBuf[diskSize-1] & 0x80) == 0)
					{
						for(int i=diskSize; i<memorySize; i++) swapBuf[i] = 0;
					} else
					{
						for(int i=diskSize; i<memorySize; i++) swapBuf[i] = (byte)0xFF;
					}
					for(int i=0; i<memorySize; i++) data[i] = swapBuf[i];
				}
			}
		}
		byteCount += diskSize;
//		if (io_verbose < 0 && io_binindata.filelength > 0 && io_inputprogressdialog != 0)
//		{
//			if (byteCount > io_binindata.reported + REPORTINC)
//			{
//				DiaSetProgress(io_inputprogressdialog, byteCount, io_binindata.filelength);
//				io_binindata.reported = byteCount;
//			}
//		}
	}
}
