package com.sun.electric.database;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A Technology object contains PrimitiveNodes and ArcProtos.  There may
 * be more than one Technology object.  To get a particular Technology,
 * use <code>Electric.findTechnology</code>
 */
public class Technology extends ElectricObject
{
	private String techName;
	private String techDesc;
	private double defLambda;
	private ArrayList nodes /** list of primitive nodes */ ;
	private ArrayList arcs;

	// static list of all Technologies in Electric
	private static ArrayList technologies = new ArrayList();
	private static Technology curTech = null;

	/* quarter unit fractions */
	public final static int WHOLE= 120;
    /**  NULL */  public final static int XX=	   -WHOLE;	
    /**  0.0  */  public final static int K0=	   0;
	/**  0.25 */  public final static int Q0 =   (WHOLE/4);
	/**  0.5 */   public final static int H0=    (WHOLE/2);
	/**  0.75 */  public final static int T0=    (H0+Q0);		
	/**  1.0  */  public final static int K1=    (WHOLE);		
	/**  1.25 */  public final static int Q1=    (K1+Q0);		
	/**  1.5  */  public final static int H1=    (K1+H0);		
	/**  1.75 */  public final static int T1=    (K1+T0);		
	/**  2.0  */  public final static int K2=    (WHOLE*2);	
	/**  2.25 */  public final static int Q2=    (K2+Q0);		
	/**  2.5  */  public final static int H2=    (K2+H0);		
	/**  2.75 */  public final static int T2=    (K2+T0);		
	/**  3.0  */  public final static int K3=    (WHOLE*3);	
	/**  3.25 */  public final static int Q3=    (K3+Q0);		
	/**  3.5  */  public final static int H3=    (K3+H0);		
	/**  3.75 */  public final static int T3=    (K3+T0);		
	/**  4.0  */  public final static int K4=    (WHOLE*4);	
	/**  4.25 */  public final static int Q4=    (K4+Q0);		
	/**  4.5  */  public final static int H4=    (K4+H0);		
	/**  4.75 */  public final static int T4=    (K4+T0);		
	/**  5.0  */  public final static int K5=    (WHOLE*5);	
	/**  5.25 */  public final static int Q5=    (K5+Q0);		
	/**  5.5  */  public final static int H5=    (K5+H0);		
	/**  5.75 */  public final static int T5=    (K5+T0);		
	/**  6.0  */  public final static int K6=    (WHOLE*6);	
	/**  6.25 */  public final static int Q6=    (K6+Q0);		
	/**  6.5  */  public final static int H6=    (K6+H0);		
	/**  6.75 */  public final static int T6=    (K6+T0);		
	/**  7.0  */  public final static int K7=    (WHOLE*7);	
	/**  7.25 */  public final static int Q7=    (K7+Q0);		
	/**  7.5  */  public final static int H7=    (K7+H0);		
	/**  7.75 */  public final static int T7=    (K7+T0);		
	/**  8.0  */  public final static int K8=    (WHOLE*8);	
	/**  8.25 */  public final static int Q8=    (K8+Q0);		
	/**  8.5  */  public final static int H8=    (K8+H0);		
	/**  8.75 */  public final static int T8=    (K8+T0);		
	/**  9.0  */  public final static int K9=    (WHOLE*9);	
	/**  9.25 */  public final static int Q9=    (K9+Q0);		
	/**  9.5  */  public final static int H9=    (K9+H0);		
	/**  9.75 */  public final static int T9=    (K9+T0);		
	/** 10.0  */  public final static int K10=   (WHOLE*10);	
	/** 10.5  */  public final static int H10=   (K10+H0);	
	/** 11.0  */  public final static int K11=   (WHOLE*11);	
	/** 11.5  */  public final static int H11=   (K11+H0);	
	/** 12.0  */  public final static int K12=   (WHOLE*12);	
	/** 12.5  */  public final static int H12=   (K12+H0);	
	/** 13.0  */  public final static int K13=   (WHOLE*13);	
	/** 13.5  */  public final static int H13=   (K13+H0);	
	/** 14.0  */  public final static int K14=   (WHOLE*14);	
	/** 14.5  */  public final static int H14=   (K14+H0);	
	/** 15.0  */  public final static int K15=   (WHOLE*15);	
	/** 15.5  */  public final static int H15=   (K15+H0);	
	/** 16.0  */  public final static int K16=   (WHOLE*16);	
	/** 16.5  */  public final static int H16=   (K16+H0);	
	/** 17.0  */  public final static int K17=   (WHOLE*17);	
	/** 17.5  */  public final static int H17=   (K17+H0);	
	/** 18.0  */  public final static int K18=   (WHOLE*18);	
	/** 18.5  */  public final static int H18=   (K18+H0);	
	/** 19.0  */  public final static int K19=   (WHOLE*19);	
	/** 19.5  */  public final static int H19=   (K19+H0);	
	/** 20.0  */  public final static int K20=   (WHOLE*20);	
	/** 20.5  */  public final static int H20=   (K20+H0);	
	/** 21.0  */  public final static int K21=   (WHOLE*21);	
	/** 22.0  */  public final static int K22=   (WHOLE*22);	
	/** 22.5  */  public final static int H22=   (K22+H0);	
	/** 23.0  */  public final static int K23=   (WHOLE*23);	
	/** 23.5  */  public final static int H23=   (K23+H0);	
	/** 24.0  */  public final static int K24=   (WHOLE*24);	
	/** 25.0  */  public final static int K25=   (WHOLE*25);	
	/** 26.0  */  public final static int K26=   (WHOLE*26);	
	/** 27.0  */  public final static int K27=   (WHOLE*27);	
	/** 27.5  */  public final static int H27=   (K27+H0);	
	/** 28.0  */  public final static int K28=   (WHOLE*28);	
	/** 29.0  */  public final static int K29=   (WHOLE*29);	
	/** 30.0  */  public final static int K30=   (WHOLE*30);	
	/** 31.0  */  public final static int K31=   (WHOLE*31);	
	/** 32.0  */  public final static int K32=   (WHOLE*32);	
	/** 35.0  */  public final static int K35=   (WHOLE*35);	
	/** 37.0  */  public final static int K37=   (WHOLE*37);	
	/** 38.0  */  public final static int K38=   (WHOLE*38);	
	/** 39.0  */  public final static int K39=   (WHOLE*39);	
	/** 44.0  */  public final static int K44=   (WHOLE*44);	
	/** 55.0  */  public final static int K55=   (WHOLE*55);	
	/**135.0  */  public final static int K135=  (WHOLE*135);	

