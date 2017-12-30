package com.github.skare69.rainbowclock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.apa102.Apa102;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class ClockActivity extends Activity {
    private static final String TAG = ClockActivity.class.getSimpleName();

    private static final String I2C_BUS = "I2C1";
    private static final String SPI_BUS = "SPI0.0";
    private static final int LEDSTRIP_BRIGHTNESS = 1;
    public static final int LED_STRIP_LENGTH = 7;
    public static final float SEGMENT_DISPLAY_BRIGTHNESS = .5f;
    private AlphanumericDisplay mSegmentDisplay;
    private Apa102 ledStrip;

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.GERMANY);
    private final SimpleDateFormat simpleDateFormatMinute = new SimpleDateFormat("mm", Locale.GERMANY);

    BroadcastReceiver broadcastReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAlphanumericDisplay();
        setupLedStrip();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    updateAlphanumericDisplay();
                    updateLedStrip();
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    private void setupLedStrip() {
        try {
            ledStrip = new Apa102(SPI_BUS, Apa102.Mode.BGR);
            ledStrip.setBrightness(LEDSTRIP_BRIGHTNESS);

            updateLedStrip();
        } catch (IOException e) {
            Log.e(TAG, "Cannot initialize led strip", e);
            ledStrip = null;
        }
    }

    private void updateLedStrip() {
        try {
            String timeMinutes = simpleDateFormatMinute.format(GregorianCalendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).getTime());
            Integer minutes = Integer.valueOf(timeMinutes);
            // The 60 seconds of a minute have to be written as a double here, otherwise the result
            // of the equation would be an integer as well and e.g. 0.001 would be 0 instead of 1.
            int partsOfHour = (int)Math.ceil(minutes / (60.00 / LED_STRIP_LENGTH));

            int[] rainbow = new int[LED_STRIP_LENGTH];
            for (int i = LED_STRIP_LENGTH-1; i >= LED_STRIP_LENGTH-partsOfHour; i--) {
                float[] hsv = {i * 360.f / rainbow.length, 1.0f, 1.0f};
                rainbow[i] = Color.HSVToColor(255, hsv);
            }

            ledStrip.write(rainbow);
        } catch (IOException e) {
            Log.e(TAG, "Error writing to led strip", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyAlphanumericDisplay();
        destroyLedStrip();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
    }

    private void destroyLedStrip() {
        if (ledStrip != null) {
            Log.i(TAG, "Disabling led strip");
            try {
                ledStrip.setBrightness(0);
                ledStrip.write(new int[LED_STRIP_LENGTH]);
                ledStrip.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling led strip", e);
            } finally {
                ledStrip = null;
            }
        }
    }

    private void setupAlphanumericDisplay() {
        try {
            mSegmentDisplay = new AlphanumericDisplay(I2C_BUS);
            mSegmentDisplay.setBrightness(SEGMENT_DISPLAY_BRIGTHNESS);
            mSegmentDisplay.setEnabled(true);
            mSegmentDisplay.clear();

            updateAlphanumericDisplay();
        } catch (IOException e) {
            Log.e(TAG, "Error configuring display", e);
        }
    }

    private void updateAlphanumericDisplay() {
        try {
            String time = simpleDateFormat.format(GregorianCalendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).getTime()).replace(":", ".");
            mSegmentDisplay.display(time);
        } catch (IOException e) {
            Log.e(TAG, "Error configuring display", e);
        }
    }

    private void destroyAlphanumericDisplay() {
        if (mSegmentDisplay != null) {
            Log.i(TAG, "Closing display");
            try {
                mSegmentDisplay.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing display", e);
            } finally {
                mSegmentDisplay = null;
            }
        }
    }

}
