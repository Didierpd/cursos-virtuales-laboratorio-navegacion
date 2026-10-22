package com.cursosvirtuales.app.talleres.layouts;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.talleres.TallerActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

public class EmailActivity extends TallerActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.cl_activity_email);
        EditText email = findViewById(R.id.cl_email_address);
        EditText subject = findViewById(R.id.cl_subject);
        EditText message = findViewById(R.id.cl_message);
        android.content.SharedPreferences saved = getSharedPreferences("email_draft_" +
            new com.cursosvirtuales.app.data.SessionManager(this).currentUserId(), MODE_PRIVATE);
        email.setText(saved.getString("email", ""));
        subject.setText(saved.getString("subject", ""));
        message.setText(saved.getString("message", ""));
        findViewById(R.id.cl_send).setOnClickListener(v -> {
            String address = email.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(address).matches()) { email.setError(getString(R.string.cl_email_required)); email.requestFocus(); return; }
            if (subject.getText().toString().trim().isEmpty()) { subject.setError(getString(R.string.cl_subject_required)); subject.requestFocus(); return; }
            if (message.getText().toString().trim().isEmpty()) { message.setError(getString(R.string.cl_message_required)); message.requestFocus(); return; }
            saved.edit().putString("email", address).putString("subject", subject.getText().toString())
                .putString("message", message.getText().toString()).apply();
            Toast.makeText(this, R.string.cl_email_sent, Toast.LENGTH_LONG).show();
        });
    }
}