	/** in the center           */  public final static int[] CENTER = {0, 0};

	/** right of center by 0.5  */  public final static int[] CENTERR0H   ={0, H0};	
	/** right of center by 1.0  */  public final static int[] CENTERR1    ={0, K1};	
	/** right of center by 1.5  */  public final static int[] CENTERR1H   ={0, H1};	
	/** right of center by 2.0  */  public final static int[] CENTERR2    ={0, K2};	
	/** right of center by 2.5  */  public final static int[] CENTERR2H   ={0, H2};	
	/** right of center by 3.0  */  public final static int[] CENTERR3    ={0, K3};	
	/** right of center by 3.5  */  public final static int[] CENTERR3H   ={0, H3};	
	/** right of center by 4.0  */  public final static int[] CENTERR4    ={0, K4};	
	/** right of center by 4.5  */  public final static int[] CENTERR4H   ={0, H4};	
	/** right of center by 5.0  */  public final static int[] CENTERR5    ={0, K5};	
	/** right of center by 5.5  */  public final static int[] CENTERR5H   ={0, H5};	

	/** up from center by 0.5  */  public final static int[] CENTERU0H   ={0, H0};	
	/** up from center by 1.0  */  public final static int[] CENTERU1    ={0, K1};	
	/** up from center by 1.5  */  public final static int[] CENTERU1H   ={0, H1};	
	/** up from center by 2.0  */  public final static int[] CENTERU2    ={0, K2};	
	/** up from center by 2.5  */  public final static int[] CENTERU2H   ={0, H2};	
	/** up from center by 3.0  */  public final static int[] CENTERU3    ={0, K3};	
	/** up from center by 3.5  */  public final static int[] CENTERU3H   ={0, H3};	
	/** up from center by 4.0  */  public final static int[] CENTERU4    ={0, K4};	
	/** up from center by 4.5  */  public final static int[] CENTERU4H   ={0, H4};	
	/** up from center by 5.0  */  public final static int[] CENTERU5    ={0, K5};	
	/** up from center by 5.5  */  public final static int[] CENTERU5H   ={0, H5};	

	/** left of center by 0.5  */  public final static int[] CENTERL0H   ={0,-H0};	
	/** left of center by 1.0  */  public final static int[] CENTERL1    ={0,-K1};	
	/** left of center by 1.5  */  public final static int[] CENTERL1H   ={0,-H1};	
	/** left of center by 2.0  */  public final static int[] CENTERL2    ={0,-K2};	
	/** left of center by 2.5  */  public final static int[] CENTERL2H   ={0,-H2};	
	/** left of center by 3.0  */  public final static int[] CENTERL3    ={0,-K3};	
	/** left of center by 3.5  */  public final static int[] CENTERL3H   ={0,-H3};	
	/** left of center by 4.0  */  public final static int[] CENTERL4    ={0,-K4};	
	/** left of center by 4.5  */  public final static int[] CENTERL4H   ={0,-H4};	
	/** left of center by 5.0  */  public final static int[] CENTERL5    ={0,-K5};	
	/** left of center by 5.5  */  public final static int[] CENTERL5H   ={0,-H5};	

	/** down from center by 0.5  */  public final static int[] CENTERD0H   ={0,-H0};	
	/** down from center by 1.0  */  public final static int[] CENTERD1    ={0,-K1};	
	/** down from center by 1.5  */  public final static int[] CENTERD1H   ={0,-H1};	
	/** down from center by 2.0  */  public final static int[] CENTERD2    ={0,-K2};	
	/** down from center by 2.5  */  public final static int[] CENTERD2H   ={0,-H2};	
	/** down from center by 3.0  */  public final static int[] CENTERD3    ={0,-K3};	
	/** down from center by 3.5  */  public final static int[] CENTERD3H   ={0,-H3};	
	/** down from center by 4.0  */  public final static int[] CENTERD4    ={0,-K4};	
	/** down from center by 4.5  */  public final static int[] CENTERD4H   ={0,-H4};	
	/** down from center by 5.0  */  public final static int[] CENTERD5    ={0,-K5};	
	/** down from center by 5.5  */  public final static int[] CENTERD5H   ={0,-H5};	

