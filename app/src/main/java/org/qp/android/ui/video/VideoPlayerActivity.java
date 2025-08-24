package org.qp.android.ui.video;

import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URI = "video_uri";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create VideoView programmatically (you can also use a layout file if you prefer)
        VideoView videoView = new VideoView(this);
        setContentView(videoView);

        // Get video URI from intent extras
        String uriString = getIntent().getStringExtra(EXTRA_VIDEO_URI);
        if (uriString != null) {
            Uri videoUri = Uri.parse(uriString);
            videoView.setVideoURI(videoUri);

            // Optional: add media controls (play/pause/seek bar)
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);

            videoView.start();
        }
    }
}
