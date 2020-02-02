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
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.SparseArrayCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentTransaction;

import com.qsp.player.PermissionUtil;
import com.qsp.player.R;
import com.qsp.player.Utility;
import com.qsp.player.game.QspPlayerStart;
import com.qsp.player.settings.Settings;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class QspGameStock extends AppCompatActivity {

    public static final String GAME_INFO_FILENAME = "gamestockInfo";

    private static final int QSP_NOTIFICATION_ID = 0;
    private static final int REQUEST_CODE_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CODE_OPEN_GAME_FILE = 2;

    private static final int TAB_DOWNLOADED = 0;
    private static final int TAB_STARRED = 1;
    private static final int TAB_ALL = 2;

    private final Context uiContext = this;
    private final HashMap<String, GameItem> gamesMap = new HashMap<>();
    private final SparseArrayCompat<GameAdapter> gameAdapters = new SparseArrayCompat<>();
    private final LocalGameRepository localGameRepository = new LocalGameRepository(this);
    private final RemoteGameRepository remoteGameRepository = new RemoteGameRepository(this);

    private boolean openDefaultTab = true;
    private boolean gameListIsLoading;
    private boolean triedToLoadGameList;
    private boolean gameIsRunning;
    private boolean isActive;
    private boolean showProgressDialog;
    private SharedPreferences settings;
    private String currentLanguage = Locale.getDefault().getLanguage();
    private ListView lvGames;
    private ProgressDialog downloadProgressDialog;
    private NotificationManager notificationManager;
    private Collection<GameItem> remoteGames;
    private DocumentFile downloadDir;
    private boolean permissionsGranted;

    private DownloadGameAsyncTask downloadTask;
    private LoadGameListAsyncTask loadGameListTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gamestock);

        permissionsGranted = PermissionUtil.requestPermissionsIfNotGranted(
                this,
                REQUEST_CODE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent gameStockIntent = getIntent();
        gameIsRunning = gameStockIntent.getBooleanExtra("game_is_running", false);

        loadLocale();
        initActionBar();
        initGamesListView();
        setResult(RESULT_CANCELED);
    }

    private void loadLocale() {
        String language = settings.getString("lang", "ru");
        Utility.setLocale(uiContext, language);
        setTitle(R.string.menu_gamestock);
        currentLanguage = language;
    }

    private void initActionBar() {
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
    }

    private void initGamesListView() {
        lvGames = findViewById(R.id.games);
        lvGames.setTextFilterEnabled(true);
        lvGames.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        lvGames.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String value = getGameIdByPosition(position);
                showGameInfo(value);
            }
        });

        lvGames.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String gameId = getGameIdByPosition(position);
                GameItem game = gamesMap.get(gameId);
                playOrDownloadGame(game);
                return true;
            }
        });
    }

    private void playOrDownloadGame(GameItem game) {
        if (game.downloaded) {
            Intent data = new Intent();
            data.putExtra("file_name", game.gameFile);
            setResult(RESULT_OK, data);
            finish();
        } else {
            downloadGame(game);
        }
    }

    private String getGameIdByPosition(int position) {
        GameItem game = (GameItem) lvGames.getAdapter().getItem(position);
        return game.gameId;
    }

    @Override
    protected void onDestroy() {
        if (downloadTask != null) {
            downloadTask.cancel(true);
            downloadTask = null;
        }
        if (loadGameListTask != null) {
            loadGameListTask.cancel(true);
            loadGameListTask = null;
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        isActive = true;

        String language = settings.getString("lang", "ru");
        if (!currentLanguage.equals(language)) {
            Utility.setLocale(uiContext, language);
            setTitle(getString(R.string.menu_gamestock));
            refreshActionBar();
            invalidateOptionsMenu();
            currentLanguage = language;
        }

        if (permissionsGranted) {
            refreshDownloadDir();
        }
        if (showProgressDialog && downloadProgressDialog != null) {
            downloadProgressDialog.show();
        }
    }

    private void refreshActionBar() {
        ActionBar bar = getSupportActionBar();
        bar.getTabAt(2).setText(getString(R.string.tab_all));
        bar.getTabAt(1).setText(getString(R.string.tab_starred));
        bar.getTabAt(0).setText(getString(R.string.tab_downloaded));
    }

    private void refreshDownloadDir() {
        downloadDir = Utility.getDownloadDir(this);
        refreshGames();
    }

    private void refreshGames() {
        if (downloadDir == null) {
            Utility.ShowError(uiContext, getString(R.string.noCardAccess));
            return;
        }
        gamesMap.clear();

        if (remoteGames != null) {
            for (GameItem game : remoteGames) {
                gamesMap.put(game.gameId, game);
            }
        }
        for (GameItem game : localGameRepository.getGameItems()) {
            gamesMap.put(game.gameId, game);
        }

        refreshGameAdapters();
    }

    private void refreshGameAdapters() {
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

        if (openDefaultTab) {
            openDefaultTab = false;

            int tab;
            if (gamesDownloaded.isEmpty()) {
                if (gamesStarred.isEmpty()) {
                    tab = TAB_ALL;
                } else {
                    tab = TAB_STARRED;
                }
            } else {
                tab = TAB_DOWNLOADED;
            }

            getSupportActionBar().setSelectedNavigationItem(tab);
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

    @Override
    public void onPause() {
        isActive = false;
        if (showProgressDialog && downloadProgressDialog != null) {
            downloadProgressDialog.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CODE_EXTERNAL_STORAGE) {
            return;
        }
        for (int result : grantResults) {
            if (result != PERMISSION_GRANTED) {
                showMissingPermissionsDialog();
                return;
            }
        }
        refreshDownloadDir();
    }

    private void showMissingPermissionsDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.reqPermMissing)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finishAffinity();
                    }
                })
                .create()
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST_CODE_OPEN_GAME_FILE) {
            return;
        }
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                installGame(uri);
            }
        }
    }

    private void installGame(Uri uri) {
        DocumentFile zipFile = DocumentFile.fromSingleUri(uiContext, uri);
        if (!zipFile.exists()) {
            return;
        }

        downloadProgressDialog = new ProgressDialog(uiContext);
        downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        downloadProgressDialog.setCancelable(false);

        unzip(zipFile, zipFile.getName());
        updateSpinnerProgress(false, "", "", 0);
        refreshGames();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gamestock_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem resumeGameItem = menu.findItem(R.id.menu_resumegame);
        resumeGameItem.setVisible(gameIsRunning);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_resumegame:
                setResult(RESULT_CANCELED, null);
                finish();
                return true;

            case R.id.menu_options:
                Intent intent = new Intent();
                intent.setClass(this, Settings.class);
                startActivity(intent);
                return true;

            case R.id.menu_about:
                showAboutDialog();
                return true;

            case R.id.menu_openfile:
                showOpenFileDialog();
                return true;

            case R.id.menu_deletegames:
                showDeleteGameDialog();
                return true;
        }

        return false;
    }

    private void showOpenFileDialog() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/zip");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_OPEN_GAME_FILE);
    }

    private void showAboutDialog() {
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        String fontStyle;
        switch (Integer.parseInt(settings.getString("typeface", "0"))) {
            case 0:
            default:
                fontStyle = "DEFAULT";
                break;
            case 1:
                fontStyle = "sans-serif";
                break;
            case 2:
                fontStyle = "serif";
                break;
            case 3:
                fontStyle = "courier";
                break;
        }

        String textColor = String.format("#%06X", (0xFFFFFF & settings.getInt("textColor", Color.parseColor("#ffffff"))));
        String backColor = String.format("#%06X", (0xFFFFFF & settings.getInt("backColor", Color.parseColor("#000000"))));
        String linkColor = String.format("#%06X", (0xFFFFFF & settings.getInt("linkColor", Color.parseColor("#0000ee"))));
        String fontSize = settings.getString("fontsize", "16");

        String desc = getString(R.string.about_template)
                .replace("QSPTEXTCOLOR", textColor).replace("QSPBACKCOLOR", backColor)
                .replace("QSPLINKCOLOR", linkColor).replace("QSPFONTSIZE", fontSize)
                .replace("QSPFONTSTYLE", fontStyle)
                .replace("REPLACETEXT", getString(R.string.app_descrip) + getString(R.string.app_credits));

        WebView descView = messageView.findViewById(R.id.about_descrip);
        descView.loadDataWithBaseURL("", desc, "text/html", "utf-8", "");

        new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .setView(messageView)
                .create()
                .show();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (downloadTask != null &&
            downloadTask.getStatus() == AsyncTask.Status.RUNNING &&
            keyCode == KeyEvent.KEYCODE_BACK &&
            event.getRepeatCount() == 0) {

            moveTaskToBack(true);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void showNotification(String text, String subText) {
        Intent intent = new Intent(uiContext, QspPlayerStart.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent contentIntent = PendingIntent.getActivity(uiContext, 0, intent, 0);

        Notification note = new Notification.Builder(uiContext)
                .setSmallIcon(android.R.drawable.stat_notify_sdcard)
                .setContentText(text)
                .setWhen(System.currentTimeMillis())
                .setSubText(subText)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(QSP_NOTIFICATION_ID, note);
    }

    private void showGameInfo(String gameId) {
        final GameItem game = gamesMap.get(gameId);
        if (game == null) {
            return;
        }
        StringBuilder txt = new StringBuilder();
        if (game.gameFile.contains(" ")) {
            txt.append(getString(R.string.spaceWarn) + "\n");
        }
        if (game.author.length() > 0) {
            txt.append("Author: ").append(game.author);
        }
        if (game.version.length() > 0) {
            txt.append("\nVersion: ").append(game.version);
        }
        if (game.fileSize > 0) {
            txt.append("\nSize: ").append(game.fileSize / 1024).append(" Kilobytes");
        }

        new AlertDialog.Builder(uiContext)
                .setMessage(txt)
                .setTitle(game.title)
                .setIcon(R.drawable.icon)
                .setPositiveButton(game.downloaded ? getString(R.string.playGameCmd) : getString(R.string.dlGameCmd), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        playOrDownloadGame(game);
                    }
                })
                .setNegativeButton(getString(R.string.closeGameCmd), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }

    private void downloadGame(GameItem game) {
        if (!Utility.haveInternet(uiContext)) {
            Utility.ShowError(uiContext, getString(R.string.gameLoadNetError));
            return;
        }

        downloadProgressDialog = new ProgressDialog(uiContext);
        downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        downloadProgressDialog.setMax(game.fileSize);
        downloadProgressDialog.setCancelable(false);

        downloadTask = new DownloadGameAsyncTask(game);
        downloadTask.execute();
    }

    private void unzip(DocumentFile zipFile, String gameName) {
        if (downloadDir == null) {
            return;
        }

        runOnUiThread(new Runnable() {
            public void run() {
                downloadProgressDialog = new ProgressDialog(uiContext);
                downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                downloadProgressDialog.setCancelable(false);
            }
        });
        updateSpinnerProgress(true, gameName, getString(R.string.unpackMsg), 0);

        String folderName = Utility.ConvertGameTitleToCorrectFolderName(gameName);
        try {
            DocumentFile gameFolder = downloadDir.findFile(folderName);
            if (gameFolder == null) {
                gameFolder = downloadDir.createDirectory(folderName);
            }
            ZipUtil.unzip(this, zipFile, gameFolder);
        } catch (Exception e) {
            Log.e(getString(R.string.decompMsg), getString(R.string.unzipMsgShort), e);
        }
    }

    private void writeGameInfo(String gameId) {
        GameItem game = gamesMap.get(gameId);
        if (game == null) {
            return;
        }

        String folderName = Utility.ConvertGameTitleToCorrectFolderName(game.title);
        DocumentFile gameDir = downloadDir.findFile(folderName);
        if (gameDir == null) {
            return;
        }

        DocumentFile infoFile = gameDir.findFile(GAME_INFO_FILENAME);
        if (infoFile == null) {
            infoFile = gameDir.createFile("application/octet-stream", GAME_INFO_FILENAME);
        }
        try (OutputStream out = getContentResolver().openOutputStream(infoFile.getUri())) {
            try (OutputStreamWriter writer = new OutputStreamWriter(out)) {
                writer.write("<game>\n");
                writer.write("\t<id><![CDATA[".concat(game.gameId.substring(3)).concat("]]></id>\n"));
                writer.write("\t<list_id><![CDATA[".concat(game.listId).concat("]]></list_id>\n"));
                writer.write("\t<author><![CDATA[".concat(game.author).concat("]]></author>\n"));
                writer.write("\t<ported_by><![CDATA[".concat(game.portedBy).concat("]]></ported_by>\n"));
                writer.write("\t<version><![CDATA[".concat(game.version).concat("]]></version>\n"));
                writer.write("\t<title><![CDATA[".concat(game.title).concat("]]></title>\n"));
                writer.write("\t<lang><![CDATA[".concat(game.lang).concat("]]></lang>\n"));
                writer.write("\t<player><![CDATA[".concat(game.player).concat("]]></player>\n"));
                writer.write("\t<file_url><![CDATA[".concat(game.fileUrl).concat("]]></file_url>\n"));
                writer.write("\t<file_size><![CDATA[".concat(String.valueOf(game.fileSize)).concat("]]></file_size>\n"));
                writer.write("\t<desc_url><![CDATA[".concat(game.descUrl).concat("]]></desc_url>\n"));
                writer.write("\t<pub_date><![CDATA[".concat(game.pubDate).concat("]]></pub_date>\n"));
                writer.write("\t<mod_date><![CDATA[".concat(game.modDate).concat("]]></mod_date>\n"));
                writer.write("</game>");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Utility.WriteLog(getString(R.string.gameInfoFileCreateError));
        } catch (IOException e) {
            e.printStackTrace();
            Utility.WriteLog(getString(R.string.gameInfoFileWriteError));
        }
    }

    private void updateSpinnerProgress(final boolean show, final String title, final String message, final int progress) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateSpinnerProgress(show, title, message, progress);
                }
            });
            return;
        }

        showProgressDialog = show;
        if (!isActive || downloadProgressDialog == null) {
            return;
        }
        if (!show && downloadProgressDialog.isShowing()) {
            downloadProgressDialog.dismiss();
            downloadProgressDialog = null;
            return;
        }
        if (progress >= 0) {
            downloadProgressDialog.incrementProgressBy(progress);
        } else {
            downloadProgressDialog.setProgress(-progress);
        }
        if (show && !downloadProgressDialog.isShowing()) {
            downloadProgressDialog.setTitle(title);
            downloadProgressDialog.setMessage(message);
            downloadProgressDialog.show();
            downloadProgressDialog.setProgress(0);
        }
    }

    private void showDeleteGameDialog() {
        final ArrayList<DocumentFile> gameDirs = new ArrayList<>();
        List<String> items = new ArrayList<>();
        for (DocumentFile dir : downloadDir.listFiles()) {
            if (dir.isDirectory() && !dir.getName().startsWith(".")) {
                gameDirs.add(dir);
                items.add(dir.getName());
            }
        }
        String[] arrItems = items.toArray(new String[items.size()]);

        new AlertDialog.Builder(uiContext)
                .setTitle(getString(R.string.gameFileDeleteTitle))
                .setItems(arrItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DocumentFile gameDir = gameDirs.get(which);
                        if (gameDir == null || !gameDir.isDirectory()) {
                            return;
                        }
                        showConfirmDeleteDialog(gameDir);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .create()
                .show();
    }

    private void showConfirmDeleteDialog(final DocumentFile gameDir) {
        new AlertDialog.Builder(uiContext)
                .setMessage(getString(R.string.gameFileDeleteQuery).replace("-GAMENAME-", "\"" + gameDir.getName() + "\""))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            Utility.DeleteDocFileRecursive(gameDir);
                            Utility.ShowInfo(uiContext, getString(R.string.gameFileDeleteSuccess));
                            refreshGames();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .create()
                .show();
    }

    private class GameAdapter extends ArrayAdapter<GameItem> {

        private final ArrayList<GameItem> items;

        GameAdapter(Context context, int textViewResourceId, ArrayList<GameItem> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.game_item, null);
            }
            GameItem item = items.get(position);
            if (item != null) {
                TextView titleView = convertView.findViewById(R.id.game_title);
                if (titleView != null) {
                    titleView.setText(item.title);
                    if (item.downloaded) {
                        titleView.setTextColor(0xFFE0E0E0);
                    } else {
                        titleView.setTextColor(0xFFFFD700);
                    }
                }
                TextView authorView = convertView.findViewById(R.id.game_author);
                if (item.author.length() > 0) {
                    String text = getString(R.string.gameAuthorText).replace("-AUTHOR-", item.author);
                    authorView.setText(text);
                } else {
                    authorView.setText("");
                }
            }

            return convertView;
        }
    }

    private class TabListener implements ActionBar.TabListener {

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            int position = tab.getPosition();
            if ((position == TAB_STARRED || position == TAB_ALL) && remoteGames == null && !gameListIsLoading) {
                if (Utility.haveInternet(uiContext)) {
                    gameListIsLoading = true;
                    triedToLoadGameList = true;
                    loadGameListTask = new LoadGameListAsyncTask();
                    loadGameListTask.execute();
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

    private class LoadGameListAsyncTask extends AsyncTask<Void, Void, Collection<GameItem>> {

        @Override
        protected void onPreExecute() {
            downloadProgressDialog = new ProgressDialog(uiContext);
            downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            downloadProgressDialog.setCancelable(false);
        }

        @Override
        protected Collection<GameItem> doInBackground(Void... params) {
            updateSpinnerProgress(true, "", getString(R.string.gamelistLoadWait), 0);

            try {
                return remoteGameRepository.getGameItems();
            } catch (GameListLoadException e) {
                if (isActive && triedToLoadGameList) {
                    Utility.ShowError(uiContext, getString(R.string.gamelistLoadError));
                }
                return null;
            }
        }

        @Override
        protected void onPostExecute(Collection<GameItem> result) {
            if (result != null) {
                remoteGames = result;
            }
            refreshGames();
            updateSpinnerProgress(false, "", "", 0);
            gameListIsLoading = false;
        }
    }

    private class DownloadGameAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private final GameItem game;

        private DownloadGameAsyncTask(GameItem game) {
            this.game = game;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            DocumentFile cacheDir = DocumentFile.fromFile(getCacheDir());
            if (!cacheDir.exists()) {
                Utility.WriteLog(getString(R.string.cacheCreateError));
                return false;
            }

            String zipFilename = String.valueOf(System.currentTimeMillis()).concat("_game.zip");
            DocumentFile zipFile = cacheDir.createFile("application/zip", zipFilename);
            boolean downloaded = download(zipFile);

            updateSpinnerProgress(false, "", "", 0);

            if (downloaded) {
                unzip(zipFile, game.title);
                zipFile.delete();
                writeGameInfo(game.gameId);
                updateSpinnerProgress(false, "", "", 0);
                return true;
            }

            if (zipFile.exists()) {
                zipFile.delete();
            }
            String text = getString(R.string.cantDlGameError).replace("-GAMENAME-", game.title);
            if (isActive) {
                Utility.ShowError(uiContext, text);
            } else {
                showNotification(getString(R.string.genGameLoadError), text);
            }

            return false;
        }

        private boolean download(DocumentFile zipFile) {
            try {
                URL url = new URL(game.fileUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoOutput(true);
                conn.connect();

                try (InputStream in = conn.getInputStream()) {
                    try (OutputStream out = uiContext.getContentResolver().openOutputStream(zipFile.getUri())) {
                        byte[] b = new byte[8192];
                        int totalBytesRead = 0;
                        int bytesRead;
                        while ((bytesRead = in.read(b)) > 0) {
                            out.write(b, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            updateSpinnerProgress(true, game.title, getString(R.string.dlWaiting), -totalBytesRead);
                        }
                        return totalBytesRead == game.fileSize;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Utility.WriteLog(getString(R.string.dlGameError));
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                return;
            }
            refreshGames();

            // Если игра не появилась в списке, значит она не соответствует формату "Полки игр"
            GameItem downloadedGame = gamesMap.get(game.gameId);
            boolean downloaded = downloadedGame != null && downloadedGame.downloaded;

            if (!downloaded) {
                String folderName = Utility.ConvertGameTitleToCorrectFolderName(game.title);
                DocumentFile gameFolder = downloadDir.findFile(folderName);
                if (gameFolder != null) {
                    Utility.DeleteDocFileRecursive(gameFolder);
                }
            }

            if (isActive) {
                if (downloaded) {
                    showGameInfo(game.gameId);
                } else {
                    Utility.ShowError(uiContext, getString(R.string.cantUnzipGameError).replace("-GAMENAME-", "\"" + game.title + "\""));
                }
            } else {
                String msg;
                String desc;
                if (downloaded) {
                    msg = getString(R.string.gameDlSuccess);
                    desc = getString(R.string.gameUploadSuccess).replace("-GAMENAME-", "\"" + game.title + "\"");
                } else {
                    msg = getString(R.string.genGameLoadError);
                    desc = getString(R.string.cantUnzipGameError).replace("-GAMENAME-", "\"" + game.title + "\"");
                }
                showNotification(msg, desc);
            }
        }
    }
}