	/** at left edge               */  public final static int[] LEFTEDGE  ={-H0,  0};	
	/** in from left edge by  0.25 */  public final static int[] LEFTIN0Q  ={-H0, Q0};	
	/** in from left edge by  0.5  */  public final static int[] LEFTIN0H  ={-H0, H0};	
	/** in from left edge by  0.75 */  public final static int[] LEFTIN0T  ={-H0, T0};	
	/** in from left edge by  1.0  */  public final static int[] LEFTIN1   ={-H0, K1};	
	/** in from left edge by  1.25 */  public final static int[] LEFTIN1Q  ={-H0, Q1};	
	/** in from left edge by  1.5  */  public final static int[] LEFTIN1H  ={-H0, H1};	
	/** in from left edge by  1.75 */  public final static int[] LEFTIN1T  ={-H0, T1};	
	/** in from left edge by  2.0  */  public final static int[] LEFTIN2   ={-H0, K2};	
	/** in from left edge by  2.25 */  public final static int[] LEFTIN2Q  ={-H0, Q2};	
	/** in from left edge by  2.5  */  public final static int[] LEFTIN2H  ={-H0, H2};	
	/** in from left edge by  2.75 */  public final static int[] LEFTIN2T  ={-H0, T2};	
	/** in from left edge by  3.0  */  public final static int[] LEFTIN3   ={-H0, K3};	
	/** in from left edge by  3.25 */  public final static int[] LEFTIN3Q  ={-H0, Q3};	
	/** in from left edge by  3.5  */  public final static int[] LEFTIN3H  ={-H0, H3};	
	/** in from left edge by  3.75 */  public final static int[] LEFTIN3T  ={-H0, T3};	
	/** in from left edge by  4.0  */  public final static int[] LEFTIN4   ={-H0, K4};	
	/** in from left edge by  4.25 */  public final static int[] LEFTIN4Q  ={-H0, Q4};	
	/** in from left edge by  4.5  */  public final static int[] LEFTIN4H  ={-H0, H4};	
	/** in from left edge by  4.75 */  public final static int[] LEFTIN4T  ={-H0, T4};	
	/** in from left edge by  5.0  */  public final static int[] LEFTIN5   ={-H0, K5};	
	/** in from left edge by  5.25 */  public final static int[] LEFTIN5Q  ={-H0, Q5};	
	/** in from left edge by  5.5  */  public final static int[] LEFTIN5H  ={-H0, H5};	
	/** in from left edge by  5.75 */  public final static int[] LEFTIN5T  ={-H0, T5};	
	/** in from left edge by  6.0  */  public final static int[] LEFTIN6   ={-H0, K6};	
	/** in from left edge by  6.25 */  public final static int[] LEFTIN6Q  ={-H0, Q6};	
	/** in from left edge by  6.5  */  public final static int[] LEFTIN6H  ={-H0, H6};	
	/** in from left edge by  6.75 */  public final static int[] LEFTIN6T  ={-H0, T6};	
	/** in from left edge by  7.0  */  public final static int[] LEFTIN7   ={-H0, K7};	
	/** in from left edge by  7.25 */  public final static int[] LEFTIN7Q  ={-H0, Q7};	
	/** in from left edge by  7.5  */  public final static int[] LEFTIN7H  ={-H0, H7};	
	/** in from left edge by  7.75 */  public final static int[] LEFTIN7T  ={-H0, T7};	
	/** in from left edge by  8.0  */  public final static int[] LEFTIN8   ={-H0, K8};	
	/** in from left edge by  8.25 */  public final static int[] LEFTIN8Q  ={-H0, Q8};	
	/** in from left edge by  8.5  */  public final static int[] LEFTIN8H  ={-H0, H8};	
	/** in from left edge by  8.75 */  public final static int[] LEFTIN8T  ={-H0, T8};	
	/** in from left edge by  9.0  */  public final static int[] LEFTIN9   ={-H0, K9};	
	/** in from left edge by  9.25 */  public final static int[] LEFTIN9Q  ={-H0, Q9};	
	/** in from left edge by  9.5  */  public final static int[] LEFTIN9H  ={-H0, H9};	
	/** in from left edge by  9.75 */  public final static int[] LEFTIN9T  ={-H0, T9};	
	/** in from left edge by 10.0  */  public final static int[] LEFTIN10  ={-H0, K10};	
	/** in from left edge by 10.5  */  public final static int[] LEFTIN10H ={-H0, H10};	
	/** in from left edge by 11.0  */  public final static int[] LEFTIN11  ={-H0, K11};	
	/** in from left edge by 11.5  */  public final static int[] LEFTIN11H ={-H0, H11};	
	/** in from left edge by 12.0  */  public final static int[] LEFTIN12  ={-H0, K12};	
	/** in from left edge by 12.5  */  public final static int[] LEFTIN12H ={-H0, H12};	
	/** in from left edge by 13.0  */  public final static int[] LEFTIN13  ={-H0, K13};	
	/** in from left edge by 13.5  */  public final static int[] LEFTIN13H ={-H0, H13};	
	/** in from left edge by 14.0  */  public final static int[] LEFTIN14  ={-H0, K14};	
	/** in from left edge by 14.5  */  public final static int[] LEFTIN14H ={-H0, H14};	
	/** in from left edge by 15.0  */  public final static int[] LEFTIN15  ={-H0, K15};	
	/** in from left edge by 15.5  */  public final static int[] LEFTIN15H ={-H0, H15};	
	/** in from left edge by 16.0  */  public final static int[] LEFTIN16  ={-H0, K16};	
	/** in from left edge by 16.5  */  public final static int[] LEFTIN16H ={-H0, H16};	
	/** in from left edge by 17.0  */  public final static int[] LEFTIN17  ={-H0, K17};	
	/** in from left edge by 17.5  */  public final static int[] LEFTIN17H ={-H0, H17};	
	/** in from left edge by 18.0  */  public final static int[] LEFTIN18  ={-H0, K18};	
	/** in from left edge by 18.5  */  public final static int[] LEFTIN18H ={-H0, H18};	
	/** in from left edge by 19.0  */  public final static int[] LEFTIN19  ={-H0, K19};	
	/** in from left edge by 19.5  */  public final static int[] LEFTIN19H ={-H0, H19};	
	/** in from left edge by 27.5  */  public final static int[] LEFTIN27H ={-H0, H27};	

