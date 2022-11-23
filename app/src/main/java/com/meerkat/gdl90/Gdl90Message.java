package com.meerkat.gdl90;

import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L15M_W23M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L25M_W28P5M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L25_W34M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L35_W33M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L35_W38M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L45_W39P5M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L45_W45M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L55_W45M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L55_W52M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L65_W59P5M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L65_W67M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L75_W72P5M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L75_W80M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L85_W80M;
import static com.meerkat.gdl90.Gdl90Message.AircraftLengthWeight.L85_W90M;
import static com.meerkat.gdl90.Gdl90Message.LateralGpsOfs.LEFT_2M;
import static com.meerkat.gdl90.Gdl90Message.LateralGpsOfs.LEFT_4M;
import static com.meerkat.gdl90.Gdl90Message.LateralGpsOfs.LEFT_6M;
import static com.meerkat.gdl90.Gdl90Message.LateralGpsOfs.RIGHT_0M;
import static com.meerkat.gdl90.Gdl90Message.LateralGpsOfs.RIGHT_2M;
import static com.meerkat.gdl90.Gdl90Message.LateralGpsOfs.RIGHT_4M;
import static com.meerkat.gdl90.Gdl90Message.LateralGpsOfs.RIGHT_6M;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

import com.meerkat.R;
import com.meerkat.log.Log;

import java.io.ByteArrayInputStream;

public class Gdl90Message {
    protected byte messageId;
    private int crc;
    private final ByteArrayInputStream is;
    protected boolean crcValid;

    public enum Emitter {
        Unknown(R.drawable.ic_unknown),
        Light(R.drawable.ic_plane),
        Small(R.drawable.ic_dash8),
        Large(R.drawable.ic_737),
        VLarge(R.drawable.ic_787),
        Heavy(R.drawable.ic_c17),
        Aerobatic(R.drawable.ic_plane),
        Rotor(R.drawable.ic_helicopter),
        Unused(R.drawable.ic_unknown),
        Glider(R.drawable.ic_glider),
        Balloon(R.drawable.ic_balloon),
        Skydiver(R.drawable.ic_parachute),
        Ultralight(R.drawable.ic_plane),
        UAV(R.drawable.ic_uav),
        Spacecraft(R.drawable.ic_rocket),
        Emergency_Vehicle(R.drawable.ic_ambulance),
        Service_Vehicle(R.drawable.ic_pickup),
        Point_Obstacle(R.drawable.ic_flag),
        Cluster_Obstacle(R.drawable.ic_flag),
        Line_Obstacle(R.drawable.ic_flag);
        final public int iconId;
        public Bitmap bitmap;

        Emitter(int iconId) {
            this.iconId = iconId;
        }
    }

    public enum Priority {Normal, Gen_Emerg, Med_Emerg, Min_Fuel, No_Comms, Hijack, Downed, Reserved}

    static Emitter[] emitterLookup = new Emitter[]
            {
                    Emitter.Unknown, Emitter.Light, Emitter.Small, Emitter.Large, Emitter.VLarge, Emitter.Heavy, Emitter.Aerobatic, Emitter.Rotor,
                    Emitter.Unused, Emitter.Glider, Emitter.Balloon, Emitter.Skydiver, Emitter.Ultralight, Emitter.Unused, Emitter.UAV, Emitter.Spacecraft, Emitter.Unused,
                    Emitter.Emergency_Vehicle, Emitter.Service_Vehicle, Emitter.Point_Obstacle, Emitter.Cluster_Obstacle, Emitter.Line_Obstacle, Emitter.Unused, Emitter.Unused, Emitter.Unused,
                    Emitter.Unused, Emitter.Unused, Emitter.Unused, Emitter.Unused, Emitter.Unused, Emitter.Unused, Emitter.Unused, Emitter.Unused,
                    Emitter.Unused, Emitter.Unused, Emitter.Unused, Emitter.Unused, Emitter.Unused, Emitter.Unused, Emitter.Unused, Emitter.Unused};

    final static Priority[] priorityLookup = new Priority[]{Priority.Normal, Priority.Gen_Emerg, Priority.Med_Emerg, Priority.Min_Fuel, Priority.No_Comms, Priority.Hijack, Priority.Downed, Priority.Reserved};


    //AircraftLayer size (upper bound)
    public enum AircraftLengthWeight {
        NO_DATA, L15M_W23M, L25M_W28P5M, L25_W34M, L35_W33M, L35_W38M, L45_W39P5M, L45_W45M,
        L55_W45M, L55_W52M, L65_W59P5M, L65_W67M, L75_W72P5M, L75_W80M, L85_W80M, L85_W90M
    }

