package com.meerkat.map;

/*
 * SeeYou Waypoint format is a simple comma separated text file. Its extension is .CUP
 *
 * @author <a href="mailto:drifter.frank@gmail.com">Frank van der Hulst</a>
 * <p>
 * http://www.keepitsoaring.com/LKSC/Downloads/cup_format.pdf
 * <p>
 * Generate waypoints.cup data
 * <p>
 * Waypoints are alphabetically sorted
 * <p>
 * Waypoints Each line represents one waypoint with these fields, separated by commas. Here is an example:
 * "Lesce-Bled","LESCE",SI,4621.666N,01410.332E,505.0m,2,130,1140.0m,"123.50","Home airfield"
 * <p>
 * 1. Name: REQUIRED. The long name for the waypoint. It is supposed to be surrounded in double quotes to allow any
 * characters, including a comma in between. This field must not be empty.
 * <p>
 * 2. Code: Also known as short name for a waypoint. Many GPS devices cannot store long waypoint names, so this
 * field will store a short name to be used in various GPS types. It is advisable to put it in double quotes.
 * <p>
 * 3. Country: IANA Top level domain standard is used for the country codes. A complete list is available at
 * http://www.iana.org/cctld/cctld-whois.htm
 * <p>
 * 4. Latitude: REQUIRED. It is a decimal number where 1-2 characters are degrees, 3-4 characters are minutes, 5
 * decimal point, 6-8 characters are decimal minutes. The ellipsoid used is WGS-1984
 * <p>
 * 5. Longitude: REQUIRED. It is a decimal number where 1-3 characters are degrees, 4-5 characters are minutes, 6
 * decimal point, 7-9 characters are decimal minutes. The ellipsoid used is WGS-1984
 * <p>
 * 6. Elevation: REQUIRED. A string with a number with unit attached. Unit can be either "m" for meters or "ft" for
 * feet. Decimal separator must be a point.
 * <p>
 * 7. Waypoint styleNum It is a digit representing these values: 1 - Normal 2 - AirfieldGrass 3 - Outlanding 4 -
 * GliderSite 5 - AirfieldSolid 6 - MtPass 7 - MtTop 8 - Sender 9 - Vor 10 - Ndb 11 - CoolTower 12 - Dam 13 -
 * Tunnel 14 - Bridge 15 - PowerPlant 16 - Castle 17 - Intersection
 * <p>
 * 8. Runway direction It is a string in degrees representing heading of the runway. Only used with Waypoint
 * styleNum types 2, 3, 4 and 5
 * <p>
 * 9. Runway length It is a string for number with unit representing length of the runway. Only used with Waypoint
 * styleNum types 2, 3, 4 and 5 unit can be either "m" for meters "nm" for nautical miles "ml" for statute miles
 * Decimal separator must be a point.
 * <p>
 * 10. Airport Frequency It is a string representing the frequency of the airport. Decimal separator must be a
 * point. It can also be embraced in double quotes.
 * <p>
 * 11. Description It is a string field with no limitation in length where anything can be stored in. It should be
 * embraced with double quotes.
 * @see <a href="http://www.keepitsoaring.com/LKSC/Downloads/cup_format.pdf">http://www.keepitsoaring.com/LKSC/Downloads/cup_format.pdf</a>
 */

import static java.lang.Double.NaN;

import android.location.Location;

