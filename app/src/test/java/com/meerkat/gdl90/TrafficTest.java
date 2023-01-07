package com.meerkat.gdl90;

import static com.meerkat.SettingsActivity.altUnits;

import android.location.Location;

import com.meerkat.SettingsActivity;
import com.meerkat.log.Log;
import com.meerkat.measure.Position;
import com.meerkat.measure.Units;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;

@RunWith(MockitoJUnitRunner.class)
public class TrafficTest extends TestCase {


    //Try my own object class.
    class MockLocation extends Location {
        double alt;
        public MockLocation(String provider){
            super(provider);
        }
        @Override
        public void setAltitude(double a){
            alt = a;
        }
        //User direct access will not cause a problem when do assertEquals()
    }


    //Try my own object class.
    class MockPosition extends Position {
        double alt, latitude, longitude;
        float speed, track;
        long time;
        String provider;
        public MockPosition(String provider){
            super(provider);
        }
        @Override
        public void setProvider(String p){
            provider= p;
        }
        @Override
        public String getProvider(){
            return provider;
        }
        @Override
        public void setLatitude(double l){
            latitude = l;
        }
        @Override
        public void setLongitude(double l){
            longitude = l;
        }
        @Override
        public double getLatitude(){
            return latitude;
        }
        @Override
        public double getLongitude(){
            return longitude;
        }
        @Override
        public void setSpeed(float s){
            speed = s;
        }
        @Override
        public void setTrack(float t){
            track = t;
        }
        @Override
        public float getSpeed(){
            return speed;
        }
        @Override
        public float getTrack(){
            return track;
        }
        @Override
        public void setAltitude(double a){
            alt = a;
        }
        @Override
        public double getAltitude(){
            return alt;
        }
        //User direct access will not cause a problem when do assertEquals()
        @Override
        public void setTime(long t){
            time = t;
        }
        public double distanceTo(MockPosition p) {
            return computeDistance(latitude, longitude, p.latitude, p.longitude);
        }
        public double bearingTo(MockPosition p) {
            return computeBearing(latitude, longitude, p.latitude, p.longitude);
        }

        private double computeDistance(double lat1, double lon1,
                                                      double lat2, double lon2) {
            // Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
            // using the "Inverse Formula" (section 4)

            // Convert lat/long to radians
            lat1 *= Math.PI / 180.0;
            lat2 *= Math.PI / 180.0;
            lon1 *= Math.PI / 180.0;
            lon2 *= Math.PI / 180.0;

            double a = 6378137.0; // WGS84 major axis
            double b = 6356752.3142; // WGS84 semi-major axis
            double f = (a - b) / a;
            double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

            double l = lon2 - lon1;
            double aA = 0.0;
            double u1 = Math.atan((1.0 - f) * Math.tan(lat1));
            double u2 = Math.atan((1.0 - f) * Math.tan(lat2));

            double cosU1 = Math.cos(u1);
            double cosU2 = Math.cos(u2);
            double sinU1 = Math.sin(u1);
            double sinU2 = Math.sin(u2);
            double cosU1cosU2 = cosU1 * cosU2;
            double sinU1sinU2 = sinU1 * sinU2;

            double sigma = 0.0;
            double deltaSigma = 0.0;
            double cosSqAlpha;
            double cos2SM;
            double cosSigma;
            double sinSigma;
            double cosLambda;
            double sinLambda;

            double lambda = l; // initial guess
            for (int iter = 0; iter < 20; iter++) {
                double lambdaOrig = lambda;
                cosLambda = Math.cos(lambda);
                sinLambda = Math.sin(lambda);
                double t1 = cosU2 * sinLambda;
                double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
                double sinSqSigma = t1 * t1 + t2 * t2;
                sinSigma = Math.sqrt(sinSqSigma);
                cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda;
                sigma = Math.atan2(sinSigma, cosSigma);
                double sinAlpha = (sinSigma == 0) ? 0.0 :
                        cosU1cosU2 * sinLambda / sinSigma;
                cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
                cos2SM = (cosSqAlpha == 0) ? 0.0 : cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha;

                double uSquared = cosSqAlpha * aSqMinusBSqOverBSq;
                aA = 1 + (uSquared / 16384.0) * (4096.0 + uSquared * (-768 + uSquared * (320.0
                        - 175.0 * uSquared)));
                double bB = (uSquared / 1024.0) * (256.0 + uSquared * (-128.0 + uSquared * (74.0
                        - 47.0 * uSquared)));
                double cC = (f / 16.0) * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha));
                double cos2SMSq = cos2SM * cos2SM;
                deltaSigma = bB * sinSigma * (cos2SM + (bB / 4.0) * (cosSigma * (-1.0 + 2.0 * cos2SMSq)
                        - (bB / 6.0) * cos2SM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0
                        + 4.0 * cos2SMSq)));

