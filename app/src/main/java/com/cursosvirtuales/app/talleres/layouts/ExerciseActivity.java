package com.cursosvirtuales.app.talleres.layouts;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.talleres.TallerActivity;

import android.app.Activity;
import android.os.Bundle;

public class ExerciseActivity extends TallerActivity {
    public static final String EXTRA_EXERCISE = "exercise";
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        int exercise = getIntent().getIntExtra(EXTRA_EXERCISE, 1);
        int[] layouts = {R.layout.cl_exercise_1, R.layout.cl_exercise_2, R.layout.cl_exercise_3, R.layout.cl_exercise_4};
        setContentView(layouts[Math.max(1, Math.min(4, exercise)) - 1]);
        setTitle("Ejercicio " + exercise);
    }
}
