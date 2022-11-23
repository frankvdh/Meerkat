package com.meerkat;
/*                PUBLIC DOMAIN NOTICE
This program was prepared by Los Alamos National Security, LLC
at Los Alamos National Laboratory (LANL) under contract No.
DE-AC52-06NA25396 with the U.S. Department of Energy (DOE).
All rights in the program are reserved by the DOE and
Los Alamos National Security, LLC.  Permission is granted to the
public to copy and use this software without charge,
provided that this Notice and any statement of authorship are
reproduced on all copies.  Neither the U.S. Government nor LANS
makes any warranty, express or implied, or assumes any liability
or responsibility for the use of this software.
 */

/*           License Statement from the NOAA
The WMM source code is in the public domain and not licensed or
under copyright. The information and software may be used freely
by the public. As required by 17 U.S.C. 403, third parties producing
copyrighted works consisting predominantly of the material produced
by U.S. government agencies must provide notice with such work(s)
identifying the U.S. Government material incorporated and stating
that such material is not subject to copyright protection.
 */

////////////////////////////////////////////////////////////////////////////
//
//GeoMag.java - originally geomag.c
//Ported to Java 1.0.2 by Tim Walker
//tim.walker@worldnet.att.net
//tim@acusat.com
//
//Updated: 1/28/98
//
//Original source geomag.c available at
//http://www.ngdc.noaa.gov/seg/potfld/DoDWMM.html
//
//NOTE: original comments from the geomag.c source file are in ALL CAPS
//Tim's added comments for the port to Java are not
//
////////////////////////////////////////////////////////////////////////////

import java.util.Date;

