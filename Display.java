package com.xavi.RuSafe;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class Display extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display);

		TextView textView = findViewById(R.id.contactTextView);
		SharedPreferences prefs = getSharedPreferences("EmergencyContacts", MODE_PRIVATE);
		int count = prefs.getInt("Contact_Count", 0);
		if (count == 0) {
			textView.setText("No contacts saved.");
			return;
		}

		StringBuilder contacts = new StringBuilder();
		for (int i = 0; i < count; i++) {
			String name = prefs.getString("ContactName_" + i, "N/A");
			String number = prefs.getString("ContactNumber_" + i, "N/A");
			contacts.append("Name: ").append(name).append("\nNumber: ").append(number).append("\n\n");
		}

		textView.setText(contacts.toString());
	}
}
