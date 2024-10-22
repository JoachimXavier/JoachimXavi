package com.xavi.RuSafe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.Toast;

public class VolumeButtonReceiver extends BroadcastReceiver {
    private static int volumeButtonCount = 0;
    private static long lastPressTime = 0;
    private static final long MAX_DURATION = 3000; // Time window in milliseconds

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    long pressTime = System.currentTimeMillis();
                    if (pressTime - lastPressTime <= MAX_DURATION) {
                        volumeButtonCount++;
                        if (volumeButtonCount == 3) { // Number of required presses
                            triggerEmergencyAlert(context);
                            volumeButtonCount = 0;
                        }
                    } else {
                        volumeButtonCount = 1; // Reset count if time duration exceeds
                    }
                    lastPressTime = pressTime;
                }
            }
        }
    }

    private void triggerEmergencyAlert(Context context) {
        // Trigger your emergency actions here
        Toast.makeText(context, "Emergency Alert Activated!", Toast.LENGTH_LONG).show();
        // Send an intent to MainActivity to trigger the emergency action
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("com.xavi.RuSafe.ACTION_TRIGGER_SOS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
