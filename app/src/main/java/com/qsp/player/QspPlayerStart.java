package com.qsp.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.gesture.GestureStroke;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import androidx.core.content.PermissionChecker;
import androidx.documentfile.provider.DocumentFile;

import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import static android.content.ContentValues.TAG;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.webkit.WebView.HitTestResult.IMAGE_TYPE;


public class QspPlayerStart extends Activity implements UrlClickCatcher, OnGesturePerformedListener, Drawable.Callback {

    public static final int SWIPE_MIN = 120;
    public static final int WIN_INV = 0;
    public static final int WIN_MAIN = 1;
    public static final int WIN_EXT = 2;
    public static final int SLOTS_MAX = 5;
    public static final int ACTIVITY_SELECT_GAME = 0;

    public static final int QSP_MAXACTIONS = 50;
    Resources res;
    SharedPreferences settings;

    private Menu menuMain;

    private QSPListAdapter mActListAdapter = null;
    private QSPListAdapter mItemListAdapter = null;

    boolean invUnread, varUnread;
    int invBack, varBack;
    int currentWin;
    private boolean holdPanelAnimationsForFirstUpdate = false;

    final private Activity myActivity = this;
    final private Context uiContext = this;
    final private ReentrantLock musicLock = new ReentrantLock();

    private boolean gui_debug_mode = true;
    private boolean hotKeys = false;
    private boolean highlightActs = true;
    private boolean sdcard_mounted = false;
    private boolean backAction = false;
    private boolean bigImage = false;
    private boolean videoSwitch = false;
    private boolean hideImg = false;
    private boolean imageDensity;
    private boolean clearOnChange = false;
    private int origOrient;
    private boolean returnOrientation = false;

    private String userSetLang;
    private String curLang = Locale.getDefault().getLanguage();

    private boolean startingUpQSP = true;
    private boolean orientationRecreate = false;

    private WebView vars_desc;
    private WebView main_desc; // ** changed from TextView

    private static boolean isMainDescHTML = false;
    private static String curMainDescHTML = "";

    private static boolean isVarsDescHTML = false;
    private static String curVarsDescHTML = "";

    private boolean cheatsActive = false;
    private String cheatDesc = "INPUT COMMAND";
    private String cheatAction = "dynamic input('"+cheatDesc+"')";
    private String cheatActionTemplate = "dynamic input('REPLACETEXT')";

    private int maxW = 0;
    private int maxH = 0;
    private float playerHeightLimit = 1/2;

    private static String defaultQSPtextColor = "#ffffff";
    private static String defaultQSPbackColor = "#000000";
    private static String defaultQSPlinkColor = "#0000ee";
    private static String defaultQSPactsColor = "#ffffd7";
    private static String defaultQSPfontSize = "16";
    private static String defaultQSPfontStyle = "DEFAULT";
    private static String defaultQSPfontTheme = "0";
    private static String defaultQSPobjectFit = "scale-down";
    private static String QSPobjectFitBig = "contain";

    private static String QSPtextColor = defaultQSPtextColor;
    private static String QSPbackColor = defaultQSPbackColor;
    private static String QSPlinkColor = defaultQSPlinkColor;
    private static String QSPactsColor = defaultQSPactsColor;
    private static String QSPfontSize = defaultQSPfontSize;
    private static String QSPfontStyle = defaultQSPfontStyle;
    private static String QSPfontTheme = defaultQSPfontTheme;
    private static String QSPobjectFit = defaultQSPobjectFit;

    public static String freshPageHeadTemplate = "<html><head>"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, "
            + "minimum-scale=1, maximum-scale=1\">"
            + "<style type=\"text/css\">"
            + "body{margin: 0; padding: 0; color: QSPTEXTCOLOR; background-color: QSPBACKCOLOR; "
            + "max-width: QSPMAXWIDTH; "
            + "font-size: QSPFONTSIZE; font-family: QSPFONTSTYLE; unusedtag=\"\"} "
            + "a{color: QSPLINKCOLOR} "
            + "a:link{color: QSPLINKCOLOR} "
            + "hr{max-width: QSPMAXWIDTH} "
            + "video{object-fit: QSPOBJECTFIT; pointer-events:none; }"
            + "table{font-size: QSPFONTSIZE; font-family: QSPFONTSTYLE; } "
            + "</style></head>";
    public static String freshPageBodyTemplate = "<body>REPLACETEXT</body></html>";
    public static String curHtmlHead = freshPageHeadTemplate;

    public static String freshPageURL = "<html><head>"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, "
            + "minimum-scale=1, maximum-scale=1\">"
            + "<style type=\"text/css\">"
            + "body{margin: 0; padding: 0; color: " + QSPtextColor + "; background-color: " + QSPbackColor + "; "
            + "max-width: 100%; "
            + "font-size: " + QSPfontSize + "; font-family: " + QSPfontStyle + "; }"
            + "a{color: "+ QSPlinkColor +"; }"
            + "a:link{color: "+ QSPlinkColor +"; }"
            + "video{object-fit: "+QSPobjectFit+"; pointer-events:none; }"
            + "hr{max-width: 100%; }"
            + "table{font-size: " + QSPfontSize + "; font-family: " + QSPfontStyle + "; }"
            + "</style></head>"
            + freshPageBodyTemplate;

    public static String autoplayURL = "javascript: (function() {"+
            "var allVideos = document.getElementsByTag(\"video\"); " +
            "for (var i = 0; i < allVideos.length; i++) {" +
            "allVideos[i].removeAttribute('controls'); allVideos[i].play(); " +
            "} })()";

    //used to detect and parse "exec:" commands
    private QSPWebViewClient main_descClient;
    private QSPWebViewClient vars_descClient;

    public static String getMainDescHTML () {
        return curMainDescHTML;
    }
    public void setMainDescHTML (String newHTMLString) {
        curMainDescHTML = newHTMLString;
    }
    public static String getVarDescHTML () {
        return curVarsDescHTML;
    }
    public void setVarDescHTML (String newHTMLString) { curVarsDescHTML = newHTMLString;
    }

    private void setQSPLocale (String lang) {
        Locale myLocale;
//        Utility.WriteLog("Base Language = "+lang);

        //if TAIWAN
        if (lang.equals("zh-rTW")) {
            myLocale = Locale.TAIWAN;
//            Utility.WriteLog("Language = TAIWAN, "+lang);
        }
        //if CHINA
        else if (lang.equals("zh-rCN")) {
            myLocale = Locale.CHINA;
//            Utility.WriteLog("Language = CHINA, "+lang);
        }
        //if lang doesn't contain a region code
        else if (!lang.contains("-r"))
            myLocale = new Locale(lang);
            //if lang is not TAIWAN, CHINA, or short, use country+region
        else {
            String myRegion = lang.substring(lang.indexOf("-r")+2);
            String myLang = lang.substring(0,2);
//            Utility.WriteLog("Language = "+myLang+", Region = "+myRegion);
            myLocale = new Locale(lang,myRegion);
        }
        Resources newRes = getResources();
        DisplayMetrics dm = newRes.getDisplayMetrics();
        Configuration conf = newRes.getConfiguration();
        conf.locale = myLocale;
        newRes.updateConfiguration(conf, dm);

    }

    public class QSPWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView myView, String url) {
//            main_desc.getSettings().setJavaScriptEnabled(true);
//            main_desc.loadUrl(autoplayURL);
//            main_desc.getSettings().setJavaScriptEnabled(false);
Utility.WriteLog("onPageFinished: "+url);
//            if (url.contains("<video")) {
//                main_desc.getSettings().setJavaScriptEnabled(true);
//                main_desc.loadUrl("javascript:(function() { var firstVideo = document.getElementsByTagName(\"video\")[0]; firstVideo.play(); })()");
//            } else main_desc.getSettings().setJavaScriptEnabled(false);

        }

            @Override
        public boolean shouldOverrideUrlLoading(WebView view, String href) {

                if (href.toLowerCase().startsWith("exec:")) {

                if (libraryThreadIsRunning) return true;
                final String code = href.substring(5);
                libThreadHandler.post(new Runnable() {
                    public void run() {
                        if (libraryThreadIsRunning) return;
                        libraryThreadIsRunning = true;

                        //Exec commands with special characters in WebView must be
                        //changed back to special characters
                        String tempCode = Utility.prepareForExec(code);

                        boolean bExec = QSPExecString(tempCode, true);
                        CheckQspResult(bExec, "OnUrlClicked: QSPExecString");

                        libraryThreadIsRunning = false;
                    }
                });

                return true;
            } else return false;
        }
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        main_desc.invalidate();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        main_desc.postDelayed(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        main_desc.removeCallbacks(what);
    }



    private class QSPItem {
        private Drawable icon;
        private CharSequence text;

        public QSPItem(Drawable i, CharSequence t) {
            icon = i;
            text = t;
        }

        public Drawable getIcon() {
            return icon;
        }

        public CharSequence getText() {
            return text;
        }
    }

    private class QSPListAdapter extends ArrayAdapter<QSPItem> {

        private QSPItem[] items;
        private int id;

        public QSPListAdapter(Context context, int resource, QSPItem[] acts) {
            super(context, resource, acts);
            this.items = acts;
            this.id = resource;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(id, null);
            }
            QSPItem o = items[position];
            if (o != null) {
                int textColor = Color.parseColor(QSPtextColor);
                if (id == R.layout.act_item) {
                    if (highlightActs)
                        textColor = Color.parseColor(QSPactsColor);
                    TextView nv = (TextView) v.findViewById(R.id.item_number);
                    nv.setVisibility(hotKeys ? View.VISIBLE : View.GONE);
                    if (hotKeys) {
                        if (nv != null) {
                            ApplyFontSettingsToTextView(nv, textColor);
                            nv.setText(new StringBuilder().append("[").append(position + 1).append("]").toString());
                        }
                    }
                }
                ImageView iv = (ImageView) v.findViewById(R.id.item_icon);
                TextView tv = (TextView) v.findViewById(R.id.item_text);
                if (iv != null) {
                    iv.setImageDrawable(o.getIcon());
                }
                if (tv != null) {
                    ApplyFontSettingsToTextView(tv, textColor);
                    tv.setText(o.getText());
                }
            }
            return v;
        }
    }

    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        //Контекст UI
        if (gesture.getLength() > SWIPE_MIN) {
            ArrayList<GestureStroke> strokes = gesture.getStrokes();
            float[] points = strokes.get(0).points;
            if (points[0] > points[points.length - 1]) {
                //swipe left
                if (currentWin > 0)
                    currentWin--;
                else
                    currentWin = 2;
            } else {
                if (currentWin < 2)
                    currentWin++;
                else
                    currentWin = 0;
            }
            setCurrentWin(currentWin);
        }
    }

    public QspPlayerStart() {
        //Контекст UI
        Utility.WriteLog("constructor\\");

        gameIsRunning = false;
        qspInited = false;
        waitForImageBox = false;
        startingUpQSP = true;

        //Создаем список для звуков и музыки
        mediaPlayersList = new Vector<MusicContent>();

        //Создаем список для всплывающего меню
        menuList = new Vector<QspMenuItem>();

        //Создаем объект для таймера
        timerHandler = new Handler();

        //Запускаем поток библиотеки
        StartLibThread();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utility.WriteLog("onCreate\\");
        //Контекст UI
        super.onCreate(savedInstanceState);

        settings = PreferenceManager.getDefaultSharedPreferences(this);


            setVolumeControlStream(AudioManager.STREAM_MUSIC);

            //будем использовать свой вид заголовка, поэтому на девайсах < 3.0 отключаем заголовок
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.main);
            res = getResources();

        if (res.getConfiguration().orientation == ORIENTATION_LANDSCAPE)
            hideTitle();

        userSetLang = settings.getString("lang", "en");
            curLang = userSetLang;
            setQSPLocale(userSetLang);

            main_desc = (WebView) findViewById(R.id.main_desc);
            vars_desc = (WebView) findViewById(R.id.vars_desc);

            main_descClient = new QSPWebViewClient();
            vars_descClient = new QSPWebViewClient();
            main_desc.setWebViewClient(main_descClient);
            vars_desc.setWebViewClient(vars_descClient);
            main_desc.setWebChromeClient(new WebChromeClient());
            vars_desc.setWebChromeClient(new WebChromeClient());
            main_desc.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            main_desc.getSettings().setMediaPlaybackRequiresUserGesture(false);
            main_desc.getSettings().setAppCacheEnabled(false);
            main_desc.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            main_desc.getSettings().setJavaScriptEnabled(false);
            main_desc.clearCache(true);

            float imgPerScreen = Float.parseFloat(settings.getString("imgHeight", "1"));
            playerHeightLimit = 1 / imgPerScreen;

            freshPageURL = freshPageHeadTemplate + freshPageBodyTemplate;

