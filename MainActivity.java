package com.xavi.RuSafe;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognizerIntent;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
	private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
	private FusedLocationProviderClient fusedLocationClient;
	private TextView countdownTextView;
	private Button startTimerButton, cancelTimerButton, emergencyButton;
	private ImageButton voiceCommandButton;
	private EditText minutesEditText;
	private CountDownTimer countDownTimer;
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private ShakeDetector shakeDetector;
	private VolumeButtonReceiver volumeButtonReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

		countdownTextView = findViewById(R.id.countdown_text);
		startTimerButton = findViewById(R.id.start_timer_button);
		cancelTimerButton = findViewById(R.id.cancel_timer_button);
		emergencyButton = findViewById(R.id.emergencyButton);
		minutesEditText = findViewById(R.id.timer_minutes);
		voiceCommandButton = findViewById(R.id.voiceCommandButton);

		setupButtons();
		initSensors();

		volumeButtonReceiver = new VolumeButtonReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_BUTTON);
		registerReceiver(volumeButtonReceiver, filter);

		handleIntent(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		if ("com.xavi.RuSafe.ACTION_TRIGGER_SOS".equals(intent.getAction())) {
			sendLocationToContacts();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (volumeButtonReceiver != null) {
			unregisterReceiver(volumeButtonReceiver);
		}
	}

	private void setupButtons() {
		startTimerButton.setOnClickListener(v -> {
			int minutes = !isEmpty(minutesEditText.getText().toString()) ? Integer.parseInt(minutesEditText.getText().toString()) : 0;
			long duration = minutes * 60 * 1000; // Convert minutes to milliseconds
			startSafetyTimer(duration);
		});

		cancelTimerButton.setOnClickListener(v -> {
			if (countDownTimer != null) {
				countDownTimer.cancel();
				countdownTextView.setText("00:00");
			}
		});

		voiceCommandButton.setOnClickListener(v -> startVoiceInput());

		emergencyButton.setOnClickListener(v -> sendLocationToContacts());
	}

	private void initSensors() {
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		shakeDetector = new ShakeDetector();
		shakeDetector.setOnShakeListener(count -> sendLocationToContacts());
	}

	private void startSafetyTimer(long timeInMillis) {
		countDownTimer = new CountDownTimer(timeInMillis, 1000) {
			public void onTick(long millisUntilFinished) {
				countdownTextView.setText("Time remaining: " + millisUntilFinished / 1000 + " seconds");
			}

			public void onFinish() {
				countdownTextView.setText("00:00");
				sendLocationToContacts();  // Call to send location automatically
			}
		}.start();
	}

	private void sendLocationToContacts() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS}, 1);
			return;
		}

		fusedLocationClient.getLastLocation()
				.addOnSuccessListener(this, location -> {
					if (location != null) {
						double latitude = location.getLatitude();
						double longitude = location.getLongitude();
						sendSmsToEmergencyContacts(latitude, longitude);
					} else {
						Log.e("LocationError", "Failed to retrieve location.");
					}
				});
	}

	private void sendSmsToEmergencyContacts(double latitude, double longitude) {
		SmsManager smsManager = SmsManager.getDefault();
		SharedPreferences prefs = getSharedPreferences("EmergencyContacts", MODE_PRIVATE);
		int count = prefs.getInt("Contact_Count", 0);

		String message = "I need help! My location coordinates are: Latitude: " + latitude + ", Longitude: " + longitude;
		boolean success = true;

		for (int i = 0; i < count; i++) {
			String number = prefs.getString("ContactNumber_" + i, "");
			if (!number.isEmpty()) {
				try {
					smsManager.sendTextMessage(number, null, message, null, null);
				} catch (Exception e) {
					Log.e("SMS Error", "Failed to send SMS to " + number, e);
					success = false;
				}
			}
		}

		if (success) {
			Toast.makeText(getApplicationContext(), "Location sent to all emergency contacts.", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getApplicationContext(), "Failed to send location to some or all contacts.", Toast.LENGTH_LONG).show();
		}
	}

	private void startVoiceInput() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");
		try {
			startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
		} catch (ActivityNotFoundException a) {
			Toast.makeText(getApplicationContext(), "Speech recognition not supported", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
			if (resultCode == RESULT_OK && data != null) {
				ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				String command = result.get(0).toLowerCase();
				if (command.contains("help")) {
					sendLocationToContacts();
				}
			}
		}
	}

	private boolean isEmpty(String str) {
		return str == null || str.trim().isEmpty();
	}

	public void register(View view) {
		Intent intent = new Intent(this, Register.class); // Use Register instead of RegisterActivity
		startActivity(intent);
	}

	public void instruct(View view) {
		Intent intent = new Intent(this, Instructions.class);
		startActivity(intent);
	}

	public void display_no(View view) {
		Intent intent = new Intent(this, Display.class);
		startActivity(intent);
	}
}
