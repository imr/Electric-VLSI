package com.sun.electric.database;

public class Graphics extends ElectricObject
{
	int bits;
	int color;
	int displayMethod;
	int printMethod;
	int printRed, printGreen, printBlue;
	double printOpacity;
	int printForeground;
	int [] pattern;

	/* bit map colors (in GRAPHICS->bits) */
	public final static int LAYERN=   0000;			/* nothing                               */
	public final static int LAYERH=   0001;			/* highlight color and bit plane         */
	public final static int LAYEROE=  0002;			/* opaque layer escape bit               */
	public final static int LAYERO=   0176;			/* opaque layers                         */
	public final static int LAYERT1=  0004;			/* transparent layer 1                   */
	public final static int LAYERT2=  0010;			/* transparent layer 2                   */
	public final static int LAYERT3=  0020;			/* transparent layer 3                   */
	public final static int LAYERT4=  0040;			/* transparent layer 4                   */
	public final static int LAYERT5=  0100;			/* transparent layer 5                   */
	public final static int LAYERG=   0200;			/* grid line color and bit plane         */
	public final static int LAYERA=   0777;			/* everything                            */

	/* color map colors (in GRAPHICS->col) */
	public final static int ALLOFF=   0000;			/* no color                              */
	public final static int HIGHLIT=  0001;			/* highlight color and bit plane         */
	public final static int COLORT1=  0004;			/* transparent color 1                   */
	public final static int COLORT2=  0010;			/* transparent color 2                   */
	public final static int COLORT3=  0020;			/* transparent color 3                   */
	public final static int COLORT4=  0040;			/* transparent color 4                   */
	public final static int COLORT5=  0100;			/* transparent color 5                   */
	public final static int GRID=     0200;			/* grid line color and bit plane         */

	public final static int WHITE=    0002;			/* white                                 */
	public final static int BLACK=    0006;			/* black                                 */
	public final static int RED=      0012;			/* red                                   */
	public final static int BLUE=     0016;			/* blue                                  */
	public final static int GREEN=    0022;			/* green                                 */
	public final static int CYAN=     0026;			/* cyan                                  */
	public final static int MAGENTA=  0032;			/* magenta                               */
	public final static int YELLOW=   0036;			/* yellow                                */
	public final static int CELLTXT=  0042;			/* cell and port names                   */
	public final static int CELLOUT=  0046;			/* cell outline                          */
	public final static int WINBOR=   0052;			/* window border color                   */
	public final static int HWINBOR=  0056;			/* highlighted window border color       */
	public final static int MENBOR=   0062;			/* menu border color                     */
	public final static int HMENBOR=  0066;			/* highlighted menu border color         */
	public final static int MENTXT=   0072;			/* menu text color                       */
	public final static int MENGLY=   0076;			/* menu glyph color                      */
	public final static int CURSOR=   0102;			/* cursor color                          */
	public final static int GRAY=     0106;			/* gray                                  */
	public final static int ORANGE=   0112;			/* orange                                */
	public final static int PURPLE=   0116;			/* purple                                */
	public final static int BROWN=    0122;			/* brown                                 */
	public final static int LGRAY=    0126;			/* light gray                            */
	public final static int DGRAY=    0132;			/* dark gray                             */
	public final static int LRED=     0136;			/* light red                             */
	public final static int DRED=     0142;			/* dark red                              */
	public final static int LGREEN=   0146;			/* light green                           */
	public final static int DGREEN=   0152;			/* dark green                            */
	public final static int LBLUE=    0156;			/* light blue                            */
	public final static int DBLUE=    0162;			/* dark blue                             */
	/*               0166 */		/* unassigned                            */
	/*               0172 */		/* unassigned                            */
	/*               0176 */		/* unassigned (and should stay that way) */

	/* drawing styles (in GRAPHICS->style) */
	public final static int NATURE=       1;			/* choice between solid and patterned */
	public final static int SOLIDC=       0;			/*   solid colors */
	public final static int PATTERNED=    1;			/*   stippled with "raster" */
	public final static int INVISIBLE=    2;			/* don't draw this layer */
	public final static int INVTEMP=      4;			/* temporary for INVISIBLE bit */
	public final static int OUTLINEPAT= 010;			/* if NATURE is PATTERNED, outline it */

	public Graphics(int bits, int color, int displayMethod, int printMethod,
		int printRed, int printGreen, int printBlue, double printOpacity, int printForeground, int[] pattern)
	{
		this.bits = bits;
		this.color = color;
		this.displayMethod = displayMethod;
		this.printMethod = printMethod;
		this.printRed = printRed;
		this.printGreen = printGreen;
		this.printBlue = printBlue;
		this.printOpacity = printOpacity;
		this.printForeground = printForeground;
		this.pattern = pattern;
	}
}
