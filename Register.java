package com.xavi.RuSafe;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Register extends Activity {
	private static final String PREFS_NAME = "EmergencyContacts";
	private static final int MAX_CONTACTS = 5;
	private EditText editTextName, editTextNumber;
	private Button buttonSave;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);

		editTextName = findViewById(R.id.editTextName);
		editTextNumber = findViewById(R.id.editTextNumber);
		buttonSave = findViewById(R.id.buttonSave);

		buttonSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveContact();
			}
		});
	}

	private void saveContact() {
		String name = editTextName.getText().toString().trim();
		String number = editTextNumber.getText().toString().trim();

		if (name.isEmpty() || number.isEmpty()) {
			Toast.makeText(this, "Name or number cannot be empty", Toast.LENGTH_SHORT).show();
			return;
		}

		SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		int count = prefs.getInt("Contact_Count", 0);

		if (count >= MAX_CONTACTS) {
			Toast.makeText(this, "Maximum of 5 contacts can be saved", Toast.LENGTH_SHORT).show();
			return;
		}

		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("ContactName_" + count, name);
		editor.putString("ContactNumber_" + count, number);
		editor.putInt("Contact_Count", count + 1);
		editor.apply();

		Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show();
		editTextName.setText("");
		editTextNumber.setText("");
	}
}