//            defaultQSPtextColor = res.getString(R.string.deftextColor);
//            defaultQSPbackColor = res.getString(R.string.defbackColor);
//            defaultQSPlinkColor = res.getString(R.string.deflinkColor);
//            defaultQSPactsColor = res.getString(R.string.defactsColor);
//            defaultQSPfontSize = res.getString(R.string.deffontsize);
            //defaultQSPfontStyle = res.getString(R.string.deftypeface);

            //used to detect LongClicks on images in main_desc or vars_desc
            main_desc.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    final WebView.HitTestResult result = main_desc.getHitTestResult();

                    if (result.getType() == IMAGE_TYPE) {
                        String imageSrc = result.getExtra().replace("file:///", "/");
//                    Utility.WriteLog(result.getExtra() + " -> " + imageSrc);
                        ShowPicture(imageSrc);
                        return true;
                    }

                    return true;
                }
            });

        vars_desc.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                final WebView.HitTestResult result = main_desc.getHitTestResult();

                if (result.getType() == IMAGE_TYPE) {
                    String imageSrc = result.getExtra().replace("file:///", "/");
//                    Utility.WriteLog(result.getExtra() + " -> " + imageSrc);
                    ShowPicture(imageSrc);
                    return true;
                }

                return true;
            }
        });


        //Создаем объект для обработки ссылок
            qspLinkMovementMethod = QspLinkMovementMethod.getQspInstance();
            qspLinkMovementMethod.setCatcher(this);

            //Создаем диалог ввода текста
            LayoutInflater factory = LayoutInflater.from(uiContext);
            View textEntryView = factory.inflate(R.layout.inputbox, null);
            inputboxDialog = new AlertDialog.Builder(uiContext)
                    .setView(textEntryView)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            EditText edit = (EditText) inputboxDialog.findViewById(R.id.inputbox_edit);
                            inputboxResult = edit.getText().toString();
                            edit.setText("");
                            dialogHasResult = true;
                            Utility.WriteLog(getString(R.string.inputBoxUIMsg));
                            setThreadUnpark();
                        }
                    })
                    .setCancelable(false)
                    .create();

            if (Utility.GetDownloadPath(this) == null) {
                sdcard_mounted = false;
                Utility.ShowError(uiContext, getString(R.string.SDCardNotConnected));

            } else {
                sdcard_mounted = true;
                if (!gameIsRunning) {
                    //текущий вид - основное описание
                    invBack = 0; //нет фона
                    varBack = 0; //нет фона
                }
            }
        setCurrentWin(currentWin = WIN_MAIN);
        ShowGameStock();
        if (savedInstanceState != null) {

        }
            Utility.WriteLog("onCreate/");
    }


    @Override
    public void onResume() {

            Utility.WriteLog("onResume\\");
        //Контекст UI
        super.onResume();

        //полноэкранный режим если надо
        updateFullscreenStatus(settings.getBoolean("fullscreen", false));

        //подключаем жесты
        GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
        gestures.removeAllOnGesturePerformedListeners();
        if (settings.getBoolean("gestures", false))
            gestures.addOnGesturePerformedListener(this);


        boolean settingsChanged = false;

        boolean newBigImage = settings.getBoolean("big_image", false);
        if (bigImage != newBigImage) settingsChanged = true;
        bigImage = newBigImage;

        if (bigImage) QSPobjectFit = QSPobjectFitBig;
        else QSPobjectFit = defaultQSPobjectFit;

        boolean newVideoSwitch = settings.getBoolean("videoSwitch",false);
        if (videoSwitch != newVideoSwitch) settingsChanged = true;
        videoSwitch = newVideoSwitch;

        boolean newHideImg = settings.getBoolean("hideImg",false);
        if (hideImg != newHideImg) settingsChanged = true;
        hideImg = newHideImg;

        float tempImgPerScreen = Float.parseFloat(settings.getString("imgHeight", "1"));
        if (playerHeightLimit != 1/tempImgPerScreen) settingsChanged = true;
Utility.WriteLog("playerHeightLimit: " + playerHeightLimit + ", tempImgPerScreen: "+tempImgPerScreen);

        boolean newCheats = settings.getString("cheats", " ").equals("uuddlrlrABs");
        if (cheatsActive != newCheats) settingsChanged = true;
        cheatsActive = newCheats;

                cheatDesc = getString(R.string.cheatDesc);
        cheatAction = cheatActionTemplate.replace("REPLACETEXT",cheatDesc);

        backAction = settings.getBoolean("back_action", false);
        hotKeys = settings.getBoolean("acts_hot_keys", false);
        imageDensity = settings.getBoolean("image_density", true);
        highlightActs = settings.getBoolean("highlight_acts", true);
        clearOnChange = settings.getBoolean("rotate_key", false);

        //Reset to default display values if requested
        if (settings.getBoolean("resetAll",false)) {
//Utility.WriteLog("resetAll? "+settings.getBoolean("resetAll",false));
            QSPtextColor = defaultQSPtextColor;
            QSPbackColor = defaultQSPbackColor;
            QSPlinkColor = defaultQSPlinkColor;
            QSPactsColor = defaultQSPactsColor;
            QSPfontSize = defaultQSPfontSize;
            QSPfontStyle = defaultQSPfontStyle;
            QSPfontTheme = defaultQSPfontTheme;

            SharedPreferences.Editor ed = settings.edit();
            ed.putBoolean("resetAll",false);
            ed.putInt("textColor",Color.parseColor(defaultQSPtextColor));
            ed.putInt("backColor",Color.parseColor(defaultQSPbackColor));
            ed.putInt("linkColor",Color.parseColor(defaultQSPlinkColor));
            ed.putInt("actsColor",Color.parseColor(defaultQSPactsColor));
            ed.putString("fontsize",defaultQSPfontSize);
            ed.putString("typeface",res.getString(R.string.deftypeface));
            ed.putString("theme",res.getString(R.string.deftheme));
            ed.apply();
        }
        //If not resetting to default, check for font color/theme changes
        else
            settingsChanged = ApplyFontTheme(settingsChanged);


        //Set the language if it has changed
        boolean langChanged = false;
        userSetLang = settings.getString("lang","en");
//        Utility.WriteLog("userSetLang = "+userSetLang+", curLang = "+curLang);
        if (!curLang.equals(userSetLang)) {
//            Utility.WriteLog(userSetLang+" <> "+curLang+", setting language");
            curLang = userSetLang;
            setQSPLocale(userSetLang);
            langChanged = true;
//            Utility.WriteLog(curLang+" <- "+userSetLang);
        }
//            Utility.WriteLog(userSetLang+" == "+curLang+", no change");

        //Here is where the display settings are applied
        SetImageLimits();
        ApplyViewSettings();
        if (settingsChanged) {
            RefreshMainDesc();
            RefreshVarsDesc();
        }

        if (sdcard_mounted && gameIsRunning && !waitForImageBox) {
            //Запускаем таймер
            timerHandler.postDelayed(timerUpdateTask, timerInterval);

            //Запускаем музыку
            PauseMusic(false);
        }
        waitForImageBox = false;

        //Refresh QspPlayerStart if the user changed the language
        if (langChanged) {
            setCurrentWin(currentWin);
            setTitle(R.string.app_name);
            startingUpQSP = false;
            invalidateOptionsMenu();
        }
/*
        Utility.WriteLog("startingUpQSP: "+startingUpQSP+", langChanged: "+langChanged);
        if ((!startingUpQSP) && (langChanged)) {
Utility.WriteLog("RECREATE*");
            //recreate();
Utility.WriteLog("RECREATE/");

Utility.WriteLog("FreeResources*");
            FreeResources();
Utility.WriteLog("FreeResources*");
            ShowGameStock();
        }
        else startingUpQSP = false;
*/
        Utility.WriteLog("onResume/");
    }

    @Override
    public void onPause() {
        //Контекст UI
        Utility.WriteLog("onPause\\");

        if (gameIsRunning && !waitForImageBox) {
            Utility.WriteLog(getString(R.string.pauseGameMsg));
            //Останавливаем таймер
            timerHandler.removeCallbacks(timerUpdateTask);

            //Приостанавливаем музыку
            PauseMusic(true);
        }

        Utility.WriteLog("onPause/");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        //Контекст UI
        Utility.WriteLog("onDestroy\\");
        FreeResources();
        Utility.WriteLog("onDestroy/");
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // UI Context
        // Catch the "Back" button, and do not close the activations, but only
        // send to the background (as if pressed "Home")
        if (sdcard_mounted && keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (backAction) {
                ListView lv = (ListView) findViewById(R.id.acts);
                if (lv.getCount() >= 1) {
                    lv.setSelection(0);
                    actionExecute(0);
                }
            } else ShowExitDialog();
            return true;
        }
        if (currentWin == WIN_MAIN && keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
            int position = keyCode - KeyEvent.KEYCODE_1; //переводим код клавиши в индекс :)
            ListView lv = (ListView) findViewById(R.id.acts);
            if (cheatsActive && (position == lv.getCount()-1)) {
                lv.setSelection(position);
                cheatExecute();
            }
            else if (position < lv.getCount()) {
                lv.setSelection(position);
                actionExecute(position);
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Контекст UI
        if (!sdcard_mounted)
            return false;

        // Сохраняем ссылку
        menuMain = menu;

        // Загружаем меню из XML-файла
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //Контекст UI
        if (!sdcard_mounted)
            return true;

        //Прячем или показываем группу пунктов меню "Начать заново", "Загрузить", "Сохранить"
        menu.setGroupVisible(R.id.menugroup_running, gameIsRunning);
        if (gameIsRunning) {
            //Заполняем слоты
            MenuItem loadItem = menu.findItem(R.id.menu_loadgame);
            LoadSlots(loadItem, getString(R.string.menu_load), true);
            MenuItem saveItem = menu.findItem(R.id.menu_savegame);
            LoadSlots(saveItem, getString(R.string.menu_save), false);
        }
        return true;
    }

    private void ShowGameStock() {
        Intent myIntent = new Intent(this, QspGameStock.class);
        //myIntent.setClassName("com.qsp.player", "com.qsp.player.QspGameStock");
        myIntent.putExtra("game_is_running", gameIsRunning);
        startActivityForResult(myIntent, ACTIVITY_SELECT_GAME);
    }

    private boolean ApplyFontTheme (Boolean settingsChanged) {
        String newText = getString(R.string.deftextColor);
        String newBack = getString(R.string.defbackColor);
        String newLink = getString(R.string.deflinkColor);
        String newActs = getString(R.string.defactsColor);

        //Get current theme from settings (theme change overrides all color changes)
        String newTheme = settings.getString("theme",getString(R.string.deftheme));
        //if theme has changed AND is not custom (-1), apply colors and exit
Utility.WriteLog("QSPfonttheme: "+QSPfontTheme+", newTheme = "+newTheme);

        if (!newTheme.equals(QSPfontTheme)) {
            SharedPreferences.Editor ed = settings.edit();
            QSPfontTheme = newTheme;
            //If switching from Custom theme, save the color values for Custom
            if (!newTheme.equals("-1")) {
                ed.putInt("customtextColor",Color.parseColor(QSPtextColor));
                ed.putInt("custombackColor",Color.parseColor(QSPbackColor));
                ed.putInt("customlinkColor",Color.parseColor(QSPlinkColor));
                ed.putInt("customactsColor",Color.parseColor(QSPactsColor));
            }

            switch (Integer.parseInt(newTheme)) {
                //Default - already set values to default above
                case 0:
                    break;
                //Light Theme
                case 1:
                    newText = getString(R.string.lighttextColor);
                    newBack = getString(R.string.lightbackColor);
                    newLink = getString(R.string.lightlinkColor);
                    newActs = getString(R.string.lightactsColor);
                    break;
                //Desktop Theme
                case 2:
                    newText = getString(R.string.desktextColor);
                    newBack = getString(R.string.deskbackColor);
                    newLink = getString(R.string.desklinkColor);
                    newActs = getString(R.string.deskactsColor);
                    break;
                //Custom Theme - saves last user settings
                case -1:
                    newText = String.format("#%06X",(0xFFFFFF & settings.getInt("customtextColor",Color.parseColor(getString(R.string.defcustomtextColor)))));
                    newBack = String.format("#%06X",(0xFFFFFF & settings.getInt("custombackColor", Color.parseColor(getString(R.string.defcustombackColor)))));
                    newLink = String.format("#%06X",(0xFFFFFF & settings.getInt("customlinkColor",Color.parseColor(getString(R.string.defcustomlinkColor)))));
                    newActs = String.format("#%06X",(0xFFFFFF & settings.getInt("customactsColor",Color.parseColor(getString(R.string.defcustomactsColor)))));
                    break;
            }
            QSPtextColor = newText;
            QSPbackColor = newBack;
            QSPlinkColor = newLink;
            QSPactsColor = newActs;

            ed.putInt("textColor",Color.parseColor(newText));
            ed.putInt("backColor",Color.parseColor(newBack));
            ed.putInt("linkColor",Color.parseColor(newLink));
            ed.putInt("actsColor",Color.parseColor(newActs));
            ed.apply();
            Utility.WriteLog("theme change:\n"+
                    "new text: "+String.format("#%06X",(0xFFFFFF & settings.getInt("textColor",Color.parseColor(defaultQSPtextColor))))+
                    "\nnew back: "+String.format("#%06X",(0xFFFFFF & settings.getInt("backColor", Color.parseColor(defaultQSPbackColor))))+
                    "\nnew link: "+String.format("#%06X",(0xFFFFFF & settings.getInt("linkColor",Color.parseColor(defaultQSPlinkColor)))) +
                    "\nnew acts: "+String.format("#%06X",(0xFFFFFF & settings.getInt("actsColor",Color.parseColor(defaultQSPactsColor)))));
            return true;
        }

        //If not changed OR custom theme, get current colors from settings
        newText = String.format("#%06X",(0xFFFFFF & settings.getInt("textColor",Color.parseColor(defaultQSPtextColor))));
        newBack = String.format("#%06X",(0xFFFFFF & settings.getInt("backColor", Color.parseColor(defaultQSPbackColor))));
        newLink = String.format("#%06X",(0xFFFFFF & settings.getInt("linkColor",Color.parseColor(defaultQSPlinkColor))));
        newActs = String.format("#%06X",(0xFFFFFF & settings.getInt("actsColor",Color.parseColor(defaultQSPactsColor))));

        //Compare to current theme colors; if any changed, change theme to "Custom"
        boolean setCustom = false;
        if (!newText.equals(QSPtextColor)) {
            QSPtextColor = newText;
            setCustom = true;
        }
        if (!newBack.equals(QSPbackColor)) {
            QSPbackColor = newBack;
            setCustom = true;
        }
        if (!newLink.equals(QSPlinkColor)) {
            QSPlinkColor = newLink;
            setCustom = true;
        }
        if (!newActs.equals(QSPactsColor)) {
            QSPactsColor = newActs;
            setCustom = true;
        }

        if (setCustom) settingsChanged = true;

        //Set theme to custom and then save all colors as "Custom" color values
        if(setCustom && settings.getString("theme",getString(R.string.deftheme)) != getString(R.string.customtheme)) {
            SharedPreferences.Editor ed = settings.edit();
            ed.putString("theme", res.getString(R.string.customtheme));
            ed.putInt("customtextColor",Color.parseColor(QSPtextColor));
            ed.putInt("custombackColor",Color.parseColor(QSPbackColor));
            ed.putInt("customlinkColor",Color.parseColor(QSPlinkColor));
            ed.putInt("customactsColor",Color.parseColor(QSPactsColor));
            ed.apply();
        }

        return settingsChanged;
    }

    private void ApplyFontSettingsToTextView(TextView tv, int textColor) {
        Typeface tf = Typeface.DEFAULT;
        switch (Integer.parseInt(settings.getString("typeface", "0"))) {
            case 1:
                tf = Typeface.SANS_SERIF;
                break;
            case 2:
                tf = Typeface.SERIF;
                break;
            case 3:
                tf = Typeface.MONOSPACE;
                break;
        }
        tv.setTypeface(tf);
        String tempFontSize = settings.getString("fontsize", defaultQSPfontSize);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(tempFontSize));

        QSPfontSize = tempFontSize;

        tv.setBackgroundColor(Color.parseColor(QSPbackColor));
        tv.setTextColor(textColor);
        tv.setLinkTextColor(Color.parseColor(QSPlinkColor));

    }

    private void ApplyViewSettings() {
        int backColor = settings.getInt("backColor", Color.parseColor(QSPbackColor));
        View v = findViewById(R.id.main);
        v.setBackgroundColor(backColor);
        ListView lv = (ListView) findViewById(R.id.inv);
        lv.setCacheColorHint(backColor);

// Log entry to check font settings update
//        Utility.WriteLog("defaultQSPtextColor = " + defaultQSPtextColor +
//        "\ndefaultQSPbackColor = " + defaultQSPbackColor +
//        "\ndefaultQSPlinkColor = " + defaultQSPlinkColor +
//        "\ndefaultQSPactsColor = " + defaultQSPactsColor +
//        "\ndefaultQSPfontStyle = " + defaultQSPfontStyle +
//        "\ndefaultQSPfontTheme = " + defaultQSPfontTheme);

        //Inject javascript to change the current page's settings to the new style
        //--using a for loop to apply QSPlinkColor to every link
        String command = "javascript:(function() {"+
                "document.getElementsByTagName(\"body\")[0].style.backgroundColor = \"" + QSPbackColor + "\"; "+
                "document.getElementsByTagName(\"body\")[0].style.fontFamily = \"" + QSPfontStyle + "\"; "+
                "document.getElementsByTagName(\"body\")[0].style.fontSize = \"" + QSPfontSize + "\"; "+
                "document.getElementsByTagName(\"body\")[0].style.color = \"" + QSPtextColor + "\"; "+

                "var links = document.getElementsByTagName(\"a\");" +
                "for(var i=0; i<links.length; i++) { if (links[i].href) { links[i].style.color = \"" + QSPlinkColor + "\"; } } "+
                "})()";
        Utility.WriteLog("command: "+command);

        //First change main_desc
        main_desc.getSettings().setJavaScriptEnabled(true);
        main_desc.loadUrl(command);
        main_desc.getSettings().setJavaScriptEnabled(false);

        //Then change vars_desc
        vars_desc.getSettings().setJavaScriptEnabled(true);
        vars_desc.loadUrl(command);
        vars_desc.getSettings().setJavaScriptEnabled(false);


        if (mActListAdapter != null)
            mActListAdapter.notifyDataSetChanged();
        if (mItemListAdapter != null)
            mItemListAdapter.notifyDataSetChanged();

        updateFreshPageURL();
    }

    private float SetImageLimits () {
        int padding = main_desc.getPaddingLeft() + main_desc.getPaddingRight();
//Utility.WriteLog("padding: "+padding);
        DisplayMetrics QSP_displayMetrics = getResources().getDisplayMetrics();

        float imgPerScreen = Float.parseFloat(settings.getString("imgHeight", "1"));
        playerHeightLimit = 1/imgPerScreen;
        if (playerHeightLimit <= 0) playerHeightLimit = 1;

        Point size = new Point();
        Display myDisplay = getWindowManager().getDefaultDisplay();
        myDisplay.getSize(size);
        float density = imageDensity ? QSP_displayMetrics.density : 1;
        if (density <=0) density = 1;
        maxW = Math.round(size.x/density - padding/2);
        maxH = Math.round(size.y/density * playerHeightLimit);

        return density;
    }

    private void RefreshMainDesc () {
        if (clearOnChange) {
            //Set all aspects to null, clear everything, including databases and cache directories
            main_desc.setWebChromeClient(null);
            main_desc.setWebViewClient(null);
            main_desc.clearCache(true);
            main_desc.clearHistory();
            uiContext.deleteDatabase("WebView.db");
            uiContext.deleteDatabase("WebViewCache.db");
            File cacheDir = uiContext.getCacheDir();
            if (cacheDir.exists()) cacheDir.delete();

            //Turn off Javascript, AppCache
            main_desc.getSettings().setJavaScriptEnabled(false);
            main_desc.getSettings().setAppCacheEnabled(false);
            main_desc.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            main_desc.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

            main_desc = (WebView) findViewById(R.id.main_desc);

            //Regenerate the web client
            main_descClient = new QSPWebViewClient();
            main_desc.setWebViewClient(main_descClient);
            main_desc.setWebChromeClient(new WebChromeClient());
            main_desc.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            main_desc.getSettings().setMediaPlaybackRequiresUserGesture(false);
            main_desc.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    final WebView.HitTestResult result = main_desc.getHitTestResult();

                    if (result.getType() == IMAGE_TYPE) {
                        String imageSrc = result.getExtra().replace("file:///", "/");
//                    Utility.WriteLog(result.getExtra() + " -> " + imageSrc);
                        ShowPicture(imageSrc);
                        return true;
                    }

                    return true;
                }
            });

            //Affirm the new setup with a blank page
            main_desc.loadUrl("about:blank");
            main_desc.reload();
        }

        String tempHtml = "";
        if (isMainDescHTML) {
            int tempMaxH = maxH;
            if(playerHeightLimit == 1) tempMaxH = -1;
            Utility.WriteLog("mainDescHTML1 ="+getMainDescHTML());
            if (settings.getBoolean("showLoadingPage",true))
                main_desc.loadDataWithBaseURL("", freshPageURL.replace("REPLACETEXT", getString(R.string.loadingURL)), "text/html", "UTF-8", "");
            if (curMainDescHTML != null)
                tempHtml = Utility.QspStrToWebView(curMainDescHTML, curGameDir, maxW, tempMaxH, settings.getBoolean("sound", true), bigImage, videoSwitch, hideImg, uiContext);
            Utility.WriteLog("mainDescHTML2 ="+tempHtml);

            if(clearOnChange) {
            }

            main_desc.loadDataWithBaseURL("", freshPageURL.replace("REPLACETEXT",tempHtml), "text/html", "UTF-8", "");
        } else {
            Utility.WriteLog("mainDesc!HTML1 ="+getMainDescHTML());
            if (settings.getBoolean("showLoadingPage",true))
                main_desc.loadDataWithBaseURL("", freshPageURL.replace("REPLACETEXT", getString(R.string.loadingURL)), "text/html", "UTF-8", "");
            if (curMainDescHTML != null)
                tempHtml = Utility.QspStrToStr(curMainDescHTML);

            Utility.WriteLog("mainDesc!HTML2 ="+tempHtml);

            if(clearOnChange) {
                main_desc.clearCache(true);
                main_desc.loadUrl("about:blank");
                main_desc.reload();
            }

            main_desc.loadDataWithBaseURL("", freshPageURL.replace("REPLACETEXT",tempHtml), "text/html", "UTF-8", "");
        }
    }

    private void RefreshVarsDesc () {
        if (clearOnChange) {
            vars_desc = (WebView) findViewById(R.id.vars_desc);
            vars_descClient = new QSPWebViewClient();
            vars_desc.setWebViewClient(vars_descClient);
            vars_desc.setWebChromeClient(new WebChromeClient());
            vars_desc.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    final WebView.HitTestResult result = main_desc.getHitTestResult();

                    if (result.getType() == IMAGE_TYPE) {
                        String imageSrc = result.getExtra().replace("file:///", "/");
//                    Utility.WriteLog(result.getExtra() + " -> " + imageSrc);
                        ShowPicture(imageSrc);
                        return true;
                    }

                    return true;
                }
            });
        }


        String tempHtml = "";

        if (isVarsDescHTML) {
            Utility.WriteLog("varDescHTML1 ="+getVarDescHTML());
            if (settings.getBoolean("showLoadingPage",true))
                vars_desc.loadDataWithBaseURL("", freshPageURL.replace("REPLACETEXT", getString(R.string.loadingURL)), "text/html", "UTF-8", "");
            if(getVarDescHTML() != null)
                tempHtml = Utility.QspStrToWebView(getVarDescHTML(),curGameDir,maxW,maxH,settings.getBoolean("sound",true), bigImage, videoSwitch, hideImg, uiContext);
            Utility.WriteLog("varDescHTML2 ="+tempHtml);

            vars_desc.loadDataWithBaseURL("",freshPageURL.replace("REPLACETEXT",tempHtml),"text/html","UTF-8","");
        } else {
            Utility.WriteLog("varDesc!HTML1 ="+getVarDescHTML());
            if (settings.getBoolean("showLoadingPage",true))
                vars_desc.loadDataWithBaseURL("", freshPageURL.replace("REPLACETEXT", getString(R.string.loadingURL)), "text/html", "UTF-8", "");
            if (getVarDescHTML() != null)
                tempHtml = Utility.QspStrToStr(getVarDescHTML());
            Utility.WriteLog("varDesc!HTML2 ="+tempHtml);

            vars_desc.loadDataWithBaseURL("", freshPageURL.replace("REPLACETEXT", tempHtml), "text/html", "UTF-8", "");
        }
    }

    private void updateFreshPageURL () {
        String tempFont = "\"\"";
        switch (Integer.parseInt(settings.getString("typeface", "0"))) {
            case 1:
                tempFont = "sans-serif";
                break;
            case 2:
                tempFont = "serif";
                break;
            case 3:
                tempFont = "courier";
                break;
            default:
                tempFont = defaultQSPfontStyle;
                break;
        }
        String tempText = String.format("#%06X",(0xFFFFFF & settings.getInt("textColor",Color.parseColor(defaultQSPtextColor))));
        String tempBack = String.format("#%06X",(0xFFFFFF & settings.getInt("backColor", Color.parseColor(defaultQSPbackColor))));
        String tempLink = String.format("#%06X",(0xFFFFFF & settings.getInt("linkColor",Color.parseColor(defaultQSPlinkColor))));
        String tempActs = String.format("#%06X",(0xFFFFFF & settings.getInt("actsColor",Color.parseColor(defaultQSPactsColor))));
        String tempSize = settings.getString("fontsize",defaultQSPfontSize);
/*
        Utility.WriteLog("text "+tempText);
        Utility.WriteLog("back "+tempBack);
        Utility.WriteLog("link "+tempLink);
        Utility.WriteLog("fontsize "+tempSize);
        Utility.WriteLog("fonttype "+tempFont);
        Utility.WriteLog("old: "+freshPageHeadTemplate);
*/
        curHtmlHead = freshPageHeadTemplate.replace("QSPFONTSTYLE",tempFont);
        curHtmlHead = curHtmlHead.replace("QSPFONTSIZE",tempSize);
        curHtmlHead = curHtmlHead.replace("QSPTEXTCOLOR",tempText);
        curHtmlHead = curHtmlHead.replace("QSPLINKCOLOR",tempLink);
        curHtmlHead = curHtmlHead.replace("QSPBACKCOLOR",tempBack);
        curHtmlHead = curHtmlHead.replace("QSPMAXWIDTH", ""+maxW+"px");
        curHtmlHead = curHtmlHead.replace("QSPOBJECTFIT",QSPobjectFit);
//        Utility.WriteLog("new: "+curHtmlHead);


        QSPbackColor = tempBack;
        QSPtextColor = tempText;
        QSPlinkColor = tempLink;
        QSPactsColor = tempActs;
        QSPfontStyle = tempFont;
        QSPfontSize = tempSize;
        freshPageURL = curHtmlHead + freshPageBodyTemplate;

        Utility.WriteLog("UpdateFreshPageURL: "+freshPageURL);
    }

    private void updateFullscreenStatus(boolean bUseFullscreen) {
        if (bUseFullscreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        findViewById(R.id.main).requestLayout();
    }

    private void ShowExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(uiContext);
        final CharSequence[] items = new String[2];
        items[0] = getString(R.string.menu_gamestock);
        items[1] = getString(R.string.menu_closeapp);
        builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            ShowGameStock();
                        } else if (which == 1) {
                            Utility.WriteLog(getString(R.string.appCloseByUser));
                            moveTaskToBack(true);
                        }
                    }
                }
        );
        AlertDialog alert = builder.create();
        alert.show();
    }

    /*
    protected void onSaveInstanceState (final Bundle outState)
    {
Utility.WriteLog("onSaveInstanceState\\");
        if (gameIsRunning) {
            outState.putString("game_name", curGameFile);
            Utility.WriteLog("1save");

            libThreadHandler.post(new Runnable() {
                public void run() {
                    if (libraryThreadIsRunning) {
                        Utility.WriteLog(getString(R.string.SSF_libRun));
                        return;
                    }
                    Utility.WriteLog("2save");
                    libraryThreadIsRunning = true;
                    Utility.WriteLog("3save");
                    final byte[] dataToSave = QSPSaveGameAsData(false);
                    Utility.WriteLog("4save");
                    outState.putByteArray("game_data", dataToSave);
                    Utility.WriteLog("5save");
                    libraryThreadIsRunning = false;
                }
            });

            Utility.WriteLog("onSaveInstanceState/");
        }
        Utility.WriteLog("6save");
        super.onSaveInstanceState(outState);
    }


    protected void onRestoreInstanceState (final Bundle savedState)
    {
Utility.WriteLog("onRestoreInstanceState\\");
        super.onRestoreInstanceState(savedState);
Utility.WriteLog("1restore");
        String savedCurGameFile = savedState.getString("game_name");
        if (savedCurGameFile != null) {
            StopGame(true);
            curGameFile = savedCurGameFile;
            runGame(curGameFile);
            libThreadHandler.post(new Runnable() {
                public void run() {
                    if (libraryThreadIsRunning) {
                        Utility.WriteLog(getString(R.string.LSF_libRun));
                        return;
                    }
                    libraryThreadIsRunning = true;
                    holdPanelAnimationsForFirstUpdate = true;
                    Utility.WriteLog("2restore");

                    final byte[] savedInstanceSave = savedState.getByteArray("game_data");
                    Utility.WriteLog("3restore = "+savedInstanceSave.length);
                    boolean result = QSPOpenSavedGameFromData(savedInstanceSave, savedInstanceSave.length, true);
                    Utility.WriteLog("4restore");
                    CheckQspResult(result, "LoadSlot: QSPOpenSavedGameFromData");
                    Utility.WriteLog("5restore");

                    libraryThreadIsRunning = false;

                    if (!result) {
                        Utility.WriteLog(getString(R.string.LSF_dataBuff));
                        return;
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            //Start the timer
                            timerHandler.postDelayed(timerUpdateTask, timerInterval);
                        }
                    });
                }
            });
        }

Utility.WriteLog("onRestoreInstanceState/");
    }
*/

    private void LoadSlots (MenuItem rootItem, String name, boolean isLoad) {
        //Контекст UI
        if (rootItem == null)
            return;

        SubMenu slotsMenu;

        if (rootItem.hasSubMenu()) {
            slotsMenu = rootItem.getSubMenu();
            slotsMenu.clear();
        } else {
            int id = rootItem.getItemId();
            menuMain.removeItem(id);
            slotsMenu = menuMain.addSubMenu(Menu.NONE, id, isLoad ? 65535 : 65536, name);
            slotsMenu.setHeaderTitle(getString(R.string.selectSlotTitle));

        }

        int position=0;

        for (int i = 0; i < SLOTS_MAX; i++) {
            String title = String.valueOf(i + 1).concat(": ");
            String Slotname = savePrefix + String.valueOf(i + 1).concat(".sav");

            File checkSlot = new File(saveGameDir.concat(Slotname));

            if (checkSlot.exists()) {
                String datetime = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", checkSlot.lastModified());
                title = title.concat(datetime);
            } else
                title = title.concat("[Empty]");
            slotsMenu.add(isLoad ? 1: 2, position,position, title);
            position ++;
        }
        if (!isLoad) return;
        File currDir = new File(saveGameDir);
        File[] files = currDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
Utility.WriteLog("file: "+pathname.toString());
                return (pathname.isFile() && pathname.toString().endsWith(".sav"));
            }
        });
        if (files.length>0) {
            for (File file : files) {
                String fname = file.getName();
Utility.WriteLog("fname = " + fname);
//                fname = "save" + fname.substring(fname.length()-5,fname.length()-4);
//Utility.WriteLog("fname2 = " + fname);
                boolean isSlot = false;
                for (int i = 0; i <= SLOTS_MAX+1; i++) {
//Utility.WriteLog("test = \""+ fname+ "\" vs. \"save"+String.valueOf(i+1)+"\"");
                    if (fname.startsWith(savePrefix+String.valueOf(i+1))) {
                        isSlot=true;
                        break;
                    }
                }
                if (isSlot) continue;
                slotsMenu.add(isLoad ? 1: 2, position,position, file.getName());
                position ++;
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Контекст UI
        switch (item.getItemId()) {
            case R.id.menu_gamestock:
                //Выбираем игру
                ShowGameStock();
                return true;

            case R.id.menu_exit:
                //Выбираем игру
                Utility.WriteLog(getString(R.string.appCloseByUser));
                moveTaskToBack(true);
                return true;

            case R.id.menu_options:
                Intent intent = new Intent();
                intent.setClass(this, Settings.class);
                startActivity(intent);
                return true;

            case R.id.menu_about:
                showAbout();
                return true;
/*                Intent updateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.market_details_url)));
                try {
                    startActivity(updateIntent);
                } catch (ActivityNotFoundException e) {
                    Utility.ShowError(uiContext, getString(R.string.marketNotFound));
                    return false;
                }
                return true;
*/
            case R.id.menu_newgame:
                String gameFile = curGameFile;
                StopGame(true);
                runGame(gameFile);
                return true;

            case R.id.menu_loadgame:
                return true;

            case R.id.menu_savegame:
                return true;

            default: {
                if (item.getGroupId()==1) {

                    MenuItem l = menuMain.findItem(R.id.menu_loadgame);
                    SubMenu ls = l.getSubMenu();
                    for (int i = 0; i < SLOTS_MAX; i++) {
                        MenuItem li = ls.getItem(i);
                        if (li == item) {
                            LoadSlot(i + 1);
                            return true;
                        }
                    }
                    LoadSlot(item.getTitle().toString());
                }
                if (item.getGroupId()==2) {

                    MenuItem l = menuMain.findItem(R.id.menu_savegame);
                    SubMenu ls = l.getSubMenu();
                    for (int i = 0; i < SLOTS_MAX; i++) {
                        MenuItem li = ls.getItem(i);
                        if (li == item) {
                            SaveSlot(i + 1);
                            return true;
                        }
                    }
                }
/*
                MenuItem l = menuMain.findItem(R.id.menu_savegame);
                SubMenu ls = l.getSubMenu();
                if (ls != null) {
                    item.getGroupId()
                    for (int i = 0; i < SLOTS_MAX; i++) {
                        MenuItem li = ls.getItem(i);
                        if (li == item) {
                            LoadSlot(i + 1);
                        }
                    }
                }
                MenuItem s = menuMain.findItem(R.id.menu_savegame);
                SubMenu ss = s.getSubMenu();
                if (ss != null) {
                    for (int i = 0; i < SLOTS_MAX; i++) {
                        MenuItem si = ss.getItem(i);
                        if (si == item) {
                            SaveSlot(i + 1);
                        }
                    }
                }*/
            }
            break;
        }
        return false;
    }

    //Show About dialog
    protected void showAbout() {
        // Inflate the about message contents
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        // When linking text, force to always use default color. This works
        // around a pressed color state bug.
//        WebView textView = (WebView) messageView.findViewById(R.id.about_credits);
//        int defaultColor = textView.getTextColors().getDefaultColor();
//        textView.setTextColor(defaultColor);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.icon);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }



    private void LoadSlot(int index) {
        //Контекст UI
        String path = savePrefix + String.valueOf(index).concat(".sav");
        LoadSlot(path);
    }

    private void LoadSlot(String filename) {
        if (curDownloadDir != null) {
            LoadSlotDD(filename);
            return;
        }

        //Контекст UI
        String path = saveGameDir.concat(filename);
//Utility.WriteLog("Opening: "+path);
        File f = new File(path);
        if (!f.exists()) {
            Utility.WriteLog(getString(R.string.LSF_fileNotFound));
            return;
        }

        FileInputStream fIn;
        int size;
        try {
            fIn = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            Utility.ShowError(uiContext, getString(R.string.openFileFail));
            e.printStackTrace();
            return;
        }
        try {
            size = fIn.available();
        } catch (IOException e) {
            Utility.ShowError(uiContext, getString(R.string.accessFileFail));
            e.printStackTrace();
            try {
                fIn.close();
            } catch (IOException e1) {
                Utility.ShowError(uiContext, getString(R.string.fileHandleReleaseFail));
                e1.printStackTrace();
            }
            return;
        }

        final byte[] inputBuffer = new byte[size];
        try {
            fIn.read(inputBuffer);
            fIn.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        final int bufferSize = size;

        //Останавливаем таймер
        timerHandler.removeCallbacks(timerUpdateTask);
        //Выключаем музыку
        CloseFileUI(null);

        libThreadHandler.post(new Runnable() {
            public void run() {
                if (libraryThreadIsRunning) {
                    Utility.WriteLog(getString(R.string.LSF_libRun));
                    return;
                }
                libraryThreadIsRunning = true;
                holdPanelAnimationsForFirstUpdate = true;

                boolean result = QSPOpenSavedGameFromData(inputBuffer, bufferSize, true);
                CheckQspResult(result, "LoadSlot: QSPOpenSavedGameFromData");

                libraryThreadIsRunning = false;

                if (!result) {
                    Utility.WriteLog(getString(R.string.LSF_dataBuff));
                    return;
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        //Start the timer
                        timerHandler.postDelayed(timerUpdateTask, timerInterval);
                    }
                });
            }
        });
    }

    private void LoadSlotDD(String filename) {

        //Контекст UI
        DocumentFile df = curDownloadDir.findFile(filename);
        if (!df.exists()) {
            Utility.WriteLog(getString(R.string.LSF_fileNotFound));
            return;
        }

        InputStream fIn;
        int size;
        try {
            Uri tempUri = df.getUri();
            fIn = uiContext.getContentResolver().openInputStream(tempUri);
        } catch (FileNotFoundException e) {
            Utility.ShowError(uiContext, getString(R.string.openFileFail));
            e.printStackTrace();
            return;
        }

        if (fIn == null) return;
        try {
            size = fIn.available();
        } catch (IOException e) {
            Utility.ShowError(uiContext, getString(R.string.accessFileFail));
            e.printStackTrace();
            try {
                fIn.close();
            } catch (IOException e1) {
                Utility.ShowError(uiContext, getString(R.string.fileHandleReleaseFail));
                e1.printStackTrace();
            }
            return;
        }

        final byte[] inputBuffer = new byte[size];
        try {
            fIn.read(inputBuffer);
            fIn.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        final int bufferSize = size;

        //Останавливаем таймер
        timerHandler.removeCallbacks(timerUpdateTask);
        //Выключаем музыку
        CloseFileUI(null);

        libThreadHandler.post(new Runnable() {
            public void run() {
                if (libraryThreadIsRunning) {
                    Utility.WriteLog(getString(R.string.LSF_libRun));
                    return;
                }
                libraryThreadIsRunning = true;
                holdPanelAnimationsForFirstUpdate = true;

                boolean result = QSPOpenSavedGameFromData(inputBuffer, bufferSize, true);
                CheckQspResult(result, "LoadSlot: QSPOpenSavedGameFromData");

                libraryThreadIsRunning = false;

                if (!result) {
                    Utility.WriteLog(getString(R.string.LSF_dataBuff));
                    return;
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        //Start the timer
                        timerHandler.postDelayed(timerUpdateTask, timerInterval);
                    }
                });
            }
        });
    }

    private void c(int index) {
        //Контекст UI
        final String path = savePrefix + String.valueOf(index).concat(".sav");
        SaveSlot(path);
    }


    private void SaveSlot(int index) {
        //Контекст UI
        String path = savePrefix + String.valueOf(index).concat(".sav");
        SaveSlot(path);
    }

    private void SaveSlot(String filename) {
        //Use downloadDir if present for saving games
        if (curDownloadDir != null) {
            SaveSlotDD(filename);
            return;
        }

        //Контекст UI
//        final String path = saveGameDir.concat(filename);
        final String path = saveGameDir+filename;


//        Utility.WriteLog("Save file: "+path);

        libThreadHandler.post(new Runnable() {
            public void run() {
                if (libraryThreadIsRunning) {
                    Utility.WriteLog(getString(R.string.SSF_libRun));
                    return;
                }
                libraryThreadIsRunning = true;

                final byte[] dataToSave = QSPSaveGameAsData(false);

                if (dataToSave == null) {
                    CheckQspResult(false, "SaveSlot: QSPSaveGameAsData");
                    Utility.WriteLog(getString(R.string.SSF_noCreate));
                    return;
                }

                runOnUiThread(new Runnable() {
                    public void run() {

                        File f = new File(path);
//Utility.WriteLog("Saving to: "+path);

                        FileOutputStream fileOutput;
                        try {
                            fileOutput = new FileOutputStream(f);
//                            Utility.WriteLog("Open file: "+path);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            return;
                        }
                        try {
                            fileOutput.write(dataToSave, 0, dataToSave.length);
//                            Utility.WriteLog("Writing to file: "+path);
                            fileOutput.close();
//                            Utility.WriteLog("Closed file: "+path);
                            MediaScannerConnection.scanFile(uiContext, new String[] {path.toString()},null,null);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
//                        Utility.WriteLog("Saved to: "+path);

                    }
                });

                libraryThreadIsRunning = false;
            }
        });
    }

    private void SaveSlotDD (final String filename) {

        libThreadHandler.post(new Runnable() {
            public void run() {
                if (libraryThreadIsRunning) {
                    Utility.WriteLog(getString(R.string.SSF_libRun));
                    return;
                }
                libraryThreadIsRunning = true;

                final byte[] dataToSave = QSPSaveGameAsData(false);

                if (dataToSave == null) {
                    CheckQspResult(false, "SaveSlot: QSPSaveGameAsData");
                    Utility.WriteLog(getString(R.string.SSF_noCreate));
                    return;
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        DocumentFile df = curDownloadDir.findFile(filename.replace("/","_"));
                        if (df != null) df.delete();
                        df = curDownloadDir.createFile("application/octet-stream",filename);
Utility.WriteLog("Saving to (DF): "+filename+" in "+FileUtil.getFullPathFromTreeUri(df.getUri(),uiContext) + " + "+df.getName());

                        OutputStream fileOutput;
                        try {
                            Uri tempUri = df.getUri();
                            fileOutput = uiContext.getContentResolver().openOutputStream(tempUri);
//                            Utility.WriteLog("Open file: "+path);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            return;
                        }
                        if (fileOutput == null)
                            return;
                        try {
                            fileOutput.write(dataToSave, 0, dataToSave.length);
                            Utility.WriteLog("Writing to DF: "+filename);
                            fileOutput.close();
                            Utility.WriteLog("Closed DF: "+filename);
                            MediaScannerConnection.scanFile(uiContext, new String[] {curGameDir+filename},null,null);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
//                        Utility.WriteLog("Saved to: "+path);

                    }
                });

                libraryThreadIsRunning = false;
            }
        });
    }

    DocumentFile downloadDir = null;
    DocumentFile curDownloadDir = null;

    private void getDownloadDirFromSettings() {
        Uri treeUri = null;

        //get treeUri from settings
        String tempUri = settings.getString(getString(R.string.key_internal_uri_extsdcard),"");
        if (!tempUri.isEmpty())
            treeUri = Uri.parse(tempUri);

        //if not null/empty, use for downloadDir
        if (treeUri != null)
            downloadDir = DocumentFile.fromTreeUri(uiContext, treeUri);

        //if downloadDir is not a writable directory, clear downloadDir
        //and set the R.string.key_internal_uri_extsdcard as an empty string
        if (downloadDir == null) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getString(R.string.key_internal_uri_extsdcard), "");
            editor.commit();
            return;
        }
        else if (!downloadDir.exists() || !downloadDir.isDirectory() || !downloadDir.canWrite()) {
            downloadDir = null;
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getString(R.string.key_internal_uri_extsdcard), "");
            editor.commit();
        }
        else
            setFullGamesPath();
    }

    private void setFullGamesPath () {
        //**** START set SDPath/storageType ****
        String newDownDirPath = getString(R.string.defDownDirPath);
        String SDPath = "";
        if (downloadDir != null) {
            String untestedDirPath = FileUtil.getFullPathFromTreeUri(downloadDir.getUri(), uiContext);
            if (untestedDirPath != null)
                newDownDirPath = untestedDirPath;

            if (!newDownDirPath.endsWith("/"))
                newDownDirPath += "/";
        }

        String tempDir = FileUtil.getSdCardPath();
        Boolean usingExtSDCard = false;
        if (newDownDirPath.startsWith(tempDir)) {
            Utility.WriteLog(newDownDirPath + " contains " + tempDir);
            SDPath = tempDir;
        } else {
            Utility.WriteLog(newDownDirPath + " doesn't contain " + tempDir);

            ArrayList<String> extSDPaths = FileUtil.getExtSdCardPaths(uiContext);
            for (int i = 0; i < extSDPaths.size(); i++) {
                tempDir = extSDPaths.get(i);
                if (newDownDirPath.startsWith(tempDir)) {
                    Utility.WriteLog(newDownDirPath + " contains " + tempDir);
                    SDPath = tempDir;
                    usingExtSDCard = true;
                    break;
                } else Utility.WriteLog(newDownDirPath + " doesn't contain " + tempDir);
            }
        }
        //**** END set SDPath/storageType ****

        //set String "relGamePath"; "compGamePath" is newDownDirPath
        String relPath = newDownDirPath.replace(SDPath, "");

        //Store adjusted storage type, relative path, and complete game directory
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("storageType", usingExtSDCard);
        editor.putString("relGamePath", relPath);
        editor.putString("compGamePath", newDownDirPath);
        editor.putString("downDirPath", newDownDirPath);
        editor.commit();

        Utility.WriteLog("_Using downloadDir DocumentFile_");
        Utility.WriteLog("storageType: " + settings.getBoolean("storageType", true));
        Utility.WriteLog("relGamePath: " + settings.getString("relGamePath", getString(R.string.defRelPath)));
        Utility.WriteLog("compGamePath: " + settings.getString("compGamePath", getString(R.string.defGamePath)));
        Utility.WriteLog("downDirPath: " + settings.getString("downDirPath", getString(R.string.defDownDirPath)));
    }


    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == ACTIVITY_SELECT_GAME) {
            if (resultCode == RESULT_OK) {
                //Игра выбрана, запускаем ее
                if (data == null) {
                    return;
                }
                String file_name = data.getStringExtra("file_name");
                if ((file_name == null) || file_name.equals(curGameFile))
                    return;
                StopGame(true);
                runGame(file_name);
            }
        }
    }

    //******************************************************************************
    //******************************************************************************
    //****** / THREADS \ ***********************************************************
    //******************************************************************************
    //******************************************************************************

    /**
     * паркует-останавливает указанный тред, и сохраняет на него указатель в parkThread
     */
    protected void setThreadPark() {
        Utility.WriteLog("setThreadPark: enter ");
        //Контекст библиотеки
        if (libThread == null) {
            Utility.WriteLog("setThreadPark: failed, libthread is null");
            return;
        }
        parkThread = libThread;
        LockSupport.park();
        Utility.WriteLog("setThreadPark: success ");
    }

    /**
     * возобновляет работу треда сохраненного в указателе parkThread
     */
    protected boolean setThreadUnpark() {
        Utility.WriteLog("setThreadUnPark: "+getString(R.string.enterS)+" ");
        //Контекст UI
        if (parkThread != null && parkThread.isAlive()) {
            LockSupport.unpark(parkThread);
            Utility.WriteLog("setThreadUnPark: "+getString(R.string.successS)+" ");
            return true;
        }
        Utility.WriteLog("setThreadUnPark: "+getString(R.string.failedS)+" | ");
        if (parkThread == null)
            Utility.WriteLog("parkThread: "+getString(R.string.isNullS)+" ");
        else
            Utility.WriteLog("parkThread: "+getString(R.string.isDeadS)+" ");
        return false;
    }

    protected void StartLibThread() {
        Utility.WriteLog("StartLibThread()");
        //Контекст UI
        if (libThread != null) {
            Utility.WriteLog("StartLibThread: exit | libThread = null");
            return;
        }
        //Запускаем поток библиотеки
        Thread t = new Thread() {
            public void run() {
                Looper.prepare();
                libThreadHandler = new Handler();
                Utility.WriteLog("LibThread runnable: libThreadHandler = new Handler()");
                Looper.loop();
                Utility.WriteLog("LibThread runnable: libThread exit");
            }
        };
        libThread = t;
        t.start();
        Utility.WriteLog("StartLibThread.start()");
    }

    protected void StopLibThread() {
        Utility.WriteLog("StopLibThread()");
        //Контекст UI
        //Останавливаем поток библиотеки
        libThreadHandler.getLooper().quit();
        libThread = null;
        Utility.WriteLog("StopLibThread: exit");
    }
    //******************************************************************************
    //******************************************************************************
    //****** \ THREADS / ***********************************************************
    //******************************************************************************
    //******************************************************************************

    //устанавливаем текст заголовка окна
    private void setTitle(String second) {
        TextView winTitle = (TextView) findViewById(R.id.title_text);
        winTitle.setText(second);
        updateTitle();
    }

    //анимация иконок при смене содержимого скрытых окон
    private void updateTitle() {
        ImageButton image = (ImageButton) findViewById(R.id.title_button_1);
        image.clearAnimation();
/*        if (invUnread) {
            Animation update = AnimationUtils.loadAnimation(this, R.anim.update);
            image.startAnimation(update);
            image.setBackgroundResource(invBack = R.drawable.btn_bg_pressed);
            invUnread = false;
        }
*/

        image = (ImageButton) findViewById(R.id.title_button_2);
        image.clearAnimation();
        if (varUnread) {
            //Animation update = AnimationUtils.loadAnimation(this, R.anim.update);
            //image.startAnimation(update);
            image.setBackgroundResource(varBack = R.drawable.btn_bg_pressed);
            varUnread = false;
        }
    }

    //обработчик "Описание" в заголовке
    public void onHomeClick(View v) {
        setCurrentWin(WIN_MAIN);
    }

    //обработчик текста в заголовке
    public void onTitleClick(View v) {
        if (currentWin > 0)
            currentWin--;
        else
            currentWin = 2;
        setCurrentWin(currentWin);
    }

    //обработчик "Инвентарь" в заголовке
    public void onInvClick(View v) {
        setCurrentWin(WIN_INV);
    }

    //обработчик "Доп" в заголовке
    public void onExtClick(View v) {
        setCurrentWin(WIN_EXT);
    }

    //смена активного экрана
    private void setCurrentWin(int win) {
        switch (win) {
            case WIN_INV:
                toggleInv(true);
                toggleMain(false);
                toggleExt(false);
                invUnread = false;
                invBack = 0;
                setTitle(getString(R.string.invTitle));
                break;
            case WIN_MAIN:
                toggleInv(false);
                toggleMain(true);
                toggleExt(false);
                setTitle(getString(R.string.mainDescTitle));
                break;
            case WIN_EXT:
                toggleInv(false);
                toggleMain(false);
                toggleExt(true);
                varUnread = false;
                varBack = 0;
// "Доп. описание"
                setTitle(getString(R.string.varDescTitle));
                break;
        }
        currentWin = win;
    }

    private void toggleInv(boolean vis) {
        findViewById(R.id.inv).setVisibility(vis ? View.VISIBLE : View.GONE);
        findViewById(R.id.title_button_1).setBackgroundResource(vis ? R.drawable.btn_bg_active : invBack);
    }

    private void toggleMain(boolean vis) {
        findViewById(R.id.main_tab).setVisibility(vis ? View.VISIBLE : View.GONE);
        findViewById(R.id.title_home_button).setBackgroundResource(vis ? R.drawable.btn_bg_active : 0);
    }

    private void toggleExt(boolean vis) {
        findViewById(R.id.vars_tab).setVisibility(vis ? View.VISIBLE : View.GONE);
        findViewById(R.id.title_button_2).setBackgroundResource(vis ? R.drawable.btn_bg_active : varBack);
    }

    private Runnable timerUpdateTask = new Runnable() {
        //Контекст UI
        public void run() {
            libThreadHandler.post(new Runnable() {
                public void run() {
                    if (!gameIsRunning)
                        return;
                    if (libraryThreadIsRunning)
                        return;
                    libraryThreadIsRunning = true;
                    boolean result = QSPExecCounter(true);
                    CheckQspResult(result, "timerUpdateTask: QSPExecCounter");
                    libraryThreadIsRunning = false;
                }
            });
            timerHandler.postDelayed(this, timerInterval);
        }
    };


    private void FreeResources() {
        //Контекст UI

        //Процедура "честного" высвобождения всех ресурсов - в т.ч. остановка потока библиотеки
        //Сейчас не вызывается вообще, т.к. у нас нет соотв. пункта меню.

        //Вызовется только при закрытии активити в обработчике onDestroy(завершение активити),
        //но этого не произойдет при нормальной работе, т.к. кнопка Back не закрывает, а только
        //останавливает активити.

        //Очищаем ВСЕ на выходе
        if (qspInited) {
            Utility.WriteLog("onDestroy: "+getString(R.string.stopGame));
            StopGame(false);
        }
        //Останавливаем поток библиотеки
        StopLibThread();
    }

    private void runGame(String fileName) {
Utility.WriteLog("runGame\\");
        if (PermissionChecker.checkSelfPermission(uiContext,"android.permission.WRITE_EXTERNAL_STORAGE") == PermissionChecker.PERMISSION_GRANTED) {
            Log.d(TAG, "Write granted");
            Utility.WriteLog("Write granted");
        } else {
            Log.e(TAG, "Write refused");
            Utility.WriteLog("Write refused");
        }
        //Контекст UI
        File f = new File(fileName);
        if (!f.exists()) {
            Utility.ShowError(uiContext, getString(R.string.fileNotFound));
            return;
        }

        if (libThreadHandler == null) {
            Utility.WriteLog("runGame: "+getString(R.string.failedS)+", libThreadHandler "+getString(R.string.isNullS));
            return;
        }

        if (libraryThreadIsRunning) {
            Utility.WriteLog("runGame: "+getString(R.string.failedS)+" | "+getString(R.string.libRunError)+".");
            return;
        }

        final boolean inited = qspInited;
        qspInited = true;
        final String gameFileName = fileName;
        curGameFile = gameFileName;
        curGameDir = gameFileName.substring(0, gameFileName.lastIndexOf(File.separator, gameFileName.length() - 1) + 1);
        Utility.WriteLog("curGameFile: "+curGameFile);
        Utility.WriteLog("curGameDir: "+curGameDir);

        //Use the game directory as the name for the save directory
        String relGameDir = curGameDir.substring(0,curGameDir.lastIndexOf("/"));
        relGameDir = relGameDir.substring(relGameDir.lastIndexOf("/")+1);
        Utility.WriteLog("relGameDir: "+relGameDir);
        curRelGameDir = relGameDir;
        curSaveTitle = Utility.safetyString(relGameDir);

        //The prefix for the save file will be the game file name, without ".qsp", + "_"
        //Default name is "save_"
        //so, filename "/QSP/games/test/test.qsp" will make "test_" or "save_"
        savePrefix = "save_";
        int relGFIdx = curGameFile.lastIndexOf("/")+1;
        if ((relGFIdx > 0) && (curGameFile.length() > relGFIdx)) {
            String relGameFile = curGameFile.substring(relGFIdx);

            int cgfEndIdx = relGameFile.toLowerCase().lastIndexOf(".qsp");
            if (cgfEndIdx >= 0) savePrefix = relGameFile.substring(0, cgfEndIdx)+"_";
            else savePrefix = relGameFile + "_";

            savePrefix = savePrefix.replace("__","_");
        }

        //Added to handle SAF save system
        getDownloadDirFromSettings();
        if (downloadDir != null) {
            curDownloadDir = downloadDir.findFile(curRelGameDir);
            Utility.WriteLog("curDownloadDir == null? " + (curDownloadDir == null));
            Utility.WriteLog("curRelGameDir == " + curRelGameDir);

            Uri tempUri = downloadDir.getUri();
            saveGameDir = FileUtil.getFullPathFromTreeUri(tempUri,uiContext) + "/" + curRelGameDir + "/";

            Utility.WriteLog("saveGameDir: "+saveGameDir);
            Utility.WriteLog("downloadDir Uri: "+FileUtil.getFullPathFromTreeUri(downloadDir.getUri(),uiContext));
            Utility.WriteLog("curDownloadDir Uri: "+FileUtil.getFullPathFromTreeUri(curDownloadDir.getUri(),uiContext));
        }
        else {

//        saveGameDir = uiContext.getFilesDir().getAbsolutePath();
            File tempSaveDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/QSP_saves/" + curSaveTitle);
//Utility.WriteLog("tempSaveDir = "+tempSaveDir.getAbsolutePath());
            //if the the save directory doesn't exist, create it
            if (!tempSaveDir.exists()) {
                tempSaveDir.mkdirs();
//Utility.WriteLog("Doesn't exist, make a directory");
            }
            //if the save directory exists but is not a directory, delete it, then create it
            else if (!tempSaveDir.isDirectory()) {
//Utility.WriteLog("Exists, delete and make a directory");
                tempSaveDir.delete();
                tempSaveDir.mkdirs();
            }
//Utility.WriteLog("Exists as a directory");

            saveGameDir = tempSaveDir.getAbsolutePath() + "/";
        }

        float density = SetImageLimits();
        int padding = main_desc.getPaddingLeft() + main_desc.getPaddingRight();

        Utility.WriteLog("maxW = "+maxW+", maxH = "+maxH+", density = "+density+", playerHeightLimit = "+playerHeightLimit);

        imgGetter.setDensity(density);
        imgGetterDesc.setDensity(density);
        imgGetter.setDirectory(curGameDir);
        imgGetter.setScreenWidth(getWindow().getWindowManager().getDefaultDisplay().getWidth() - padding);
        imgGetter.setScreenHeight(getWindow().getWindowManager().getDefaultDisplay().getHeight() - padding);
        imgGetterDesc.setDirectory(curGameDir);
        imgGetterDesc.setScreenWidth(getWindow().getWindowManager().getDefaultDisplay().getWidth() - padding);
        imgGetterDesc.setScreenHeight(getWindow().getWindowManager().getDefaultDisplay().getHeight() - padding);
        imgGetter.setFullSize(bigImage);
        imgGetterDesc.setFullSize(bigImage);

        //Clear all fields
        final QSPItem[] emptyItems = new QSPItem[0];
        ListView lv = (ListView) findViewById(R.id.acts);
        lv.setAdapter(new QSPListAdapter(uiContext, R.layout.act_item, emptyItems));
        lv = (ListView) findViewById(R.id.inv);
        lv.setAdapter(new QSPListAdapter(uiContext, R.layout.obj_item, emptyItems));
//        main_desc.setText("");
//        vars_desc.setText("");
        main_desc.loadDataWithBaseURL("",freshPageURL.replace("REPLACETEXT",""),"text/html","utf-8","");
        vars_desc.loadDataWithBaseURL("",freshPageURL.replace("REPLACETEXT",""),"text/html","utf-8","");
        main_desc.getSettings().setLoadsImagesAutomatically(true);
        vars_desc.getSettings().setLoadsImagesAutomatically(true);
        main_desc.getSettings().setAllowFileAccess(true);
        vars_desc.getSettings().setAllowFileAccess(true);

        main_desc.getSettings().setUseWideViewPort(false);
        vars_desc.getSettings().setUseWideViewPort(false);
        main_desc.getSettings().setLoadWithOverviewMode(true);
        vars_desc.getSettings().setLoadWithOverviewMode(true);


        setCurrentWin(WIN_MAIN);

        libThreadHandler.post(new Runnable() {
            public void run() {
                File tqsp = new File(gameFileName);
                FileInputStream fIn = null;
                int size = 0;
                try {
                    fIn = new FileInputStream(tqsp);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Utility.ShowError(uiContext, getString(R.string.fileNotFound));
                    return;
                }
                try {
                    size = fIn.available();
                } catch (IOException e) {
                    e.printStackTrace();
                    Utility.ShowError(uiContext, getString(R.string.accessFileFail));
                    try {
                        fIn.close();
                    } catch (IOException e1) {
                        Utility.ShowError(uiContext, getString(R.string.fileHandleReleaseFail));
                        e1.printStackTrace();
                    }
                    return;
                }

                byte[] inputBuffer = new byte[size];
                try {
                    // Fill the Buffer with data from the file
                    fIn.read(inputBuffer);
                    fIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Utility.ShowError(uiContext, getString(R.string.readFileError));
                    return;
                }

                if (!inited)
                    QSPInit();
                final boolean gameLoaded = QSPLoadGameWorldFromData(inputBuffer, size, gameFileName);
                CheckQspResult(gameLoaded, "runGame: QSPLoadGameWorldFromData");

                if (gameLoaded) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            //init acts callbacks
                            ListView lvAct = (ListView) findViewById(R.id.acts);
                            lvAct.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                            lvAct.setFocusableInTouchMode(true);
                            lvAct.setFocusable(true);
                            lvAct.setItemsCanFocus(true);
                            lvAct.setOnItemClickListener(actListClickListener);
                            lvAct.setOnItemSelectedListener(actListSelectedListener);

                            //init objs callbacks
                            ListView lvInv = (ListView) findViewById(R.id.inv);
                            lvInv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                            lvInv.setFocusableInTouchMode(true);
                            lvInv.setFocusable(true);
                            lvInv.setItemsCanFocus(true);
                            lvInv.setOnItemClickListener(objListClickListener);

                            //Запускаем таймер
                            timerInterval = 500;
                            timerStartTime = System.currentTimeMillis();
                            timerHandler.removeCallbacks(timerUpdateTask);
                            timerHandler.postDelayed(timerUpdateTask, timerInterval);

                            //Запускаем счетчик миллисекунд
                            gameStartTime = System.currentTimeMillis();

                            //Все готово, запускаем игру
                            libThreadHandler.post(new Runnable() {
                                public void run() {
                                    libraryThreadIsRunning = true;
                                    boolean result = QSPRestartGame(true);
                                    CheckQspResult(result, "runGame: QSPRestartGame");
                                    libraryThreadIsRunning = false;
                                }
                            });

                            gameIsRunning = true;
                            holdPanelAnimationsForFirstUpdate = true;
                        }
                    });
                }
            }
        });
        Utility.WriteLog("runGame/");
    }

    private void StopGame(boolean restart) {
        //Контекст UI
        if (gameIsRunning) {
            //останавливаем таймер
            timerHandler.removeCallbacks(timerUpdateTask);

            //останавливаем музыку
            CloseFileUI(null);

            //отключаем колбэки действий
            ListView lvAct = (ListView) findViewById(R.id.acts);
            lvAct.setOnItemClickListener(null);
            lvAct.setOnItemSelectedListener(null);

            //отключаем колбэки инвентаря
            ListView lvInv = (ListView) findViewById(R.id.inv);
            lvInv.setOnItemClickListener(null);
            lvInv.setOnItemSelectedListener(null);

            gameIsRunning = false;
        }
        curGameDir = "";
        curGameFile = "";
        curSaveTitle = "";

        //Очищаем библиотеку
        if (restart || libraryThreadIsRunning)
            return;

        qspInited = false;
        libThreadHandler.post(new Runnable() {
            public void run() {
                libraryThreadIsRunning = true;
                QSPDeInit();
                libraryThreadIsRunning = false;
            }
        });
    }

    private void PlayFileUI(String file, int volume) {
        if (!settings.getBoolean("sound",true)) return;

        //Контекст UI
        if (file == null || file.length() == 0)
            return;

        //Проверяем, проигрывается ли уже этот файл.
        //Если проигрывается, ничего не делаем.
        if (IsPlayingFileUI(file))
            return;

        //Проверяем, существует ли файл.
        //Если нет, ничего не делаем.
        File mediaFile = new File(curGameDir, file);
        if (!mediaFile.exists())
            return;

        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(curGameDir + file);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            mediaPlayer.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        final String fileName = file;
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                musicLock.lock();
                try {
                    for (int i = 0; i < mediaPlayersList.size(); i++) {
                        MusicContent it = mediaPlayersList.elementAt(i);
                        if (it.path.equals(fileName)) {
                            mediaPlayersList.remove(it);
                            break;
                        }
                    }
                } finally {
                    musicLock.unlock();
                }
            }
        });

        float realVolume = GetRealVolume(volume);
        mediaPlayer.setVolume(realVolume, realVolume);
        mediaPlayer.start();
        MusicContent musicContent = new MusicContent();
        musicContent.path = file;
        musicContent.volume = volume;
        musicContent.player = mediaPlayer;
        musicLock.lock();
        try {
            mediaPlayersList.add(musicContent);
        } finally {
            musicLock.unlock();
        }
    }

    private float GetRealVolume(int volume) {
//Utility.WriteLog("sound test");
        float result = 0;
        if (settings.getBoolean("sound", true))
            result = ((float) volume) / 100;
        else
            result = 0;
        return result;
    }

    private boolean IsPlayingFileUI(String file) {
        //Контекст UI
        if (file == null || file.length() == 0)
            return false;
        boolean foundPlaying = false;
        musicLock.lock();
        try {
            for (int i = 0; i < mediaPlayersList.size(); i++) {
                MusicContent it = mediaPlayersList.elementAt(i);
                if (it.path.equals(file)) {
                    foundPlaying = true;
                    break;
                }
            }
        } finally {
            musicLock.unlock();
        }
        return foundPlaying;
    }

    private void CloseFileUI(String file) {
        //Контекст UI
        //Если вместо имени файла пришел null, значит закрываем все файлы(CLOSE ALL)
        boolean bCloseAll = false;
        if (file == null)
            bCloseAll = true;
        else if (file.length() == 0)
            return;
        musicLock.lock();
        try {
            for (int i = 0; i < mediaPlayersList.size(); i++) {
                MusicContent it = mediaPlayersList.elementAt(i);
                if (bCloseAll || it.path.equals(file)) {
                    if (it.player.isPlaying())
                        it.player.stop();
                    it.player.release();
                    mediaPlayersList.remove(it);
                    break;
                }
            }
        } finally {
            musicLock.unlock();
        }
    }

    private void PauseMusic(boolean pause) {
        //Контекст UI
        //pause == true : приостанавливаем
        //pause == false : запускаем
        musicLock.lock();
        try {
            for (int i = 0; i < mediaPlayersList.size(); i++) {

                MusicContent it = mediaPlayersList.elementAt(i);
                if (pause) {
                    if (it.player.isPlaying())
                        it.player.pause();
                } else {
                    float realVolume = GetRealVolume(it.volume);
                    it.player.setVolume(realVolume, realVolume);
                    it.player.start();
                }
            }
        } finally {
            musicLock.unlock();
        }
    }

    //******************************************************************************
    //******************************************************************************
    //****** / QSP  LIBRARY  REQUIRED  CALLBACKS \ *********************************
    //******************************************************************************
    //******************************************************************************
    private void RefreshInt() {
        //Контекст библиотеки
        JniResult htmlResult = (JniResult) QSPGetVarValues("USEHTML", 0);
        //CheckQspResult(htmlResult.success, "RefreshInt: QSPGetVarValues");
        final boolean html = htmlResult.success && (htmlResult.int1 != 0);


        //основное описание
        if (QSPIsMainDescChanged()) {
            final String txtMainDesc = QSPGetMainDesc();
            runOnUiThread(new Runnable() {
                public void run() {
                    String newPage = txtMainDesc;
                    //Change txMainDesc to UTF-8 encoding if possible
                    if (newPage == null || newPage.equals(""))
                        main_desc.loadDataWithBaseURL("", freshPageURL.replace("REPLACETEXT", ""), "text/html", "UTF-8", "");
                    else {
Utility.WriteLog("original: "+newPage);
                        newPage = Utility.encodeExec(newPage);
                        try {
                            //Decode the URL, but be sure to remove any % signs before doing so
                            //as these can cause crashes. Change back after URLDecoder.
                            newPage = newPage.replace("%", "-PERCENTSIGN-");
                            newPage = URLDecoder.decode(newPage, "UTF-8");
                            newPage = newPage.replace("-PERCENTSIGN-", "%");

                        } catch (UnsupportedEncodingException e) {
                            Utility.ShowError(uiContext, getString(R.string.urlNotComp).replace("-URLTEXT-", txtMainDesc));
                        }
                        //Return the visible (+) symbols to their normal form
                        newPage = newPage.replace("QSPPLUSSYMBOLCODE","+");

                        isMainDescHTML = html;
                        setMainDescHTML(newPage);
                        RefreshMainDesc();
                    }
                }
            });
        }

        //список действий
        if (QSPIsActionsChanged()) {
            int nActsCount = QSPGetActionsCount();
            if (cheatsActive) nActsCount++;
            final QSPItem[] acts = new QSPItem[nActsCount];
            if (cheatsActive) nActsCount--;
            for (int i = 0; i < nActsCount; i++) {
                JniResult actsResult = (JniResult) QSPGetActionData(i);
Utility.WriteLog("str1: "+actsResult.str1+"\nstr2: "+actsResult.str2);
                if (html)
                    acts[i] = new QSPItem(imgGetter.getDrawable(actsResult.str2),
                            Utility.QspStrToHtml(actsResult.str1, imgGetter, curGameDir,maxW,maxH, bigImage, hideImg, uiContext));
                else
                    acts[i] = new QSPItem(imgGetter.getDrawable(actsResult.str2), actsResult.str1);
            }
            if (cheatsActive) acts[nActsCount] = new QSPItem(null,cheatDesc);
            runOnUiThread(new Runnable() {
                public void run() {
                    ListView lvAct = (ListView) findViewById(R.id.acts);

                    mActListAdapter = new QSPListAdapter(uiContext, R.layout.act_item, acts);

                    lvAct.setAdapter(mActListAdapter);
                    //Разворачиваем список действий
                    Utility.setListViewHeightBasedOnChildren(lvAct);
                    if (mActListAdapter != null)
                        mActListAdapter.notifyDataSetChanged();

//                    if (clearOnChange) {
//                        origOrient = myActivity.getRequestedOrientation();
//                        RotateClear(myActivity);
//                        returnOrientation = true;
//                    }
                }
            });
        }

        //инвентарь
        if (QSPIsObjectsChanged()) {
            int nObjsCount = QSPGetObjectsCount();
            final QSPItem[] objs = new QSPItem[nObjsCount];
            for (int i = 0; i < nObjsCount; i++) {
                JniResult objsResult = (JniResult) QSPGetObjectData(i);
                if (html) {
                    objs[i] = new QSPItem(imgGetter.getDrawable(objsResult.str2),
                            Utility.QspStrToHtml(objsResult.str1, imgGetter, curGameDir, maxW, maxH, bigImage, hideImg, uiContext));
Utility.WriteLog("objsHTML str1: "+objsResult.str1+", str2: "+objsResult.str2);
                }
                else {
                    objs[i] = new QSPItem(imgGetter.getDrawable(objsResult.str2), objsResult.str1);
Utility.WriteLog("obsELSE str1: "+objsResult.str1+", str2: "+objsResult.str2);
                }
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    if ((currentWin != WIN_INV) && !holdPanelAnimationsForFirstUpdate) {
                        invUnread = true;
                        updateTitle();
                    }
                    ListView lvInv = (ListView) findViewById(R.id.inv);
                    mItemListAdapter = new QSPListAdapter(uiContext, R.layout.obj_item, objs);
                    lvInv.setAdapter(mItemListAdapter);
                }
            });
        }

        //доп. описание
        if (QSPIsVarsDescChanged()) {
            final String txtVarsDesc = QSPGetVarsDesc();
            runOnUiThread(new Runnable() {
                public void run() {
                    if ((currentWin != WIN_EXT) && !holdPanelAnimationsForFirstUpdate) {
                        varUnread = true;
                        updateTitle();
                    }
                    isVarsDescHTML = html;
                    setVarDescHTML(txtVarsDesc);
                    RefreshVarsDesc();
                    /*if (html) {
                        //vars_desc.setText(Utility.QspStrToHtml(txtVarsDesc, imgGetter, curGameDir));
                        //vars_desc.setMovementMethod(QspLinkMovementMethod.getInstance());
                        if (settings.getBoolean("showLoadingPage",true))
                            vars_desc.loadDataWithBaseURL("", freshPageURL.replace("REPLACETEXT", getString(R.string.loadingURL)), "text/html", "UTF-8", "");
                        String tempVars = Utility.QspStrToWebView(getVarDescHTML(),curGameDir,maxW,maxH,settings.getBoolean("sound",true), bigImage, videoSwitch, hideImg, uiContext);
                        vars_desc.loadDataWithBaseURL("",freshPageURL.replace("REPLACETEXT",tempVars),"text/html","UTF-8","");
                    } else {
                        //vars_desc.setText(txtVarsDesc);
                        if (settings.getBoolean("showLoadingPage",true))
                            vars_desc.loadDataWithBaseURL("", freshPageURL.replace("REPLACETEXT", getString(R.string.loadingURL)), "text/html", "UTF-8", "");
                        String tempVars = Utility.QspStrToStr(txtVarsDesc);
                        vars_desc.loadDataWithBaseURL("", freshPageURL.replace("REPLACETEXT", tempVars), "text/html", "UTF-8", "");
                    }*/
                }
            });
        }
        if (holdPanelAnimationsForFirstUpdate)
            runOnUiThread(new Runnable() {
                public void run() {
                    holdPanelAnimationsForFirstUpdate = false;
                }
            });
    }

    private void SetTimer(int msecs) {
        //Контекст библиотеки
        final int timeMsecs = msecs;
        runOnUiThread(new Runnable() {
            public void run() {
                timerInterval = timeMsecs;
            }
        });
    }

    private void ShowMessage(String message) {
        //Контекст библиотеки
        if (libThread == null) {
            Utility.WriteLog("ShowMessage: "+getString(R.string.failedS)+", libThread "+getString(R.string.isNullS));
            return;
        }

        String msgValue = "";
        if (message != null)
            msgValue = message;

        dialogHasResult = false;
        JniResult htmlResult = (JniResult) QSPGetVarValues("USEHTML", 0);
        final boolean html = htmlResult.success && (htmlResult.int1 != 0);

        final String msg = msgValue;
        runOnUiThread(new Runnable() {
            public void run() {
                AlertDialog msgBox = new AlertDialog.Builder(uiContext)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialogHasResult = true;
                                Utility.WriteLog("ShowMessage(UI): OK, unparking libThread");
                                setThreadUnpark();
                            }
                        })
                        .create();
                if (html)
                    msgBox.setMessage(Utility.QspStrToHtml(msg, imgGetter, curGameDir,maxW,maxH, bigImage, hideImg,uiContext));
                else
                    msgBox.setMessage(msg);
                msgBox.setCancelable(false);
                msgBox.show();
                Utility.WriteLog("ShowMessage(UI): msgBox.show()");
            }
        });

        Utility.WriteLog("ShowMessage: parking libThread");
        while (!dialogHasResult) {
            setThreadPark();
        }
        parkThread = null;
        Utility.WriteLog("ShowMessage: libThread unparked, "+getString(R.string.exitedS));
    }

    private void PlayFile(String file, int volume) {
        //Контекст библиотеки
        final String musicFile = Utility.QspPathTranslate(file);
        final int musicVolume = volume;
        runOnUiThread(new Runnable() {
            public void run() {
                PlayFileUI(musicFile, musicVolume);
            }
        });
    }

    private boolean IsPlayingFile(String file) {
        //Контекст библиотеки
        return IsPlayingFileUI(Utility.QspPathTranslate(file));
    }

    private void CloseFile(String file) {
        //Контекст библиотеки
        final String musicFile = Utility.QspPathTranslate(file);
        runOnUiThread(new Runnable() {
            public void run() {
                CloseFileUI(musicFile);
            }
        });
    }

    private void SaveGame(String file) {
        //Контекст библиотеки
        if (file == null || file.length() == 0)
            return;
        Toast.makeText(this, "SaveGame "+file, Toast.LENGTH_LONG).show();
        SaveSlot(file);
    }
    private void OpenGame(String file) {
        //Контекст библиотеки
        if (file == null || file.length() == 0)
            return;
        Toast.makeText(this, "OpenGame "+file, Toast.LENGTH_LONG).show();
        LoadSlot(file);
    }
    private void ShowPicture(String file)
    {
        //Контекст библиотеки
        if (file == null || file.length() == 0)
            return;

        final String fileName = Utility.QspPathTranslate(file);

        runOnUiThread(new Runnable() {
            public void run() {
                String prefix = "";

                if ((curGameDir != null) && !fileName.startsWith(curGameDir))
                    prefix = curGameDir;

                //Проверяем, существует ли файл.
                //Если нет - выходим
                File gfxFile = new File(prefix.concat(fileName));
                if (!gfxFile.exists())
                    return;

                waitForImageBox = true;

                Intent imageboxIntent = new Intent(QspPlayerStart.this, QspImageBox.class);
                //imageboxIntent.setClassName("com.qsp.player", "com.qsp.player.QspImageBox");
                Bundle b = new Bundle();
                b.putString("imageboxFile", prefix.concat(fileName));
                imageboxIntent.putExtras(b);
                startActivity(imageboxIntent);
            }
        });
    }

    private String InputBox(String prompt) {
        //Контекст библиотеки
        if (libThread == null) {
            Utility.WriteLog("InputBox: failed, libThread "+getString(R.string.isNullS));
            return "";
        }

        String promptValue = "";
        if (prompt != null)
            promptValue = prompt;

        dialogHasResult = false;
        JniResult htmlResult = (JniResult) QSPGetVarValues("USEHTML", 0);
        final boolean html = htmlResult.success && (htmlResult.int1 != 0);

        final String inputboxTitle = promptValue;

        runOnUiThread(new Runnable() {
            public void run() {
                inputboxResult = "";
                if (html)
                    inputboxDialog.setMessage(Utility.QspStrToHtml(inputboxTitle, imgGetter, curGameDir,maxW,maxH, bigImage,hideImg,uiContext));
                else
                    inputboxDialog.setMessage(inputboxTitle);
                inputboxDialog.show();
                Utility.WriteLog("InputBox(UI): inputboxDialog.show()");
            }
        });

        Utility.WriteLog("InputBox: parking libThread");
        while (!dialogHasResult) {
            setThreadPark();
        }
        parkThread = null;
        Utility.WriteLog("InputBox: libThread unparked, "+getString(R.string.exitedS));
        return inputboxResult;
    }

    private int GetMSCount() {
        //Контекст библиотеки
        return (int) (System.currentTimeMillis() - gameStartTime);
    }

    private void AddMenuItem(String name, String imgPath) {
        if ((imgPath != null) && (imgPath.length()>0)) {
            if (imgPath.startsWith("/")) imgPath = imgPath.replaceFirst("/", curGameDir);
            else imgPath += curGameDir;
        }

        //Контекст библиотеки
        QspMenuItem item = new QspMenuItem();
        item.imgPath = Utility.QspPathTranslate(imgPath);
        item.name = name;
        menuList.add(item);
    }

    private int ShowMenu() {
        //Контекст библиотеки
        if (libThread == null) {
            Utility.WriteLog("ShowMenu: "+getString(R.string.failedS)+", libThread "+getString(R.string.isNullS));
            return -1;
        }

        dialogHasResult = false;
        menuResult = -1;

        int total = menuList.size();
        final CharSequence[] items = new String[total];
        for (int i = 0; i < total; i++) {
            items[i] = menuList.elementAt(i).name;
            Utility.WriteLog("menuList["+i+"]: "+menuList.elementAt(i).imgPath+", "+menuList.elementAt(i).name);
        }

        runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(uiContext)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                menuResult = which;
                                dialogHasResult = true;
                                Utility.WriteLog("ShowMenu(UI): menuResult, unparking libThread");
                                setThreadUnpark();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                dialogHasResult = true;
                                Utility.WriteLog("ShowMenu(UI): "+getString(R.string.exitedS)+", unparking libThread");
                                setThreadUnpark();
                            }
                        })
                        .show();
                Utility.WriteLog("ShowMenu(UI): .show()");
            }
        });

        Utility.WriteLog("ShowMenu: parking libThread");
        while (!dialogHasResult) {
            setThreadPark();
        }
        parkThread = null;
        Utility.WriteLog("ShowMenu: libThread unparked, "+getString(R.string.exitedS));

        return menuResult;
    }

    private void DeleteMenu() {
        //Контекст библиотеки
        menuList.clear();
    }

    private void Wait(int msecs) {
        //Контекст библиотеки
        try {
            Thread.sleep(msecs);
        } catch (InterruptedException e) {
            Utility.WriteLog("WAIT - libThread(InterruptedException)");
            e.printStackTrace();
        }
    }
    //******************************************************************************
    //******************************************************************************
    //****** \ QSP  LIBRARY  REQUIRED  CALLBACKS / *********************************
    //******************************************************************************
    //******************************************************************************

    private void CheckQspResult(boolean successfull, String failMsg) {
        //Контекст библиотеки
        if (!successfull) {
            Utility.WriteLog(failMsg + " failed");
            JniResult error = (JniResult) QSPGetLastErrorData();
            error.str2 = QSPGetErrorDesc(error.int1);
            String locName = (error.str1 == null) ? "" : error.str1;
            String errDesc = (error.str2 == null) ? "" : error.str2;
            final String message = "Location: " + locName + "\n" +
                    "Action: " + String.valueOf(error.int2) + "\n" +
                    "Line: " + String.valueOf(error.int3) + "\n" +
                    "Error number: " + String.valueOf(error.int1) + "\n" +
                    "Description: " + errDesc;
            runOnUiThread(new Runnable() {
                public void run() {
                    Utility.ShowError(uiContext, message);
                }
            });
        }
    }

    private void actionExecute(int position) {
        if (libraryThreadIsRunning)
            return;
        final int actionIndex = position;
        libThreadHandler.post(new Runnable() {
            public void run() {
                if (libraryThreadIsRunning)
                    return;
                libraryThreadIsRunning = true;
                boolean result = QSPSetSelActionIndex(actionIndex, false);
                CheckQspResult(result, "actListClickListener: QSPSetSelActionIndex");
                result = QSPExecuteSelActionCode(true);
                CheckQspResult(result, "actListClickListener: QSPExecuteSelActionCode");
                libraryThreadIsRunning = false;
            }
        });
    }

    private void cheatExecute() {
        if (libraryThreadIsRunning)
            return;
        libThreadHandler.post(new Runnable() {
            public void run() {
                if (libraryThreadIsRunning)
                    return;
                libraryThreadIsRunning = true;

                String tempCode = Utility.prepareForExec(cheatAction);

                boolean bExec = QSPExecString(tempCode, true);
                CheckQspResult(bExec, "OnUrlClicked: QSPExecString");

                libraryThreadIsRunning = false;
            }
        });
    }

    public void OnUrlClicked(String href) {

        //Контекст UI
        if ((href == null) || (href.length() == 0))
            return;

        if (href.toLowerCase().startsWith("exec:")) {
            if (libraryThreadIsRunning)
                return;
            final String code = href.substring(5);
            libThreadHandler.post(new Runnable() {
                public void run() {
                    if (libraryThreadIsRunning)
                        return;
                    libraryThreadIsRunning = true;

                    //Exec commands with special characters in WebView must be
                    //changed back to special characters
                    String tempCode = Utility.prepareForExec(code);

                    boolean bExec = QSPExecString(tempCode, true);
                    CheckQspResult(bExec, "OnUrlClicked: QSPExecString");

                    libraryThreadIsRunning = false;
                }
            });
        } else {
            try {
                int actNum = Integer.parseInt(href);
                if ((actNum > 0) && (actNum <= QSP_MAXACTIONS)) {
                    actionExecute(actNum - 1);
                }
            } catch (NumberFormatException e) {
                String url = href;
                if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://"))
                    url = "http://" + url;
                Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
                try {
                    startActivity(viewIntent);
                } catch (ActivityNotFoundException viewException) {
                    Utility.ShowError(uiContext, getString(R.string.noBrowser));
                }
            }
        }
    }

    @Override
    public void OnImageClicked(String source) {
        ShowPicture(source);
    }

    //Callback for click on selected act
    private OnItemClickListener actListClickListener = new OnItemClickListener() {
        //Контекст UI
        @Override
        public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) {
            if (cheatsActive && (position == parent.getCount() -1))
                cheatExecute();
            else
                actionExecute(position);
        }
    };

    //Callback for select act
    private OnItemSelectedListener actListSelectedListener = new OnItemSelectedListener() {
        //Контекст UI
        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1,
                                   int arg2, long arg3) {
            if (libraryThreadIsRunning)
                return;
            final int actionIndex = arg2;
            libThreadHandler.post(new Runnable() {
                public void run() {
                    if (libraryThreadIsRunning)
                        return;
                    libraryThreadIsRunning = true;
                    boolean result = QSPSetSelActionIndex(actionIndex, true);
                    CheckQspResult(result, "actListSelectedListener: QSPSetSelActionIndex");
                    libraryThreadIsRunning = false;
                }
            });
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    };

    //Callback for click on selected object
    private OnItemClickListener objListClickListener = new OnItemClickListener() {
        //Контекст UI
        @Override
        public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) {
            if (libraryThreadIsRunning)
                return;
            final int itemIndex = position;
//Utility.WriteLog("OnItemClickListener()");
            libThreadHandler.post(new Runnable() {
                public void run() {
                    if (libraryThreadIsRunning)
                        return;
                    libraryThreadIsRunning = true;
                    boolean result = QSPSetSelObjectIndex(itemIndex, true);
                    CheckQspResult(result, "objListClickListener: QSPSetSelObjectIndex");
                    libraryThreadIsRunning = false;
                }
            });
        }
    };

    public void RotateClear(Activity myActivity) {
        if (myActivity == null) return;

        Configuration config = this.getResources().getConfiguration();

        //landscape -> portrait -> reverse landscape -> reverse potrait
        if (config.orientation == ORIENTATION_LANDSCAPE)
            myActivity.setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        else if (config.orientation == ORIENTATION_PORTRAIT)
            myActivity.setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);

        Utility.WriteLog("origOrient: "+origOrient+", temp: "+myActivity.getRequestedOrientation());

