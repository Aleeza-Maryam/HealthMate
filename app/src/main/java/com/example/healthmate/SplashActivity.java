package com.example.healthmate;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 3000; // 3 seconds
    private Handler handler = new Handler();
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logoImage);


        // Animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 1f, 1.1f, 1f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(logo, "rotation", 0f, 6f, 0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, rotate);
        animatorSet.setDuration(4000);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();

        runnable = new Runnable() {
            @Override
            public void run() {
                openMain();
            }
        };
        handler.postDelayed(runnable, SPLASH_TIME);


    }

    private void openMain() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
