package org.qp.android.view.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.qp.android.R;
import org.qp.android.view.stock.StockActivity;

public class SquashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        makeFullScreen();

        setContentView(R.layout.activity_squash);

        var pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        findViewById(R.id.imageView3).startAnimation(pulse);

        var handler = new Handler();
        handler.postDelayed(() -> {
            startActivity(new Intent(this, StockActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        } , 2000);
    }

    private void makeFullScreen() {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }
}
