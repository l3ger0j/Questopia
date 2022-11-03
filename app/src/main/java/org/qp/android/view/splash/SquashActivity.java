package org.qp.android.view.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.qp.android.R;
import org.qp.android.view.stock.StockActivity;

import java.util.Objects;

public class SquashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        makeFullScreen();

        setContentView(R.layout.activity_squash);

        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        findViewById(R.id.imageView3).startAnimation(pulse);

        Handler handler = new Handler();
        handler.postDelayed((Runnable) () -> {
            startActivity(new Intent(this, StockActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2000);
    }

    private void makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        Objects.requireNonNull(getSupportActionBar()).hide();
    }
}