//        if (returnOrientation) {
//            myActivity.setRequestedOrientation(origOrient);
//            Utility.WriteLog("Returning to origOrient: "+origOrient);
//        }
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == ORIENTATION_LANDSCAPE) {
Utility.WriteLog("Config changed: landscape");
//            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
            hideTitle();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
Utility.WriteLog("Config changed: portrait");
//            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
            showTitle();
        }
        if (gameIsRunning) {
            SetImageLimits();
            ApplyViewSettings();
//Utility.WriteLog("maxH: "+maxH+", maxW: "+maxW);
            RefreshMainDesc();
            RefreshVarsDesc();
       }

//        if (returnOrientation) {
//            RotateClear(myActivity);
//            returnOrientation = false;
//        }
    }

    public void hideTitle() {
        try {
            ((View) findViewById(android.R.id.title).getParent())
                    .setVisibility(View.GONE);
        } catch (Exception e) {
        }
        getActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

    }

    public void showTitle() {
        try {
            ((View) findViewById(android.R.id.title).getParent())
                    .setVisibility(View.VISIBLE);
        } catch (Exception e) {
        }
        getActionBar().show();
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

    }



    //Для отображения картинок в HTML
    static QspImageGetter imgGetterDesc = new QspImageGetter();
    static QspImageGetter imgGetter = new QspImageGetter();

    //Хэндлер для UI-потока
    final Handler uiThreadHandler = new Handler();

    //Хэндлер для потока библиотеки
    private Handler libThreadHandler;

    //Поток библиотеки
    private Thread libThread;
    private Thread parkThread;

    //Запущен ли поток библиотеки
    boolean libraryThreadIsRunning = false;

    //Есть ответ от MessageBox, InputBox либо Menu
    private boolean dialogHasResult;
    String inputboxResult;
    int menuResult;
    AlertDialog inputboxDialog;

    String curGameDir;
    String curGameFile;
    String curSaveTitle;
    String savePrefix;