	/** at bottom edge               */  public final static int[] BOTEDGE   ={-H0,  0};	
	/** up from bottom edge by  0.25 */  public final static int[] BOTIN0Q   ={-H0, Q0};	
	/** up from bottom edge by  0.5  */  public final static int[] BOTIN0H   ={-H0, H0};	
	/** up from bottom edge by  0.75 */  public final static int[] BOTIN0T   ={-H0, T0};	
	/** up from bottom edge by  1.0  */  public final static int[] BOTIN1    ={-H0, K1};	
	/** up from bottom edge by  1.25 */  public final static int[] BOTIN1Q   ={-H0, Q1};	
	/** up from bottom edge by  1.5  */  public final static int[] BOTIN1H   ={-H0, H1};	
	/** up from bottom edge by  1.75 */  public final static int[] BOTIN1T   ={-H0, T1};	
	/** up from bottom edge by  2.0  */  public final static int[] BOTIN2    ={-H0, K2};	
	/** up from bottom edge by  2.25 */  public final static int[] BOTIN2Q   ={-H0, Q2};	
	/** up from bottom edge by  2.5  */  public final static int[] BOTIN2H   ={-H0, H2};	
	/** up from bottom edge by  2.75 */  public final static int[] BOTIN2T   ={-H0, T2};	
	/** up from bottom edge by  3.0  */  public final static int[] BOTIN3    ={-H0, K3};	
	/** up from bottom edge by  3.25 */  public final static int[] BOTIN3Q   ={-H0, Q3};	
	/** up from bottom edge by  3.5  */  public final static int[] BOTIN3H   ={-H0, H3};	
	/** up from bottom edge by  3.75 */  public final static int[] BOTIN3T   ={-H0, T3};	
	/** up from bottom edge by  4.0  */  public final static int[] BOTIN4    ={-H0, K4};	
	/** up from bottom edge by  4.25 */  public final static int[] BOTIN4Q   ={-H0, Q4};	
	/** up from bottom edge by  4.5  */  public final static int[] BOTIN4H   ={-H0, H4};	
	/** up from bottom edge by  4.75 */  public final static int[] BOTIN4T   ={-H0, T4};	
	/** up from bottom edge by  5.0  */  public final static int[] BOTIN5    ={-H0, K5};	
	/** up from bottom edge by  5.25 */  public final static int[] BOTIN5Q   ={-H0, Q5};	
	/** up from bottom edge by  5.5  */  public final static int[] BOTIN5H   ={-H0, H5};	
	/** up from bottom edge by  5.75 */  public final static int[] BOTIN5T   ={-H0, T5};	
	/** up from bottom edge by  6.0  */  public final static int[] BOTIN6    ={-H0, K6};	
	/** up from bottom edge by  6.25 */  public final static int[] BOTIN6Q   ={-H0, Q6};	
	/** up from bottom edge by  6.5  */  public final static int[] BOTIN6H   ={-H0, H6};	
	/** up from bottom edge by  6.75 */  public final static int[] BOTIN6T   ={-H0, T6};	
	/** up from bottom edge by  7.0  */  public final static int[] BOTIN7    ={-H0, K7};	
	/** up from bottom edge by  7.25 */  public final static int[] BOTIN7Q   ={-H0, Q7};	
	/** up from bottom edge by  7.5  */  public final static int[] BOTIN7H   ={-H0, H7};	
	/** up from bottom edge by  7.75 */  public final static int[] BOTIN7T   ={-H0, T7};	
	/** up from bottom edge by  8.0  */  public final static int[] BOTIN8    ={-H0, K8};	
	/** up from bottom edge by  8.25 */  public final static int[] BOTIN8Q   ={-H0, Q8};	
	/** up from bottom edge by  8.5  */  public final static int[] BOTIN8H   ={-H0, H8};	
	/** up from bottom edge by  8.75 */  public final static int[] BOTIN8T   ={-H0, T8};	
	/** up from bottom edge by  9.0  */  public final static int[] BOTIN9    ={-H0, K9};	
	/** up from bottom edge by  9.25 */  public final static int[] BOTIN9Q   ={-H0, Q9};	
	/** up from bottom edge by  9.5  */  public final static int[] BOTIN9H   ={-H0, H9};	
	/** up from bottom edge by  9.75 */  public final static int[] BOTIN9T   ={-H0, T9};	
	/** up from bottom edge by 10.0  */  public final static int[] BOTIN10   ={-H0, K10};	
	/** up from bottom edge by 10.5  */  public final static int[] BOTIN10H  ={-H0, H10};	
	/** up from bottom edge by 11.0  */  public final static int[] BOTIN11   ={-H0, K11};	
	/** up from bottom edge by 11.5  */  public final static int[] BOTIN11H  ={-H0, H11};	
	/** up from bottom edge by 12.0  */  public final static int[] BOTIN12   ={-H0, K12};	
	/** up from bottom edge by 12.5  */  public final static int[] BOTIN12H  ={-H0, H12};	
	/** up from bottom edge by 13.0  */  public final static int[] BOTIN13   ={-H0, K13};	
	/** up from bottom edge by 13.5  */  public final static int[] BOTIN13H  ={-H0, H13};	
	/** up from bottom edge by 14.0  */  public final static int[] BOTIN14   ={-H0, K14};	
	/** up from bottom edge by 14.5  */  public final static int[] BOTIN14H  ={-H0, H14};	
	/** up from bottom edge by 15.0  */  public final static int[] BOTIN15   ={-H0, K15};	
	/** up from bottom edge by 15.5  */  public final static int[] BOTIN15H  ={-H0, H15};	
	/** up from bottom edge by 16.0  */  public final static int[] BOTIN16   ={-H0, K16};	
	/** up from bottom edge by 16.5  */  public final static int[] BOTIN16H  ={-H0, H16};	
	/** up from bottom edge by 17.0  */  public final static int[] BOTIN17   ={-H0, K17};	
	/** up from bottom edge by 17.5  */  public final static int[] BOTIN17H  ={-H0, H17};	
	/** up from bottom edge by 18.0  */  public final static int[] BOTIN18   ={-H0, K18};	
	/** up from bottom edge by 18.5  */  public final static int[] BOTIN18H  ={-H0, H18};	
	/** up from bottom edge by 19.0  */  public final static int[] BOTIN19   ={-H0, K19};	
	/** up from bottom edge by 19.5  */  public final static int[] BOTIN19H  ={-H0, H19};	
	/** up from bottom edge by 27.5  */  public final static int[] BOTIN27H  ={-H0, H27};	

