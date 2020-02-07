package com.qsp.player.stock;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.qsp.player.R;
import com.qsp.player.settings.SettingsActivity;
import com.qsp.player.util.FileUtil;
import com.qsp.player.util.ViewUtil;
import com.qsp.player.util.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.qsp.player.util.FileUtil.GAME_INFO_FILENAME;

public class GameStockActivity extends AppCompatActivity {

    private static final String TAG = GameStockActivity.class.getName();
    private static final int REQUEST_CODE_INSTALL_GAME = 1;

    private static final int TAB_LOCAL = 0;
    private static final int TAB_REMOTE = 1;
    private static final int TAB_ALL = 2;

    private static final String ABOUT_TEMPLATE = "<html><head>\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1\">\n" +
            "<style type=\"text/css\">\n" +
            "body{margin: 0; padding: 0; color: QSPTEXTCOLOR; background-color: QSPBACKCOLOR; max-width: 100%; font-size: QSPFONTSIZE; font-family: QSPFONTSTYLE; }\n" +
            "a{color: QSPLINKCOLOR; }\n" +
            "a:link{color: QSPLINKCOLOR; }\n" +
            "table{font-size: QSPFONTSIZE; font-family: QSPFONTSTYLE; }\n" +
            "</style></head><body>REPLACETEXT</body></html>";

    private final Context uiContext = this;
    private final HashMap<String, GameStockItem> gamesMap = new HashMap<>();
    private final SparseArrayCompat<GameStockItemAdapter> gameAdapters = new SparseArrayCompat<>();
    private final LocalGameRepository localGameRepository = new LocalGameRepository(this);

