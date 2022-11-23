package com.meerkat.test;

import static org.junit.Assert.assertEquals;

import com.meerkat.TSAGeoMag;

import org.junit.Test;

import java.util.Date;

public class TSAGeoMagTest {

    /**
     * Test method for {@link TSAGeoMag#getDeclination(double, double, Date, int)}
     */
    @Test public final void getDeclination()
    {
        assertEquals(25.96, TSAGeoMag.getDeclination( -45, 175, new Date(2022-1900, 10, 1), 500) , 5.0E-1);

        assertEquals(-112.06, TSAGeoMag.getDeclination( 89, -121, new Date(2020-1900, 0, 1), 28000) , 5.0E-1);
        assertEquals( -38.65, TSAGeoMag.getDeclination( 80,  -96, new Date(2020-1900, 0, 1), 8000) , 5.0E-1);
        assertEquals(  54.13, TSAGeoMag.getDeclination( 82,   87, new Date(2020-1900, 0, 1), 4000) , 5.0E-1);
        assertEquals(   0.71, TSAGeoMag.getDeclination( 43,   93, new Date(2020-1900, 0, 1), 5000) , 5.0E-1);
        assertEquals(  -5.78, TSAGeoMag.getDeclination(-33,  109, new Date(2020-1900, 0, 1), 1000) , 5.0E-1);
        assertEquals( -15.79, TSAGeoMag.getDeclination(-59,   -8, new Date(2020-1900, 0, 1), 9000) , 5.0E-1);
        assertEquals(  28.10, TSAGeoMag.getDeclination(-50, -103, new Date(2020-1900, 0, 1),  3000) , 5.0E-1);
        assertEquals(  15.82, TSAGeoMag.getDeclination(-29, -110, new Date(2020-1900, 0, 1), 4000) , 5.0E-1);
        assertEquals(   0.12, TSAGeoMag.getDeclination( 14,  143, new Date(2020-1900, 0, 1), 6000) , 5.0E-1);
        assertEquals(   1.05, TSAGeoMag.getDeclination(  0,   21, new Date(2020-1900, 0, 1), 18000) , 5.0E-1);

        assertEquals( 20.16, TSAGeoMag.getDeclination(-36, -137, new Date(2020-1900, 6, 1),  6000) , 5.0E-1);
        assertEquals(  0.43, TSAGeoMag.getDeclination( 26,   81, new Date(2020-1900, 6, 1), 3000) , 5.0E-1);
        assertEquals( 13.39, TSAGeoMag.getDeclination( 38, -144, new Date(2020-1900, 6, 1), 9000) , 5.0E-1);
        assertEquals( 57.40, TSAGeoMag.getDeclination(-70, -133, new Date(2020-1900, 6, 1), 5000) , 5.0E-1);
        assertEquals( 15.39, TSAGeoMag.getDeclination(-52,  -75, new Date(2020-1900, 6, 1),  8000) , 5.0E-1);
        assertEquals(-32.56, TSAGeoMag.getDeclination(-66,   17, new Date(2020-1900, 6, 1),  8000) , 5.0E-1);
        assertEquals(  9.15, TSAGeoMag.getDeclination(-37,  140, new Date(2020-1900, 6, 1), 22000) , 5.0E-1);
        assertEquals( 10.83, TSAGeoMag.getDeclination(-12, -129, new Date(2020-1900, 6, 1), 40000) , 5.0E-1);
        assertEquals( 11.46, TSAGeoMag.getDeclination( 33, -118, new Date(2020-1900, 6, 1), 4000) , 5.0E-1);
        assertEquals( 28.65, TSAGeoMag.getDeclination(-81,  -67, new Date(2020-1900, 6, 1), 5000) , 5.0E-1);

        assertEquals(-22.29, TSAGeoMag.getDeclination(-57,    3, new Date(2021-1900, 0, 1), 4000) , 5.0E-1);
        assertEquals( 14.02, TSAGeoMag.getDeclination(-24, -122, new Date(2021-1900, 0, 1), 6000) , 5.0E-1);
        assertEquals(  1.08, TSAGeoMag.getDeclination( 23,   63, new Date(2021-1900, 0, 1), 9000) , 5.0E-1);
        assertEquals(  9.74, TSAGeoMag.getDeclination( -3, -147, new Date(2021-1900, 0, 1), 33000) , 5.0E-1);
        assertEquals( -6.05, TSAGeoMag.getDeclination(-72,  -22, new Date(2021-1900, 0, 1), 7000) , 5.0E-1);
        assertEquals( -1.71, TSAGeoMag.getDeclination(-14,   99, new Date(2021-1900, 0, 1), 2000) , 5.0E-1);
        assertEquals(-36.71, TSAGeoMag.getDeclination( 86,  -46, new Date(2021-1900, 0, 1), 3000) , 5.0E-1);
        assertEquals(-81.32, TSAGeoMag.getDeclination(-64,   87, new Date(2021-1900, 0, 1), 2000) , 5.0E-1);
        assertEquals(-14.32, TSAGeoMag.getDeclination(-19,   43, new Date(2021-1900, 0, 1), 34000) , 5.0E-1);
        assertEquals(-59.03, TSAGeoMag.getDeclination(-81,   40, new Date(2021-1900, 0, 1), 6000) , 5.0E-1);

        assertEquals(  -3.41, TSAGeoMag.getDeclination(  0,   80, new Date(2021-1900, 6, 1), 14000) , 5.0E-1);
        assertEquals(  30.36, TSAGeoMag.getDeclination(-82,  -68, new Date(2021-1900, 6, 1), 12000) , 5.0E-1);
        assertEquals( -11.54, TSAGeoMag.getDeclination(-46,  -42, new Date(2021-1900, 6, 1), 4000) , 5.0E-1);
        assertEquals(   1.23, TSAGeoMag.getDeclination( 17,   52, new Date(2021-1900, 6, 1), 3000) , 5.0E-1);
        assertEquals(  -1.71, TSAGeoMag.getDeclination( 10,   78, new Date(2021-1900, 6, 1), 4000) , 5.0E-1);
        assertEquals(  12.37, TSAGeoMag.getDeclination( 33, -145, new Date(2021-1900, 6, 1), 12000) , 5.0E-1);
        assertEquals(-136.34, TSAGeoMag.getDeclination(-79,  115, new Date(2021-1900, 6, 1), 12000) , 5.0E-1);
        assertEquals(  18.10, TSAGeoMag.getDeclination(-33, -114, new Date(2021-1900, 6, 1), 14000) , 5.0E-1);
        assertEquals(   2.13, TSAGeoMag.getDeclination( 29,   66, new Date(2021-1900, 6, 1), 19000) , 5.0E-1);
        assertEquals(  10.11, TSAGeoMag.getDeclination(-11,  167, new Date(2021-1900, 6, 1), 6000) , 5.0E-1);

        assertEquals(-16.99, TSAGeoMag.getDeclination(-66,   -5, new Date(2022-1900, 0, 1), 37000) , 5.0E-1);
        assertEquals( 15.47, TSAGeoMag.getDeclination( 72, -115, new Date(2022-1900, 0, 1), 7000) , 5.0E-1);
        assertEquals(  6.56, TSAGeoMag.getDeclination( 22,  174, new Date(2022-1900, 0, 1), 4000) , 5.0E-1);
        assertEquals(  1.43, TSAGeoMag.getDeclination( 54,  178, new Date(2022-1900, 0, 1), 4000) , 5.0E-1);
        assertEquals(-48.06, TSAGeoMag.getDeclination(-43,   50, new Date(2022-1900, 0, 1), 7000) , 5.0E-1);
        assertEquals( 24.32, TSAGeoMag.getDeclination(-43, -111, new Date(2022-1900, 0, 1), 4000) , 5.0E-1);
        assertEquals( 57.08, TSAGeoMag.getDeclination(-63,  178, new Date(2022-1900, 0, 1), 12000) , 5.0E-1);
        assertEquals(  8.76, TSAGeoMag.getDeclination( 27, -169, new Date(2022-1900, 0, 1), 38000) , 5.0E-1);
        assertEquals(-17.63, TSAGeoMag.getDeclination( 59,  -77, new Date(2022-1900, 0, 1), 1000) , 5.0E-1);
        assertEquals(-14.09, TSAGeoMag.getDeclination(-47,  -32, new Date(2022-1900, 0, 1), 7000) , 5.0E-1);

        assertEquals(  18.95, TSAGeoMag.getDeclination(  62,   53, new Date(2022-1900, 6, 1),  8000) , 5.0E-1);
        assertEquals( -15.94, TSAGeoMag.getDeclination( -68,   -7, new Date(2022-1900, 6, 1), 7000) , 5.0E-1);
        assertEquals(   7.79, TSAGeoMag.getDeclination(  -5,  159, new Date(2022-1900, 6, 1), 8000) , 5.0E-1);
        assertEquals(  15.68, TSAGeoMag.getDeclination( -29, -107, new Date(2022-1900, 6, 1), 34000) , 5.0E-1);
        assertEquals(   1.78, TSAGeoMag.getDeclination(  27,   65, new Date(2022-1900, 6, 1), 6000) , 5.0E-1);
        assertEquals(-101.49, TSAGeoMag.getDeclination( -72,   95, new Date(2022-1900, 6, 1), 3000) , 5.0E-1);
        assertEquals(  18.90, TSAGeoMag.getDeclination( -46,  -85, new Date(2022-1900, 6, 1), 6000) , 5.0E-1);
        assertEquals( -16.65, TSAGeoMag.getDeclination( -13,  -59, new Date(2022-1900, 6, 1),  0000) , 5.0E-1);
        assertEquals(   1.92, TSAGeoMag.getDeclination(  66, -178, new Date(2022-1900, 6, 1), 16000) , 5.0E-1);
        assertEquals( -64.66, TSAGeoMag.getDeclination( -87,   38, new Date(2022-1900, 6, 1), 2000) , 5.0E-1);

        assertEquals(   5.20, TSAGeoMag.getDeclination(  20 , 167, new Date(2023-1900, 0, 1), 9000) , 5.0E-1);
        assertEquals(  -7.26, TSAGeoMag.getDeclination(   5 , -13, new Date(2023-1900, 0, 1), 1000) , 5.0E-1);
        assertEquals(  -0.56, TSAGeoMag.getDeclination(  14 ,  65, new Date(2023-1900, 0, 1), 5000) , 5.0E-1);
        assertEquals(  42.27, TSAGeoMag.getDeclination( -85 , -79, new Date(2023-1900, 0, 1), 6000) , 5.0E-1);
        assertEquals(  -3.87, TSAGeoMag.getDeclination( -36 , -64, new Date(2023-1900, 0, 1), 30000) , 5.0E-1);
        assertEquals( -15.61, TSAGeoMag.getDeclination(  79 , 125, new Date(2023-1900, 0, 1), 5000) , 5.0E-1);
        assertEquals( -15.22, TSAGeoMag.getDeclination(   6 , -32, new Date(2023-1900, 0, 1), 21000) , 5.0E-1);
        assertEquals(  30.36, TSAGeoMag.getDeclination( -76 , -75, new Date(2023-1900, 0, 1),  1000) , 5.0E-1);
        assertEquals( -11.94, TSAGeoMag.getDeclination( -46 , -41, new Date(2023-1900, 0, 1), 5000) , 5.0E-1);
        assertEquals( -24.12, TSAGeoMag.getDeclination( -22 , -21, new Date(2023-1900, 0, 1), 11000) , 5.0E-1);

        assertEquals(  16.20, TSAGeoMag.getDeclination(  54 , -120, new Date(2023-1900, 6, 1), 28000) , 5.0E-1);
        assertEquals(  40.48, TSAGeoMag.getDeclination( -58 ,  156, new Date(2023-1900, 6, 1), 8000) , 5.0E-1);
        assertEquals(  29.86, TSAGeoMag.getDeclination( -65 ,  -88, new Date(2023-1900, 6, 1), 39000) , 5.0E-1);
        assertEquals( -13.98, TSAGeoMag.getDeclination( -23 ,   81, new Date(2023-1900, 6, 1), 27000) , 5.0E-1);
        assertEquals(   1.08, TSAGeoMag.getDeclination(  34 ,    0, new Date(2023-1900, 6, 1), 11000) , 5.0E-1);
        assertEquals( -66.98, TSAGeoMag.getDeclination( -62 ,   65, new Date(2023-1900, 6, 1), 2000) , 5.0E-1);
        assertEquals(  63.12, TSAGeoMag.getDeclination(  86 ,   70, new Date(2023-1900, 6, 1), 5000) , 5.0E-1);
        assertEquals(   0.36, TSAGeoMag.getDeclination(  32 ,  163, new Date(2023-1900, 6, 1), 9000) , 5.0E-1);
        assertEquals(  -9.39, TSAGeoMag.getDeclination(  48 ,  148, new Date(2023-1900, 6, 1), 5000) , 5.0E-1);
        assertEquals(   4.49, TSAGeoMag.getDeclination(  30 ,   28, new Date(2023-1900, 6, 1), 5000) , 5.0E-1);

        assertEquals(   8.86, TSAGeoMag.getDeclination( -60 ,  -59, new Date(2024-1900, 0, 1), 5000) , 5.0E-1);
        assertEquals( -54.29, TSAGeoMag.getDeclination( -70 ,   42, new Date(2024-1900, 0, 1), 5000) , 5.0E-1);
        assertEquals( -85.69, TSAGeoMag.getDeclination(  87 , -154, new Date(2024-1900, 0, 1), 5000) , 5.0E-1);
        assertEquals(   3.94, TSAGeoMag.getDeclination(  32 ,   19, new Date(2024-1900, 0, 1), 8000) , 5.0E-1);
        assertEquals(  -2.62, TSAGeoMag.getDeclination(  34 ,  -13, new Date(2024-1900, 0, 1), 7000) , 5.0E-1);
        assertEquals( -63.51, TSAGeoMag.getDeclination( -76 ,   49, new Date(2024-1900, 0, 1), 38000) , 5.0E-1);
        assertEquals(  31.57, TSAGeoMag.getDeclination( -50 , -179, new Date(2024-1900, 0, 1), 9000) , 5.0E-1);
        assertEquals(  38.07, TSAGeoMag.getDeclination( -55 , -171, new Date(2024-1900, 0, 1), 9000) , 5.0E-1);
        assertEquals(  -5.00, TSAGeoMag.getDeclination(  42 ,  -19, new Date(2024-1900, 0, 1), 1000) , 5.0E-1);
        assertEquals(  -6.60, TSAGeoMag.getDeclination(  46 ,  -22, new Date(2024-1900, 0, 1), 19000) , 5.0E-1);

        assertEquals(   9.21, TSAGeoMag.getDeclination(  13 , -132, new Date(2024-1900, 6, 1), 31000) , 5.0E-1);
        assertEquals(   7.15, TSAGeoMag.getDeclination(  -2 ,  158, new Date(2024-1900, 0, 1), 3000) , 5.0E-1);
        assertEquals( -55.55, TSAGeoMag.getDeclination( -76 ,   40, new Date(2024-1900, 0, 1), 1000) , 5.0E-1);
        assertEquals(  10.54, TSAGeoMag.getDeclination(  22 , -132, new Date(2024-1900, 0, 1), 4000) , 5.0E-1);
        assertEquals( -62.51, TSAGeoMag.getDeclination( -65 ,   55, new Date(2024-1900, 0, 1), 26000) , 5.0E-1);
        assertEquals( -13.28, TSAGeoMag.getDeclination( -21 ,   32, new Date(2024-1900, 0, 1), 6000) , 5.0E-1);
        assertEquals(   9.36, TSAGeoMag.getDeclination(   9 , -172, new Date(2024-1900, 0, 1), 18000) , 5.0E-1);
        assertEquals(  30.31, TSAGeoMag.getDeclination(  88 ,   26, new Date(2024-1900, 0, 1), 3000) , 5.0E-1);
        assertEquals(   0.56, TSAGeoMag.getDeclination(  17 ,    5, new Date(2024-1900, 0, 1), 33000) , 5.0E-1);
        assertEquals(   4.66, TSAGeoMag.getDeclination( -18 ,  138, new Date(2024-1900, 0, 1), 7000) , 5.0E-1);
    }
}