/**
 * <p>
 * Last updated on Jan 6, 2020</p><p>
 * <b>NOTE: </b>Comment out the logger references, and put back in the System.out.println
 * statements if not using log4j in your application. Checks are not made on the method inputs
 * to ensure they are within a valid range.</p><p>
 * <p>
 * Verified by a JUnit test using the test values distributed with the 2020 epoch update.</p><p>
 * <p>
 * This is a class to generate the magnetic declination,
 * magnetic field strength and inclination for any point
 * on the earth.  The true bearing = magnetic bearing + declination.
 * This class is adapted from an Applet from the NOAA National Data Center
 * at <a href ="http://www.ngdc.noaa.gov/seg/segd.shtml"> http://www.ngdc.noaa.gov/seg/segd.shtml.</a>
 * None of the calculations
 * were changed.  This class requires an input file named WMM.COF, which
 * must be in the same directory that the application is run from. <br>
 * <b>NOTE:</b> If the WMM.COF file is missing, the internal fit coefficients
 * for 2020 will be used.
 * <p>
 * Using the correct date, the declination is accurate to about 0.5 degrees.</p><p>
 * <p>
 * This is the LANL D-3 version of the GeoMagnetic calculator from
 * the NOAA National Data Center at http://www.ngdc.noaa.gov/seg/segd.shtml.</p><p>
 * <p>
 * Adapted by John St. Ledger, Los Alamos National Laboratory
 * June 25, 1999</p><p>
 * <p>
 * <p>
 * Version 2 Comments:  The world magnetic model is updated every 5 years.
 * The data for 2000 uses the same algorithm to calculate the magnetic
 * field variables.  The only change is in the spherical harmonic coefficients
 * in the input file.  The input file has been renamed to WMM.COF.  Once again,
 * the date was fixed.  This time to January 1, 2001.  Also, a deprecated
 * constructor for StreamTokenizer was replaced, and the error messages in the catch
 * clause were changed.  Methods to get the field strength and inclination
 * were added.</p><p>
 * <p>
 * Found out some interesting information about the altitude. The altitude entered
 * for the calculations is the height above the WGS84 spheroid, not height MSL. Using
 * MSL height means that the altitude could be in error by as much as 200 meters.
 * This should not be significant for our applications.</p>
 *
 * <p><b>NOTE:</b> This class is not thread safe.</p>
 *
 * @version 5.9 January 6, 2020
 * <p>Updated the internal coefficients to the 2020 epoch values. Passes the new JUnit tests.</p>
 *
 * </li></ul>
 *    <ul>References:
 *
 *      <li>JOHN M. QUINN, DAVID J. KERRIDGE AND DAVID R. BARRACLOUGH,
 *           WORLD MAGNETIC CHARTS FOR 1985 - SPHERICAL HARMONIC
 *           MODELS OF THE GEOMAGNETIC FIELD AND ITS SECULAR
 *           VARIATION, GEOPHYS. J. R. ASTR. SOC. (1986) 87,
 *           PP 1143-1157</li>
 *
 *      <li>DEFENSE MAPPING AGENCY TECHNICAL REPORT, TR 8350.2:
 *           DEPARTMENT OF DEFENSE WORLD GEODETIC SYSTEM 1984,
 *           SEPT. 30 (1987)</li>
 *
 *      <li>JOSEPH C. CAIN, ET AL.; A PROPOSED MODEL FOR THE
 *           INTERNATIONAL GEOMAGNETIC REFERENCE FIELD - 1965,
 *           J. GEOMAG. AND GEOELECT. VOL. 19, NO. 4, PP 335-355
 *           (1967) (SEE APPENDIX)</li>
 *
 *      <li>ALFRED J. ZMUDA, WORLD MAGNETIC SURVEY 1957-1969,
 *           INTERNATIONAL ASSOCIATION OF GEOMAGNETISM AND
 *           AERONOMY (IAGA) BULLETIN #28, PP 186-188 (1971)</li>
 *
 *      <li>JOHN M. QUINN, RACHEL J. COLEMAN, MICHAEL R. PECK, AND
 *           STEPHEN E. LAUBER; THE JOINT US/UK 1990 EPOCH
 *           WORLD MAGNETIC MODEL, TECHNICAL REPORT NO. 304,
 *           NAVAL OCEANOGRAPHIC OFFICE (1991)</li>
 *
 *      <li>JOHN M. QUINN, RACHEL J. COLEMAN, DONALD L. SHIEL, AND
 *           JOHN M. NIGRO; THE JOINT US/UK 1995 EPOCH WORLD
 *           MAGNETIC MODEL, TECHNICAL REPORT NO. 314, NAVAL
 *           OCEANOGRAPHIC OFFICE (1995)</li></ul>
 *
 *
 *
 *
 *    <p>WMM-2000 is a National Imagery and Mapping Agency (NIMA) standard
 *    product. It is covered under NIMA Military Specification:
 *    MIL-W-89500 (1993).
 * <p>
 *    For information on the use and applicability of this product contact</p>
 * <p>
 *                    DIRECTOR<br>
 *                    NATIONAL IMAGERY AND MAPPING AGENCY/HEADQUARTERS<br>
 *                    ATTN: CODE P33<br>
 *                    12310 SUNRISE VALLEY DRIVE<br>
 *                    RESTON, VA 20191-3449<br>
 *                    (703) 264-3002<br>
 *
 *
 *    <p>The FORTRAN version of GEOMAG PROGRAMMED BY:</p>
 * <p>
 *                    JOHN M. QUINN  7/19/90<br>
 *                    FLEET PRODUCTS DIVISION, CODE N342<br>
 *                    NAVAL OCEANOGRAPHIC OFFICE (NAVOCEANO)<br>
 *                    STENNIS SPACE CENTER (SSC), MS 39522-5001<br>
 *                    USA<br>
 *                    PHONE:   COM:  (601) 688-5828<br>
 *                              AV:        485-5828<br>
 *                             FAX:  (601) 688-5521<br>
 *
 *    <p>NOW AT:</p>
 * <p>
 *                    GEOMAGNETICS GROUP<br>
 *                    U. S. GEOLOGICAL SURVEY   MS 966<br>
 *                    FEDERAL CENTER<br>
 *                    DENVER, CO   80225-0046<br>
 *                    USA<br>
 *                    PHONE:   COM: (303) 273-8475<br>
 *                             FAX: (303) 273-8600<br>
 *                    EMAIL:   quinn@ghtmail.cr.usgs.gov<br>
 */
