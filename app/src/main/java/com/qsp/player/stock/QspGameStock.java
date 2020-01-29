package com.qsp.player.stock;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.SparseArrayCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentTransaction;

import com.qsp.player.FileUtil;
import com.qsp.player.PermissionUtil;
import com.qsp.player.R;
import com.qsp.player.Utility;
import com.qsp.player.game.QspPlayerStart;
import com.qsp.player.settings.Settings;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class QspGameStock extends AppCompatActivity {

    final private Context uiContext = this;
    private String xmlGameListCached;
    private boolean openDefaultTab;
    private boolean gameListIsLoading;
    private boolean triedToLoadGameList;
    private GameItem selectedGame;

    private String SDPath;

    private boolean gameIsRunning;
    private boolean startingGSUp = true;

    public static final int MAX_SPINNER = 1024;
    public static final int TAB_DOWNLOADED = 0;
    public static final int TAB_STARRED = 1;
    public static final int TAB_ALL = 2;
    public static final int REQUEST_CODE_STORAGE_ACCESS = 42;

    public static final String GAME_INFO_FILENAME = "gamestockInfo";
    private static final int REQUEST_CODE_EXTERNAL_STORAGE = 1;

    public static DocumentFile downloadDir = null;

    private static String defaultQSPtextColor = "#ffffff";
    private static String defaultQSPbackColor = "#000000";
    private static String defaultQSPlinkColor = "#0000ee";
    private static String defaultQSPactsColor = "#ffffd7";
    private static String defaultQSPfontTheme = "0";

    private static String QSPtextColor = defaultQSPtextColor;
    private static String QSPbackColor = defaultQSPbackColor;
    private static String QSPlinkColor = defaultQSPlinkColor;
    private static String QSPactsColor = defaultQSPactsColor;
    private static String QSPfontTheme = defaultQSPfontTheme;

    private boolean isActive;
    private boolean showProgressDialog;

    private String _zipFile;
    private String _location;

    String					startRootPath;
    String					backPath;
    ArrayList<File> 		qspGamesBrowseList;
    ArrayList<File> 		qspGamesToDeleteList;

    SharedPreferences settings;
    String userSetLang;
    String curLang = Locale.getDefault().getLanguage();
    boolean usingSDcard = true;
    String relGameDir = "QSP/games/";
    String compGameDir = "/storage/sdcard1/QSP/games/";

    HashMap<String, GameItem> gamesMap;

    ListView lvGames;
    ProgressDialog downloadProgressDialog = null;
    Thread downloadThread = null;

    private NotificationManager mNotificationManager;
    private int QSP_NOTIFICATION_ID;
    private SparseArrayCompat<GameAdapter> gameAdapters = new SparseArrayCompat<>();
    
    public QspGameStock() {
        Utility.WriteLog("[G]constructor\\");
        gamesMap = new HashMap<String, GameItem>();
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //orientationRecreate = true;
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }

    private void setGSLocale(String lang) {
        Locale myLocale;
        Utility.WriteLog("Base Language = "+lang);

        //if TAIWAN
        if (lang.equals("zh-rTW")) {
            myLocale = Locale.TAIWAN;
            Utility.WriteLog("Language = TAIWAN, "+lang);
        }
        //if CHINA
        else if (lang.equals("zh-rCN")) {
            myLocale = Locale.CHINA;
            Utility.WriteLog("Language = CHINA, "+lang);
        }
        //if lang doesn't contain a region code
        else if (!lang.contains("-r"))
            myLocale = new Locale(lang);
        //if lang is not TAIWAN, CHINA, or short, use country+region
        else {
            String myRegion = lang.substring(lang.indexOf("-r")+2);
            String myLang = lang.substring(0,2);
            Utility.WriteLog("Language = "+myLang+", Region = "+myRegion);
            myLocale = new Locale(lang,myRegion);
        }
        Resources newRes = getResources();


        DisplayMetrics dm = newRes.getDisplayMetrics();
        Configuration conf = newRes.getConfiguration();
        conf.locale = myLocale;
        newRes.updateConfiguration(conf, dm);
        setTitle(getString(R.string.menu_gamestock));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utility.WriteLog("[G]onCreate\\");
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gamestock);

        PermissionUtil.requestPermissionsIfNotGranted(
                this,
                REQUEST_CODE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        userSetLang = settings.getString("lang", "en");
        curLang = userSetLang;
        setGSLocale(userSetLang);

        getDownloadDirFromSettings();

        setFullGamesPath(checkDownloadDirectory());

        Intent gameStockIntent = getIntent();
        gameIsRunning = gameStockIntent.getBooleanExtra("game_is_running", false);
        startingGSUp = true;

        isActive = false;
        showProgressDialog = false;

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        TabListener tabListener = new TabListener();
        ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.addTab(bar.newTab()
                .setText(R.string.tab_downloaded)
                .setTabListener(tabListener), false);

        bar.addTab(bar.newTab()
                .setText(R.string.tab_starred)
                .setTabListener(tabListener), false);

        bar.addTab(bar.newTab()
                .setText(R.string.tab_all)
                .setTabListener(tabListener), false);

        openDefaultTab = true;
        gameListIsLoading = false;
        triedToLoadGameList = false;

        xmlGameListCached = null;

        InitListViews(savedInstanceState);

        setResult(RESULT_CANCELED);

        //TODO:
        // 1v. Отображение статуса "Загружено", например цветом фона.
        // 2. Авто-обновление игр
        // 3. Кэширование списка игр
        // 4v. Доступ к играм, даже когда сервер недоступен
        // 5. Вывод игр в папке "Загруженные" в порядке последнего доступа к ним
        // 6v. Возможность открыть файл из любой папки(через специальное меню этой активити)
        // 7v. Доступ к настройкам приложения через меню этой активити
        Utility.WriteLog("[G]onCreate/");
    }

    @Override
    public void onResume()
    {
        Utility.WriteLog("[G]onResume\\");

        //Set the language if it has changed
        boolean langChanged = false;
        userSetLang = settings.getString("lang","en");
        Utility.WriteLog("userSetLang = "+userSetLang+", curLang = "+curLang);
        if (!curLang.equals(userSetLang)) {
            Utility.WriteLog("GameStock:"+userSetLang+" <> "+curLang+", setting language");
            curLang = userSetLang;
            langChanged = true;
            setGSLocale(userSetLang);
            settings = PreferenceManager.getDefaultSharedPreferences(this);
            Utility.WriteLog(curLang+" <- "+userSetLang);
        } else
            Utility.WriteLog("GameStock:"+userSetLang+" == "+curLang+", no change");

        //Set the storage location/games directory
        usingSDcard = settings.getBoolean("storageType",true);

        Utility.WriteLog("getFullGamesPath = "+getFullGamesPath());
        getDownloadDirFromSettings();

        if (downloadDir != null)
            setFullGamesPath(true);
        else {
            setFullGamesPath(false);
        }

        RefreshLists();
//end storage/directory settings

        ApplyFontTheme();

        isActive = true;

        //Refresh QspGameStock tabs if the user changed the language
        if (langChanged) {
            RefreshAllTabs();
            setTitle(getString(R.string.menu_gamestock));
            invalidateOptionsMenu();
        }


        if (downloadDir == null) {
            ShowNoDownloadDir();
        }

        super.onResume();
        Utility.WriteLog("[G]onResume/");
    }

    private void ApplyFontTheme () {
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
            return;
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
        //Set theme to custom and then save all colors as "Custom" color values
        if(setCustom && settings.getString("theme",getString(R.string.deftheme)) != getString(R.string.customtheme)) {
            SharedPreferences.Editor ed = settings.edit();
            ed.putString("theme", getString(R.string.customtheme));
            ed.putInt("customtextColor",Color.parseColor(QSPtextColor));
            ed.putInt("custombackColor",Color.parseColor(QSPbackColor));
            ed.putInt("customlinkColor",Color.parseColor(QSPlinkColor));
            ed.putInt("customactsColor",Color.parseColor(QSPactsColor));
            ed.apply();
        }
    }


    //Call only if settings is initialized
    public String getFullGamesPath () {
        return settings.getString("compGamePath", getString(R.string.defGamePath));
    }

    public void setFullGamesPath (boolean useDownloadDir) {
        if (!useDownloadDir) {
            //Get external SD card flag and get directory; set extSDCard false if no external SD card
            boolean extSDCard = settings.getBoolean("storageType", true);
            if (extSDCard) {
                SDPath = System.getenv("SECONDARY_STORAGE");

                //if SECONDARY_STORAGE fails, try EXTERNAL_SDCARD_STORAGE
                if (null == SDPath)
                    SDPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
                else if (SDPath.length() == 0)
                    SDPath = System.getenv("EXTERNAL_SDCARD_STORAGE");

                //if EXTERNAL_SDCARD_STORAGE fails, check all directories in /storage/ for usable path
                if ((null == SDPath) || (SDPath.length() == 0)) {
                    Utility.WriteLog("internal DIR: " + Environment.getExternalStorageDirectory().getAbsolutePath());
                    File internalFileList[] = new File(Environment.getExternalStorageDirectory().getAbsolutePath()).listFiles();

                    File fileList[] = new File("/storage/").listFiles();
                    for (File file : fileList) {
                        Utility.WriteLog("storage DIR: " + file.getAbsolutePath());

                        //A suitable candidate (directory/readable/not internal) is found...
                        if (!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()) && file.isDirectory() && file.canRead()) {

                            //Check that it is not the internal directory under another name
                            File emulatedFileList[] = file.listFiles();
                            if (Utility.directoriesAreEqual(internalFileList, emulatedFileList)) continue;

                            //if it is not the internal storage, it must be the external storage
                            SDPath = file.getAbsolutePath();
                            Utility.WriteLog("chosen DIR: " + file.getAbsolutePath());
                            break;
                        }
                    }
                }

                //if that fails, go to the internal directory and set extSDCard as false
                if (null == SDPath) {
                    SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                    extSDCard = false;
                } else if (SDPath.length() == 0) {
                    SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                    extSDCard = false;
                }
            } else
                SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            SDPath += "/";
            Utility.WriteLog("TEMP:" + SDPath);
            //Get the relative games path and merge the two directories
            //Add a trailing "/" to relPath and remove change "//" to "/" if present
            String relPath = settings.getString("relGamePath", getString(R.string.defRelPath));
            if (!relPath.endsWith("/")) relPath += "/";
            if (!relPath.startsWith("/")) relPath = "/" + relPath;
            String fullGamesPath = SDPath + relPath;
            relPath = relPath.replace("//", "/");
            fullGamesPath = fullGamesPath.replace("//", "/");

            //Store adjusted storage type, relative path, and complete game directory
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("storageType", extSDCard);
            editor.putString("relGamePath", relPath);
            editor.putString("compGamePath", fullGamesPath);
            editor.commit();


            Utility.WriteLog("_NOT using downloadDir DocumentFile_");
            Utility.WriteLog("storageType: " + settings.getBoolean("storageType", true));
            Utility.WriteLog("relGamePath: " + settings.getString("relGamePath", getString(R.string.defRelPath)));
            Utility.WriteLog("compGamePath: " + settings.getString("compGamePath", getString(R.string.defGamePath)));
        }

        //if useDownloadDir is true...
        else {

            //**** START set SDPath/storageType ****
            String newDownDirPath = getString(R.string.defDownDirPath);
            if (downloadDir != null) {
                newDownDirPath = FileUtil.getFullPathFromTreeUri(downloadDir.getUri(),uiContext);
                if (!newDownDirPath.endsWith("/"))
                    newDownDirPath += "/";
            }

            String tempDir = FileUtil.getSdCardPath();
            Boolean usingExtSDCard = false;
            if (newDownDirPath.startsWith(tempDir)) {
                Utility.WriteLog(newDownDirPath + " contains " + tempDir);
                SDPath = tempDir;
            }
            else {
                Utility.WriteLog(newDownDirPath+" doesn't contain "+tempDir);

                ArrayList<String> extSDPaths = FileUtil.getExtSdCardPaths(uiContext);
                for (int i=0; i<extSDPaths.size(); i++) {
                    tempDir = extSDPaths.get(i);
                    if (newDownDirPath.startsWith(tempDir)) {
                        Utility.WriteLog(newDownDirPath+" contains "+tempDir);
                        SDPath = tempDir;
                        usingExtSDCard = true;
                        break;
                    }
                    else Utility.WriteLog(newDownDirPath+" doesn't contain "+tempDir);
                }
            }
            //**** END set SDPath/storageType ****

            //set String "relGamePath"; "compGamePath" is newDownDirPath
            String relPath = newDownDirPath.replace(SDPath,"");

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
    }


    @Override
    public void onPostResume()
    {
        Utility.WriteLog("[G]onPostResume\\");
        super.onPostResume();
        if (showProgressDialog && (downloadProgressDialog != null))
        {
            downloadProgressDialog.show();
        }
        Utility.WriteLog("[G]onPostResume/");
    }
    
    @Override
    public void onPause() {
        Utility.WriteLog("[G]onPause\\");
        isActive = false;
        if (showProgressDialog && (downloadProgressDialog != null))
        {
            downloadProgressDialog.dismiss();
        }
        super.onPause();
        Utility.WriteLog("[G]onPause/");
    }

    @Override
    public void onDestroy()
    {
        Utility.WriteLog("[G]onDestroy\\");
        Utility.WriteLog("[G]onDestroy/");
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putString("gameList", xmlGameListCached);
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        Utility.WriteLog("[G]onNewIntent\\");
        super.onNewIntent(intent);
        Utility.WriteLog("[G]onNewIntent/");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Загружаем меню из XML-файла
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gamestock_menu, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        //Прячем или показываем пункт меню "Продолжить игру"
        MenuItem resumeGameItem = menu.findItem(R.id.menu_resumegame);
        resumeGameItem.setVisible(gameIsRunning);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Контекст UI
        switch (item.getItemId()) {
            case R.id.menu_resumegame:
                //Продолжить игру
                setResult(RESULT_CANCELED, null);
                finish();
                return true;

            case R.id.menu_options:
                Intent intent = new Intent();
                intent.setClass(this, Settings.class);
                startActivity(intent);
                return true;

            case R.id.menu_about:
                showAbout();
                return true;
/*            	Intent updateIntent = null;
                updateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.market_details_url)));
                startActivity(updateIntent);
                return true;
*/
            case R.id.menu_openfile:
            // ** original code for BrowseGame directory checking **
                if (!settings.getBoolean("storageType",true))
                    BrowseGame(Environment.getExternalStorageDirectory().getPath(), true);
                else if (settings.getBoolean("storageType",true)) {
                // ** begin replacement code for checking storage directory **
                    String strSDCardPath = System.getenv("SECONDARY_STORAGE");
                    if (null == strSDCardPath)
                        strSDCardPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
                    else if (strSDCardPath.length() == 0)
                        strSDCardPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
                    BrowseGame(strSDCardPath, true);
                // ** end replacement code for checking storage directory **
                }
                return true;

            case R.id.menu_deletegames:
                DeleteGames();
                return true;

        }        
        return false;
    }

    //Show About dialog
    protected void showAbout() {
        // Inflate the about message contents
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        String tempFont = "\"\"";
        switch (Integer.parseInt(settings.getString("typeface", "0"))) {
            case 0:
                tempFont = "DEFAULT";
                break;
            case 1:
                tempFont = "sans-serif";
                break;
            case 2:
                tempFont = "serif";
                break;
            case 3:
                tempFont = "courier";
                break;
        }

        String tempText = String.format("#%06X",(0xFFFFFF & settings.getInt("textColor", Color.parseColor("#ffffff"))));
        String tempBack = String.format("#%06X",(0xFFFFFF & settings.getInt("backColor", Color.parseColor("#000000"))));
        String tempLink = String.format("#%06X",(0xFFFFFF & settings.getInt("linkColor", Color.parseColor("#0000ee"))));
        String tempSize = settings.getString("fontsize","16");

        String descrip = getString(R.string.about_template);
        descrip = descrip.replace("QSPTEXTCOLOR",tempText).replace("QSPBACKCOLOR",tempBack).replace("QSPLINKCOLOR",tempLink).replace("QSPFONTSIZE",tempSize).replace("QSPFONTSTYLE",tempFont);
        descrip = descrip.replace("REPLACETEXT", getString(R.string.app_descrip)+getString(R.string.app_credits));

        WebView descView = (WebView) messageView.findViewById(R.id.about_descrip);
        if (descView == null) Utility.WriteLog("descView null");
        descView.loadDataWithBaseURL("",descrip,"text/html","utf-8","");


        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) { }	});

        builder.setView(messageView);
        builder.create();
        builder.show();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        //Ловим кнопку "Back", и не закрываем активити, а только
        //отправляем в бэкграунд (как будто нажали "Home"),
        //при условии, что запущен поток скачивания игры
        if ((downloadThread != null) && downloadThread.isAlive() && 
                (keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
            moveTaskToBack(true);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void InitListViews(Bundle savedInstanceState)
    {
        lvGames = findViewById(R.id.games);
        lvGames.setTextFilterEnabled(true);
        lvGames.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lvGames.setOnItemClickListener(gameListClickListener);
        lvGames.setOnItemLongClickListener(gameListLongClickListener);

        //Забираем список игр из предыдущего состояния активити, если оно было пересоздано (при повороте экрана)
        if (savedInstanceState != null) {
            xmlGameListCached = savedInstanceState.getString("gameList");
        }

        RefreshLists();
    }

    private void Notify(String text, String details)
    {
            Intent notificationIntent = new Intent(uiContext, QspPlayerStart.class);
            notificationIntent.setAction(Intent.ACTION_MAIN);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            PendingIntent contentIntent = PendingIntent.getActivity(uiContext, 0, notificationIntent, 0); 

            Notification note = new Notification.Builder(uiContext)
                    .setSmallIcon(android.R.drawable.stat_notify_sdcard)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setSubText(details)
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .build();

            mNotificationManager.notify(
                       QSP_NOTIFICATION_ID, // we use a string id because it is a unique
                                            // number.  we use it later to cancel the
                                            // notification
                       note);
    }

    private String getGameIdByPosition(int position)
    {
        final GameItem game = (GameItem) lvGames.getAdapter().getItem(position);
        return game.game_id;
    }

    //Выбрана игра в списке
    private OnItemClickListener gameListClickListener = new OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3)
        {
            String value = getGameIdByPosition(position);
            ShowGameInfo(value);
        }
    };
    
    //Выбрана игра в списке(LongClick)
    private OnItemLongClickListener gameListLongClickListener = new OnItemLongClickListener() 
    {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View arg1, int position, long arg3)
        {
            String value = getGameIdByPosition(position);
            selectedGame = gamesMap.get(value);
            if (selectedGame.downloaded){
                //Если игра загружена, стартуем
                Intent data = new Intent();
                data.putExtra("file_name", selectedGame.game_file);
                setResult(RESULT_OK, data);
                finish();
            } else {
                //иначе загружаем
                checkDownloadDirectory();
                DownloadGame(selectedGame.file_url, selectedGame.file_size, selectedGame.game_id);
            }
            return true;
        }
    };

    private boolean checkDownloadDirectory() {
        //Returns true if downloadDir is not null, exists, is a directory, and is writable
        //returns false otherwise

        if (downloadDir == null) {
            getDownloadDirFromSettings();

            if (downloadDir == null) return false;
        }

        //check if downloadDir exists, is a directory and is writable. If not, get it from Settings
        if (downloadDir.exists() && downloadDir.isDirectory() && downloadDir.canWrite()) {
            //Make sure to set the game path in case downloadDir hasn't been used to update
            if (!FileUtil.getFullPathFromTreeUri(downloadDir.getUri(),uiContext).equals(getFullGamesPath()))
                setFullGamesPath(true);
            return true;
        }
        else {
            getDownloadDirFromSettings();

            if (downloadDir == null) return false;
        }

        return false;
    }

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
            setFullGamesPath(true);
    }





    private void ShowGameInfo(String gameId)
    {
        selectedGame = gamesMap.get(gameId);
        if (selectedGame == null)
            return;

            StringBuilder txt = new StringBuilder();
            if (selectedGame.game_file.contains(" ")) {
                txt.append(getString(R.string.spaceWarn)+"\n");
            }
            if(selectedGame.author.length()>0)
                txt.append("Author: ").append(selectedGame.author);
            if(selectedGame.version.length()>0)
                txt.append("\nVersion: ").append(selectedGame.version);
            if(selectedGame.file_size>0)
                txt.append("\nSize: ").append(selectedGame.file_size/1024).append(" Kilobytes");
Utility.WriteLog("Dialog txt: "+txt);
            AlertDialog.Builder bld = new AlertDialog.Builder(uiContext).setMessage(txt)
            .setTitle(selectedGame.title)
            .setIcon(R.drawable.icon)
            .setPositiveButton((selectedGame.downloaded ? getString(R.string.playGameCmd) : getString(R.string.dlGameCmd)), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (selectedGame.downloaded){
                    //Если игра загружена, стартуем
                    Intent data = new Intent();
                    data.putExtra("file_name", selectedGame.game_file);
                    setResult(RESULT_OK, data);
                    finish();
                }else
                    //иначе загружаем
                    DownloadGame(selectedGame.file_url, selectedGame.file_size, selectedGame.game_id);
            }
        })
            .setNegativeButton(getString(R.string.closeGameCmd), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
            bld.create().show();
    }
    
    private void DownloadGame(String file_url, int file_size, String game_id)
    {
        if (!Utility.haveInternet(uiContext))
        {
            Utility.ShowError(uiContext, getString(R.string.gameLoadNetError));
            return;
        }
        GameItem gameToDownload = gamesMap.get(game_id);
        String folderName = Utility.ConvertGameTitleToCorrectFolderName(gameToDownload.title);

        final String urlToDownload = file_url;
        final String unzipLocation = Utility.GetDownloadPath(this).concat("/").concat(folderName).concat("/");
        final String gameId = game_id;
        final String gameName = gameToDownload.title;
        final String gamesPath = Utility.GetDownloadPath(this);
        final int totalSize = file_size;
        downloadProgressDialog = new ProgressDialog(uiContext);
        downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        downloadProgressDialog.setMax(totalSize);
        downloadProgressDialog.setCancelable(false);


        downloadThread = new Thread() {
            public void run() {
                //set the path where we want to save the file
                //in this case, going to save it in program cache directory
                //on sd card.

                final File cacheDir = QspGameStock.this.getCacheDir();
                if (!cacheDir.exists())
                {
                    if (!cacheDir.mkdirs())
                    {
                        Utility.WriteLog(getString(R.string.cacheCreateError));
                        return;
                    }
                }

                //create a new file, specifying the path, and the filename
                //which we want to save the file as.
                String filename = String.valueOf(System.currentTimeMillis()).concat("_game.zip");
                File file = new File(cacheDir, filename);

                boolean successDownload = false;

                try {
                    //set the download URL, a url that points to a file on the internet
                    //this is the file to be downloaded
                    URL url = new URL(urlToDownload);

                    //create the new connection
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                    //set up some things on the connection
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setDoOutput(true);

                    //and connect!
                    urlConnection.connect();

                    //this will be used to write the downloaded data into the file we created
                    FileOutputStream fileOutput = new FileOutputStream(file);

                    //this will be used in reading the data from the internet
                    InputStream inputStream = urlConnection.getInputStream();

                    //create a buffer...
                    byte[] buffer = new byte[1024];
                    int bufferLength = 0; //used to store a temporary size of the buffer
                    int downloadedCount = 0;

                    //now, read through the input buffer and write the contents to the file
                    while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                        //add the data in the buffer to the file in the file output stream (the file on the sd card
                        fileOutput.write(buffer, 0, bufferLength);
                        //this is where you would do something to report the prgress, like this maybe
                        downloadedCount += bufferLength;
                        updateSpinnerProgress(true, gameName, getString(R.string.dlWaiting), -downloadedCount);
                    }
                    //close the output stream when done
                    fileOutput.close();
                    successDownload = totalSize == downloadedCount;
                } catch (Exception e) {
                    e.printStackTrace();
                    Utility.WriteLog(getString(R.string.dlGameError));
                }

                updateSpinnerProgress(false, "", "", 0);

                final String checkGameName = gameName;
                final String checkGameId = gameId;

                if (!successDownload)
                {
                    if (file.exists())
                        file.delete();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            String desc = getString(R.string.cantDlGameError).replace("-GAMENAME-",checkGameName);
                            if (isActive)
                                Utility.ShowError(uiContext, desc);
                            else
                                Notify(getString(R.string.genGameLoadError), desc);
                        }
                    });
                    return;
                }

                //Unzip
                Unzip(file.getPath(), unzipLocation, gameName);
                file.delete();
                //Пишем информацию об игре
                WriteGameInfo(gameId);

                updateSpinnerProgress(false, "", "", 0);

                runOnUiThread(new Runnable() {
                    public void run() {
                        RefreshLists();
                        GameItem selectedGame = gamesMap.get(checkGameId);

                        //Если игра не появилась в списке, значит она не соответствует формату "Полки игр"
                        boolean success = (selectedGame != null) && selectedGame.downloaded;

                        if (!success)
                        {
                            if (downloadDir == null) {
                                //Удаляем неудачно распакованную игру
                                File gameFolder = new File(gamesPath.concat("/").concat(Utility.ConvertGameTitleToCorrectFolderName(checkGameName)));
                                Utility.DeleteRecursive(gameFolder);
                            }
                            else {
                                DocumentFile gameFolder = downloadDir.findFile(Utility.ConvertGameTitleToCorrectFolderName(checkGameName));
                                if (gameFolder != null) Utility.DeleteDocFileRecursive(gameFolder);
                            }
                        }

                        if (isActive)
                        {
                            if ( !success )
                            {
                                //Показываем сообщение об ошибке
                                Utility.ShowError(uiContext, getString(R.string.cantUnzipGameError).replace("-GAMENAME-", "\""+checkGameName+"\""));
                            }
                            else
                            {
                                ShowGameInfo(checkGameId);
                            }
                        }
                        else
                        {
                            String msg = null;
                            String desc = null;
                            if ( !success )
                            {
                                msg = getString(R.string.genGameLoadError);
                                desc = getString(R.string.cantUnzipGameError).replace("-GAMENAME-", "\""+checkGameName+"\"");
                            }
                            else
                            {
                                msg = getString(R.string.gameDlSuccess);
                                desc = getString(R.string.gameUploadSuccess).replace("-GAMENAME-","\""+checkGameName+"\"");
                            }
                            Notify(msg, desc);
                        }
                    }
                });
            }
        };
        downloadThread.start();
    }
    
    private void Unzip(String zipFile, String location, String gameName)
    {
        _zipFile = zipFile;
        _location = location;

        runOnUiThread(new Runnable() {
            public void run() {
                downloadProgressDialog = new ProgressDialog(uiContext);
                downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                downloadProgressDialog.setCancelable(false);
            }
        });
        updateSpinnerProgress(true, gameName, getString(R.string.unpackMsg), 0);

        if (downloadDir != null) {
            String folderName = Utility.ConvertGameTitleToCorrectFolderName(gameName);

            try {
                DocumentFile gameFolder = downloadDir.findFile(folderName);
                if (gameFolder == null)
                    gameFolder = downloadDir.createDirectory(folderName);

                ZipUtil.unzip(_zipFile, gameFolder, this);
            } catch (Exception e) {
                Log.e(getString(R.string.decompMsg), getString(R.string.unzipMsgShort), e);
            }

        }
        else {
            try {
                ZipUtil.unzip(_zipFile, _location);
            } catch (Exception e) {
                Log.e(getString(R.string.decompMsg), getString(R.string.unzipMsgShort), e);
            }
        }
    }

    private void WriteGameInfo(String gameId)
    {
        //Записываем всю информацию об игре
        GameItem game = gamesMap.get(gameId);
        if (game==null)
            return;

        File gameFolder = new File(Utility.GetGamesPath(this).concat("/").concat(Utility.ConvertGameTitleToCorrectFolderName(game.title)));
        if (!gameFolder.exists())
            return;

        String infoFilePath = gameFolder.getPath().concat("/").concat(GAME_INFO_FILENAME);
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(infoFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Utility.WriteLog(getString(R.string.gameInfoFileCreateError));
            return;
        }
        OutputStreamWriter osw = new OutputStreamWriter(fOut);

        try {
            osw.write("<game>\n");
            osw.write("\t<id><![CDATA[".concat(game.game_id.substring(3)).concat("]]></id>\n"));
            osw.write("\t<list_id><![CDATA[".concat(game.list_id).concat("]]></list_id>\n"));
            osw.write("\t<author><![CDATA[".concat(game.author).concat("]]></author>\n"));
            osw.write("\t<ported_by><![CDATA[".concat(game.ported_by).concat("]]></ported_by>\n"));
            osw.write("\t<version><![CDATA[".concat(game.version).concat("]]></version>\n"));
            osw.write("\t<title><![CDATA[".concat(game.title).concat("]]></title>\n"));
            osw.write("\t<lang><![CDATA[".concat(game.lang).concat("]]></lang>\n"));
            osw.write("\t<player><![CDATA[".concat(game.player).concat("]]></player>\n"));
            osw.write("\t<file_url><![CDATA[".concat(game.file_url).concat("]]></file_url>\n"));
            osw.write("\t<file_size><![CDATA[".concat(String.valueOf(game.file_size)).concat("]]></file_size>\n"));
            osw.write("\t<desc_url><![CDATA[".concat(game.desc_url).concat("]]></desc_url>\n"));
            osw.write("\t<pub_date><![CDATA[".concat(game.pub_date).concat("]]></pub_date>\n"));
            osw.write("\t<mod_date><![CDATA[".concat(game.mod_date).concat("]]></mod_date>\n"));
            osw.write("</game>");

            osw.flush();
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
            Utility.WriteLog(getString(R.string.gameInfoFileWriteError));
            return;
        }
    }

    private void updateSpinnerProgress(boolean enabled, String title, String message, int nProgress)
    {
        final boolean show = enabled;
        final String dialogTitle = title;
        final String dialogMessage = message;
        final int nCount = nProgress;
        runOnUiThread(new Runnable() {
            public void run() {
                showProgressDialog = show;
                if (!isActive || (downloadProgressDialog == null))
                    return;
                if (!show && downloadProgressDialog.isShowing())
                {
                    downloadProgressDialog.dismiss();
                    downloadProgressDialog = null;
                    return;
                }
                if (nCount>=0)
                    downloadProgressDialog.incrementProgressBy(nCount);
                else
                    downloadProgressDialog.setProgress(-nCount);
                if (show && !downloadProgressDialog.isShowing())
                {
                    downloadProgressDialog.setTitle(dialogTitle);
                    downloadProgressDialog.setMessage(dialogMessage);
                    downloadProgressDialog.show();
                    downloadProgressDialog.setProgress(0);
                }
            }
        });
    }

    private void RefreshLists()
    {
        gamesMap.clear();

        ParseGameList(xmlGameListCached);

        if (!ScanDownloadedGames())
        {
            Utility.ShowError(uiContext, getString(R.string.noCardAccess));
            return;
        }

        GetCheckedGames();


        RefreshAllTabs();
    }
    
    private boolean ScanDownloadedGames()
    {
        //Check that either downloadPath or path exist
        String path = Utility.GetGamesPath(this);
        String downloadPath = Utility.GetDownloadPath(this);
Utility.WriteLog("downloadPath: "+downloadPath);
Utility.WriteLog("path: "+path);

        if (TextUtils.isEmpty(path) && TextUtils.isEmpty(downloadPath))
            return false;

        File gameStartDir = null;
        File downloadStartDir = null;

        //if path exists, get gameStartDir as File
        if (!TextUtils.isEmpty(path))
            gameStartDir = new File (path);
        //if downloadDir exists and is NOT same as path, get downloadDir as File
        if (!TextUtils.isEmpty(downloadPath) && !path.equals(downloadPath))
            downloadStartDir = new File (downloadPath);

        //Create a complete list of all files in the download and game directories;
        //exit function if there are no files
        ArrayList<File> completeFileList = new ArrayList<File>();

        //first check the main game directory
        if (gameStartDir != null) {
            if (gameStartDir.exists() && gameStartDir.isDirectory()) {
                List<File> fileListGameStartDir = Arrays.asList(gameStartDir.listFiles());
                if (!fileListGameStartDir.isEmpty())
                    completeFileList.addAll(fileListGameStartDir);
            }
        }
Utility.WriteLog("games in gameStartDir: "+completeFileList.size());

        //then the download directory
        if (downloadStartDir != null) {
            if (downloadStartDir.exists() && downloadStartDir.isDirectory()) {
                List<File> fileListDownloadStartDir = Arrays.asList(downloadStartDir.listFiles());
                if (!fileListDownloadStartDir.isEmpty())
                    completeFileList.addAll(Arrays.asList(downloadStartDir.listFiles()));
            }
        }
Utility.WriteLog("games in both: "+completeFileList.size());

        if (completeFileList.isEmpty()) return true;

        //Compare each File(i) to File(i+1) in List; delete File(i) if somehow duplicated
        if (completeFileList.size()>1)
            for (int i=0; i<completeFileList.size()-1; i++) {
                if (completeFileList.get(i).equals(completeFileList.get(i+1)))
                    completeFileList.remove(i);
            }

        //Convert completeFileList (ArrayList<File>) to simple array
        File[] sdcardFiles = completeFileList.toArray(new File[completeFileList.size()]);
        //File[] sdcardFiles = gameStartDir.listFiles();
        ArrayList<File> qspGameDirs = new ArrayList<File>();
        ArrayList<File> qspGameFiles = new ArrayList<File>();
        ArrayList<Boolean> qspGamePack = new ArrayList<Boolean>();
        String lastGame = "[ ---- ]"; //placeholder in case no files are present


        //Look at every directory in the QSP games folder after first sorting the list
        if (sdcardFiles!=null) {
            sdcardFiles = Utility.FileSorter(sdcardFiles);
            for (File currentFile : sdcardFiles) {
                if (currentFile.isDirectory() && !currentFile.isHidden() && !currentFile.getName().startsWith(".")) {

                    //Sort the files in the current game directory, then for each QSP/GAM file, add
                    //the directory and game to an array
                    File[] curDirFiles = currentFile.listFiles();
                    curDirFiles = Utility.FileSorter(curDirFiles);
                    for (File innerFile : curDirFiles) {
                        if (!innerFile.isHidden() && (innerFile.getName().toLowerCase().endsWith(".qsp") || innerFile.getName().toLowerCase().endsWith(".gam"))) {
                            Utility.WriteLog("[" + qspGamePack.size() + "], currentFile: " + currentFile.getName() + ", lastGame: " + lastGame);

                            //Mark the "Pack" array if the directory holds more than one QSP game file
                            if (currentFile.getName().equals(lastGame) && !lastGame.equals("")) {
                                qspGamePack.set(qspGamePack.size() - 1, true);
                                qspGamePack.add(true);
                            } else qspGamePack.add(false);
                            qspGameDirs.add(currentFile);
                            qspGameFiles.add(innerFile);
//                		break; <-- removed so all QSP and GAM files are checked and loaded, not just the first
                            lastGame = currentFile.getName();
                        }
                    }
                }
            }
        }

        //Ищем загруженные игры в карте
        for (int i=0; i<qspGameDirs.size(); i++)
        {
Utility.WriteLog("qspGameDirs["+i+"]: "+qspGameDirs.get(i).getName()+
                    ", qspGameFiles["+i+"]: "+qspGameFiles.get(i).getName()+
                    ", qspGamePack["+i+"]: "+qspGamePack.get(i));
            File d = qspGameDirs.get(i);
            GameItem game = null;
            File infoFile = new File(d.getPath().concat("/").concat(GAME_INFO_FILENAME));
            if (infoFile.exists())
            {
                String text = "";
                try {
                    FileInputStream instream = new FileInputStream(infoFile.getPath());
                    if (instream != null) {
                        InputStreamReader inputreader = new InputStreamReader(instream);
                        BufferedReader buffreader = new BufferedReader(inputreader);
                        boolean exit = false;
                        while (!exit) {
                            String line = null;
                            try {
                                line = buffreader.readLine();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Utility.WriteLog(getString(R.string.gameInfoFileReadError));
                            }
                            exit = line == null;
                            if (!exit)
                                text = text.concat(line);
                        }
                        buffreader.close();
                    }
                    instream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Utility.WriteLog(getString(R.string.gameInfoFileReadError));
                    continue;
                }
                game = ParseGameInfo(text);
            }
            if (game == null)
            {
                game = new GameItem();
                String displayName = d.getName();
                game.title = displayName;
                game.game_id = displayName;
            }
            //If this is part of a pack of game files, add in the individual file name, too
            if (qspGamePack.get(i)) {
                game.title += " ("+qspGameFiles.get(i).getName()+")";
                game.game_id += " ("+qspGameFiles.get(i).getName()+")";
            }
            File f = qspGameFiles.get(i);
            game.game_file = f.getPath();
            game.downloaded = true;
            gamesMap.put(game.game_id, game);
        }
    
        return true;
    }
    
    private void GetCheckedGames()
    {
        //!!! STUB
        //Заполняем список отмеченных игр

    }
   
    private void RefreshAllTabs()
    {
        //Update all tab titles
        ActionBar actionBar = getSupportActionBar();
        actionBar.getTabAt(2).setText(getString(R.string.tab_all));
        actionBar.getTabAt(1).setText(getString(R.string.tab_starred));
        actionBar.getTabAt(0).setText(getString(R.string.tab_downloaded));

        //Выводим списки игр на экран
        ArrayList<GameItem> games = new ArrayList<>(gamesMap.values());
        Utility.GameSorter(games);

        ArrayList<GameItem> gamesDownloaded = new ArrayList<>();
        ArrayList<GameItem> gamesStarred = new ArrayList<>();
        ArrayList<GameItem> gamesAll = new ArrayList<>();

        for (GameItem game : games) {
            if (game.downloaded) {
                gamesDownloaded.add(game);
            } else {
                gamesStarred.add(game);
            }
            gamesAll.add(game);
        }

        gameAdapters.put(TAB_DOWNLOADED, new GameAdapter(this, R.layout.game_item, gamesDownloaded));
        gameAdapters.put(TAB_STARRED, new GameAdapter(this, R.layout.game_item, gamesStarred));
        gameAdapters.put(TAB_ALL, new GameAdapter(this, R.layout.game_item, gamesAll));

        //Determine which tab to open
        if (openDefaultTab)
        {
            openDefaultTab = false;

            int tabIndex = 0;//Uploaded
            if (gamesDownloaded.isEmpty())
            {
                if (gamesStarred.isEmpty())
                    tabIndex = 2;//All
                else
                    tabIndex = 1;//Reported
            }

            getSupportActionBar().setSelectedNavigationItem(tabIndex);
        } else {
            int tab = getSupportActionBar().getSelectedNavigationIndex();
            setGameAdapterFromTab(tab);
        }
    }

    private void setGameAdapterFromTab(int tab) {
        switch (tab) {
            case TAB_DOWNLOADED:
            case TAB_STARRED:
            case TAB_ALL:
                lvGames.setAdapter(gameAdapters.get(tab));
                break;
        }
    }
    
    private GameItem ParseGameInfo(String xml)
    {
        //Читаем информацию об игре
        GameItem resultItem = null;
        GameItem curItem = null;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput( new StringReader ( xml ) );
            int eventType = xpp.getEventType();
            boolean doc_started = false;
            boolean game_started = false;
            String lastTagName = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_DOCUMENT) {
                    doc_started = true;
                } else if(eventType == XmlPullParser.END_DOCUMENT) {
                    //Never happens
                } else if(eventType == XmlPullParser.START_TAG) {
                    if (doc_started)
                    {
                        lastTagName = xpp.getName();
                        if (lastTagName.equals("game"))
                        {
                            game_started = true;
                            curItem = new GameItem();
                        }
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                    if (doc_started && game_started)
                    {
                        if (xpp.getName().equals("game"))
                            resultItem = curItem;
                        lastTagName = "";
                    }
                } else if(eventType == XmlPullParser.CDSECT) {
                    if (doc_started && game_started)
                    {
                        String val = xpp.getText();
                        if (lastTagName.equals("id"))
                            curItem.game_id = "id:".concat(val);
                        else if (lastTagName.equals("list_id"))
                            curItem.list_id = val;
                        else if (lastTagName.equals("author"))
                            curItem.author = val;
                        else if (lastTagName.equals("ported_by"))
                            curItem.ported_by = val;
                        else if (lastTagName.equals("version"))
                            curItem.version = val;
                        else if (lastTagName.equals("title"))
                            curItem.title = val;
                        else if (lastTagName.equals("lang"))
                            curItem.lang = val;
                        else if (lastTagName.equals("player"))
                            curItem.player = val;
                        else if (lastTagName.equals("file_url"))
                            curItem.file_url = val;
                        else if (lastTagName.equals("file_size"))
                            curItem.file_size = Integer.parseInt(val);
                        else if (lastTagName.equals("desc_url"))
                            curItem.desc_url = val;
                        else if (lastTagName.equals("pub_date"))
                            curItem.pub_date = val;
                        else if (lastTagName.equals("mod_date"))
                            curItem.mod_date = val;
                    }
                }
                eventType = xpp.nextToken();
            }
        } catch (XmlPullParserException e) {
            String errTxt = getString(R.string.parseGameInfoXMLError);
            errTxt = errTxt.replace("-LINENUM-",String.valueOf(e.getLineNumber()));
            errTxt = errTxt.replace("-COLNUM-",String.valueOf(e.getColumnNumber()));
            Utility.WriteLog(errTxt);
        } catch (Exception e) {
            Utility.WriteLog(getString(R.string.parseGameInfoUnkError));
        }
        return resultItem;
    }
    
    private boolean ParseGameList(String xml)
    {
        boolean parsed = false;
        if (xml != null)
        {
            GameItem curItem = null;
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();

                xpp.setInput( new StringReader ( xml ) );
                int eventType = xpp.getEventType();
                boolean doc_started = false;
                boolean list_started = false;
                String lastTagName = "";
                String listId = "unknown";
                String listTitle = "";
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_DOCUMENT) {
                        doc_started = true;
                    } else if(eventType == XmlPullParser.END_DOCUMENT) {
                        //Never happens
                    } else if(eventType == XmlPullParser.START_TAG) {
                        if (doc_started)
                        {
                            lastTagName = xpp.getName();
                            if (lastTagName.equals("game_list"))
                            {
                                list_started = true;
                                listId = xpp.getAttributeValue(null, "id");
                                listTitle = xpp.getAttributeValue(null, "title");
                            }
                            if (list_started)
                            {
                                if (lastTagName.equals("game"))
                                {
                                    curItem = new GameItem();
                                    curItem.list_id = listId;
                                }
                            }
                        }
                    } else if(eventType == XmlPullParser.END_TAG) {
                        if (doc_started && list_started)
                        {
                            if (xpp.getName().equals("game"))
                            {
                                gamesMap.put(curItem.game_id, curItem);
                            }
                            if (xpp.getName().equals("game_list"))
                                parsed = true;
                            lastTagName = "";
                        }
                    } else if(eventType == XmlPullParser.CDSECT) {
                        if (doc_started && list_started)
                        {
                            String val = xpp.getText();
                            if (lastTagName.equals("id"))
                                curItem.game_id = "id:".concat(val);
                            else if (lastTagName.equals("author"))
                                curItem.author = val;
                            else if (lastTagName.equals("ported_by"))
                                curItem.ported_by = val;
                            else if (lastTagName.equals("version"))
                                curItem.version = val;
                            else if (lastTagName.equals("title"))
                                curItem.title = val;
                            else if (lastTagName.equals("lang"))
                                curItem.lang = val;
                            else if (lastTagName.equals("player"))
                                curItem.player = val;
                            else if (lastTagName.equals("file_url"))
                                curItem.file_url = val;
                            else if (lastTagName.equals("file_size"))
                                curItem.file_size = Integer.parseInt(val);
                            else if (lastTagName.equals("desc_url"))
                                curItem.desc_url = val;
                            else if (lastTagName.equals("pub_date"))
                                curItem.pub_date = val;
                            else if (lastTagName.equals("mod_date"))
                                curItem.mod_date = val;
                        }
                    }
                    eventType = xpp.nextToken();
                }
            } catch (XmlPullParserException e) {
                String errTxt = getString(R.string.parseGameInfoXMLError);
                errTxt = errTxt.replace("-LINENUM-",String.valueOf(e.getLineNumber()));
                errTxt = errTxt.replace("-COLNUM-",String.valueOf(e.getColumnNumber()));
                Utility.WriteLog(errTxt);
            } catch (Exception e) {
                Utility.WriteLog(getString(R.string.parseGameInfoUnkError));
            }
        }
        if ( !parsed && isActive && triedToLoadGameList )
        {
            //Показываем сообщение об ошибке
            Utility.ShowError(uiContext, getString(R.string.gamelistLoadError));
        }
        return parsed;
    }

    private class LoadGameListThread extends Thread {
        //Загружаем список игр
        public void run() {
            runOnUiThread(new Runnable() {
                public void run() {
                    downloadProgressDialog = new ProgressDialog(uiContext);
                    downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    downloadProgressDialog.setCancelable(false);
                }
            });
            try {
                updateSpinnerProgress(true, "", getString(R.string.gamelistLoadWait), 0);
                URL updateURL = new URL("http://qsp.su/tools/gamestock/gamestock.php");
                URLConnection conn = updateURL.openConnection();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                final String xml;

                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    int b;
                    do {
                        b = bis.read();
                        if (b == -1) {
                            break;
                        }
                        os.write(b);
                    } while (true);

                    xml = new String(os.toByteArray());
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        xmlGameListCached = xml;
                        RefreshLists();
                        updateSpinnerProgress(false, "", "", 0);
                        gameListIsLoading = false;
                    }
                });
            } catch (Exception e) {
                Utility.WriteLog(getString(R.string.gamelistLoadExcept));
                runOnUiThread(new Runnable() {
                    public void run() {
                        RefreshLists();
                        updateSpinnerProgress(false, "", "", 0);
                        gameListIsLoading = false;
                    }
                });
            }
        }
    };
    
    
    //***********************************************************************
    //			выбор файла "напрямую", через пролистывание папок
    //***********************************************************************
    android.content.DialogInterface.OnClickListener browseFileClick = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            if (!((AlertDialog)dialog).isShowing())
                return;
            dialog.dismiss();
            boolean canGoUp = !backPath.equals("");
            int shift = 0;
            if (canGoUp)
                shift = 1;
            if (which == 0 && canGoUp)
                BrowseGame(backPath, false);
            else
            {
                File f = qspGamesBrowseList.get(which - shift);
                if (f.isDirectory())
                    BrowseGame(f.getPath(), false);
                else
                {
                    //Запускаем файл
                    Intent data = new Intent();
                    data.putExtra("file_name", f.getPath());
                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        }
    };

    private void BrowseGame(String startpath, boolean start)
    {
        if (startpath == null)
            return;

        if (!startpath.endsWith("/"))
            startpath = startpath.concat("/");

        //Устанавливаем путь "выше"
        if (!start)
            if (startRootPath.equals(startpath))
                start = true;
        if (!start)
        {
            int slash = startpath.lastIndexOf(File.separator, startpath.length() - 2);
            if (slash >= 0)
                backPath = startpath.substring(0, slash + 1);
            else
                start = true;
        }
        if (start)
        {
            startRootPath = startpath;
            backPath = "";
        }

        //Ищем все файлы .qsp и .gam в корне флэшки
        File sdcardRoot = new File (startpath);
        if ((sdcardRoot == null) || !sdcardRoot.exists())
        {
            String failedPath = ("/"+startpath.replace(SDPath,"")).replace("//","/");
            //If at SDPath already and File is null/doesn't exist, ShowError and exit
            if (startpath.equals(SDPath)) {
                Utility.ShowInfo(uiContext, getString(R.string.SDpathNotFound).replace("-PATHNAME-", SDPath));
                BrowseGame("/",true);
                return;
            }
            //If not at SPath yet, ShowInfo and drop to lowest directory
            else {
                Utility.ShowInfo(uiContext,getString(R.string.pathNotFound).replace("-PATHNAME-", failedPath));
                BrowseGame(SDPath,true);
                return;
            }
        }
        File[] sdcardFiles = sdcardRoot.listFiles();        
        qspGamesBrowseList = new ArrayList<File>();
        //Сначала добавляем все папки
        for (File currentFile : sdcardFiles)
        {
            if (currentFile.isDirectory() && !currentFile.isHidden() && !currentFile.getName().startsWith("."))
                qspGamesBrowseList.add(currentFile);
        }
        //Потом добавляем все QSP-игры
        for (File currentFile : sdcardFiles)
        {
            if (!currentFile.isHidden() && (currentFile.getName().endsWith(".qsp") || currentFile.getName().endsWith(".gam")))
                qspGamesBrowseList.add(currentFile);
        }
        
        //Если мы не на самом верхнем уровне, то добавляем ссылку 
        int shift = 0;
        if (!start)
            shift = 1;
        int total = qspGamesBrowseList.size() + shift;
        final CharSequence[] items = new String[total];
        if (!start)
            items[0] = "[..]";
        for (int i=shift; i<total; i++)
        {
            File f = qspGamesBrowseList.get(i - shift);
            String displayName = f.getName();
            if (f.isDirectory())
                displayName = "["+ displayName + "]";
            items[i] = displayName;
        }
        
        //Показываем диалог выбора файла
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.gameFileSelect));
        builder.setItems(items, browseFileClick);
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    //***********************************************************************
    //			удаление игр (выбор папки с игрой)
    //***********************************************************************
    private void DeleteGames()
    {
        //Ищем папки в /qsp/games
Utility.WriteLog("GetGamesPath() = "+Utility.GetGamesPath(this));
        File sdcardRoot = new File (Utility.GetGamesPath(this));
        File[] sdcardFiles = sdcardRoot.listFiles();
Utility.WriteLog("total files: "+sdcardFiles.length);
        for (File currentFile : sdcardFiles)
            Utility.WriteLog("- "+currentFile.getPath());
        qspGamesToDeleteList = new ArrayList<File>();
        for (File currentFile : sdcardFiles)
        {
            if (currentFile.isDirectory() && !currentFile.isHidden() && !currentFile.getName().startsWith("."))
                qspGamesToDeleteList.add(currentFile);
        }
        
        int total = qspGamesToDeleteList.size();
        final CharSequence[] items = new String[total];
        for (int i=0; i<total; i++)
        {
            File f = qspGamesToDeleteList.get(i);
            items[i] = f.getName();
        }
        
        //Показываем диалог выбора файла
        AlertDialog.Builder builder = new AlertDialog.Builder(uiContext);
        builder.setTitle(getString(R.string.gameFileDeleteTitle));
        builder.setItems(items, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(uiContext);
                    final File f = qspGamesToDeleteList.get(which);
                    if ((f == null) || !f.isDirectory())
                        return;
                    confirmBuilder.setMessage(getString(R.string.gameFileDeleteQuery).replace("-GAMENAME-","\""+f.getName()+"\""));
                    confirmBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (whichButton==DialogInterface.BUTTON_POSITIVE)
                            {
                                if (downloadDir != null) {
                                    final DocumentFile df = downloadDir.findFile(f.getName());
                                    Utility.DeleteDocFileRecursive(df);
                                }
                                else {
                                    Utility.DeleteRecursive(f);
                                }
                                Utility.ShowInfo(uiContext, getString(R.string.gameFileDeleteSuccess));
                                RefreshLists();
                            }
                        }
                    });
                    confirmBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) { }
                    });

                    AlertDialog confirm = confirmBuilder.create();
                    confirm.show();
                }
            }
        );
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) { }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private class GameAdapter extends ArrayAdapter<GameItem> {

        private ArrayList<GameItem> items;

        public GameAdapter(Context context, int textViewResourceId, ArrayList<GameItem> items) {
                super(context, textViewResourceId, items);
                this.items = items;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.game_item, null);
            }
            GameItem o = this.items.get(position);
            if (o != null) {
                    TextView tt = (TextView) v.findViewById(R.id.game_title);
                    if (tt != null) {
                          tt.setText(o.title);
                          if (o.downloaded)
                              tt.setTextColor(0xFFE0E0E0);
                          else
                              tt.setTextColor(0xFFFFD700);
                    }
                    tt = (TextView) v.findViewById(R.id.game_author);
                    if(o.author.length()>0)
                        tt.setText(new StringBuilder().append(getString(R.string.gameAuthorText).replace("-AUTHOR-",o.author)));//.append("  (").append(o.file_size).append(" байт)"));
                    else
                        tt.setText("");
            }
            return v;
        }
    }

    //Choose a new directory for all Market downloads
    public void getNewDownloadDir() {
Utility.WriteLog("getNewDownloadDir()");
        Intent data = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(data, REQUEST_CODE_STORAGE_ACCESS);

    }

    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent resultData) {
        if ((requestCode == REQUEST_CODE_STORAGE_ACCESS)) {

            if (resultCode == RESULT_OK) {
                Uri treeUri = resultData.getData();

                //Skip if the user exited the directory selection
                if (treeUri == null)
                    return;

                grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                //Save the treeUri as a string for later use
                SharedPreferences.Editor ed = settings.edit();
                ed.putString(getString(R.string.key_internal_uri_extsdcard),treeUri.toString());
                ed.apply();

                //Get the downloadDir DocumentFile from Settings and check that it is a usable
                //DocumentsProvider Uri
                String untestedUri = settings.getString(getString(R.string.key_internal_uri_extsdcard), "");
                if (!untestedUri.isEmpty()) {
                    downloadDir = DocumentFile.fromTreeUri(uiContext, Uri.parse(untestedUri));
                }
                else
                    downloadDir = null;

                if (downloadDir != null) {
                    int takeFlags = resultData.getFlags();
                    takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    uiContext.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                }
            }

            //if user cancels the downloadDir search, try to get the downloadDir from settings
            else {
                //Get the downloadDir DocumentFile from Settings and check that it is a usable
                //DocumentsProvider Uri
                String untestedUri = settings.getString(getString(R.string.key_internal_uri_extsdcard), "");
                if (!untestedUri.isEmpty()) {
                    downloadDir = DocumentFile.fromTreeUri(uiContext, Uri.parse(untestedUri));
                }
                else
                    downloadDir = null;

            }

            if (downloadDir == null) {
                updatePrefFromDD(uiContext);
                return;
            }
            //If the current (or previous) downloadDir is not a writable directory, make it null
            //and clear Settings
            if (!downloadDir.exists() || !downloadDir.isDirectory() || !downloadDir.canWrite()) {
                SharedPreferences.Editor ed = settings.edit();
                ed.putString(getString(R.string.key_internal_uri_extsdcard),"");
                ed.apply();
                downloadDir = null;
            }

            //Make sure to update everything after determining downloadDir as null or valid
            updatePrefFromDD(uiContext);
        }
    }

    private void updatePrefFromDD(Context uiContext) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(uiContext);

        //if downloadDir is null, clear all relevant Settings info
        if (downloadDir == null) {
            SharedPreferences.Editor ed = sharedPref.edit();
            ed.putString(getString(R.string.key_internal_uri_extsdcard),"");
            ed.apply();
            setFullGamesPath(false);
            return;
        }

        //if there is a downloadDir, extract the path for use as "downDirPath"; use the
        //default value if downloadDir is null
        String newDownDirPath = getString(R.string.defDownDirPath);
        if (downloadDir != null) {
            newDownDirPath = FileUtil.getFullPathFromTreeUri(downloadDir.getUri(),uiContext);

            if (newDownDirPath == null) {
                downloadDir = null;
                SharedPreferences.Editor ed = sharedPref.edit();
                ed.putString(getString(R.string.key_internal_uri_extsdcard), "");
                ed.apply();
                setFullGamesPath(false);
                return;
            }
        }

        if (newDownDirPath.startsWith(FileUtil.getSdCardPath())) {
            Utility.WriteLog(newDownDirPath + " contains " + FileUtil.getSdCardPath());
            newDownDirPath = newDownDirPath.replace(FileUtil.getSdCardPath(), "");
            if (!newDownDirPath.endsWith("/")) newDownDirPath += "/";
        }
        else {
            Utility.WriteLog(newDownDirPath+" doesn't contain "+FileUtil.getSdCardPath());

            ArrayList<String> extSDPaths = FileUtil.getExtSdCardPaths(uiContext);
            for (int i=0; i<extSDPaths.size(); i++) {
                if (newDownDirPath.startsWith(extSDPaths.get(i))) {
                    Utility.WriteLog(newDownDirPath+" contains "+extSDPaths.get(i));
                    newDownDirPath = newDownDirPath.replace(extSDPaths.get(i),"");
                    if (!newDownDirPath.endsWith("/")) newDownDirPath += "/";
                    break;
                }
                else Utility.WriteLog(newDownDirPath+" doesn't contain "+extSDPaths.get(i));
            }
        }

        //Store the new downDirPath value
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("downDirPath", newDownDirPath);
        editor.commit();

        setFullGamesPath(true);

    }

    private void ShowNoDownloadDir()
    {
        Utility.WriteLog("downloadDir is null - notify user! [ShowNoDownloadDir()]");
        String msg = getString(R.string.DDWarnMsg);
        String desc = getString(R.string.DDWarnDesc);

        desc = desc.replace("-MENUOPTS-", getString(R.string.menu_options));
        desc = desc.replace("-SELECTDIRTITLE-", getString(R.string.selectDirTitle));
        desc = desc.replace("-SHOWSDCARD-", getString(R.string.ShowSDCard));
        AlertDialog.Builder bld = new AlertDialog.Builder(uiContext).setMessage(desc)
                .setTitle(getString(R.string.DDWarnMsg))
                .setIcon(R.drawable.icon)
                .setPositiveButton(getString(R.string.alertChoose), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                            getNewDownloadDir();
                    }
                })
                .setNegativeButton(getString(R.string.alertCancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        bld.create().show();

    }

    private class TabListener implements ActionBar.TabListener {

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            int position = tab.getPosition();
            if ((position == TAB_STARRED || position == TAB_ALL) && xmlGameListCached == null && !gameListIsLoading) {
                if (Utility.haveInternet(uiContext)) {
                    gameListIsLoading = true;
                    triedToLoadGameList = true;
                    LoadGameListThread tLoadGameList = new LoadGameListThread();
                    tLoadGameList.start();
                } else if (!triedToLoadGameList) {
                    Utility.ShowError(uiContext, getString(R.string.gamelistLoadError));
                    triedToLoadGameList = true;
                }
            }
            setGameAdapterFromTab(position);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }
    }
}
