package com.qsp.player.game;

import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Dictionary;
import java.util.HashMap;

import pl.droidsonroids.gif.GifDrawable;

class QspImageGetter implements ImageGetter {
    private String mDirectory;
    private int mScreenWidth;
    private int mScreenHeight;
    private boolean mFullSize;
    private float mDensity;

    private HashMap<String, Drawable> cache = new HashMap<>();
    private boolean cacheInit;

    public QspImageGetter() {
        initCache();
    }

    private void initCache() {
        if (TextUtils.isEmpty(mDirectory)) return;
        if (cacheInit) return;
        File path = new File(mDirectory.concat("img_cache.txt")); // Fallback
        if (!path.exists()) return;
        InputStream instream = null;
        try {
// open the file for reading
            instream = new FileInputStream(path);

// if file the available for reading
            if (instream != null) {
                // prepare the file for reading
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);

                String line;

                // read every line of the file into the line-variable, on line at the time
                do {
                    line = buffreader.readLine();
                    cache.put(line, null);
                    // do something with the line
                } while (line != null);
                instream.close();
            }
        } catch (Exception ex) {
            // print stack trace.
        } finally {
        }

        cacheInit = true;
    }

    void setDirectory(String directory) {
        mDirectory = directory;
        initCache();
    }

    void setScreenWidth(int width) {
        mScreenWidth = width;
    }

    void setScreenHeight(int height) {
        mScreenHeight = height;
    }

    void setFullSize(boolean fullSize) {
        mFullSize = fullSize;
    }

    void setDensity(float density) {
        this.mDensity = density;
    }

    @Override
    public Drawable getDrawable(String source) {
        Drawable drawable = null;
        if ((source != null) && (!source.contains("://"))) {

            try {
                boolean haveRequestedSize = false;
                int widthRequest = 0;
                int heightRequest = 0;
                if (source.contains("?size")) {
                    int index = source.indexOf("?size");
                    String size = source.substring(index + 6);
                    source = source.substring(0, index);
                    if (!TextUtils.isEmpty(size)) {
                        String[] sizes = size.split("x");
                        if (sizes.length == 2) {
                            haveRequestedSize = true;
                            widthRequest = Integer.parseInt(sizes[0]);
                            heightRequest = Integer.parseInt(sizes[1]);
                        }
                    }
                }

                boolean needToCache = false;
                boolean fromCache = false;
                if (cache.containsKey(source)) {
                    drawable = cache.get(source);
                    if (drawable == null) needToCache = true;
                    else fromCache=true;
                }


                if (drawable == null) {

                    File path = new File(mDirectory.concat(source)); // Fallback

                    boolean isGif = path.getName().toLowerCase().endsWith(".gif");

                    // Even though File.exists may be case insensitive, for some weird reason
                    // the Drawable.createFromPath may be case sensitive and will fail.
                    // So, we're searching for the resource here no matter what path.exists() may return.
                    if (path.exists()) {
                        drawable = getDrawableInternal(path, isGif);
                        if (drawable == null) {
                            File curPath = new File(mDirectory);
                            String[] segments = source.split("[/\\\\]");
                            boolean found = false;
                            for (int i = 0; i < segments.length; i++) {
                                String segment = segments[i];
                                boolean lastSegment = i == segments.length - 1;
                                found = false;
                                for (File f : curPath.listFiles()) {
                                    if ((lastSegment ? f.isFile() : f.isDirectory())
                                            && f.getName().equalsIgnoreCase(segment)) {
                                        found = true;
                                        curPath = f;
                                        break;
                                    }
                                }
                            }
                            if (found && curPath.exists()) {
                                path = curPath;

                                if (path.exists()) {
                                    drawable = getDrawableInternal(path, isGif);
                                }
                            }
                        }
                    }
                }


                if (drawable != null) {

                    if (fromCache) drawable = drawable.mutate().getConstantState().newDrawable();

                    int iw = drawable.getIntrinsicWidth();
                    int ih = drawable.getIntrinsicHeight();

                    if (needToCache) cache.put(source, drawable);
                    if (!cache.containsKey(source) && iw <= 25 && ih <= 25)
                        cache.put(source, drawable);

                    int nWidth = (int) (haveRequestedSize ? widthRequest : iw * mDensity);
                    int nHeight = (int) (haveRequestedSize ? heightRequest : ih * mDensity);

/* Original image size correction
                    if (mFullSize || nWidth > mScreenWidth) {
                        float k = mScreenWidth;
                        k = k / nWidth;
                        nWidth = mScreenWidth;
                        nHeight = (int) (nHeight * k);
                    }

                    if (mFullSize || nHeight > mScreenHeight/2) {
                        float kh = mScreenHeight;
                        kh = kh / nHeight;
                        nHeight = mScreenHeight/2;
                        nWidth = (int) (nWidth * kh);
                    }
*/

                    if (mFullSize || nWidth > mScreenWidth) {
                        float k = mScreenWidth;
                        k = k / nWidth;
                        nWidth = mScreenWidth;
                        nHeight = (int) (nHeight * k);
                        if (nHeight > mScreenHeight) {
                            float kh = mScreenHeight;
                            kh = kh / nHeight;
                            nHeight = mScreenHeight;
                            nWidth = (int) (nWidth * kh);
                        }
                    } else if (mFullSize || nHeight > mScreenHeight) {
                        float kh = mScreenHeight;
                        kh = kh / nHeight;
                        nHeight = mScreenHeight;
                        nWidth = (int) (nWidth * kh);
                    }


                    drawable.setBounds(0, 0, nWidth, nHeight);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return drawable;
    }

    private Drawable getDrawableInternal(File path, boolean isGif) throws IOException {
        Drawable drawable;
        if (isGif) {
            drawable = new GifDrawable(path.getCanonicalPath());
        } else {
            drawable = Drawable.createFromPath(path.getCanonicalPath());
        }
        return drawable;
    }
}