	/** at top edge                 */  public final static int[] TOPEDGE    ={H0,  0};	
	/** down from top edge by  0.25 */  public final static int[] TOPIN0Q    ={H0,-Q0};	
	/** down from top edge by  0.5  */  public final static int[] TOPIN0H    ={H0,-H0};	
	/** down from top edge by  0.75 */  public final static int[] TOPIN0T    ={H0,-T0};	
	/** down from top edge by  1.0  */  public final static int[] TOPIN1     ={H0,-K1};	
	/** down from top edge by  1.25 */  public final static int[] TOPIN1Q    ={H0,-Q1};	
	/** down from top edge by  1.5  */  public final static int[] TOPIN1H    ={H0,-H1};	
	/** down from top edge by  1.75 */  public final static int[] TOPIN1T    ={H0,-T1};	
	/** down from top edge by  2.0  */  public final static int[] TOPIN2     ={H0,-K2};	
	/** down from top edge by  2.25 */  public final static int[] TOPIN2Q    ={H0,-Q2};	
	/** down from top edge by  2.5  */  public final static int[] TOPIN2H    ={H0,-H2};	
	/** down from top edge by  2.75 */  public final static int[] TOPIN2T    ={H0,-T2};	
	/** down from top edge by  3.0  */  public final static int[] TOPIN3     ={H0,-K3};	
	/** down from top edge by  3.25 */  public final static int[] TOPIN3Q    ={H0,-Q3};	
	/** down from top edge by  3.5  */  public final static int[] TOPIN3H    ={H0,-H3};	
	/** down from top edge by  3.75 */  public final static int[] TOPIN3T    ={H0,-T3};	
	/** down from top edge by  4.0  */  public final static int[] TOPIN4     ={H0,-K4};	
	/** down from top edge by  4.25 */  public final static int[] TOPIN4Q    ={H0,-Q4};	
	/** down from top edge by  4.5  */  public final static int[] TOPIN4H    ={H0,-H4};	
	/** down from top edge by  4.75 */  public final static int[] TOPIN4T    ={H0,-T4};	
	/** down from top edge by  5.0  */  public final static int[] TOPIN5     ={H0,-K5};	
	/** down from top edge by  5.25 */  public final static int[] TOPIN5Q    ={H0,-Q5};	
	/** down from top edge by  5.5  */  public final static int[] TOPIN5H    ={H0,-H5};	
	/** down from top edge by  5.75 */  public final static int[] TOPIN5T    ={H0,-T5};	
	/** down from top edge by  6.0  */  public final static int[] TOPIN6     ={H0,-K6};	
	/** down from top edge by  6.25 */  public final static int[] TOPIN6Q    ={H0,-Q6};	
	/** down from top edge by  6.5  */  public final static int[] TOPIN6H    ={H0,-H6};	
	/** down from top edge by  6.75 */  public final static int[] TOPIN6T    ={H0,-T6};	
	/** down from top edge by  7.0  */  public final static int[] TOPIN7     ={H0,-K7};	
	/** down from top edge by  7.25 */  public final static int[] TOPIN7Q    ={H0,-Q7};	
	/** down from top edge by  7.5  */  public final static int[] TOPIN7H    ={H0,-H7};	
	/** down from top edge by  7.75 */  public final static int[] TOPIN7T    ={H0,-T7};	
	/** down from top edge by  8.0  */  public final static int[] TOPIN8     ={H0,-K8};	
	/** down from top edge by  8.25 */  public final static int[] TOPIN8Q    ={H0,-Q8};	
	/** down from top edge by  8.5  */  public final static int[] TOPIN8H    ={H0,-H8};	
	/** down from top edge by  8.75 */  public final static int[] TOPIN8T    ={H0,-T8};	
	/** down from top edge by  9.0  */  public final static int[] TOPIN9     ={H0,-K9};	
	/** down from top edge by  9.25 */  public final static int[] TOPIN9Q    ={H0,-Q9};	
	/** down from top edge by  9.5  */  public final static int[] TOPIN9H    ={H0,-H9};	
	/** down from top edge by  9.75 */  public final static int[] TOPIN9T    ={H0,-T9};	
	/** down from top edge by 10.0  */  public final static int[] TOPIN10    ={H0,-K10};	
	/** down from top edge by 10.5  */  public final static int[] TOPIN10H   ={H0,-H10};	
	/** down from top edge by 11.0  */  public final static int[] TOPIN11    ={H0,-K11};	
	/** down from top edge by 11.5  */  public final static int[] TOPIN11H   ={H0,-H11};	
	/** down from top edge by 12.0  */  public final static int[] TOPIN12    ={H0,-K12};	
	/** down from top edge by 12.5  */  public final static int[] TOPIN12H   ={H0,-H12};	
	/** down from top edge by 13.0  */  public final static int[] TOPIN13    ={H0,-K13};	
	/** down from top edge by 13.5  */  public final static int[] TOPIN13H   ={H0,-H13};	
	/** down from top edge by 14.0  */  public final static int[] TOPIN14    ={H0,-K14};	
	/** down from top edge by 14.5  */  public final static int[] TOPIN14H   ={H0,-H14};	
	/** down from top edge by 15.0  */  public final static int[] TOPIN15    ={H0,-K15};	
	/** down from top edge by 15.5  */  public final static int[] TOPIN15H   ={H0,-H15};	
	/** down from top edge by 16.0  */  public final static int[] TOPIN16    ={H0,-K16};	
	/** down from top edge by 16.5  */  public final static int[] TOPIN16H   ={H0,-H16};	
	/** down from top edge by 17.0  */  public final static int[] TOPIN17    ={H0,-K17};	
	/** down from top edge by 17.5  */  public final static int[] TOPIN17H   ={H0,-H17};	
	/** down from top edge by 18.0  */  public final static int[] TOPIN18    ={H0,-K18};	
	/** down from top edge by 18.5  */  public final static int[] TOPIN18H   ={H0,-H18};	
	/** down from top edge by 19.0  */  public final static int[] TOPIN19    ={H0,-K19};	
	/** down from top edge by 19.5  */  public final static int[] TOPIN19H   ={H0,-H19};	
	/** down from top edge by 27.5  */  public final static int[] TOPIN27H   ={H0,-H27};	