                lambda = l + (1.0 - cC) * f * sinAlpha * (sigma + cC * sinSigma * (cos2SM
                        + cC * cosSigma * (-1.0 + 2.0 * cos2SM * cos2SM)));

                double delta = (lambda - lambdaOrig) / lambda;
                if (Math.abs(delta) < 1.0e-12) {
                    break;
                }
            }
            return (float) (b * aA * (sigma - deltaSigma));
        }
    }

    private double computeBearing(double lat1, double lon1,
                                                  double lat2, double lon2) {
        // Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
        // using the "Inverse Formula" (section 4)

        // Convert lat/long to radians
        lat1 *= Math.PI / 180.0;
        lat2 *= Math.PI / 180.0;
        lon1 *= Math.PI / 180.0;
        lon2 *= Math.PI / 180.0;

        double a = 6378137.0; // WGS84 major axis
        double b = 6356752.3142; // WGS84 semi-major axis
        double f = (a - b) / a;
        double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

        double l = lon2 - lon1;
        double u1 = Math.atan((1.0 - f) * Math.tan(lat1));
        double u2 = Math.atan((1.0 - f) * Math.tan(lat2));

        double cosU1 = Math.cos(u1);
        double cosU2 = Math.cos(u2);
        double sinU1 = Math.sin(u1);
        double sinU2 = Math.sin(u2);
        double cosU1cosU2 = cosU1 * cosU2;
        double sinU1sinU2 = sinU1 * sinU2;

        double sigma;
        double cosSqAlpha;
        double cos2SM;
        double cosSigma;
        double sinSigma;
        double cosLambda = 0.0;
        double sinLambda = 0.0;

        double lambda = l; // initial guess
        for (int iter = 0; iter < 20; iter++) {
            double lambdaOrig = lambda;
            cosLambda = Math.cos(lambda);
            sinLambda = Math.sin(lambda);
            double t1 = cosU2 * sinLambda;
            double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
            double sinSqSigma = t1 * t1 + t2 * t2;
            sinSigma = Math.sqrt(sinSqSigma);
            cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            double sinAlpha = (sinSigma == 0) ? 0.0 :
                    cosU1cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
            cos2SM = (cosSqAlpha == 0) ? 0.0 : cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha;

            double uSquared = cosSqAlpha * aSqMinusBSqOverBSq;
            double cC = (f / 16.0) * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha));
            lambda = l + (1.0 - cC) * f * sinAlpha * (sigma + cC * sinSigma * (cos2SM
                    + cC * cosSigma * (-1.0 + 2.0 * cos2SM * cos2SM)));

            double delta = (lambda - lambdaOrig) / lambda;
            if (Math.abs(delta) < 1.0e-12) {
                break;
            }
        }

        float initialBearing = (float) Math.atan2(cosU2 * sinLambda,
                cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
        initialBearing = (float) (initialBearing * (180.0 / Math.PI));
        return initialBearing;
    }

    //    @Mock
    MockPosition p1;

    @Test
    public void test() {
         altUnits = Units.Height.FT;
        SettingsActivity.speedUnits = Units.Speed.KNOTS;
        SettingsActivity.distanceUnits = Units.Distance.NM;
        SettingsActivity.vertSpeedUnits = Units.VertSpeed.FPM;
        Log.level(Log.Level.D);
        Traffic prev = null;
        long prevTime = 0;
        String[] rawHex = {
                "07.773", "7e1400c82349e36df37cc02e0f89800d4ff08000414e5a3131334d2006f3427e",
                "11.870", "7e1400c82349e36d317cc02d0f59800d4ff08000414e5a3131334d20062aef7e",
                "14.018", "7e1400c82349e36cd97cc02d0f49800d4ff08000414e5a3131334d200657bf7e",
                "17.910", "7e1400c82349e36c267cc02d0f29800d3ff38000414e5a3131334d200601aa7e",
                "19.138", "7e1400c82349e36bfd7cc02d0f19800d3ff38000414e5a3131334d2006fef87e"};
        for (int i = 0; i < rawHex.length; i += 2) {
            p1 = new MockPosition("ADSB");
            long time = (long) (Double.parseDouble(rawHex[i]) * 1000);
            var data = rawHex[i + 1];

            byte[] raw = new byte[data.length() / 2];
            for (int j = 0; j < data.length(); j += 2) {
                raw[j / 2] = (byte) Integer.parseInt(data.substring(j, j + 2), 16);
            }

            ByteArrayInputStream is = new ByteArrayInputStream(raw);
            Assert.assertEquals(0x7e, is.read());
            byte messageId = (byte) is.read();
            Assert.assertEquals(20, messageId);
            Traffic t = new Traffic(messageId, time, p1, is);
            Assert.assertTrue("CRC", t.crcValid);
            System.out.println(t);
            if (prev != null) {
                int elapsed = (int) (time - prevTime);
                MockPosition predicted = (MockPosition) prev.point;
                predicted.moveBy(elapsed);
                System.out.printf("Actual    %s\n", t.point);
                System.out.printf("Predicted %s\n", predicted);
                System.out.printf("%5.1f @ %d\n", predicted.distanceTo((MockPosition) t.point), (int) predicted.bearingTo((MockPosition)t.point));
            }
            prev = t;
            prevTime = time;
            Assert.assertEquals(13116233, t.participantAddr);
            Assert.assertEquals(8, t.nic);
            Assert.assertEquals(0, t.nac);
            Assert.assertEquals(Gdl90Message.Priority.Normal, t.priority);
//            Assert.assertEquals(Gdl90Message.Emitter.Light, t.emitterType);
            Assert.assertEquals("ANZ113M", t.callsign);
//            Assert.assertEquals(altUnits.FT.toM(5200), p1.getAltitude(), 1e-5);
        }
    }
    @Test
    public void testdeg() {
        Log.level(Log.Level.V);
        byte[] raw = {0, 0, 0};
        ByteArrayInputStream is = new ByteArrayInputStream(raw);
        Gdl90Message msg = new Gdl90Message(is, 1, (byte) 10);
        double lat = msg.get3BytesDegrees();
        Assert.assertEquals(0, lat, 1e-5);
        raw[2] = 1;
        is = new ByteArrayInputStream(raw);
        msg = new Gdl90Message(is, 1, (byte) 10);
        lat = msg.get3BytesDegrees();
        Assert.assertEquals(180.0 / 0x800000, lat, 180.0 / 0x800000 / 2);
        raw = new byte[]{0x1f, (byte) 0xF0, (byte) 0xb6};
        is = new ByteArrayInputStream(raw);
        msg = new Gdl90Message(is, 1, (byte) 10);
        lat = msg.get3BytesDegrees();
        Assert.assertEquals(44.91602, lat, 180.0 / 0x800000 / 2);
        raw = new byte[]{(byte) 0xa8, (byte) 0x8c, 0x31};
        is = new ByteArrayInputStream(raw);
        msg = new Gdl90Message(is, 1, (byte) 10);
        lat = msg.get3BytesDegrees();
        Assert.assertEquals(-122.9799, lat, 180.0 / 0x800000 / 2);
        raw = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff};
        is = new ByteArrayInputStream(raw);
        msg = new Gdl90Message(is, 1, (byte) 10);
        lat = msg.get3BytesDegrees();
        Assert.assertEquals(-180.0 / 0x800000, lat, 180.0 / 0x800000 / 2);
        raw = new byte[]{0x20, 0, 0};
        is = new ByteArrayInputStream(raw);
        msg = new Gdl90Message(is, 1, (byte) 10);
        lat = msg.get3BytesDegrees();
        Assert.assertEquals(45, lat, 180.0 / 0x800000 / 2);
        raw = new byte[]{(byte) 0xe0, 0, 0};
        is = new ByteArrayInputStream(raw);
        msg = new Gdl90Message(is, 1, (byte) 10);
        lat = msg.get3BytesDegrees();
        Assert.assertEquals(-45, lat, 180.0 / 0x800000 / 2);
        raw = new byte[]{(byte) 0x80, 0, 0};
        is = new ByteArrayInputStream(raw);
        msg = new Gdl90Message(is, 1, (byte) 10);
        lat = msg.get3BytesDegrees();
        Assert.assertEquals(-180, lat, 180.0 / 0x800000 / 2);
        raw = new byte[]{(byte) 0x7f, (byte) 0xff, (byte) 0xff};
        is = new ByteArrayInputStream(raw);
        msg = new Gdl90Message(is, 1, (byte) 10);
        lat = msg.get3BytesDegrees();
        Assert.assertEquals(180 - 180.0 / 0x800000, lat, 180.0 / 0x800000 / 2);
    }
}