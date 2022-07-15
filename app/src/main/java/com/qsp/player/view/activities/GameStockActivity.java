package com.qsp.player.view.activities;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.ACTION_OPEN_DOCUMENT_TREE;
import static com.qsp.player.utils.FileUtil.GAME_INFO_FILENAME;
import static com.qsp.player.utils.FileUtil.createFile;
import static com.qsp.player.utils.FileUtil.deleteDirectory;
import static com.qsp.player.utils.FileUtil.findFileOrDirectory;
import static com.qsp.player.utils.FileUtil.getOrCreateDirectory;
import static com.qsp.player.utils.FileUtil.isWritableDirectory;
import static com.qsp.player.utils.FileUtil.isWritableFile;
import static com.qsp.player.utils.LanguageUtil.setLocale;
import static com.qsp.player.utils.PathUtil.removeExtension;
import static com.qsp.player.utils.XmlUtil.objectToXml;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;

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
import com.qsp.player.viewModel.viewModels.FragmentAllVM;
import com.qsp.player.viewModel.viewModels.FragmentLocalVM;
import com.qsp.player.viewModel.viewModels.FragmentRemoteVM;
import com.qsp.player.viewModel.viewModels.GameStockActivityVM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
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

    private static final Logger logger = LoggerFactory.getLogger(GameStockActivity.class);

    private final HashMap<String, GameData> gamesMap = new HashMap<>();
    private final LocalGameRepository localGameRepository = new LocalGameRepository();
    private final HashMap<InstallType, GameInstaller> installers = new HashMap<>();

    private SettingsAdapter settingsAdapter;
    private boolean showProgressDialog;
    private String currentLanguage = Locale.getDefault().getLanguage();
    private ProgressDialog progressDialog;
    private Collection<GameData> remoteGameData;
    private File gamesDir;
    private GameData gameData;
    private InstallType lastInstallType = InstallType.ZIP_ARCHIVE;

    private GameStockActivityVM gameStockActivityVM;
    private FragmentAllVM allStockViewModel;
    private FragmentLocalVM localStockViewModel;
    private FragmentRemoteVM remoteStockViewModel;
    private final StockFragmentAdapter stockFragmentAdapter =
            new StockFragmentAdapter(this);
    private ViewPager2 stockPager;

    private int tabPosition;

    private ActivityStockBinding activityStockBinding;

    public String getGameIdByPosition(int position, String tag) {
        switch (tag) {
            case "f0":
                localStockViewModel.getGameData().observe(this, gameDataArrayList -> {
                    if (!gameDataArrayList.isEmpty() && gameDataArrayList.size() > position) {
                        gameData = gameDataArrayList.get(position);
                    }
                });
                break;
            case "f1":
                remoteStockViewModel.getGameData().observe(this , gameDataArrayList -> {
                    if (!gameDataArrayList.isEmpty() && gameDataArrayList.size() > position) {
                        gameData = gameDataArrayList.get(position);
                    }
                });
                break;
            case "f2":
                allStockViewModel.getGameData().observe(this , gameDataArrayList -> {
                    if (!gameDataArrayList.isEmpty() && gameDataArrayList.size() > position) {
                        gameData = gameDataArrayList.get(position);
                    }
                });
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

        allStockViewModel =
                new ViewModelProvider(this).get(FragmentAllVM.class);
        allStockViewModel.activityObservableField.set(this);

        localStockViewModel =
                new ViewModelProvider(this).get(FragmentLocalVM.class);
        localStockViewModel.activityObservableField.set(this);

        remoteStockViewModel =
                new ViewModelProvider(this).get(FragmentRemoteVM.class);
        remoteStockViewModel.activityObservableField.set(this);

        gameStockActivityVM =
                new ViewModelProvider(this).get(GameStockActivityVM.class);
        gameStockActivityVM.getData().observe(GameStockActivity.this , gameData -> {
            if (gameData != null) {
                setRemoteGames(gameData);
                updateProgressDialog(false, "", "");
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
        if (gameData.portedBy.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.ported_by)
                    .replace("-PORTED_BY-", gameData.portedBy));
        }
        if (gameData.version.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.version).replace("-VERSION-", gameData.version));
        }
        if (gameData.getFileSize() > 0) {
            message.append('\n');
            message.append(getString(R.string.fileSize).replace("-SIZE-",
                    Integer.toString(gameData.getFileSize() / 1024)));
        }
        if (gameData.fileExt.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.fileType).replace("-TYPE-", gameData.fileExt));
            if (gameData.fileExt.equals("aqsp")) {
                message.append(" ");
                message.append(getString(R.string.experimental));
            }
        }
        if (gameData.pubDate.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.pub_data).replace("-PUB_DATA-", gameData.pubDate));
        }
        if (gameData.modDate.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.mod_data).replace("-MOD_DATA-", gameData.pubDate));
        }
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this)
                .setMessage(message)
                .setTitle(gameData.title)
                .setIcon(R.drawable.icon)
                .setNegativeButton(getString(R.string.close), (dialog, which) -> dialog.cancel());

        if (gameData.isInstalled()) {
            alertBuilder.setNeutralButton(getString(R.string.play), (dialog, which) -> playGame(gameData));
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

    @Override
    protected void onDestroy() {
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

    private void installGame(DocumentFile gameFile, InstallType type, GameData gameData) {
        if (!isWritableDirectory(gamesDir)) {
            logger.error("Games directory is not writable");
            return;
        }
        GameInstaller installer = installers.get(type);
        if (installer == null) {
            logger.error(String.format("Installer not found by install type '%s'", type));
            return;
        }
        try {
            doInstallGame(installer, gameFile, gameData);
        } catch (InstallException ex) {
            logger.error(ex.getMessage());
        }
    }

    private void doInstallGame(GameInstaller installer, DocumentFile gameFile, GameData gameData) {
        File gameDir = getOrCreateGameDirectory(gameData.title);
        if (!isWritableDirectory(gameDir)) {
            logger.error("GameData directory is not writable");
            return;
        }
        updateProgressDialog(true, gameData.title, getString(R.string.installing));

        boolean installed = installer.install(gameData.title, gameFile, gameDir);
        if (installed) {
            writeGameInfo(gameData , gameDir);
            refreshGames();
        }

        updateProgressDialog(false, "", "");
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    private void updateProgressDialog(boolean show ,
                                      String title ,
                                      String message) {
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
}