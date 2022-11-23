package com.meerkat.gdl90;
import androidx.annotation.NonNull;

import com.meerkat.log.Log;

import java.io.ByteArrayInputStream;
import java.util.Locale;

public class Identification extends Gdl90Message {
    private final byte msgVersion;
    private final byte priFwMajorVersion, priFwMinorVersion, priFwBuildVersion, priHwId;
    private final byte secFwMajorVersion, secFwMinorVersion, secFwBuildVersion, secHwId;
    private final long priSerialNo, secSerialNo;
    private byte priFwId, secFwId;
    private long priFwCrc, secFwCrc;
    private String priFwPartNo, secFwPartNo;

    // uAvionix - uAvionix-UCP-Transponder-ICD-Rev-Q.pdf

    public Identification(ByteArrayInputStream is) {
        super(is, 22, (byte) 37);
        msgVersion = (byte) getByte();
        int msgSize = msgVersion == 1 ? 18 : msgVersion == 2 ? 36 : 66;
        if (is.available() < msgSize + 1) {
            Log.w("Message too short: expected " + msgSize + " but received " + (is.available()-1));
        }
        priFwMajorVersion = (byte) getByte();
        priFwMinorVersion = (byte) getByte();
        priFwBuildVersion = (byte) getByte();
        priHwId = (byte) getByte();
        priSerialNo = getLong();
        secFwMajorVersion = (byte) getByte();
        secFwMinorVersion = (byte) getByte();
        secFwBuildVersion = (byte) getByte();
        secHwId = (byte) getByte();
        secSerialNo = getLong();
        if (msgVersion > 1) {
            priFwId = (byte) getByte();
            priFwCrc = getInt();
            secFwId = (byte) getByte();
            secFwCrc = getInt();
            if (msgVersion > 2) {
                priFwPartNo = getString(15).trim();
                secFwPartNo = getString(15).trim();
            }
        }
        checkCrc();
    }

    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "I%c: msg v%d FW v%d.%d.%d HW %d ser %08x, v%d.%d.%d HW %d ser %08x %s",
                crcValidChar(), msgVersion,
                priFwMajorVersion, priFwMinorVersion, priFwBuildVersion, priHwId, priSerialNo,
                secFwMajorVersion, secFwMinorVersion, secFwBuildVersion, secHwId, secSerialNo,
                msgVersion < 2 ? "" : (String.format(Locale.ENGLISH, "FW %d %04x %d %04x ", priFwId, priFwCrc, secFwId, secFwCrc) +
                        (msgVersion < 3 ? "" : String.format(Locale.ENGLISH, "Part# %s,%s", priFwPartNo, secFwPartNo))));
    }
}