    static final protected AircraftLengthWeight[] AircraftLengthWeightLookup = new AircraftLengthWeight[]{
            AircraftLengthWeight.NO_DATA, L15M_W23M, L25M_W28P5M, L25_W34M, L35_W33M, L35_W38M, L45_W39P5M, L45_W45M,
            L55_W45M, L55_W52M, L65_W59P5M, L65_W67M, L75_W72P5M, L75_W80M, L85_W80M, L85_W90M};

    public enum LateralGpsOfs {NO_DATA, LEFT_2M, LEFT_4M, LEFT_6M, RIGHT_0M, RIGHT_2M, RIGHT_4M, RIGHT_6M}

    protected static final LateralGpsOfs[] lateralGpsOfsLookup = new LateralGpsOfs[]{LateralGpsOfs.NO_DATA, LEFT_2M, LEFT_4M, LEFT_6M, RIGHT_0M, RIGHT_2M, RIGHT_4M, RIGHT_6M};

    protected Gdl90Message(ByteArrayInputStream is, int msgSize, byte messageId) {
        this.messageId = messageId;
        this.is = is;
        if (is.available() < msgSize + 2) {
            Log.i("Message too short: expected " + msgSize + " but received " + (is.available() - 2));
            throw new RuntimeException("Message too short: expected " + msgSize + " but received " + (is.available() - 2));
        }
        crc = Crc16Table[0] ^ messageId;
    }

    protected void checkCrc() {
        if (is.available() < 3)
            Log.w("Message to short");
        int savedCrc = crc;
        // Call getByte because it handles escaping
        int recCrc = getByte() + (getByte() << 8);
        if (is.available() < 1) {
            Log.w("Message unexpectedly short");
        } else if (is.read() != 0x7e)
            Log.w("Missing closing flag");
        crcValid = (savedCrc == recCrc);
    }

    private static final int[] Crc16Table = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int crc = i << 8;
            for (int bitNum = 0; bitNum < 8; bitNum++) {
                crc = ((crc << 1) ^ ((crc & 0x8000) != 0 ? 0x1021 : 0)) & 0xffff;
            }
            Crc16Table[i] = crc;
        }
    }

    protected char getChar() {
        short b = (short) (((byte) is.read()) & 0xff);
        if (b == 0x7e) {
            Log.w("Flag found unexpectedly");
            return 0x7e;
        }
        if (b == 0x7d) b = (byte) (is.read() ^ 0x20);
        crc = (Crc16Table[crc >> 8] ^ (crc << 8) ^ b) & 0xffff;
        return (char) (b & 0xff);
    }

    // Return short instead of byte because byte is signed, and sign-extends the MSB
    protected short getByte() {
        short b = (short) (((byte) is.read()) & 0xff);
        if (b == 0x7e) {
            Log.w("Flag found unexpectedly");
            return 0x7e;
        }
        if (b == 0x7d) b = (byte) (is.read() ^ 0x20);
        crc = (Crc16Table[crc >> 8] ^ (crc << 8) ^ b) & 0xffff;
        return (short) (b & 0xff);
    }

    protected long getLong() {
        long result = 0;
        for (int i = 0; i < 8; i++)
            result = (result << 8) + getByte();
        return result;
    }

    protected long getInt() {
        long result = 0;
        for (int i = 0; i < 4; i++)
            result = (result << 8) + getByte();
        return result;
    }

    protected int getShort() {
        return (getByte() << 8) + getByte();
    }

    protected double get3BytesDegrees() {
        short b = getByte();
        int val = (b << 16) | (getByte() << 8) | getByte(); // MSB first, signed
        if ((val & 0x800000) != 0) {
            val = val - 0x1000000;
        }
        return val * 180.0 / 0x800000;
    }

    protected String getString(int numBytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numBytes; i++)
            sb.append((char) getByte());
        return sb.toString();
    }

    protected char crcValidChar() {
        return crcValid ? ' ' : '!';
    }

    public static Gdl90Message getMessage(ByteArrayInputStream is, long time) {
        while (is.available() > 0) {
            byte messageId = (byte) is.read();
            if ((messageId & 0x80) != 0 && (messageId & 0x7f) == 0x7e) {
                Log.w("MSB set on message ID");
                continue;
            }
            if (messageId == 0x7e) continue;     // Flag byte
            Log.v("messageId = " + messageId);
            switch (messageId) {
                case 0:
                    return new Heartbeat(is);
                case 11:
                    return new OwnShipGeometricAltitude(is);
                case 10:
                case 20:
                    return new Traffic(messageId, time, is);
                case 37:
                    return new Identification(is);
                case 40:
                    return new Barometer(is);
                case 43:
                    return new TransponderConfiguration(is);
                case 45:
                    return new Control(is);
                case 46:
                    return new GnssData(is);
                case 47:
                    return new TransponderStatus(is);
                case 101:
                    return new SkyRadar(is);
                case 117:
                    return new UavionixOem(is);
                default:
                    return new Invalid(messageId, is);
            }
        }
        return null;
    }
}