import com.meerkat.log.Log;
import com.meerkat.measure.Units;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cup extends Location {

    String name, code, country;
    int style;
    int runwayDir;
    float runwayLen;
    String frequency;
    String description;

    /* Latitude is denoted by DDMM.mmmS
     * Longitude is denoted by DDDMM.mmmE
     * The ellipsoid used is WGS-1984
     */
    private double DegreesMinutes(String s) {
        final Pattern dm = Pattern.compile("([+-]?)(\\d*)([0-5]\\d\\.\\d*)([NSEW]?)");
        Matcher m = dm.matcher(s);
        if (!m.find()) {
            Log.e("Invalid degrees/minutes string %s", s);
            return NaN;
        }
        var sign = m.group(1);
        var deg = m.group(2);
        var min = m.group(3);
        var suffix = m.group(4);
        if (deg == null) return NaN;
        if (min == null) min = "0";
        return ((sign != null && sign.equals("-")) || (suffix != null && "SW".contains(suffix.toUpperCase())) ? -1 : 1) * (Integer.parseInt(deg) + Double.parseDouble(min) / 60f);
    }

    private double Alt(String s) {
        if (s.isBlank()) return NaN;
        final Pattern alt = Pattern.compile("([+-]?\\d*(?:\\.\\d*)?)(ft|m)", Pattern.CASE_INSENSITIVE);
        Matcher m = alt.matcher(s);
        if (!m.find()) {
            Log.e("Invalid altitude string %s", s);
            return NaN;
        }
        var valStr = m.group(1);
        if (valStr == null) return NaN;
        var val = Double.parseDouble(valStr);
        var units = m.group(2);
        return units == null || units.equalsIgnoreCase("M") ? val : units.equalsIgnoreCase("FT") ? Units.Height.FT.toM(val) : NaN;
    }

    private double Distance(String s) {
        if (s.isBlank()) return NaN;
        final Pattern alt = Pattern.compile("([+-]?\\d*(?:\\.\\d*)?)(ft|m|km|nm)", Pattern.CASE_INSENSITIVE);
        Matcher m = alt.matcher(s);
        if (!m.find()) {
            Log.e("Invalid distance string %s", s);
            return NaN;
        }
        var valStr = m.group(1);
        if (valStr == null) return NaN;
        var val = Double.parseDouble(valStr);
        var units = m.group(2);
        if (units == null) units = "m";
        return switch (units.toLowerCase()) {
            case "m" -> val;
            case "nm" -> Units.Distance.NM.toM((float) val);
            case "km" -> Units.Distance.KM.toM((float) val);
            default -> NaN;
        };
    }

    private int getInt(String s) throws NumberFormatException {
        if (s.isBlank()) return Integer.MIN_VALUE;
        return Integer.parseInt(s);
    }

    public Cup(String[] fields) throws NumberFormatException {
        super("Cup");
        /*
         * 1. Name: Long name for the waypoint. Surround in double quotes to allow any characters, including a comma.
         * This field must not be empty.
         *
         * 2. Code: Also known as short name for a waypoint. Many GPS devicees cannot store long waypoint names, so
         * this field will store a short name to be used in various GPS types. It is advisable to put it in double
         * quotes.
         *
         * 3. Country: IANA Top level domain standard is used for the country codes.
         * A complete list is available at http://www.iana.org/cctld/cctld-whois.htm
         *
         * 4. Latitude: A decimal number DDMM.mmm
         *
         * 5. Longitude: A decimal number DDDMM.mmm
         * 6. Elevation: A string containing a number plus unit. Unit can be "m" for meters or "ft" for feet. Decimal separator must be a point. REQUIRED
         */
        name = fields[0];
        code = fields[1];
        country = fields[2];
        setLatitude(DegreesMinutes(fields[3]));
        setLongitude(DegreesMinutes(fields[4]));
        setAccuracy(20);
        setAltitude(Alt(fields[5]));
        setVerticalAccuracyMeters(20);

        style = Integer.parseInt(fields[6]);
        if (style >= GroundIcon.Icons.values().length) style = 0;

        /* Runway direction
         * 8. Runway direction: string in degrees representing heading of the runway (XCSoar expects degrees True)
         * 9 Runway length: string for number plus unit representing length of the runway. Unit can be "m" for meters, "nm" for nautical miles, "ml" for statute miles
         *
         * Only used with Waypoint styleNum types 2, 3, 4 and 5
         */
        if (fields.length > 7) {
            runwayDir = getInt(fields[7]);
            runwayLen = (float) Distance(fields[8]);

            /* 10. Aerodrome Frequency: a string representing the frequency of the airport. May include commas, so surround in double quotes. */
            if (fields.length > 9) {
                frequency = fields[9];

                /* 11. Description: String field with no limitation in length where anything can be stored. It should be surrounded with double quotes. */
                if (fields.length > 10) {
                    description = fields[10];
                }
            }
        }
    }
}

