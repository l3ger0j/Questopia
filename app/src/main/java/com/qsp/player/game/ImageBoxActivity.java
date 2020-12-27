package com.qsp.player.game;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.qsp.player.R;

import java.io.File;

public class ImageBoxActivity extends AppCompatActivity {

    private final ImageProvider imageProvider = new ImageProvider();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_box);

        Intent intent = getIntent();

        String gameDirUri = intent.getStringExtra("gameDirUri");
        imageProvider.setGameDirectory(new File(gameDirUri));

        String imagePath = intent.getStringExtra("imagePath");
        Drawable drawable = imageProvider.getDrawable(imagePath);
        initImageView(drawable);
    }

    private void initImageView(Drawable drawable) {
        ImageView imageView = findViewById(R.id.imagebox);
        imageView.setImageDrawable(drawable);
        imageView.setOnClickListener(v -> finish());
    }
}
