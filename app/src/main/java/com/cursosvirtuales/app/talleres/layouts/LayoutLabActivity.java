package com.cursosvirtuales.app.talleres.layouts;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.talleres.TallerActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class LayoutLabActivity extends TallerActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.cl_activity_main);
        int[] buttons = {R.id.cl_exercise1, R.id.cl_exercise2, R.id.cl_exercise3, R.id.cl_exercise4};
        for (int i = 0; i < buttons.length; i++) {
            final int exercise = i + 1;
            findViewById(buttons[i]).setOnClickListener(v -> startActivity(
                    new Intent(this, ExerciseActivity.class).putExtra(ExerciseActivity.EXTRA_EXERCISE, exercise)));
        }
        findViewById(R.id.cl_email).setOnClickListener(v -> startActivity(new Intent(this, EmailActivity.class)));
        findViewById(R.id.cl_delivery).setOnClickListener(v -> startActivity(new Intent(this, StudentDeliveryActivity.class)));
    }
}
