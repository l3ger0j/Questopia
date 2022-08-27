package com.qsp.player.view.activities;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.ACTION_OPEN_DOCUMENT_TREE;
import static com.qsp.player.utils.FileUtil.deleteDirectory;
import static com.qsp.player.utils.FileUtil.getOrCreateDirectory;
import static com.qsp.player.utils.FileUtil.isWritableDirectory;
import static com.qsp.player.utils.LanguageUtil.setLocale;
import static com.qsp.player.utils.PathUtil.removeExtension;
import static com.qsp.player.viewModel.viewModels.GameStockActivityVM.normalizeGameFolderName;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.SearchView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.qsp.player.view.fragments.FragmentLocal;
import com.qsp.player.viewModel.repository.LocalGameRepository;
import com.qsp.player.viewModel.viewModels.FragmentLocalVM;
import com.qsp.player.viewModel.viewModels.GameStockActivityVM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class GameStockActivity extends AppCompatActivity {
    private static final Logger logger = LoggerFactory.getLogger(GameStockActivity.class);

    private final HashMap<String, GameData> gamesMap = new HashMap<>();
    private final LocalGameRepository localGameRepository = new LocalGameRepository();
    private final HashMap<InstallType, GameInstaller> installers = new HashMap<>();

    private SettingsAdapter settingsAdapter;
    private boolean showProgressDialog;
    private String currentLanguage = Locale.getDefault().getLanguage();
    private ProgressDialog progressDialog;
    private File gamesDir;
    private GameData gameData;
    private InstallType lastInstallType = InstallType.ZIP_ARCHIVE;

    private GameStockActivityVM gameStockActivityVM;
    private FragmentLocalVM localStockViewModel;

    private ActionMode actionMode;
    private ActivityStockBinding activityStockBinding;
    private ActivityResultLauncher<Intent> resultLauncher;

    private boolean isFABOpen;
    private boolean isStartActionMode = false;

    private FloatingActionButton mFAB;
    private ExtendedFloatingActionButton fab1;
    private ExtendedFloatingActionButton fab2;
    private ExtendedFloatingActionButton fab3;
    private RecyclerView mRecyclerView;
    private final ArrayList<GameData> tempList = getSortedGames();
    private final ArrayList<GameData> selectList = new ArrayList<>();

    public String getGameIdByPosition(int position) {
        localStockViewModel.getGameData().observe(this, gameDataArrayList -> {
            if (!gameDataArrayList.isEmpty() && gameDataArrayList.size() > position) {
                gameData = gameDataArrayList.get(position);
            }
        });
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

    public void setRecyclerView(RecyclerView mRecyclerView) {
        this.mRecyclerView = mRecyclerView;
    }

    private void setRecyclerAdapter () {
        ArrayList<GameData> gameData = getSortedGames();
        ArrayList<GameData> localGameData = new ArrayList<>();

        for (GameData data : gameData) {
            if (data.isInstalled()) {
                localGameData.add(data);
            }
        }

        localStockViewModel.setGameDataArrayList(localGameData);
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

        mFAB = activityStockBinding.floatingActionButton;
        fab1 = activityStockBinding.floatingActionButton1;
        fab2 = activityStockBinding.floatingActionButton2;
        fab3 = activityStockBinding.floatingActionButton3;

        mFAB.setOnClickListener(onClickListener);
        fab1.setOnClickListener(onClickListener);
        fab2.setOnClickListener(onClickListener);
        fab3.setOnClickListener(onClickListener);

        localStockViewModel =
                new ViewModelProvider(this).get(FragmentLocalVM.class);
        localStockViewModel.activityObservableField.set(this);
        if (mRecyclerView != null) {
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView , int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE)
                    {
                        mFAB.show();
                    }

                    super.onScrollStateChanged(recyclerView, newState);
                }

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView , int dx , int dy) {
                    if (dy > 0 ||dy<0 && mFAB.isShown())
                    {
                        mFAB.hide();
                    }
                }
            });
        }

        gameStockActivityVM = new ViewModelProvider(this).get(GameStockActivityVM.class);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, FragmentLocal.class, null)
                    .commit();
        }

        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri uri;
                    DocumentFile file;
                    if (result.getResultCode() == RESULT_OK) {
                        if ((uri = Objects.requireNonNull(result.getData()).getData()) == null) {
                            logger.error("GameData archive or directory is not selected");
                        }
                        if (lastInstallType == InstallType.FOLDER) {
                            file = DocumentFile.fromTreeUri(this, Objects.requireNonNull(uri));
                        } else {
                            file = DocumentFile.fromSingleUri(this, Objects.requireNonNull(uri));
                        }
                        assert file != null;
                        String gameName = removeExtension(Objects.requireNonNull(file.getName()));
                        GameData gameData = new GameData();
                        gameData.id = gameName;
                        gameData.title = gameName;
                        installGame(file, lastInstallType, gameData);
                    }
                }
        );

        loadSettings();
        loadLocale();

        logger.info("GameStockActivity created");

        setContentView(activityStockBinding.getRoot());
    }

    private void showFABMenu(){
        isFABOpen = true;
        fab1.show();
        fab2.show();
        fab3.show();
    }

    private void closeFABMenu(){
        isFABOpen = false;
        fab1.hide();
        fab2.hide();
        fab3.hide();
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        String action;
        String extension = null;

        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == R.id.floatingActionButton) {
                if (!isFABOpen) {
                    showFABMenu();
                } else {
                    closeFABMenu();
                }
            } else if (id == R.id.floatingActionButton1) {
                action = ACTION_OPEN_DOCUMENT;
                extension = "zip";
                Intent intent = new Intent(action);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (extension != null) {
                    intent.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
                }
                lastInstallType = InstallType.ZIP_ARCHIVE;
                resultLauncher.launch(intent);
            } else if (id == R.id.floatingActionButton2) {
                action = ACTION_OPEN_DOCUMENT;
                extension = "rar";
                Intent intent2 = new Intent(action);
                intent2.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (extension != null) {
                    intent2.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
                }
                lastInstallType = InstallType.RAR_ARCHIVE;
                resultLauncher.launch(intent2);
            } else if (id == R.id.floatingActionButton3) {
                action = ACTION_OPEN_DOCUMENT_TREE;
                Intent intent3 = new Intent(action);
                intent3.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (extension != null) {
                    intent3.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
                }
                lastInstallType = InstallType.FOLDER;
                resultLauncher.launch(intent3);
            }
        }
    };

    private void loadSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        settingsAdapter = SettingsAdapter.from(preferences);
    }

    private void loadLocale() {
        setLocale(this, settingsAdapter.language);
        setTitle(R.string.gameStock);
        currentLanguage = settingsAdapter.language;
    }

    public void onItemClick(int position) {
        if (isStartActionMode) {
            for (GameData gameData : gamesMap.values()) {
                if (!gameData.isInstalled()) continue;
                tempList.add(gameData);
            }

            RecyclerView.ViewHolder mViewHolder =
                    mRecyclerView.findViewHolderForAdapterPosition(position);
            GameData gameData = tempList.get(mViewHolder.getAdapterPosition());

            if (selectList.isEmpty() || !selectList.contains(gameData)) {
                selectList.add(gameData);
                mViewHolder.itemView.setBackgroundColor(Color.LTGRAY);
            } else {
                selectList.remove(gameData);
                mViewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
            }
        } else {
            String gameId = getGameIdByPosition(position);
            showGameInfo(gameId);
        }
    }

    public void onLongItemClick(int position) {
        String gameId = getGameIdByPosition(position);
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
        refreshGames();
        invalidateOptionsMenu();

        currentLanguage = settingsAdapter.language;
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
            gameStockActivityVM.writeGameInfo(gameData , gameDir, logger);
            refreshGames();
        }

        updateProgressDialog(false, "", "");
    }

    @NonNull
    private File getOrCreateGameDirectory(String gameName) {
        String folderName = normalizeGameFolderName(gameName);
        return getOrCreateDirectory(gamesDir, folderName);
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
        } else if (itemId == R.id.menu_deletegame) {
            mFAB.hide();
            actionMode = startSupportActionMode(callback);
            return true;
        }
        return false;
    }

    private final ActionMode.Callback callback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode , Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_delete, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode , Menu menu) {
            isStartActionMode = true;
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode , MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.app_bar_search) {
                SearchView searchView = (SearchView) item.getActionView();
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String s) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String s) {
                        filter(s);
                        return false;
                    }
                });
            } else if (itemId == R.id.delete_game) {
                for(GameData data : selectList)
                {
                    tempList.remove(data);
                    deleteDirectory(data.gameDir);
                    refreshGames();
                }
                actionMode.finish();
            } else if (itemId == R.id.select_all) {
                if(selectList.size() == tempList.size()) {
                    selectList.clear();
                    for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
                        final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
                        holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                    }
                } else {
                    selectList.clear();
                    selectList.addAll(tempList);
                    for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
                        final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
                        holder.itemView.setBackgroundColor(Color.LTGRAY);
                    }
                }
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            isStartActionMode = false;
            selectList.clear();
            mFAB.show();
        }
    };

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

    private void filter(String text){
        ArrayList<GameData> gameData = getSortedGames();
        ArrayList<GameData> filteredList = new ArrayList<>();

        for (GameData item : gameData) {
            if (item.title.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        if (filteredList.isEmpty()) {
            logger.error("No Data Found");
        } else {
            localStockViewModel.setGameDataArrayList(filteredList);
        }
    }
}