	/** in from right edge by  0.0  */  public final static int[] RIGHTEDGE  ={H0,  0};	
	/** in from right edge by  0.25 */  public final static int[] RIGHTIN0Q  ={H0,-Q0};	
	/** in from right edge by  0.5  */  public final static int[] RIGHTIN0H  ={H0,-H0};	
	/** in from right edge by  0.75 */  public final static int[] RIGHTIN0T  ={H0,-T0};	
	/** in from right edge by  1.0  */  public final static int[] RIGHTIN1   ={H0,-K1};	
	/** in from right edge by  1.25 */  public final static int[] RIGHTIN1Q  ={H0,-Q1};	
	/** in from right edge by  1.5  */  public final static int[] RIGHTIN1H  ={H0,-H1};	
	/** in from right edge by  1.75 */  public final static int[] RIGHTIN1T  ={H0,-T1};	
	/** in from right edge by  2.0  */  public final static int[] RIGHTIN2   ={H0,-K2};	
	/** in from right edge by  2.25 */  public final static int[] RIGHTIN2Q  ={H0,-Q2};	
	/** in from right edge by  2.5  */  public final static int[] RIGHTIN2H  ={H0,-H2};	
	/** in from right edge by  2.75 */  public final static int[] RIGHTIN2T  ={H0,-T2};	
	/** in from right edge by  3.0  */  public final static int[] RIGHTIN3   ={H0,-K3};	
	/** in from right edge by  3.25 */  public final static int[] RIGHTIN3Q  ={H0,-Q3};	
	/** in from right edge by  3.5  */  public final static int[] RIGHTIN3H  ={H0,-H3};	
	/** in from right edge by  3.75 */  public final static int[] RIGHTIN3T  ={H0,-T3};	
	/** in from right edge by  4.0  */  public final static int[] RIGHTIN4   ={H0,-K4};	
	/** in from right edge by  4.25 */  public final static int[] RIGHTIN4Q  ={H0,-Q4};	
	/** in from right edge by  4.5  */  public final static int[] RIGHTIN4H  ={H0,-H4};	
	/** in from right edge by  4.75 */  public final static int[] RIGHTIN4T  ={H0,-T4};	
	/** in from right edge by  5.0  */  public final static int[] RIGHTIN5   ={H0,-K5};	
	/** in from right edge by  5.25 */  public final static int[] RIGHTIN5Q  ={H0,-Q5};	
	/** in from right edge by  5.5  */  public final static int[] RIGHTIN5H  ={H0,-H5};	
	/** in from right edge by  5.75 */  public final static int[] RIGHTIN5T  ={H0,-T5};	
	/** in from right edge by  6.0  */  public final static int[] RIGHTIN6   ={H0,-K6};	
	/** in from right edge by  6.25 */  public final static int[] RIGHTIN6Q  ={H0,-Q6};	
	/** in from right edge by  6.5  */  public final static int[] RIGHTIN6H  ={H0,-H6};	
	/** in from right edge by  6.75 */  public final static int[] RIGHTIN6T  ={H0,-T6};	
	/** in from right edge by  7.0  */  public final static int[] RIGHTIN7   ={H0,-K7};	
	/** in from right edge by  7.25 */  public final static int[] RIGHTIN7Q  ={H0,-Q7};	
	/** in from right edge by  7.5  */  public final static int[] RIGHTIN7H  ={H0,-H7};	
	/** in from right edge by  7.75 */  public final static int[] RIGHTIN7T  ={H0,-T7};	
	/** in from right edge by  8.0  */  public final static int[] RIGHTIN8   ={H0,-K8};	
	/** in from right edge by  8.25 */  public final static int[] RIGHTIN8Q  ={H0,-Q8};	
	/** in from right edge by  8.5  */  public final static int[] RIGHTIN8H  ={H0,-H8};	
	/** in from right edge by  8.75 */  public final static int[] RIGHTIN8T  ={H0,-T8};	
	/** in from right edge by  9.0  */  public final static int[] RIGHTIN9   ={H0,-K9};	
	/** in from right edge by  9.25 */  public final static int[] RIGHTIN9Q  ={H0,-Q9};	
	/** in from right edge by  9.5  */  public final static int[] RIGHTIN9H  ={H0,-H9};	
	/** in from right edge by  9.75 */  public final static int[] RIGHTIN9T  ={H0,-T9};	
	/** in from right edge by 10.0  */  public final static int[] RIGHTIN10  ={H0,-K10};	
	/** in from right edge by 10.5  */  public final static int[] RIGHTIN10H ={H0,-H10};	
	/** in from right edge by 11.0  */  public final static int[] RIGHTIN11  ={H0,-K11};	
	/** in from right edge by 11.5  */  public final static int[] RIGHTIN11H ={H0,-H11};	
	/** in from right edge by 12.0  */  public final static int[] RIGHTIN12  ={H0,-K12};	
	/** in from right edge by 12.5  */  public final static int[] RIGHTIN12H ={H0,-H12};	
	/** in from right edge by 13.0  */  public final static int[] RIGHTIN13  ={H0,-K13};	
	/** in from right edge by 13.5  */  public final static int[] RIGHTIN13H ={H0,-H13};	
	/** in from right edge by 14.0  */  public final static int[] RIGHTIN14  ={H0,-K14};	
	/** in from right edge by 14.5  */  public final static int[] RIGHTIN14H ={H0,-H14};	
	/** in from right edge by 15.0  */  public final static int[] RIGHTIN15  ={H0,-K15};	
	/** in from right edge by 15.5  */  public final static int[] RIGHTIN15H ={H0,-H15};	
	/** in from right edge by 16.0  */  public final static int[] RIGHTIN16  ={H0,-K16};	
	/** in from right edge by 16.5  */  public final static int[] RIGHTIN16H ={H0,-H16};	
	/** in from right edge by 17.0  */  public final static int[] RIGHTIN17  ={H0,-K17};	
	/** in from right edge by 17.5  */  public final static int[] RIGHTIN17H ={H0,-H17};	
	/** in from right edge by 18.0  */  public final static int[] RIGHTIN18  ={H0,-K18};	
	/** in from right edge by 18.5  */  public final static int[] RIGHTIN18H ={H0,-H18};	
	/** in from right edge by 19.0  */  public final static int[] RIGHTIN19  ={H0,-K19};	
	/** in from right edge by 19.5  */  public final static int[] RIGHTIN19H ={H0,-H19};	
	/** in from right edge by 27.5  */  public final static int[] RIGHTIN27H ={H0,-H27};	

