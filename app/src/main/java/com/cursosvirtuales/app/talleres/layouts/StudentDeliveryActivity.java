package com.cursosvirtuales.app.talleres.layouts;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.talleres.TallerActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

public class StudentDeliveryActivity extends TallerActivity {
    private EditText name, code, email, course, activity, repository;
    private android.content.SharedPreferences saved;
    private EditText comments;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.cl_activity_student_delivery);
        name = findViewById(R.id.cl_student_name); code = findViewById(R.id.cl_student_code);
        email = findViewById(R.id.cl_student_email); course = findViewById(R.id.cl_course);
        activity = findViewById(R.id.cl_activity_name); repository = findViewById(R.id.cl_repository_url);
        comments = findViewById(R.id.cl_comments);
        saved = getSharedPreferences("delivery_" + new com.cursosvirtuales.app.data.SessionManager(this).currentUserId(), MODE_PRIVATE);
        EditText[] fields = {name, code, email, course, activity, repository, comments};
        String[] keys = {"name", "code", "email", "course", "activity", "repository", "comments"};
        for (int i = 0; i < fields.length; i++) fields[i].setText(saved.getString(keys[i], ""));
        findViewById(R.id.cl_register).setOnClickListener(v -> register());
    }
    private void register() {
        EditText[] required = {name, code, email, course, activity, repository};
        for (EditText field : required) if (field.getText().toString().trim().isEmpty()) {
            field.setError(getString(R.string.cl_required_field)); field.requestFocus(); return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.getText().toString().trim()).matches()) {
            email.setError(getString(R.string.cl_invalid_email)); email.requestFocus(); return;
        }
        String url = repository.getText().toString().trim();
        android.net.Uri uri = android.net.Uri.parse(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !"github.com".equalsIgnoreCase(uri.getHost())
                || uri.getPath() == null || uri.getPath().split("/").length < 3) {
            repository.setError(getString(R.string.cl_invalid_url)); repository.requestFocus(); return;
        }
        EditText[] fields = {name, code, email, course, activity, repository, comments};
        String[] keys = {"name", "code", "email", "course", "activity", "repository", "comments"};
        android.content.SharedPreferences.Editor editor = saved.edit();
        for (int i = 0; i < fields.length; i++) editor.putString(keys[i], fields[i].getText().toString().trim());
        editor.putLong("saved_at", System.currentTimeMillis()).apply();
        Toast.makeText(this, R.string.cl_delivery_registered, Toast.LENGTH_LONG).show();
    }
}