public class TSAGeoMag {
    // The input arrays contain each line of input for the 2020 WMM.COF input file, transposed.
    // The date for the start of the valid time of the fit coefficients
    private static final Date EPOCH = new Date(2020-1900, 0, 1);
    private static final int[] n0 = new int[]{1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12};
    private static final int[] m0 = new int[]{0, 1, 0, 1, 2, 0, 1, 2, 3, 0, 1, 2, 3, 4, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 6, 0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7, 8, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final float[] gnm0 = new float[]{-29404.5f, -1450.7f, -2500f, 2982f, 1676.8f, 1363.9f, -2381f, 1236.2f, 525.7f, 903.1f, 809.4f, 86.2f, -309.4f, 47.9f, -234.4f, 363.1f, 187.8f, -140.7f, -151.2f, 13.7f, 65.9f, 65.6f, 73f, -121.5f, -36.2f, 13.5f, -64.7f, 80.6f, -76.8f, -8.3f, 56.5f, 15.8f, 6.4f, -7.2f, 9.8f, 23.6f, 9.8f, -17.5f, -0.4f, -21.1f, 15.3f, 13.7f, -16.5f, -0.3f, 5f, 8.2f, 2.9f, -1.4f, -1.1f, -13.3f, 1.1f, 8.9f, -9.3f, -11.9f, -1.9f, -6.2f, -0.1f, 1.7f, -0.9f, 0.6f, -0.9f, 1.9f, 1.4f, -2.4f, -3.9f, 3f, -1.4f, -2.5f, 2.4f, -0.9f, 0.3f, -0.7f, -0.1f, 1.4f, -0.6f, 0.2f, 3.1f, -2f, -0.1f, 0.5f, 1.3f, -1.2f, 0.7f, 0.3f, 0.5f, -0.2f, -0.5f, 0.1f, -1.1f, -0.3f};
    private static final float[] hnm0 = new float[]{0f, 4652.9f, 0f, -2991.6f, -734.8f, 0f, -82.2f, 241.8f, -542.9f, 0f, 282f, -158.4f, 199.8f, -350.1f, 0f, 47.7f, 208.4f, -121.3f, 32.2f, 99.1f, 0f, -19.1f, 25f, 52.7f, -64.4f, 9f, 68.1f, 0f, -51.4f, -16.8f, 2.3f, 23.5f, -2.2f, -27.2f, -1.9f, 0f, 8.4f, -15.3f, 12.8f, -11.8f, 14.9f, 3.6f, -6.9f, 2.8f, 0f, -23.3f, 11.1f, 9.8f, -5.1f, -6.2f, 7.8f, 0.4f, -1.5f, 9.7f, 0f, 3.4f, -0.2f, 3.5f, 4.8f, -8.6f, -0.1f, -4.2f, -3.4f, -0.1f, -8.8f, 0f, 0f, 2.6f, -0.5f, -0.4f, 0.6f, -0.2f, -1.7f, -1.6f, -3f, -2f, -2.6f, 0f, -1.2f, 0.5f, 1.3f, -1.8f, 0.1f, 0.7f, -0.1f, 0.6f, 0.2f, -0.9f, 0f, 0.5f};
    private static final float[] dgnm0 = new float[]{6.7f, 7.7f, -11.5f, -7.1f, -2.2f, 2.8f, -6.2f, 3.4f, -12.2f, -1.1f, -1.6f, -6f, 5.4f, -5.5f, -0.3f, 0.6f, -0.7f, 0.1f, 1.2f, 1f, -0.6f, -0.4f, 0.5f, 1.4f, -1.4f, 0f, 0.8f, -0.1f, -0.3f, -0.1f, 0.7f, 0.2f, -0.5f, -0.8f, 1f, -0.1f, 0.1f, -0.1f, 0.5f, -0.1f, 0.4f, 0.5f, 0f, 0.4f, -0.1f, -0.2f, 0f, 0.4f, -0.3f, 0f, 0.3f, 0f, 0f, -0.4f, 0f, 0f, 0f, 0.2f, -0.1f, -0.2f, 0f, -0.1f, -0.2f, -0.1f, 0f, 0f, -0.1f, 0f, 0f, 0f, -0.1f, 0f, 0f, -0.1f, -0.1f, -0.1f, -0.1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, -0.1f};
    private static final float[] dhnm0 = new float[]{0f, -25.1f, 0f, -30.2f, -23.9f, 0f, 5.7f, -1f, 1.1f, 0f, 0.2f, 6.9f, 3.7f, -5.6f, 0f, 0.1f, 2.5f, -0.9f, 3f, 0.5f, 0f, 0.1f, -1.8f, -1.4f, 0.9f, 0.1f, 1f, 0f, 0.5f, 0.6f, -0.7f, -0.2f, -1.2f, 0.2f, 0.3f, 0f, -0.3f, 0.7f, -0.2f, 0.5f, -0.3f, -0.5f, 0.4f, 0.1f, 0f, -0.3f, 0.2f, -0.4f, 0.4f, 0.1f, 0f, -0.2f, 0.5f, 0.2f, 0f, 0f, 0.1f, -0.3f, 0.1f, -0.2f, 0.1f, 0f, -0.1f, 0.2f, 0f, 0f, 0f, 0.1f, 0f, 0.2f, 0f, 0f, 0.1f, 0f, -0.1f, 0f, 0f, 0f, 0f, 0f, -0.1f, 0.1f, 0f, 0f, 0f, 0.1f, 0f, 0f, 0f, -0.1f};

    private static final double DegToRad = Math.PI / 180.0;


    // The maximum number of degrees of the spherical harmonic model.
    private static final int MAX_DEG = 12;

    // The Gauss coefficients of main geomagnetic model (nt).
    private static final double[][] C = new double[13][13];

    // The Gauss coefficients of secular geomagnetic model (nt/yr).
    private static final double[][] Cd = new double[13][13];

    // The theta derivative of p(n,m) (unnormalized).
    private static final double[][] dp = new double[13][13];

    // The Schmidt normalization factors.
    private static final double[] snorm = new double[169];

    // The sine of (m*spherical coord. longitude).
    private static final double[] sp = new double[13];

    // The cosine of (m*spherical coord. longitude).
    private static final double[] cp = new double[13];
    private static final double[] fn = new double[13];
    private static final double[] fm = new double[13];

    // The associated Legendre polynomials for m=1 (unnormalized).
    private static final double[] pp = new double[13];
    private static final double[][] k = new double[13][13];

    // Semi-major axis of WGS-84 ellipsoid, in km.
    private static final double A = 6378.137;
    // Semi-minor axis of WGS-84 ellipsoid, in km.
    private static final double B = 6356.7523142;

    /**
     * re is the Mean radius of IAU-66 ellipsoid, in km.
     * a2 is the Semi-major axis of WGS-84 ellipsoid, in km, squared.
     * b2 is the Semi-minor axis of WGS-84 ellipsoid, in km, squared.
     * c2 is c2 = a2 - b2
     * a4 is a2 squared.
     * b4 is b2 squared.
     * c4 is c4 = a4 - b4.
     */
    private static final double radEarth = 6371.2;
    private static final double a2 = A * A;
    private static final double b2 = B * B;
    private static final double c2 = a2 - b2;
    private static final double a4 = a2 * a2;
    private static final double b4 = b2 * b2;
    private static final double c4 = a4 - b4;

    static {
        // Initialize constants
        sp[0] = 0.0;
        cp[0] = snorm[0] = pp[0] = 1.0;
        dp[0][0] = 0.0;

        setCoeff();
        // Convert schmidt normalized gauss coefficients to unnormalized
        snorm[0] = 1.0;
        for (int n = 1; n <= MAX_DEG; n++) {
            snorm[n] = snorm[n - 1] * (2 * n - 1) / n;
            int j = 2;

            for (int m = 0, D1 = 1, D2 = (n - m + D1) / D1; D2 > 0; D2--, m += D1) {
                k[m][n] = (double) (((n - 1) * (n - 1)) - (m * m)) / (double) ((2 * n - 1) * (2 * n - 3));
                if (m > 0) {
                    double flnmj = ((n - m + 1) * j) / (double) (n + m);
                    snorm[n + m * 13] = snorm[n + (m - 1) * 13] * Math.sqrt(flnmj);
                    j = 1;
                    C[n][m - 1] = snorm[n + m * 13] * C[n][m - 1];
                    Cd[n][m - 1] = snorm[n + m * 13] * Cd[n][m - 1];
                }
                C[m][n] = snorm[n + m * 13] * C[m][n];
                Cd[m][n] = snorm[n + m * 13] * Cd[m][n];
            }
            fn[n] = (n + 1);
            fm[n] = n;
        }
        k[1][1] = 0.0;
    }

    /*
     * by is the north south field intensity
     * bx is the east west field intensity
     * bz is the vertical field intensity positive downward
     */
    private final double bx, by, bz;

    /**
     * <p><b>Purpose:</b>  Computes the declination (dec), inclination (dip), total intensity (ti) and
     * grid variation (gv - polar regions only, referenced to grid north of polar stereographic projection) of
     * the earth's magnetic field in geodetic coordinates from the coefficients of the current official
     * department of defense (dod) spherical harmonic world magnetic model (wmm-2010). The WMM series of models is
     * updated every 5 years on Jan 1st of those years which are divisible by 5 (i.e. 1980, 1985, 1990 etc.)
     * by the Naval Oceanographic Office in cooperation with the British Geological Survey (bgs). The model
     * is based on geomagnetic survey measurements from aircraft, satellite and geomagnetic observatories.</p><p>
     *
     * <b>Accuracy:</b>  in ocean areas at the earth's surface over the entire 5 year life of a degree and order 12
     * spherical harmonic model, the estimated rms errors for the various magnetic components are:</p>
     * <ul>
     *                DEC  -   0.5 Degrees<br>
     *                DIP  -   0.5 Degrees<br>
     *                TI   - 280.0 nanoTeslas (nT)<br>
     *                GV   -   0.5 Degrees<br></ul>
     *
     * <p>Other magnetic components that can be derived from these four by simple trigonometric relations will
     * have the following approximate errors over ocean areas:</p>
     * <ul>
     *                X    - 140 nT (North)<br>
     *                Y    - 140 nT (East)<br>
     *                Z    - 200 nT (Vertical)  Positive is down<br>
     *                H    - 200 nT (Horizontal)<br></ul>
     *
     * <p>Over land the rms errors are expected to be somewhat higher, although the rms errors for dec, dip and gv
     * are still estimated to be less than 0.5 degree, for the entire 5-year life of the model at the earth's
     * surface. The other component errors over land are more difficult to estimate and so are not given.</p><p>
     * <p>
     * The accuracy at any given time of all four geomagnetic parameters depends on the geomagnetic
     * latitude. The errors are least at the equator and greatest at the magnetic poles.</p><p>
     * <p>
     * It is very important to note that a degree and order 12 model describes only
     * the long wavelength spatial magnetic fluctuations due to earth's core. Not included in the WMM series
     * models are intermediate and short wavelength spatial fluctuations of the geomagnetic field
     * which originate in the earth's mantle and crust. Consequently, isolated angular errors at various
     * positions on the surface (primarily over land, in continental margins and over oceanic seamounts,
     * ridges and trenches) of several degrees may be expected. Also not included in the model are
     * nonsecular temporal fluctuations of the geomagnetic field of magnetospheric and ionospheric origin.
     * during magnetic storms, temporal fluctuations can cause substantial deviations of the geomagnetic
     * field from model values. In arctic and antarctic regions, as well as in equatorial regions, deviations
     * from model values are both frequent and persistent.</p><p>
     * <p>
     * This version of geomag uses the WMM-2010 geomagnetic model referenced to the WGS84 gravity model ellipsoid</p>
     *
     * @param fLat     The latitude in decimal degrees.
     * @param fLon     The longitude in decimal degrees.
     * @param date     The date.
     * @param alt      The altitude in feet.
     */
    public TSAGeoMag(double fLat, double fLon, Date date, int alt) {
        double lonR = fLon * DegToRad;
        double latR = fLat * DegToRad;
        double altitude = alt/3280.84;
        double sinLon = Math.sin(lonR);
        double sinLat = Math.sin(latR);
        double cosLon = Math.cos(lonR);
        double cosLat = Math.cos(latR);
        double sinLat2 = sinLat * sinLat;
        double cosLat2 = cosLat * cosLat;
        sp[1] = sinLon;
        cp[1] = cosLon;

        // Convert from geodetic coords. to spherical coords.

        double q = Math.sqrt(a2 - c2 * sinLat2);
        double q1 = altitude * q;
        double q2 = ((q1 + a2) / (q1 + b2)) * ((q1 + a2) / (q1 + b2));
        double ct = sinLat / Math.sqrt(q2 * cosLat2 + sinLat2);
        double st = Math.sqrt(1.0 - (ct * ct));
        double r2 = ((altitude * altitude) + 2.0 * q1 + (a4 - c4 * sinLat2) / (q * q));
        double r = Math.sqrt(r2);
        double d = Math.sqrt(a2 * cosLat2 + b2 * sinLat2);
        double ca = (altitude + d) / r;
        double sa = c2 * cosLat * sinLat / (r * d);

        for (int m = 2; m <= MAX_DEG; m++) {
            sp[m] = sp[1] * cp[m - 1] + cp[1] * sp[m - 1];
            cp[m] = cp[1] * cp[m - 1] - sp[1] * sp[m - 1];
        }
        double aor = radEarth / r;
        double ar = aor * aor;
        double br = 0, bt = 0, bp = 0, bpp = 0;

        // The time adjusted geomagnetic gauss coefficients (nt).
        double dt = (date.getTime() - EPOCH.getTime()) / (365.25 * 24 * 60 * 60 * 1000);
        double[][] tc = new double[13][13];

        for (int n = 1; n <= MAX_DEG; n++) {
            ar = ar * aor;
            for (int m = 0, D3 = 1, D4 = (n + m + D3) / D3; D4 > 0; D4--, m += D3) {

                // Compute unnormalized associated legendre polynomials and derivatives via recursion relations
                if (n == m) {
                    snorm[n + m * 13] = st * snorm[n - 1 + (m - 1) * 13];
                    dp[m][n] = st * dp[m - 1][n - 1] + ct * snorm[n - 1 + (m - 1) * 13];
                }
                if (n == 1 && m == 0) {
                    snorm[n] = ct * snorm[n - 1];
                    dp[m][n] = ct * dp[m][n - 1] - st * snorm[n - 1];
                }
                if (n > 1 && n != m) {
                    if (m > n - 2)
                        snorm[n - 2 + m * 13] = 0.0;
                    if (m > n - 2)
                        dp[m][n - 2] = 0.0;
                    snorm[n + m * 13] = ct * snorm[n - 1 + m * 13] - k[m][n] * snorm[n - 2 + m * 13];
                    dp[m][n] = ct * dp[m][n - 1] - st * snorm[n - 1 + m * 13] - k[m][n] * dp[m][n - 2];
                }

                // Time adjust the gauss coefficients

                tc[m][n] = C[m][n] + dt * Cd[m][n];

                if (m != 0)
                    tc[n][m - 1] = C[n][m - 1] + dt * Cd[n][m - 1];

                // Accumulate terms of the spherical harmonic expansions
                double temp1, temp2;
                double par = ar * snorm[n + m * 13];
                if (m == 0) {
                    temp1 = tc[m][n] * cp[m];
                    temp2 = tc[m][n] * sp[m];
                } else {
                    temp1 = tc[m][n] * cp[m] + tc[n][m - 1] * sp[m];
                    temp2 = tc[m][n] * sp[m] - tc[n][m - 1] * cp[m];
                }

                bt = bt - ar * temp1 * dp[m][n];
                bp += (fm[m] * temp2 * par);
                br += (fn[n] * temp1 * par);

                // Special case:  north/south geographic poles

                if (st == 0.0 && m == 1) {
                    if (n == 1)
                        pp[n] = pp[n - 1];
                    else
                        pp[n] = ct * pp[n - 1] - k[m][n] * pp[n - 2];
                    double parp = ar * pp[n];
                    bpp += (fm[m] * temp2 * parp);
                }
            }
        }

        if (st == 0.0)
            bp = bpp;
        else
            bp /= st;

        // Rotate magnetic vector components from spherical to geodetic coordinates
        // bx is the east-west field component
        // by is the north-south field component
        // bz is the vertical field component.
        bx = -bt * ca - br * sa;
        by = bp;
        bz = bt * sa - br * ca;
    }

    /**
     * This method sets the input data to the internal fit coefficients.
     */
    private static void setCoeff() {
        C[0][0] = 0.0f;
        Cd[0][0] = 0.0f;

        //loop to get data from internal values
        for (int i = 0; i < n0.length; i++) {
            int n = n0[i];
            int m = m0[i];
            float gnm = gnm0[i];
            float hnm = hnm0[i];
            float dgnm = dgnm0[i];
            float dhnm = dhnm0[i];

            if (m <= n) {
                C[m][n] = gnm;
                Cd[m][n] = dgnm;

                if (m != 0) {
                    C[n][m - 1] = hnm;
                    Cd[n][m - 1] = dhnm;
                }
            }
        }
    }

    public static double getDeclination(double dlat, double dlong, Date date, int altitude) {
        TSAGeoMag mf = new TSAGeoMag(dlat, dlong, date, altitude);
        return Math.atan2(mf.by, mf.bx)/DegToRad;
    }
}