	protected Technology(String techName)
	{
		this.nodes = new ArrayList();
		this.arcs = new ArrayList();
		this.techName = techName;

		// add the technology to the global list
		technologies.add(this);
	}

	protected static boolean validTechnology(String techName)
	{
		if (Technology.findTechnology(techName) != null)
		{
			System.out.println("ERROR: Multiple technologies named " + techName);
			return false;
		}
		return true;
	}

	void addNodeProto(PrimitiveNode pn)
	{
		nodes.add(pn);
	}

//	void removeNodeProto(PrimitiveNode pn)
//	{
//		nodes.remove(pn);
//	}

	void addArcProto(ArcProto ap)
	{
		arcs.add(ap);
	}

//	void removeArcProto(ArcProto ap)
//	{
//		arcs.remove(ap);
//	}

	/**
	 * Return the current Technology
	 */
	public static Technology getCurrent()
	{
		return curTech;
	}

	/**
	 * Set the current Technology
	 */
	public static void setCurrent(Technology tech)
	{
		curTech = tech;
	}

	/** 
	 * get the name (short) of this technology
	 */
	public String getTechName()
	{
		return techName;
	}

	/**
	 * get the description (long) of this technology
	 */
	public String getTechDesc()
	{
		return techDesc;
	}

	/**
	 * get the description (long) of this technology
	 */
	public void setTechDesc(String techDesc)
	{
		this.techDesc = techDesc;
	}

