package com.qsp.player;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.qsp.player.service.GameContentResolver;
import com.qsp.player.service.ImageProvider;

public class ImageBoxActivity extends AppCompatActivity {
    private GameContentResolver gameContentResolver;
    private ImageProvider imageProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        initServices();
        initImageView();
    }

    private void initServices() {
        QuestPlayerApplication application = (QuestPlayerApplication) getApplication();
        gameContentResolver = application.getGameContentResolver();
        imageProvider = application.getImageProvider();
    }

    private void initImageView() {
        String relPath = getIntent().getStringExtra("imagePath");
        String absPath = gameContentResolver.getAbsolutePath(relPath);

        Drawable drawable = imageProvider.get(absPath);
        if (drawable == null) return;

        ImageView imageView = findViewById(R.id.imagebox);
        imageView.setImageDrawable(drawable);
        imageView.setOnClickListener(v -> finish());
    }
}
