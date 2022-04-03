package com.qsp.player.stock;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.ACTION_OPEN_DOCUMENT_TREE;
import static com.qsp.player.shared.util.ColorUtil.getHexColor;
import static com.qsp.player.shared.util.FileUtil.GAME_INFO_FILENAME;
import static com.qsp.player.shared.util.FileUtil.createFile;
import static com.qsp.player.shared.util.FileUtil.deleteDirectory;
import static com.qsp.player.shared.util.FileUtil.findFileOrDirectory;
import static com.qsp.player.shared.util.FileUtil.getOrCreateDirectory;
import static com.qsp.player.shared.util.FileUtil.isWritableDirectory;
import static com.qsp.player.shared.util.FileUtil.isWritableFile;
import static com.qsp.player.shared.util.PathUtil.removeExtension;
import static com.qsp.player.shared.util.ThreadUtil.isMainThread;
import static com.qsp.player.shared.util.ViewUtil.getFontStyle;
import static com.qsp.player.shared.util.ViewUtil.setLocale;
import static com.qsp.player.shared.util.XmlUtil.objectToXml;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
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
import androidx.preference.PreferenceManager;

import com.qsp.player.R;
import com.qsp.player.install.ArchiveGameInstaller;
import com.qsp.player.install.ArchiveType;
import com.qsp.player.install.FolderGameInstaller;
import com.qsp.player.install.GameInstaller;
import com.qsp.player.install.InstallException;
import com.qsp.player.install.InstallType;
import com.qsp.player.settings.Settings;
import com.qsp.player.settings.SettingsActivity;
import com.qsp.player.shared.util.ViewUtil;
import com.qsp.player.stock.dto.Game;
import com.qsp.player.stock.repository.LocalGameRepository;
import com.qsp.player.stock.repository.RemoteGameRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class GameStockActivity extends AppCompatActivity {
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

    private static final Logger logger = LoggerFactory.getLogger(GameStockActivity.class);

    private final HashMap<String, Game> gamesMap = new HashMap<>();
    private final SparseArrayCompat<GameStockItemAdapter> gameAdapters = new SparseArrayCompat<>();
    private final LocalGameRepository localGameRepository = new LocalGameRepository();
    private final HashMap<InstallType, GameInstaller> installers = new HashMap<>();

    private Settings settings;
    private String gameRunning;
    private boolean showProgressDialog;
    private String currentLanguage = Locale.getDefault().getLanguage();
    private ListView gamesView;
    private ProgressDialog progressDialog;
    private ConnectivityManager connectivityManager;
    private Collection<Game> remoteGames;
    private File gamesDir;
    private DownloadGameAsyncTask downloadTask;
    private LoadGameListAsyncTask loadGameListTask;
    private InstallType lastInstallType = InstallType.ZIP_ARCHIVE;

    public GameStockActivity() {
        installers.put(InstallType.ZIP_ARCHIVE, new ArchiveGameInstaller(this, ArchiveType.ZIP));
        installers.put(InstallType.RAR_ARCHIVE, new ArchiveGameInstaller(this, ArchiveType.RAR));
        installers.put(InstallType.AQSP_ARCHIVE, new ArchiveGameInstaller(this, ArchiveType.ZIP));
        installers.put(InstallType.FOLDER, new FolderGameInstaller(this));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock);

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        Intent intent = getIntent();
        gameRunning = intent.getStringExtra("gameRunning");

        loadSettings();
        loadLocale();
        initGamesListView();
        initActionBar(savedInstanceState);
        setResult(RESULT_CANCELED);

        logger.info("GameStockActivity created");
    }

    private void loadSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        settings = Settings.from(preferences);
    }

    private void loadLocale() {
        setLocale(this, settings.language);
        setTitle(R.string.gameStock);
        currentLanguage = settings.language;
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
        gamesView.setOnItemClickListener((parent, view, position, id) -> {
            String gameId = getGameIdByPosition(position);
            showGameInfo(gameId);
        });
        gamesView.setOnItemLongClickListener((parent, view, position, id) -> {
            String gameId = getGameIdByPosition(position);
            Game game = gamesMap.get(gameId);
            if (game != null) {
                playGame(game);
            } else {
                logger.error("Game not found: " + gameId);
            }
            return true;
        });
    }

    private String getGameIdByPosition(int position) {
        Game game = (Game) gamesView.getAdapter().getItem(position);
        return game.id;
    }

    private void showGameInfo(String gameId) {
        final Game game = gamesMap.get(gameId);
        if (game == null) {
            logger.error("Game not found: " + gameId);
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
        if (game.getFileSize() > 0) {
            message.append('\n');
            message.append(getString(R.string.fileSize).replace("-SIZE-", Integer.toString(game.getFileSize() / 1024)));
        }
        if (game.fileExt.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.fileType).replace("-TYPE-", game.fileExt));
            if (game.fileExt.equals("aqsp")) {
                message.append(" ");
                message.append(getString(R.string.experimental));
            }
        }
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this)
                .setMessage(message)
                .setTitle(game.title)
                .setIcon(R.drawable.icon)
                .setNegativeButton(getString(R.string.close), (dialog, which) -> dialog.cancel());

        if (game.isInstalled()) {
            alertBuilder.setNeutralButton(getString(R.string.play), (dialog, which) -> playGame(game));
        }
        if (game.hasRemoteUrl()) {
            alertBuilder.setPositiveButton(game.isInstalled() ? getString(R.string.update) : getString(R.string.download), (dialog, which) -> downloadGame(game));
        }
        alertBuilder.create().show();
    }

    private void playGame(@NonNull final Game game) {
        final Intent data = new Intent();
        data.putExtra("gameId", game.id);
        data.putExtra("gameTitle", game.title);
        data.putExtra("gameDirUri", game.gameDir.getAbsolutePath());

        int gameFileCount = game.gameFiles.size();
        switch (gameFileCount) {
            case 0:
                logger.warn("Game has no game files");
                return;
            case 1:
                data.putExtra("gameFileUri", game.gameFiles.get(0).getAbsolutePath());
                setResult(RESULT_OK, data);
                finish();
                return;
            default:
                break;
        }

        ArrayList<String> names = new ArrayList<>();
        for (File file : game.gameFiles) {
            names.add(file.getName());
        }
        new AlertDialog.Builder(GameStockActivity.this)
                .setTitle(getString(R.string.selectGameFile))
                .setCancelable(false)
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    data.putExtra("gameFileUri", game.gameFiles.get(which).getAbsolutePath());
                    setResult(RESULT_OK, data);
                    finish();
                })
                .show();
    }

    private void downloadGame(Game game) {
        if (!isNetworkConnected()) {
            ViewUtil.showErrorDialog(this, getString(R.string.downloadNetworkError));
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
        logger.info("GameStockActivity destroyed");
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

        loadSettings();
        updateLocale();
        refreshGamesDirectory();

        if (showProgressDialog && progressDialog != null) {
            progressDialog.show();
        }
    }

    private void updateLocale() {
        if (currentLanguage.equals(settings.language)) return;

        setLocale(this, settings.language);
        setTitle(getString(R.string.gameStock));
        refreshActionBar();
        invalidateOptionsMenu();

        currentLanguage = settings.language;
    }

    private void refreshActionBar() {
        ActionBar bar = getSupportActionBar();
        if (bar == null) return;

        bar.getTabAt(2).setText(getString(R.string.tabAll));
        bar.getTabAt(1).setText(getString(R.string.tabRemote));
        bar.getTabAt(0).setText(getString(R.string.tabLocal));
    }

    private void refreshGamesDirectory() {
        File extFilesDir = getExternalFilesDir(null);
        if (extFilesDir == null) {
            logger.error("External files directory not found");
            return;
        }
        File dir = getOrCreateDirectory(extFilesDir, "games");
        if (!isWritableDirectory(dir)) {
            logger.error("Games directory is not writable");
            String message = getString(R.string.gamesDirError);
            ViewUtil.showErrorDialog(this, message);
            return;
        }
        gamesDir = dir;
        localGameRepository.setGamesDirectory(gamesDir);
        refreshGames();
    }

    private void refreshGames() {
        gamesMap.clear();

        if (remoteGames != null) {
            for (Game game : remoteGames) {
                gamesMap.put(game.id, game);
            }
        }
        for (Game localGame : localGameRepository.getGames()) {
            Game remoteGame = gamesMap.get(localGame.id);
            if (remoteGame != null) {
                Game aggregateGame = new Game(remoteGame);
                aggregateGame.gameDir = localGame.gameDir;
                aggregateGame.gameFiles = localGame.gameFiles;
                gamesMap.put(localGame.id, aggregateGame);
            } else {
                gamesMap.put(localGame.id, localGame);
            }
        }


        if (isMainThread()) {
            refreshGameAdapters();
        } else {
            runOnUiThread(this::refreshGameAdapters);
        }
    }

    private void refreshGameAdapters() {
        ArrayList<Game> games = getSortedGames();
        ArrayList<Game> localGames = new ArrayList<>();
        ArrayList<Game> remoteGames = new ArrayList<>();

        for (Game game : games) {
            if (game.isInstalled()) {
                localGames.add(game);
            }
            if (game.hasRemoteUrl()) {
                remoteGames.add(game);
            }
        }

        gameAdapters.put(TAB_LOCAL, new GameStockItemAdapter(this, R.layout.list_item_game, localGames));
        gameAdapters.put(TAB_REMOTE, new GameStockItemAdapter(this, R.layout.list_item_game, remoteGames));
        gameAdapters.put(TAB_ALL, new GameStockItemAdapter(this, R.layout.list_item_game, games));

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            setGameAdapterFromTab(bar.getSelectedNavigationIndex());
        }
    }

    @NonNull
    private ArrayList<Game> getSortedGames() {
        Collection<Game> unsortedGames = gamesMap.values();
        ArrayList<Game> games = new ArrayList<>(unsortedGames);

        if (games.size() < 2) {
            return games;
        }
        Collections.sort(games, (first, second) -> first.title.toLowerCase()
                .compareTo(second.title.toLowerCase()));

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
            logger.error("Game archive or directory is not selected");
            return;
        }
        DocumentFile file;
        if (lastInstallType == InstallType.FOLDER) {
            file = DocumentFile.fromTreeUri(this, uri);
        } else {
            file = DocumentFile.fromSingleUri(this, uri);
        }
        assert file != null;
        String gameName = removeExtension(Objects.requireNonNull(file.getName()));
        Game game = new Game();
        game.id = gameName;
        game.title = gameName;
        installGame(file, lastInstallType, game);
    }

    private boolean installGame(DocumentFile gameFile, InstallType type, Game game) {
        if (!isWritableDirectory(gamesDir)) {
            logger.error("Games directory is not writable");
            return false;
        }
        GameInstaller installer = installers.get(type);
        if (installer == null) {
            logger.error(String.format("Installer not found by install type '%s'", type));
            return false;
        }
        try {
            doInstallGame(installer, gameFile, game);
            return true;
        } catch (InstallException ex) {
            logger.error(ex.getMessage());
            return false;
        }
    }

    private void doInstallGame(GameInstaller installer, DocumentFile gameFile, @NonNull Game game) {
        File gameDir = getOrCreateGameDirectory(game.title);
        if (!isWritableDirectory(gameDir)) {
            logger.error("Game directory is not writable");
            return;
        }
        updateProgressDialog(true, game.title, getString(R.string.installing), null);

        boolean installed = installer.install(game.title, gameFile, gameDir);
        if (installed) {
            writeGameInfo(game, gameDir);
            refreshGames();
        }

        updateProgressDialog(false, "", "", null);
    }

    private File getOrCreateGameDirectory(String gameName) {
        String folderName = normalizeGameFolderName(gameName);
        return getOrCreateDirectory(gamesDir, folderName);
    }

    private void writeGameInfo(Game game, File gameDir) {
        File infoFile = findFileOrDirectory(gameDir, GAME_INFO_FILENAME);
        if (infoFile == null) {
            infoFile = createFile(gameDir, GAME_INFO_FILENAME);
        }
        if (!isWritableFile(infoFile)) {
            logger.error("Game info file is not writable");
            return;
        }
        try (FileOutputStream out = new FileOutputStream(infoFile);
             OutputStreamWriter writer = new OutputStreamWriter(out)) {
            writer.write(objectToXml(game));
        } catch (Exception ex) {
            logger.error("Failed to write to a game info file", ex);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stock, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem resumeGameItem = menu.findItem(R.id.menu_resumegame);
        resumeGameItem.setVisible(gameRunning != null);

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

            case R.id.menu_installfromzip:
                showInstallGameDialog(InstallType.ZIP_ARCHIVE);
                return true;

            case R.id.menu_installfromrar:
                showInstallGameDialog(InstallType.RAR_ARCHIVE);
                return true;

            case R.id.menu_installfromfolder:
                showInstallGameDialog(InstallType.FOLDER);
                return true;

            case R.id.menu_deletegame:
                showDeleteGameDialog();
                return true;
        }

        return false;
    }

    private void showInstallGameDialog(InstallType installType) {
        String action;
        String extension = null;
        switch (installType) {
            case ZIP_ARCHIVE:
                action = ACTION_OPEN_DOCUMENT;
                extension = "zip";
                break;
            case RAR_ARCHIVE:
                action = ACTION_OPEN_DOCUMENT;
                extension = "rar";
                break;
            case FOLDER:
                action = ACTION_OPEN_DOCUMENT_TREE;
                break;
            default:
                throw new IllegalArgumentException("Unsupported install type: " + installType);
        }
        lastInstallType = installType;
        Intent intent = new Intent(action);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (extension != null) {
            intent.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
        }
        startActivityForResult(intent, REQUEST_CODE_INSTALL_GAME);
    }

    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void showAboutDialog() {
        View messageView = getLayoutInflater().inflate(R.layout.dialog_about, null, false);

        String desc = ABOUT_TEMPLATE
                .replace("QSPFONTSTYLE", getFontStyle(settings.typeface))
                .replace("QSPFONTSIZE", Integer.toString(settings.fontSize))
                .replace("QSPTEXTCOLOR", getHexColor(settings.textColor))
                .replace("QSPBACKCOLOR", getHexColor(settings.backColor))
                .replace("QSPLINKCOLOR", getHexColor(settings.linkColor))
                .replace("REPLACETEXT", getString(R.string.appDescription) + getString(R.string.appCredits));

        WebView descView = messageView.findViewById(R.id.about_descrip);
        descView.loadDataWithBaseURL("", desc, "text/html", "utf-8", "");

        new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
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

    private void updateProgressDialog(boolean show, @NonNull String title, String message, final Runnable onCancel) {
        showProgressDialog = show;
        if (title.isEmpty()) {
            if (show) {
                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(this);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                }
                progressDialog.setTitle(title);
                progressDialog.setMessage(message);
                progressDialog.setCancelable(false);
                progressDialog.setCanceledOnTouchOutside(false);
                if (onCancel != null) {
                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), (dialog, which) -> {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            dialog.dismiss();
                            onCancel.run();
                        }
                    });
                }
                if (!progressDialog.isShowing()) {
                    progressDialog.show();
                }
            } else if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        } else {
            if (show) {
                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(this);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                }
                progressDialog.setTitle(title);
                progressDialog.setMessage(message);
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.setCanceledOnTouchOutside(false);
                if (onCancel != null) {
                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), (dialog, which) -> {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            dialog.dismiss();
                            onCancel.run();
                        }
                    });
                }
                if (!progressDialog.isShowing()) {
                    progressDialog.show();
                }
            } else if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }

    private void showDeleteGameDialog() {
        ArrayList<Game> deletableGames = new ArrayList<>();
        ArrayList<String> items = new ArrayList<>();

        for (Game game : gamesMap.values()) {
            if (!game.isInstalled()) continue;

            deletableGames.add(game);
            items.add(game.title);
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.deleteGameCmd))
                .setItems(items.toArray(new String[0]), (dialog, which) -> {
                    Game game = deletableGames.get(which);
                    showConfirmDeleteDialog(game);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
                })
                .create()
                .show();
    }

    private void showConfirmDeleteDialog(@NonNull final Game game) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.deleteGameQuery).replace("-GAMENAME-", "\"" + game.title + "\""))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        if (gameRunning != null && gameRunning.equals(game.id)) {
                            gameRunning = null;
                            invalidateOptionsMenu();
                        }
                        deleteDirectory(game.gameDir);
                        ViewUtil.showToast(this, getString(R.string.gameDeleted));
                        refreshGames();
                    }
                })
                .setNegativeButton(android.R.string.no, (dialog, whichButton) -> {
                })
                .create()
                .show();
    }

    private void setRemoteGames(List<Game> games) {
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

    private static String normalizeGameFolderName(String name) {
        String result = name.endsWith("...") ? name.substring(0, name.length() - 3) : name;

        return result.replaceAll("[:\"?*|<> ]", "_")
                .replace("__", "_");
    }

    private class GameStockItemAdapter extends ArrayAdapter<Game> {
        private final ArrayList<Game> items;

        GameStockItemAdapter(Context context, int resource, ArrayList<Game> items) {
            super(context, resource, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.list_item_game, null);
            }
            Game item = items.get(position);
            if (item != null) {
                TextView titleView = convertView.findViewById(R.id.game_title);
                if (titleView != null) {
                    titleView.setText(item.title);
                    if (item.isInstalled()) {
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
        public void onTabSelected(@NonNull ActionBar.Tab tab, FragmentTransaction ft) {
            int position = tab.getPosition();
            boolean tabHasRemoteGames = position == TAB_REMOTE || position == TAB_ALL;
            boolean gamesNotBeingLoaded = loadGameListTask == null || loadGameListTask.getStatus() == AsyncTask.Status.FINISHED;

            if (tabHasRemoteGames && gamesNotBeingLoaded) {
                if (isNetworkConnected()) {
                    LoadGameListAsyncTask task = new LoadGameListAsyncTask(GameStockActivity.this);
                    loadGameListTask = task;
                    task.execute();
                } else {
                    ViewUtil.showErrorDialog(GameStockActivity.this, getString(R.string.loadGameListNetworkError));
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

    private static class LoadGameListAsyncTask extends AsyncTask<Void, Void, List<Game>> {
        private final WeakReference<GameStockActivity> activity;
        private final RemoteGameRepository remoteGameRepository = new RemoteGameRepository();

        private LoadGameListAsyncTask(GameStockActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            GameStockActivity activity = this.activity.get();
            if (activity != null) {
                activity.updateProgressDialog(true, "", activity.getString(R.string.gameListLoading), null);
            }
        }

        @Override
        protected List<Game> doInBackground(Void... params) {
            return remoteGameRepository.getGames();
        }

        @Override
        protected void onPostExecute(List<Game> result) {
            GameStockActivity activity = this.activity.get();
            if (activity == null) {
                return;
            }
            activity.updateProgressDialog(false, "", "", null);

            if (result == null) {
                String message = activity.getString(R.string.loadGameListError);
                ViewUtil.showErrorDialog(activity, message);
                return;
            }
            activity.setRemoteGames(result);
        }
    }

    private static class DownloadGameAsyncTask extends AsyncTask<Void,
            DownloadGameAsyncTask.DownloadPhase,
            DownloadGameAsyncTask.DownloadResult> {
        private final WeakReference<GameStockActivity> activity;
        private final Game game;

        private volatile boolean cancelled = false;

        private DownloadGameAsyncTask(GameStockActivity activity, Game game) {
            this.activity = new WeakReference<>(activity);
            this.game = game;
        }

        @Override
        protected void onPreExecute() {
            GameStockActivity activity = this.activity.get();
            if (activity != null) {
                activity.updateProgressDialog(true, game.title, activity.getString(R.string.downloading), () -> cancelled = true);
                activity.progressDialog.setIndeterminate(false);
                activity.progressDialog.setMax(game.getFileSize());
            }
        }

        @Override
        protected DownloadResult doInBackground(Void... params) {
            GameStockActivity activity = this.activity.get();
            if (activity == null) {
                return DownloadResult.DOWNLOAD_FAILED;
            }

            File cacheDir = activity.getCacheDir();
            if (!isWritableDirectory(cacheDir)) {
                logger.error("Cache directory is not writable");
                return DownloadResult.DOWNLOAD_FAILED;
            }

            String archiveFilename = String.valueOf(SystemClock.elapsedRealtime()).concat("_game");
            File archiveFile = createFile(cacheDir, archiveFilename);
            if (archiveFile == null) {
                logger.error("Failed to create an archive file: " + archiveFilename);
                return DownloadResult.DOWNLOAD_FAILED;
            }

            boolean downloaded = download(archiveFile);
            boolean installed = false;

            if (downloaded) {
                publishProgress(DownloadPhase.INSTALL);
                InstallType installType = installTypeFromExtension(game.fileExt);
                installed = activity.installGame(DocumentFile.fromFile(archiveFile), installType, game);
            }
            if (archiveFile.exists()) {
                archiveFile.delete();
            }
            if (!downloaded) {
                return cancelled ? DownloadResult.CANCELLED : DownloadResult.DOWNLOAD_FAILED;
            }
            if (!installed) {
                return DownloadResult.INSTALL_FAILED;
            }

            return DownloadResult.OK;
        }

        private boolean download(File zipFile) {
            GameStockActivity activity = this.activity.get();
            if (activity == null) {
                return false;
            }
            try {
                URL url = new URL(game.fileUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                try (InputStream in = conn.getInputStream()) {
                    try (FileOutputStream out = new FileOutputStream(zipFile)) {
                        byte[] b = new byte[8192];
                        int totalBytesRead = 0;
                        int bytesRead;
                        while ((bytesRead = in.read(b)) > 0 ||
                                activity.progressDialog.getProgress() < activity.progressDialog.getMax()) {
                            if (cancelled) {
                                logger.info("Game download was cancelled");
                                return false;
                            }
                            activity.progressDialog.incrementProgressBy(bytesRead);
                            out.write(b, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        }
                        return totalBytesRead == game.getFileSize();
                    }
                }
            } catch (IOException ex) {
                logger.error("Failed to download a ZIP file", ex);
                return false;
            }
        }

        private InstallType installTypeFromExtension(@NonNull String ext) {
            switch (ext) {
                case "zip":
                    return InstallType.ZIP_ARCHIVE;
                case "rar":
                    return InstallType.RAR_ARCHIVE;
                case "aqsp":
                    return InstallType.AQSP_ARCHIVE;
                default:
                    throw new IllegalArgumentException("Unsupported file type: " + ext);
            }
        }

        @Override
        protected void onProgressUpdate(DownloadPhase... values) {
            super.onProgressUpdate(values);
            GameStockActivity activity = this.activity.get();
            if (activity == null) {
                return;
            }
            activity.updateProgressDialog(true, game.title, activity.getString(R.string.installing), null);
        }

        @Override
        protected void onPostExecute(DownloadResult result) {
            GameStockActivity activity = this.activity.get();
            if (activity == null) {
                return;
            }
            activity.updateProgressDialog(false, "", "", null);

            String message;
            switch (result) {
                case OK:
                    activity.refreshGames();
                    activity.showGameInfo(game.id);
                    break;
                case DOWNLOAD_FAILED:
                    message = activity.getString(R.string.downloadError).replace("-GAMENAME-", game.title);
                    ViewUtil.showErrorDialog(activity, message);
                    break;
                case INSTALL_FAILED:
                    message = activity.getString(R.string.installError).replace("-GAMENAME-", game.title);
                    ViewUtil.showErrorDialog(activity, message);
                    break;
            }
        }

        private enum DownloadPhase {
            DOWNLOAD,
            INSTALL
        }

        private enum DownloadResult {
            OK,
            CANCELLED,
            DOWNLOAD_FAILED,
            INSTALL_FAILED
        }
    }
}
