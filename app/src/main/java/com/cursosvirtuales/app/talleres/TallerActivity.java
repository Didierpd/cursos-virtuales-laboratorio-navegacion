package com.cursosvirtuales.app.talleres;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.cursosvirtuales.app.data.SessionManager;
import com.cursosvirtuales.app.ui.LoginActivity;

/** Los talleres comparten la sesión y la navegación de la app principal. */
public abstract class TallerActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    @Override protected void onResume() {
        super.onResume();
        SessionManager session = new SessionManager(this);
        if (!session.isSessionValid()) {
            startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        } else session.touch();
    }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