    private boolean gameRunning;
    private boolean showProgressDialog;
    private SharedPreferences settings;
    private String currentLanguage = Locale.getDefault().getLanguage();
    private ListView gamesView;
    private ProgressDialog progressDialog;
    private ConnectivityManager connectivityManager;
    private Collection<GameStockItem> remoteGames;
    private DocumentFile gamesDir;
    private DownloadGameAsyncTask downloadTask;
    private LoadGameListAsyncTask loadGameListTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gamestock);

        settings = PreferenceManager.getDefaultSharedPreferences(uiContext);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        Intent intent = getIntent();
        gameRunning = intent.getBooleanExtra("gameRunning", false);

        loadLocale();
        initGamesListView();
        initActionBar(savedInstanceState);
        setResult(RESULT_CANCELED);
    }

    private void loadLocale() {
        String language = settings.getString("lang", "ru");
        ViewUtil.setLocale(uiContext, language);
        setTitle(R.string.gameStock);
        currentLanguage = language;
    }

    private void initActionBar(Bundle savedInstanceState) {
        TabListener tabListener = new TabListener();
        ActionBar bar = getSupportActionBar();
        if (bar == null) {
            return;
        }
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.addTab(bar.newTab()
                .setText(R.string.tabLocal)
                .setTabListener(tabListener), false);

        bar.addTab(bar.newTab()
                .setText(R.string.tabRemote)
                .setTabListener(tabListener), false);

        bar.addTab(bar.newTab()
                .setText(R.string.tabAll)
                .setTabListener(tabListener), false);

        int tab;
        if (savedInstanceState != null) {
            tab = savedInstanceState.getInt("tab", TAB_LOCAL);
        } else {
            tab = TAB_LOCAL;
        }
        bar.setSelectedNavigationItem(tab);
    }

    private void initGamesListView() {
        gamesView = findViewById(R.id.games);
        gamesView.setTextFilterEnabled(true);
        gamesView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        gamesView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String value = getGameIdByPosition(position);
                showGameInfo(value);
            }
        });

        gamesView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String gameId = getGameIdByPosition(position);
                GameStockItem game = gamesMap.get(gameId);
                if (game != null) {
                    playOrDownloadGame(game);
                } else {
                    Log.e(TAG, "Game not found: " + gameId);
                }

                return true;
            }
        });
    }

    private String getGameIdByPosition(int position) {
        GameStockItem game = (GameStockItem) gamesView.getAdapter().getItem(position);
        return game.gameId;
    }

    private void showGameInfo(String gameId) {
        final GameStockItem game = gamesMap.get(gameId);
        if (game == null) {
            Log.e(TAG, "Game not found: " + gameId);
            return;
        }
        StringBuilder message = new StringBuilder();
        if (game.author.length() > 0) {
            message.append(getString(R.string.author).replace("-AUTHOR-", game.author));
        }
        if (game.version.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.version).replace("-VERSION-", game.version));
        }
        if (game.fileSize > 0) {
            message.append('\n');
            message.append(getString(R.string.fileSize).replace("-SIZE-", Integer.toString(game.fileSize / 1024)));
        }

        new AlertDialog.Builder(uiContext)
                .setMessage(message)
                .setTitle(game.title)
                .setIcon(R.drawable.icon)
                .setPositiveButton(game.downloaded ? getString(R.string.play) : getString(R.string.download), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        playOrDownloadGame(game);
                    }
                })
                .setNegativeButton(getString(R.string.close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }

    private void playOrDownloadGame(GameStockItem game) {
        if (game.downloaded) {
            Intent data = new Intent();
            data.putExtra("gameTitle", game.title);
            data.putExtra("gameDirUri", game.localDirUri);
            data.putExtra("gameFileUri", game.localFileUri);
            setResult(RESULT_OK, data);
            finish();
            return;
        }

        downloadGame(game);
    }

    private void downloadGame(GameStockItem game) {
        if (!isNetworkConnected()) {
            ViewUtil.showErrorDialog(uiContext, getString(R.string.downloadNetworkError));
            return;
        }
        DownloadGameAsyncTask task = new DownloadGameAsyncTask(this, game);
        task.execute();
        downloadTask = task;
    }

    private boolean isNetworkConnected() {
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    @Override
    protected void onDestroy() {
        if (downloadTask != null) {
            downloadTask.cancel(true);
        }
        if (loadGameListTask != null) {
            loadGameListTask.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            outState.putInt("tab", bar.getSelectedNavigationIndex());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLocale();
        refreshGamesDirectory();

        if (showProgressDialog && progressDialog != null) {
            progressDialog.show();
        }
    }

    private void updateLocale() {
        String language = settings.getString("lang", "ru");
        if (!currentLanguage.equals(language)) {
            ViewUtil.setLocale(uiContext, language);
            setTitle(getString(R.string.gameStock));
            refreshActionBar();
            invalidateOptionsMenu();
            currentLanguage = language;
        }
    }

    private void refreshActionBar() {
        ActionBar bar = getSupportActionBar();
        if (bar == null) {
            return;
        }
        bar.getTabAt(2).setText(getString(R.string.tabAll));
        bar.getTabAt(1).setText(getString(R.string.tabRemote));
        bar.getTabAt(0).setText(getString(R.string.tabLocal));
    }

    private void refreshGamesDirectory() {
        File dirFile = getExternalFilesDir(null);
        if (dirFile == null) {
            Log.e(TAG, "External files directory not found");
            return;
        }
        DocumentFile extFilesDir = DocumentFile.fromFile(dirFile);
        DocumentFile dir = extFilesDir.findFile("games");
        if (dir == null) {
            dir = extFilesDir.createDirectory("games");
        }
        if (!FileUtil.isWritableDirectory(dir)) {
            Log.e(TAG, "Games directory is not writable");
            String message = getString(R.string.gamesDirError);
            ViewUtil.showErrorDialog(uiContext, message);
            return;
        }

        gamesDir = dir;
        localGameRepository.setGamesDirectory(gamesDir);
        refreshGames();
    }

    private void refreshGames() {
        gamesMap.clear();

        if (remoteGames != null) {
            for (GameStockItem game : remoteGames) {
                gamesMap.put(game.gameId, game);
            }
        }
        for (GameStockItem game : localGameRepository.getGames()) {
            gamesMap.put(game.gameId, game);
        }

        refreshGameAdapters();
    }

    private void refreshGameAdapters() {
        ArrayList<GameStockItem> games = getSortedGames();
        ArrayList<GameStockItem> localGames = new ArrayList<>();
        ArrayList<GameStockItem> remoteGames = new ArrayList<>();

        for (GameStockItem game : games) {
            if (game.downloaded) {
                localGames.add(game);
            } else {
                remoteGames.add(game);
            }
        }

        gameAdapters.put(TAB_LOCAL, new GameStockItemAdapter(this, R.layout.game_item, localGames));
        gameAdapters.put(TAB_REMOTE, new GameStockItemAdapter(this, R.layout.game_item, remoteGames));
        gameAdapters.put(TAB_ALL, new GameStockItemAdapter(this, R.layout.game_item, games));

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            setGameAdapterFromTab(bar.getSelectedNavigationIndex());
        }
    }

    private ArrayList<GameStockItem> getSortedGames() {
        Collection<GameStockItem> unsortedGames = gamesMap.values();
        ArrayList<GameStockItem> games = new ArrayList<>(unsortedGames);

        if (games.size() < 2) {
            return games;
        }
        Collections.sort(games, new Comparator<GameStockItem>() {
            public int compare(GameStockItem first, GameStockItem second) {
                return first.title.toLowerCase()
                        .compareTo(second.title.toLowerCase());
            }
        });

        return games;
    }

    @Override
    public void onPause() {
        if (progressDialog != null) {
            progressDialog.hide();
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST_CODE_INSTALL_GAME) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (resultCode != RESULT_OK) {
            return;
        }
        Uri uri;
        if (data == null || (uri = data.getData()) == null) {
            Log.e(TAG, "Game archive is not selected");
            return;
        }
        installGame(uri);
    }

    private void installGame(Uri uri) {
        if (!FileUtil.isWritableDirectory(gamesDir)) {
            Log.e(TAG, "Games directory is not writable");
            return;
        }
        DocumentFile zipFile = DocumentFile.fromSingleUri(uiContext, uri);
        if (zipFile == null || !zipFile.exists()) {
            Log.e(TAG, "ZIP file not found: " + uri);
            return;
        }
        String filename = zipFile.getName();
        if (filename == null) {
            Log.e(TAG, "ZIP filename is null");
            return;
        }
        String gameName = FileUtil.removeFileExtension(filename);
        updateProgressDialog(true, gameName, getString(R.string.unpacking));

        if (unzip(zipFile, gameName)) {
            refreshGames();
        } else {
            String message = getString(R.string.unzipError)
                    .replace("-GAMENAME-", gameName);

            ViewUtil.showErrorDialog(uiContext, message);
        }

        updateProgressDialog(false, "", "");
    }

    private boolean unzip(DocumentFile zipFile, String gameName) {
        String folderName = FileUtil.normalizeGameFolderName(gameName);
        DocumentFile dir = gamesDir.findFile(folderName);

        if (dir == null) {
            dir = gamesDir.createDirectory(folderName);
        }
        if (!FileUtil.isWritableDirectory(dir)) {
            Log.e(TAG, "Game directory is not writable");
            return false;
        }

        return ZipUtil.unzip(uiContext, zipFile, dir);
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
        resumeGameItem.setVisible(gameRunning);
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
                showSettings();
                return true;

            case R.id.menu_about:
                showAboutDialog();
                return true;

            case R.id.menu_installgame:
                showInstallGameDialog();
                return true;

            case R.id.menu_deletegames:
                showDeleteGameDialog();
                return true;
        }

        return false;
    }

    private void showInstallGameDialog() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/zip");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_INSTALL_GAME);
    }

    private void showSettings() {
        Intent intent = new Intent(uiContext, SettingsActivity.class);
        startActivity(intent);
    }

    private void showAboutDialog() {
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        String typeface = settings.getString("typeface", "0");
        String fontSize = settings.getString("fontsize", "16");
        String backColor = String.format("#%06X", (0xFFFFFF & settings.getInt("backColor", Color.parseColor("#e0e0e0"))));
        String textColor = String.format("#%06X", (0xFFFFFF & settings.getInt("textColor", Color.parseColor("#000000"))));
        String linkColor = String.format("#%06X", (0xFFFFFF & settings.getInt("linkColor", Color.parseColor("#0000ff"))));

        String desc = ABOUT_TEMPLATE
                .replace("QSPFONTSTYLE", ViewUtil.getFontStyle(typeface))
                .replace("QSPFONTSIZE", fontSize)
                .replace("QSPTEXTCOLOR", textColor)
                .replace("QSPBACKCOLOR", backColor)
                .replace("QSPLINKCOLOR", linkColor)
                .replace("REPLACETEXT", getString(R.string.appDescription) + getString(R.string.appCredits));

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

    private void updateProgressDialog(boolean show, String title, String message) {
        showProgressDialog = show;

        if (show) {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(uiContext);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
            }
            if (!progressDialog.isShowing()) {
                progressDialog.setTitle(title);
                progressDialog.setMessage(message);
                progressDialog.show();
            }
        } else if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void showDeleteGameDialog() {
        final ArrayList<DocumentFile> gameDirs = new ArrayList<>();
        List<String> items = new ArrayList<>();
        for (DocumentFile file : gamesDir.listFiles()) {
            if (file.isDirectory()) {
                gameDirs.add(file);
                items.add(file.getName());
            }
        }

        new AlertDialog.Builder(uiContext)
                .setTitle(getString(R.string.deleteGameCmd))
                .setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DocumentFile dir = gameDirs.get(which);
                        showConfirmDeleteDialog(dir);
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
                .setMessage(getString(R.string.deleteGameQuery).replace("-GAMENAME-", "\"" + gameDir.getName() + "\""))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            FileUtil.deleteDirectory(gameDir);
                            ViewUtil.showToast(uiContext, getString(R.string.gameDeleted));
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

    private void setRemoteGames(List<GameStockItem> games) {
        remoteGames = games;
        refreshGames();
    }

    private void setGameAdapterFromTab(int tab) {
        switch (tab) {
            case TAB_LOCAL:
            case TAB_REMOTE:
            case TAB_ALL:
                gamesView.setAdapter(gameAdapters.get(tab));
                break;
        }
    }

    private class GameStockItemAdapter extends ArrayAdapter<GameStockItem> {

        private final ArrayList<GameStockItem> items;

        GameStockItemAdapter(Context context, int resource, ArrayList<GameStockItem> items) {
            super(context, resource, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.game_item, null);
            }
            GameStockItem item = items.get(position);
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
                    String text = getString(R.string.author).replace("-AUTHOR-", item.author);
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
            boolean tabHasRemoteGames = position == TAB_REMOTE || position == TAB_ALL;
            boolean gamesNotBeingLoaded = loadGameListTask == null || loadGameListTask.getStatus() == AsyncTask.Status.FINISHED;

            if (tabHasRemoteGames && gamesNotBeingLoaded) {
                if (isNetworkConnected()) {
                    LoadGameListAsyncTask task = new LoadGameListAsyncTask(GameStockActivity.this);
                    loadGameListTask = task;
                    task.execute();
                } else {
                    ViewUtil.showErrorDialog(uiContext, getString(R.string.loadGameListNetworkError));
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

    private static class LoadGameListAsyncTask extends AsyncTask<Void, Void, List<GameStockItem>> {

        private final WeakReference<GameStockActivity> activity;
        private final RemoteGameRepository remoteGameRepository = new RemoteGameRepository();

        private LoadGameListAsyncTask(GameStockActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            GameStockActivity activity = this.activity.get();
            if (activity != null) {
                activity.updateProgressDialog(true, "", activity.getString(R.string.gameListLoading));
            }
        }

        @Override
        protected List<GameStockItem> doInBackground(Void... params) {
            return remoteGameRepository.getGames();
        }

        @Override
        protected void onPostExecute(List<GameStockItem> result) {
            GameStockActivity activity = this.activity.get();
            if (activity == null) {
                return;
            }
            activity.updateProgressDialog(false, "", "");

            if (result == null) {
                String message = activity.getString(R.string.loadGameListError);
                ViewUtil.showErrorDialog(activity, message);
                return;
            }
            activity.setRemoteGames(result);
        }
    }

    private static class DownloadGameAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private final WeakReference<GameStockActivity> activity;
        private final GameStockItem game;

        private DownloadGameAsyncTask(GameStockActivity activity, GameStockItem game) {
            this.activity = new WeakReference<>(activity);
            this.game = game;
        }

        @Override
        protected void onPreExecute() {
            GameStockActivity activity = this.activity.get();
            if (activity != null) {
                activity.updateProgressDialog(true, game.title, activity.getString(R.string.downloading));
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            GameStockActivity activity = this.activity.get();
            if (activity == null) {
                return false;
            }

            File dirFile = activity.getCacheDir();
            DocumentFile cacheDir = DocumentFile.fromFile(dirFile);
            if (!FileUtil.isWritableDirectory(cacheDir)) {
                Log.e(TAG, "Cache directory is not writable");
                return false;
            }

            String zipFilename = String.valueOf(System.currentTimeMillis()).concat("_game");
            DocumentFile zipFile = cacheDir.createFile("application/zip", zipFilename);
            if (zipFile == null) {
                Log.e(TAG, "Failed to create a ZIP file: " + zipFilename);
                return false;
            }

            boolean result = false;

            if (download(zipFile) && activity.unzip(zipFile, game.title)) {
                result = writeGameInfo();
            }
            if (zipFile.exists()) {
                zipFile.delete();
            }

            return result;
        }

        private boolean download(DocumentFile zipFile) {
            GameStockActivity activity = this.activity.get();
            if (activity == null) {
                return false;
            }
            try {
                URL url = new URL(game.fileUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoOutput(true);
                conn.connect();

                try (InputStream in = conn.getInputStream()) {
                    try (OutputStream out = activity.getContentResolver().openOutputStream(zipFile.getUri())) {
                        byte[] b = new byte[8192];
                        int totalBytesRead = 0;
                        int bytesRead;
                        while ((bytesRead = in.read(b)) > 0) {
                            out.write(b, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        }
                        return totalBytesRead == game.fileSize;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to download a ZIP file", e);
                return false;
            }
        }

        private boolean writeGameInfo() {
            GameStockActivity activity = this.activity.get();
            if (activity == null) {
                return false;
            }
            String folderName = FileUtil.normalizeGameFolderName(game.title);
            DocumentFile gameDir = activity.gamesDir.findFile(folderName);
            if (!FileUtil.isWritableDirectory(gameDir)) {
                Log.e(TAG, "Game directory is not writable");
                return false;
            }
            DocumentFile infoFile = gameDir.findFile(GAME_INFO_FILENAME);
            if (infoFile == null) {
                infoFile = FileUtil.createBinaryFile(gameDir, GAME_INFO_FILENAME);
            }
            if (!FileUtil.isWritableFile(infoFile)) {
                Log.e(TAG, "Game info file is not writable");
                return false;
            }
            try (OutputStream out = activity.getContentResolver().openOutputStream(infoFile.getUri())) {
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

                return true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to write to a game info file", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            GameStockActivity activity = this.activity.get();
            if (activity == null) {
                return;
            }
            activity.updateProgressDialog(false, "", "");
            if (result) {
                activity.refreshGames();
                activity.showGameInfo(game.gameId);
            } else {
                String message = activity.getString(R.string.downloadError)
                        .replace("-GAMENAME-", game.title);

                ViewUtil.showErrorDialog(activity, message);
            }
        }
    }
}
