package com.qsp.player.game;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.qsp.player.R;

import java.io.File;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;
import uk.co.senab.photoview.PhotoViewAttacher;


/**
 * Show an image called by "VIEW" keyword from a game.
 */
public class QspImageBox extends Activity implements OnClickListener {

    boolean isBtnClosed;
    static boolean helpShowed;
    static boolean basicHelpShowed;
    PhotoViewAttacher mAttacher;

    private OnClickListener zoomClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (isBtnClosed) {
                finish();
                return;
            }
            zoom.setImageResource(R.drawable.ic_close);
            box.setOnClickListener(null);
            mAttacher = new PhotoViewAttacher(box);
            if (!helpShowed) {
                Toast.makeText(QspImageBox.this,getString(R.string.scaleImgHelp1), Toast.LENGTH_LONG).show();
                helpShowed=true;
            }
            isBtnClosed=true;
        }
    };
    private ImageButton zoom;
    private ImageView box;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        //set layout
        setContentView(R.layout.image_box);

        //set image click listener to activity click listener
        box = (ImageView) findViewById(R.id.imagebox);
        zoom = (ImageButton) findViewById(R.id.zoom);
        zoom.setOnClickListener(zoomClick);
        box.setOnClickListener(this);

        //load image
        Bundle b = this.getIntent().getExtras();
        String file = b.getString("imageboxFile");
        File path = new File(file);
        Drawable drawable = null;
        if (!path.exists()) finish();

        try {
            drawable = Drawable.createFromPath(path.getCanonicalPath());
            if (path.getName().toLowerCase().endsWith(".gif")) {
                drawable = new GifDrawable(path.getCanonicalPath());
            } else {
                drawable = Drawable.createFromPath(path.getCanonicalPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (drawable == null) {
            finish();
            return;
        }
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

        //assign to view
        box.setImageDrawable(drawable);
        if (!basicHelpShowed) {
            Toast.makeText(QspImageBox.this,getString(R.string.scaleImgHelp2), Toast.LENGTH_LONG).show();
            basicHelpShowed = true;
        }
    }

    @Override
    public void onClick(View arg0) {
        //Closed by any click
        finish();
    }
}
