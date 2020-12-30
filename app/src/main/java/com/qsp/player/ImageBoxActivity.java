package com.qsp.player;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.qsp.player.common.ImageProvider;

public class ImageBoxActivity extends AppCompatActivity {
    private ImageProvider imageProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        initImageProvider();
        initImageView();
    }

    private void initImageProvider() {
        imageProvider = ((QuestPlayerApplication) getApplication()).getImageProvider();
    }

    private void initImageView() {
        String imagePath = getIntent().getStringExtra("imagePath");
        Drawable drawable = imageProvider.getDrawable(imagePath);

        ImageView imageView = findViewById(R.id.imagebox);
        imageView.setImageDrawable(drawable);
        imageView.setOnClickListener(v -> finish());
    }
}
