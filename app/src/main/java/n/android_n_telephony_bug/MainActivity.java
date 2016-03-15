package n.android_n_telephony_bug;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "N_BUG";
    private static final int REQ_PERMISSION_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_PERMISSION_LOCATION);
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                // I hope there is no need to handle this on sample project... Hope you are smart enough :)
                debug("You must grant location permission to proceed");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Everything was tested on Nexus 6P (angler).
     *
     * Used shortcuts:
     * - Android M == Android 6.0.1 MHC19I
     * - Android N == Android 6.X   NPC56P
     *
     * This sample just initializes everything. Bugs are listed below in other methods.
     */
    private void init() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        // callback for signal strength change
        telephonyManager.listen(new PhoneSignalListener(), PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        List<CellInfo> cells = telephonyManager.getAllCellInfo();
        if (cells != null && !cells.isEmpty()) {
            for (CellInfo cell : cells) {
                if (cell != null) {
                    if (cell instanceof CellInfoGsm) {
                        processCell((CellInfoGsm) cell);
                    } else if (cell instanceof CellInfoWcdma) {
                        processCell((CellInfoWcdma) cell);
                    } else if (cell instanceof CellInfoLte) {
                        processCell((CellInfoLte) cell);
                    }
                }
            }
        } else {
            debug("TelephonyManager.getAllCellInfo() is null or empty - connect to mobile network please");
        }
    }

    private void processCell(CellInfoGsm cell) {

        // GSM IDENTITY

        CellIdentityGsm identity = cell.getCellIdentity();
        debug("GSM CID - " + String.valueOf(identity.getCid())); // GSM CID = OK
        debug("GSM LAC - " + String.valueOf(identity.getLac())); // GSM LAC = OK


        // Invalid values
        /*
            ARFCN should depend on band (E-GSM, R-GSM, DSC, ...)
            See more at http://niviuk.free.fr/gsm_arfcn.php

            Even if I'm on same cell this value changes (it should not).
            I'd say that value in this variable (ARFCN) is actually signal of cell in ASU
         */
        debug("GSM ARFCN - " + String.valueOf(identity.getArfcn()));

        /*
            BSIC is also invalid.
            Value 0x63 (99 in hex) is always reported.
            Documentation says that invalid value is Integer.MAX_VALUE, not 99.
         */
        debug("GSM BSIC - " + String.valueOf(identity.getBsic()));

        // GSM SIGNAL STRENGTH

        CellSignalStrengthGsm signal = cell.getCellSignalStrength();

        /*
            Method getDbm() returns always -113.
            On Android M every thing was fine.
         */
        debug("GSM DBM - " + String.valueOf(signal.getDbm()));

        /*
            Considering formula {dBm} = -113 + 2 * {asu}
            this value is OK cause dBm is -113.
            Works on Android M.
         */
        debug("GSM Asu Level - " + String.valueOf(signal.getAsuLevel()));

        /*
            GSM TA

            Valid values: <0; 65> (can be more than 65, depends on band)
            Android M: Not available. This field is new on Android N.
            Android N: Always 0.

            Android M:     -
            Android N:     0
         */
        int ta = reflectField("mTimingAdvance", signal);
        debug("GSM TA - " + String.valueOf(ta));

    }

    private void processCell(CellInfoWcdma cell) {

        // WCDMA identity
        CellIdentityWcdma identity = cell.getCellIdentity();

        debug("WCDMA CID - " + String.valueOf(identity.getCid())); // WCDMA CID == OK
        debug("WCDMA LAC - " + String.valueOf(identity.getLac())); // WCDMA LAC == OK
        debug("WCDMA PSC - " + String.valueOf(identity.getPsc())); // WCDMA PSC == OK

        /*
            UARFCN should depend on current frequency
            See more at http://niviuk.free.fr/umts_band.php

            Same case as GSM
              - values are changing even if I'm on same cell
              - also I'd say that ASU is reported instead of UARFCN
         */
        debug("WCDMA UARFCN - " + String.valueOf(identity.getUarfcn()));

        CellSignalStrengthWcdma signal = cell.getCellSignalStrength();

        /*
            Method getDbm() return always Integer.MAX_VALUE.
            On Android M every thing was fine.
         */
        debug("WCDMA DBM - " + String.valueOf(signal.getDbm()));

        /*
            Reported as unknown - always 99.
            Works on Android M.
         */
        debug("WCDMA Asu Level - " + String.valueOf(signal.getAsuLevel()));

        /*
            Always 0 (which is expected when signal is unknown)
            Works on Android M.
         */
        debug("WCDMA Level - " + String.valueOf(signal.getLevel()));
    }

    private void processCell(CellInfoLte cell) {
        CellIdentityLte identity = cell.getCellIdentity();
        CellSignalStrengthLte signal = cell.getCellSignalStrength();

        debug("LTE CI - " + String.valueOf(identity.getCi())); // LTE CI  == OK
        debug("LTE TAC - " + String.valueOf(identity.getTac())); // LTE TAC  == OK
        debug("LTE PCI - " + String.valueOf(identity.getPci())); // LTE PCI  == OK

        /*
            EARFCN is also connected with current frequency
            See more at http://niviuk.free.fr/lte_band.php

            Values are also invalid as in GSM ARFCN and WCDMA UARFCN.
            Behaviour is same:
              - values are changing even if I'm on same cell
              - some random value is reported but now it is definitely not ASU
         */
        debug("LTE EARFCN - " + String.valueOf(identity.getEarfcn()));

        /*
            This part is focused on signal connected with cell.
            Several field were added on API 16 but are hidden.
            The only way to reach them is by reflection.

            If you are a Google developer please make getters public on Android N if possible.
         */

        int rssi = reflectField("mSignalStrength", signal);
        int rsrp = reflectField("mRsrp", signal); // returned by CellSignalStrengthLte.getDbm();
        int rsrq = reflectField("mRsrq", signal);
        int snr = reflectField("mRssnr", signal);
        int cqi = reflectField("mCqi", signal);
        int ta = reflectField("mTimingAdvance", signal); //returned by CellSignalStrengthLte.getTimingAdvance();

        /*
            At this part I will compare Android M and Android N functionality
         */

        /*
           LTE RSSI

           Valid values: <-51; -113> [dBm] or <0; 31> [asu]
           Android M: OK, reports asu values.
           Android N: Broken. Reports *negated* dBm values of *RSRP*

           Android M:    12
           Android N:    103

           Formula: {dBm} = -113 + 2 * {asu}
           {dBm} = -113 + 2 * 12 =
           {dBm} = -89
         */
        debug("LTE RSSI - " + String.valueOf(rssi));

        /*
            LTE RSRP

            Valid values: <-44; -140> [dBm] or <0; 96> [asu]
            Android M: OK, reports dBm values.
            Android N: Broken. This field contains value of RSRQ instead of RSRP!

            Android M:     -95  (is RSRP)
            Android N:     -8   (is not RSRP, is RSRQ)

            Formula: {dBm} = -140 + {asu}
         */
        debug("LTE RSRP - " + String.valueOf(rsrp));

        /*
            LTE RSRQ

            Valid values: <-3; -30> [dB]
            Android M: OK, reports dBm values
            Android N: Broken. Always Integer.MIN_VALUE

            Android M:     -8
            Android N:     Integer.MIN_VALUE
         */
        debug("LTE RSRQ - " + String.valueOf(rsrq));

         /*
            LTE SNR

            Valid values: <0; 30> [dB]
            Documentation says that SNR*10 is reported. So valid values from Android are <0; 300>
            Android M: Broken. Always Integer.MAX_VALUE
            Android N: Broken. Always Integer.MAX_VALUE

            Android M:     Integer.MAX_VALUE
            Android N:     Integer.MAX_VALUE

            However, this value can still be retrieved using SignalStrength.
            Code is a bit lower - see PhoneSignalListener.class
         */
        debug("LTE SNR - " + String.valueOf(snr));

         /*
            LTE CQI

            Valid values: <1; 15>
            Android M: Broken. Always Integer.MAX_VALUE
            Android N: Broken. Value of TimingAdvance (TA) is assigned into this field

            Android M:     Integer.MAX_VALUE
            Android N:     Integer.MAX_VALUE or TA value

         */
        debug("LTE CQI - " + String.valueOf(cqi));


        /*
            LTE TA

            Valid values: <0; 100>
            Android M: Correct. If TA is available then it is assigned to this field, otherwise Integer.MAX_VALUE
            Android N: Broken. Always 0

            Android M:     21
            Android N:     0

         */
        debug("LTE TA - " + String.valueOf(ta));
    }

    /**
     * Here I just want to demonstrate a small bug which is present also on Android M.
     * Just in case u wanted to fix it...
     */
    private class PhoneSignalListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            if (signalStrength != null) {
                /*
                    Here I grab RSSNR (SNR) field.

                    SignalsStrength contains information about signal of current cell.
                    The problem occurs when I use dualSIM phone for example.
                    It's impossible to detect to which cell are signal values assigned to.
                    So the only option is to use new api - TelephonyManager.getAllCellInfo()
                    as is showed above.

                    Since Nexus 6P is single SIM it's possible to pair those values with current cell.

                    New API does not report valid SNR value, however this one reports partially correct value

                    -----------

                    Valid values: <0; 30> [dB]
                    Documentation says that SNR*10 is reported. So valid values from Android are <0; 300>
                    Android M + N: Partially broken. Initially reports value 200. If SNR is above 200, then the value is correct.
                    For example 231, 256, ...

                    So, what do I expect?
                     - Try to fix the bug when only values above 200 are reported.
                     - New API should report also valid SNR

                 */
                int snr = (reflectField("mLteRssnr", signalStrength));

                debug("LTE SNR - OLD API " + String.valueOf(snr));
            }
        }
    }

    /**
     * Gets selected field from target object
     *
     * @param name   name of field in source object
     * @param source object from which the filed should be taken
     * @return field value or {@link Integer#MAX_VALUE}
     */
    private int reflectField(String name, Object source) {
        try {
            Field field = source.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (Integer) field.get(source);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Integer.MAX_VALUE;
    }

    /**
     * @param message message which should be sent to logcat
     */
    private void debug(String message) {
        Log.d(TAG, message);
    }


}