	/**
	 * get the default size of a lambda for this technology.
	 * typically overridden by a library
	 */
	public double getDefLambda()
	{
		return defLambda;
	}

	/**
	 * get the default size of a lambda for this technology.
	 * typically overridden by a library
	 */
	public void setDefLambda(double defLambda)
	{
		if (defLambda != 0)
			this.defLambda = defLambda;
	}

	/** Find the Technology with a particular name.
	 * @param name the name of the desired Technology
	 * @return the Technology with the same name, or null if no 
	 * Technology matches.
	 */
	public static Technology findTechnology(String name)
	{
		for (int i = 0; i < technologies.size(); i++)
		{
			Technology t = (Technology) technologies.get(i);
			if (t.techName.equalsIgnoreCase(name))
				return t;
		}
		return null;
	}

	protected void getInfo()
	{
		System.out.println(" Name: " + techName);
		System.out.println(" Description: " + techDesc);
		System.out.println(" Nodes (" + nodes.size() + ")");
		for (int i = 0; i < nodes.size(); i++)
		{
			System.out.println("     " + nodes.get(i));
		}
		System.out.println(" Arcs (" + arcs.size() + ")");
		for (int i = 0; i < arcs.size(); i++)
		{
			System.out.println("     " + arcs.get(i));
		}
		super.getInfo();
	}

	public String toString()
	{
		return "Technology " + techName + " (" + techDesc + ")";
	}

	// *************************** ArcProtos ***************************

	/**
	 * get the ArcProto with a particular name from this technology
	 */
	public ArcProto findArcProto(String name)
	{
		for (int i = 0; i < arcs.size(); i++)
		{
			ArcProto ap = (ArcProto) arcs.get(i);
			if (ap.getProtoName().equalsIgnoreCase(name))
				return ap;
		}
		return null;
	}

	/**
	 * get an iterator over all of the ArcProtos in this technology
	 */
	public Iterator getArcIterator()
	{
		return arcs.iterator();
	}

	// *************************** NodeProtos ***************************

	/**
	 * get the PrimitiveNode with a particular name from this technology
	 */
	public PrimitiveNode findNodeProto(String name)
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			PrimitiveNode pn = (PrimitiveNode) nodes.get(i);
			if (pn.getProtoName().equalsIgnoreCase(name))
				return pn;
		}
		return null;
	}

	/**
	 * get the PrimitiveNode of a particular type that connects to the
	 * complete set of wires given
	 */
//	public PrimitiveNode findNodeProtoConnectingTo(int type, ArcProto[] arcs)
//	{
//		for (int i = 0; i < nodes.size(); i++)
//		{
//			PrimitiveNode pn = (PrimitiveNode) nodes.get(i);
//			if (pn.getFunction() == type)
//			{
//				boolean found = true;
//				for (int j = 0; j < arcs.length; j++)
//				{
//					if (pn.connectsTo(arcs[j]) == null)
//					{
//						found = false;
//						break;
//					}
//				}
//				if (found)
//					return pn;
//			}
//		}
//		return null;
//	}

	/**
	 * get an iterator over all of the PrimitiveNodes in this technology
	 */
	public Iterator getNodeIterator()
	{
		return nodes.iterator();
	}

}
