package com.qsp.player.view.activities;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.ACTION_OPEN_DOCUMENT_TREE;
import static com.qsp.player.utils.ColorUtil.getHexColor;
import static com.qsp.player.utils.FileUtil.GAME_INFO_FILENAME;
import static com.qsp.player.utils.FileUtil.createFile;
import static com.qsp.player.utils.FileUtil.deleteDirectory;
import static com.qsp.player.utils.FileUtil.findFileOrDirectory;
import static com.qsp.player.utils.FileUtil.getOrCreateDirectory;
import static com.qsp.player.utils.FileUtil.isWritableDirectory;
import static com.qsp.player.utils.FileUtil.isWritableFile;
import static com.qsp.player.utils.PathUtil.removeExtension;
import static com.qsp.player.utils.ViewUtil.getFontStyle;
import static com.qsp.player.utils.ViewUtil.setLocale;
import static com.qsp.player.utils.XmlUtil.objectToXml;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.qsp.player.R;
import com.qsp.player.databinding.ActivityStockBinding;
import com.qsp.player.dto.stock.GameData;
import com.qsp.player.model.install.ArchiveGameInstaller;
import com.qsp.player.model.install.ArchiveType;
import com.qsp.player.model.install.FolderGameInstaller;
import com.qsp.player.model.install.GameInstaller;
import com.qsp.player.model.install.InstallException;
import com.qsp.player.model.install.InstallType;
import com.qsp.player.utils.ViewUtil;
import com.qsp.player.view.adapters.SettingsAdapter;
import com.qsp.player.view.adapters.StockFragmentAdapter;
import com.qsp.player.viewModel.repository.LocalGameRepository;
import com.qsp.player.viewModel.repository.RemoteGameRepository;
import com.qsp.player.viewModel.viewModels.AllStockFragmentViewModel;
import com.qsp.player.viewModel.viewModels.LocalStockFragmentViewModel;
import com.qsp.player.viewModel.viewModels.RemoteStockFragmentViewModel;
import com.qsp.player.viewModel.viewModels.StockViewModel;

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
import java.util.Comparator;
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

    private final HashMap<String, GameData> gamesMap = new HashMap<>();
    private final LocalGameRepository localGameRepository = new LocalGameRepository();
    private final HashMap<InstallType, GameInstaller> installers = new HashMap<>();

    private SettingsAdapter settingsAdapter;
    private boolean showProgressDialog;
    private String currentLanguage = Locale.getDefault().getLanguage();
    private ProgressDialog progressDialog;
    private ConnectivityManager connectivityManager;
    private Collection<GameData> remoteGameData;
    private File gamesDir;
    private GameData gameData;
    private DownloadGameAsyncTask downloadTask;
    private LoadGameListAsyncTask loadGameListTask;
    private InstallType lastInstallType = InstallType.ZIP_ARCHIVE;

    private StockViewModel stockViewModel;
    private AllStockFragmentViewModel allStockViewModel;
    private LocalStockFragmentViewModel localStockViewModel;
    private RemoteStockFragmentViewModel remoteStockViewModel;
    private final StockFragmentAdapter stockFragmentAdapter =
            new StockFragmentAdapter(this);
    private ViewPager2 stockPager;

    private int tabPosition;

    private ActivityStockBinding activityStockBinding;

    public String getGameIdByPosition(int position, String tag) {
        switch (tag) {
            case "f0":
                localStockViewModel.getGameData().observe(this ,
                        gameDataArrayList -> gameData = gameDataArrayList.get(position));
                break;
            case "f1":
                remoteStockViewModel.getGameData().observe(this ,
                        gameDataArrayList -> gameData = gameDataArrayList.get(position));
                break;
            case "f2":
                allStockViewModel.getGameData().observe(this ,
                        gameDataArrayList -> gameData = gameDataArrayList.get(position));
                break;
        }
        return gameData.id;
    }

    @NonNull
    public ArrayList<GameData> getSortedGames() {
        Collection<GameData> unsortedGameData = gamesMap.values();
        ArrayList<GameData> gameData = new ArrayList<>(unsortedGameData);

        if (gameData.size() < 2) {
            return gameData;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Collections.sort(gameData , Comparator.comparing(game -> game.title.toLowerCase()));
        } else {
            Collections.sort(gameData , (first, second) -> first.title.toLowerCase()
                    .compareTo(second.title.toLowerCase()));
        }

        return gameData;
    }

    private void setRemoteGames(List<GameData> gameData) {
        remoteGameData = gameData;
        refreshGames();
    }

    public GameStockActivity() {
        installers.put(InstallType.ZIP_ARCHIVE, new ArchiveGameInstaller(this, ArchiveType.ZIP));
        installers.put(InstallType.RAR_ARCHIVE, new ArchiveGameInstaller(this, ArchiveType.RAR));
        installers.put(InstallType.AQSP_ARCHIVE, new ArchiveGameInstaller(this, ArchiveType.ZIP));
        installers.put(InstallType.FOLDER, new FolderGameInstaller(this));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityStockBinding = ActivityStockBinding.inflate(getLayoutInflater());

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        allStockViewModel =
                new ViewModelProvider(this).get(AllStockFragmentViewModel.class);
        allStockViewModel.activityObservableField.set(this);

        localStockViewModel =
                new ViewModelProvider(this).get(LocalStockFragmentViewModel.class);
        localStockViewModel.activityObservableField.set(this);

        remoteStockViewModel =
                new ViewModelProvider(this).get(RemoteStockFragmentViewModel.class);
        remoteStockViewModel.activityObservableField.set(this);

        stockViewModel =
                new ViewModelProvider(this).get(StockViewModel.class);
        stockViewModel.getData().observe(GameStockActivity.this , gameData -> {
            if (gameData != null) {
                setRemoteGames(gameData);
                updateProgressDialog(false, "", "", null);
            } else {
                ViewUtil.showErrorDialog(GameStockActivity.this,
                        getString(R.string.loadGameListNetworkError));
            }
        });

        loadSettings();
        loadLocale();
        initViewPager();
        initTabLayout();

        logger.info("GameStockActivity created");

        setContentView(activityStockBinding.getRoot());
    }

    private void loadSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        settingsAdapter = SettingsAdapter.from(preferences);
    }

    private void loadLocale() {
        setLocale(this, settingsAdapter.language);
        setTitle(R.string.gameStock);
        currentLanguage = settingsAdapter.language;
    }

    private void initViewPager() {
        stockPager = activityStockBinding.stockViewPager;
        stockPager.setAdapter(stockFragmentAdapter);
    }

    private void initTabLayout() {
        TabLayout tabLayout = activityStockBinding.stockTabLayout;

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabPosition = tab.getPosition();
                boolean tabHasRemoteGames = tabPosition == TAB_REMOTE ||
                        tabPosition == TAB_ALL;
                boolean gamesNotBeingLoaded = loadGameListTask == null ||
                        loadGameListTask.getStatus() == AsyncTask.Status.FINISHED;
                if (tabHasRemoteGames && gamesNotBeingLoaded) {
                    if (isNetworkConnected()) {
                        LoadGameListAsyncTask task = new LoadGameListAsyncTask(
                                GameStockActivity.this);
                        loadGameListTask = task;
                        task.execute();
                    } else {
                        ViewUtil.showErrorDialog(GameStockActivity.this,
                                getString(R.string.loadGameListNetworkError));
                    }
                }
                setRecyclerAdapter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        new TabLayoutMediator(
                tabLayout ,
                stockPager ,
                (tab , position) -> {
                    switch (position) {
                        case TAB_LOCAL:
                            tab.setText(R.string.tabLocal);
                            break;
                        case TAB_REMOTE:
                            tab.setText(R.string.tabRemote);
                            break;
                        case TAB_ALL:
                            tab.setText(R.string.tabAll);
                    }
                }
        ).attach();
    }

    private void setRecyclerAdapter () {
        ArrayList<GameData> gameData = getSortedGames();
        ArrayList<GameData> localGameData = new ArrayList<>();
        ArrayList<GameData> remoteGameData = new ArrayList<>();

        for (GameData data : gameData) {
            if (data.isInstalled()) {
                localGameData.add(data);
            }
            if (data.hasRemoteUrl()) {
                remoteGameData.add(data);
            }
        }

        switch (tabPosition) {
            case 0:
                localStockViewModel.setGameDataArrayList(localGameData);
                break;
            case 1:
                remoteStockViewModel.setGameDataArrayList(remoteGameData);
                break;
            case 2:
                allStockViewModel.setGameDataArrayList(gameData);
                break;
        }
    }

    public void onItemClick(int position, String tag) {
        String gameId = getGameIdByPosition(position, tag);
        showGameInfo(gameId);
    }

    public void onLongItemClick(int position, String tag) {
        String gameId = getGameIdByPosition(position, tag);
        GameData game = gamesMap.get(gameId);
        if (game != null) {
            if (game.isInstalled()) {
                playGame(game);
            } else {
                showGameInfo(gameId);
            }
        } else {
            logger.error("Game not found: " + gameId);
        }
    }

    private void showGameInfo(String gameId) {
        final GameData gameData = gamesMap.get(gameId);
        if (gameData == null) {
            logger.error("GameData not found: " + gameId);
            return;
        }
        StringBuilder message = new StringBuilder();
        if (gameData.author.length() > 0) {
            message.append(getString(R.string.author).replace("-AUTHOR-", gameData.author));
        }
        if (gameData.version.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.version).replace("-VERSION-", gameData.version));
        }
        if (gameData.getFileSize() > 0) {
            message.append('\n');
            message.append(getString(R.string.fileSize).replace("-SIZE-", Integer.toString(gameData.getFileSize() / 1024)));
        }
        if (gameData.fileExt.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.fileType).replace("-TYPE-", gameData.fileExt));
            if (gameData.fileExt.equals("aqsp")) {
                message.append(" ");
                message.append(getString(R.string.experimental));
            }
        }
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this)
                .setMessage(message)
                .setTitle(gameData.title)
                .setIcon(R.drawable.icon)
                .setNegativeButton(getString(R.string.close), (dialog, which) -> dialog.cancel());

        if (gameData.isInstalled()) {
            alertBuilder.setNeutralButton(getString(R.string.play), (dialog, which) -> playGame(gameData));
        }
        if (gameData.hasRemoteUrl()) {
            alertBuilder.setPositiveButton(gameData.isInstalled() ? getString(R.string.update) : getString(R.string.download), (dialog, which) -> downloadGame(gameData));
        }
        alertBuilder.create().show();
    }

    private void playGame(final GameData gameData) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("gameId", gameData.id);
        intent.putExtra("gameTitle", gameData.title);
        intent.putExtra("gameDirUri", gameData.gameDir.getAbsolutePath());

        int gameFileCount = gameData.gameFiles.size();
        if (gameFileCount == 0) {
            logger.warn("GameData has no gameData files");
            return;
        }
        if (gameFileCount == 1) {
            intent.putExtra("gameFileUri", gameData.gameFiles.get(0).getAbsolutePath());
            startActivity(intent);
        } else {
            ArrayList<String> names = new ArrayList<>();
            for (File file : gameData.gameFiles) {
                names.add(file.getName());
            }
            new AlertDialog.Builder(GameStockActivity.this)
                    .setTitle(getString(R.string.selectGameFile))
                    .setCancelable(false)
                    .setItems(names.toArray(new String[0]), (dialog, which) -> {
                        intent.putExtra("gameFileUri", gameData.gameFiles.get(which).getAbsolutePath());
                        startActivity(intent);
                    })
                    .show();
        }
    }

    private void downloadGame(GameData gameData) {
        if (!isNetworkConnected()) {
            ViewUtil.showErrorDialog(this, getString(R.string.downloadNetworkError));
            return;
        }
        DownloadGameAsyncTask task = new DownloadGameAsyncTask(this, gameData);
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
        super.onDestroy();
        logger.info("GameStockActivity destroyed");
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
        if (currentLanguage.equals(settingsAdapter.language)) return;

        setLocale(this, settingsAdapter.language);
        setTitle(getString(R.string.gameStock));
        refreshTabLayout();
        invalidateOptionsMenu();

        currentLanguage = settingsAdapter.language;
    }

    private void refreshTabLayout() {
        TabLayout tabLayout = activityStockBinding.stockTabLayout;
        Objects.requireNonNull(tabLayout.getTabAt(2)).setText(getString(R.string.tabAll));
        Objects.requireNonNull(tabLayout.getTabAt(1)).setText(getString(R.string.tabRemote));
        Objects.requireNonNull(tabLayout.getTabAt(0)).setText(getString(R.string.tabLocal));
        refreshGames();
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
        if (remoteGameData != null) {
            for (GameData gameData : remoteGameData) {
                gamesMap.put(gameData.id, gameData);
            }
        }
        for (GameData localGameData : localGameRepository.getGames()) {
            GameData remoteGameData = gamesMap.get(localGameData.id);
            if (remoteGameData != null) {
                GameData aggregateGameData = new GameData(remoteGameData);
                aggregateGameData.gameDir = localGameData.gameDir;
                aggregateGameData.gameFiles = localGameData.gameFiles;
                gamesMap.put(localGameData.id, aggregateGameData);
            } else {
                gamesMap.put(localGameData.id, localGameData);
            }
        }
        setRecyclerAdapter();
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
            logger.error("GameData archive or directory is not selected");
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
        GameData gameData = new GameData();
        gameData.id = gameName;
        gameData.title = gameName;
        installGame(file, lastInstallType, gameData);
    }

    private boolean installGame(DocumentFile gameFile, InstallType type, GameData gameData) {
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
            doInstallGame(installer, gameFile, gameData);
            return true;
        } catch (InstallException ex) {
            logger.error(ex.getMessage());
            return false;
        }
    }

    private void doInstallGame(GameInstaller installer, DocumentFile gameFile, GameData gameData) {
        File gameDir = getOrCreateGameDirectory(gameData.title);
        if (!isWritableDirectory(gameDir)) {
            logger.error("GameData directory is not writable");
            return;
        }
        updateProgressDialog(true, gameData.title, getString(R.string.installing), null);

        boolean installed = installer.install(gameData.title, gameFile, gameDir);
        if (installed) {
            writeGameInfo(gameData , gameDir);
            refreshGames();
        }

        updateProgressDialog(false, "", "", null);
    }

    @NonNull
    private File getOrCreateGameDirectory(String gameName) {
        String folderName = normalizeGameFolderName(gameName);
        return getOrCreateDirectory(gamesDir, folderName);
    }

    private void writeGameInfo(GameData gameData , File gameDir) {
        File infoFile = findFileOrDirectory(gameDir, GAME_INFO_FILENAME);
        if (infoFile == null) {
            infoFile = createFile(gameDir, GAME_INFO_FILENAME);
        }
        if (!isWritableFile(infoFile)) {
            logger.error("GameData info file is not writable");
            return;
        }
        try (FileOutputStream out = new FileOutputStream(infoFile);
             OutputStreamWriter writer = new OutputStreamWriter(out)) {
            writer.write(objectToXml(gameData));
        } catch (Exception ex) {
            logger.error("Failed to write to a gameData info file", ex);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stock, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_options) {
            showSettings();
            return true;
        } else if (itemId == R.id.menu_about) {
            showAboutDialog();
            return true;
        } else if (itemId == R.id.menu_installfromzip) {
            showInstallGameDialog(InstallType.ZIP_ARCHIVE);
            return true;
        } else if (itemId == R.id.menu_installfromrar) {
            showInstallGameDialog(InstallType.RAR_ARCHIVE);
            return true;
        } else if (itemId == R.id.menu_installfromfolder) {
            showInstallGameDialog(InstallType.FOLDER);
            return true;
        } else if (itemId == R.id.menu_deletegame) {
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
                .replace("QSPFONTSTYLE", getFontStyle(settingsAdapter.typeface))
                .replace("QSPFONTSIZE", Integer.toString(settingsAdapter.fontSize))
                .replace("QSPTEXTCOLOR", getHexColor(settingsAdapter.textColor))
                .replace("QSPBACKCOLOR", getHexColor(settingsAdapter.backColor))
                .replace("QSPLINKCOLOR", getHexColor(settingsAdapter.linkColor))
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
        return super.onKeyDown(keyCode, event);
    }

    private void updateProgressDialog(boolean show,
                                      String title,
                                      String message,
                                      final Runnable onCancel) {
        showProgressDialog = show;
        if (title.isEmpty()) {
            if (progressDialog != null && progressDialog.isShowing()) {
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
        ArrayList<GameData> deletableGameData = new ArrayList<>();
        ArrayList<String> items = new ArrayList<>();

        for (GameData gameData : gamesMap.values()) {
            if (!gameData.isInstalled()) continue;
            deletableGameData.add(gameData);
            items.add(gameData.title);
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.deleteGameCmd))
                .setItems(items.toArray(new String[0]), (dialog, which) -> {
                    GameData gameData = deletableGameData.get(which);
                    showConfirmDeleteDialog(gameData);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
                })
                .create()
                .show();
    }

    private void showConfirmDeleteDialog(final GameData gameData) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.deleteGameQuery).replace("-GAMENAME-", "\"" + gameData.title + "\""))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        deleteDirectory(gameData.gameDir);
                        ViewUtil.showToast(this, getString(R.string.gameDeleted));
                        refreshGames();
                    }
                })
                .setNegativeButton(android.R.string.no, (dialog, whichButton) -> {
                })
                .create()
                .show();
    }

    @NonNull
    private static String normalizeGameFolderName(String name) {
        String result = name.endsWith("...") ? name.substring(0, name.length() - 3) : name;

        return result.replaceAll("[:\"?*|<> ]", "_")
                .replace("__", "_");
    }

    private static class LoadGameListAsyncTask extends AsyncTask<Void, Void, List<GameData>> {
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
        protected List<GameData> doInBackground(Void... params) {
            return remoteGameRepository.getGames();
        }

        @Override
        protected void onPostExecute(List<GameData> result) {
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
        private final GameData gameData;

        private volatile boolean cancelled = false;

        private DownloadGameAsyncTask(GameStockActivity activity, GameData gameData) {
            this.activity = new WeakReference<>(activity);
            this.gameData = gameData;
        }

        @Override
        protected void onPreExecute() {
            GameStockActivity activity = this.activity.get();
            if (activity != null) {
                activity.updateProgressDialog(true, gameData.title, activity.getString(R.string.downloading), () -> cancelled = true);
                activity.progressDialog.setIndeterminate(false);
                activity.progressDialog.setMax(gameData.getFileSize());
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
                InstallType installType = installTypeFromExtension(gameData.fileExt);
                installed = activity.installGame(DocumentFile.fromFile(archiveFile), installType, gameData);
            }
            if (archiveFile.exists()) {
                if (archiveFile.delete()) {
                    logger.info("Archive was delete");
                } else {
                    logger.error("Archive was no delete");
                }
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
                URL url = new URL(gameData.fileUrl);
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
                                logger.info("GameData download was cancelled");
                                return false;
                            }
                            activity.progressDialog.incrementProgressBy(bytesRead);
                            out.write(b, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        }
                        return totalBytesRead == gameData.getFileSize();
                    }
                }
            } catch (IOException ex) {
                logger.error("Failed to download a ZIP file", ex);
                return false;
            }
        }

        private InstallType installTypeFromExtension(String ext) {
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
            activity.updateProgressDialog(true, gameData.title, activity.getString(R.string.installing), null);
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
                    activity.showGameInfo(gameData.id);
                    break;
                case DOWNLOAD_FAILED:
                    message = activity.getString(R.string.downloadError).replace("-GAMENAME-", gameData.title);
                    ViewUtil.showErrorDialog(activity, message);
                    break;
                case INSTALL_FAILED:
                    message = activity.getString(R.string.installError).replace("-GAMENAME-", gameData.title);
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