// saveGameDir identifies internal data storage for app; resolves write-permission problem with Android SD Cards
    String saveGameDir;
    String curRelGameDir;

    Vector<MusicContent> mediaPlayersList;
    Handler timerHandler;
    long timerStartTime;
    long gameStartTime;
    int timerInterval;
    boolean gameIsRunning;
    boolean qspInited;
    boolean waitForImageBox;
    Vector<QspMenuItem> menuList;


    QspLinkMovementMethod qspLinkMovementMethod;


    //control
    public native void QSPInit();

    public native void QSPDeInit();

    public native boolean QSPIsInCallBack();

    public native void QSPEnableDebugMode(boolean isDebug);

    public native Object QSPGetCurStateData();//!!!STUB

    public native String QSPGetVersion();

    public native int QSPGetFullRefreshCount();

    public native String QSPGetQstFullPath();

    public native String QSPGetCurLoc();

    public native String QSPGetMainDesc();

    public native boolean QSPIsMainDescChanged();

    public native String QSPGetVarsDesc();

    public native boolean QSPIsVarsDescChanged();

    public native Object QSPGetExprValue();//!!!STUB

    public native void QSPSetInputStrText(String val);

    public native int QSPGetActionsCount();

    public native Object QSPGetActionData(int ind);//!!!STUB

    public native boolean QSPExecuteSelActionCode(boolean isRefresh);

    public native boolean QSPSetSelActionIndex(int ind, boolean isRefresh);

    public native int QSPGetSelActionIndex();

    public native boolean QSPIsActionsChanged();

    public native int QSPGetObjectsCount();

    public native Object QSPGetObjectData(int ind);//!!!STUB

    public native boolean QSPSetSelObjectIndex(int ind, boolean isRefresh);

    public native int QSPGetSelObjectIndex();

    public native boolean QSPIsObjectsChanged();

    public native void QSPShowWindow(int type, boolean isShow);

    public native Object QSPGetVarValuesCount(String name);

    public native Object QSPGetVarValues(String name, int ind);//!!!STUB

    public native int QSPGetMaxVarsCount();

    public native Object QSPGetVarNameByIndex(int index);//!!!STUB

    public native boolean QSPExecString(String s, boolean isRefresh);

    public native boolean QSPExecLocationCode(String name, boolean isRefresh);

    public native boolean QSPExecCounter(boolean isRefresh);

    public native boolean QSPExecUserInput(boolean isRefresh);

    public native Object QSPGetLastErrorData();

    public native String QSPGetErrorDesc(int errorNum);

    public native boolean QSPLoadGameWorld(String fileName);

    public native boolean QSPLoadGameWorldFromData(byte data[], int dataSize, String fileName);

    public native boolean QSPSaveGame(String fileName, boolean isRefresh);

    public native byte[] QSPSaveGameAsData(boolean isRefresh);

    public native boolean QSPOpenSavedGame(String fileName, boolean isRefresh);

    public native boolean QSPOpenSavedGameFromData(byte data[], int dataSize, boolean isRefresh);

    public native boolean QSPRestartGame(boolean isRefresh);
    //public native void QSPSetCallBack(int type, QSP_CALLBACK func) 

    static {
        System.loadLibrary("ndkqsp");
    }

}