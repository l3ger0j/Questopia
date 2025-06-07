package org.qp.android.ui.game;

import static androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener;
import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFile;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.isWritableDir;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;
import static org.qp.android.helpers.utils.ThreadUtil.assertNonUiThread;
import static org.qp.android.helpers.utils.ThreadUtil.isMainThread;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.anggrayudi.storage.SimpleStorageHelper;
import com.anggrayudi.storage.file.MimeType;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.Contract;
import org.qp.android.R;
import org.qp.android.databinding.ActivityGameBinding;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.Events;
import org.qp.android.questopiabundle.lib.LibGameRequest;
import org.qp.android.ui.dialogs.GameDialogFrags;
import org.qp.android.ui.dialogs.GameDialogType;
import org.qp.android.ui.dialogs.GamePopupType;
import org.qp.android.ui.settings.SettingsActivity;
import org.qp.android.ui.settings.SettingsController;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GameActivity extends AppCompatActivity {

    public static final int TAB_MAIN_DESC_AND_ACTIONS = 0;
    public static final int TAB_OBJECTS = 1;
    public static final int TAB_VARS_DESC = 2;
    public static final int LOAD = 0;
    public static final int SAVE = 1;
    private static final int MAX_SAVE_SLOTS = 5;

    private final SimpleStorageHelper storageHelper = new SimpleStorageHelper(this);
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = (sharedPreferences, key) -> {
        if (key == null) return;
        switch (key) {
            case "lang" -> {
                switch (sharedPreferences.getString("lang", "ru")) {
                    case "ru" -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"));
                    case "en" -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
                }
            }
            case "theme" -> {
                switch (sharedPreferences.getString("theme", "auto")) {
                    case "auto" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    case "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    case "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
            }
        }
    };
    private SettingsController settingsController;
    private int activeTab;
    private ActionBar actionBar;
    private Menu mainMenu;
    private ViewPager2 pager2;
    private BottomNavigationView bottomNavigationView;
    private int slotAction = 0;
    private GameViewModel gameViewModel;
    private ActivityGameBinding activityGameBinding;
    private ActivityResultLauncher<Intent> saveResultLaunch;

    @Override
    @SuppressLint("NonConstantResourceId")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityGameBinding = ActivityGameBinding.inflate(getLayoutInflater());
        gameViewModel = new ViewModelProvider(this).get(GameViewModel.class);

        PreferenceManager
                .getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        settingsController = gameViewModel.getSettingsController();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Prevent jumping of the player on devices with cutout
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), !settingsController.isUseImmersiveMode);

        var windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        getWindow().getDecorView().setOnApplyWindowInsetsListener((v, insets) -> {
            if (settingsController.isUseImmersiveMode) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            } else {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
            }
            return v.onApplyWindowInsets(insets);
        });

        setContentView(activityGameBinding.getRoot());
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        pager2 = activityGameBinding.gamePager;
        pager2.setUserInputEnabled(false);
        pager2.setAdapter(new GameStateAdapter(this));

        bottomNavigationView = activityGameBinding.bottomNavigationView;
        bottomNavigationView.setSelectedItemId(R.id.menu_mainDesc);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_mainDesc -> setActiveTab(TAB_MAIN_DESC_AND_ACTIONS);
                case R.id.menu_varsDesc -> setActiveTab(TAB_VARS_DESC);
                case R.id.menu_inventory -> setActiveTab(TAB_OBJECTS);
            }
            return true;
        });
        setOnApplyWindowInsetsListener(bottomNavigationView, null);

        saveResultLaunch = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK) return;
                    var resultData = result.getData();
                    if (resultData == null) return;
                    var uri = resultData.getData();

                    switch (slotAction) {
                        case LOAD ->
                                gameViewModel.requestForNativeLib(LibGameRequest.LOAD_FILE, uri);
                        case SAVE ->
                                gameViewModel.requestForNativeLib(LibGameRequest.SAVE_FILE, uri);
                    }
                }
        );

        storageHelper.setOnFileSelected((integer, documentFiles) -> {
            for (var documentFile : documentFiles) {
                var document = documentWrap(documentFile);
                switch (document.getExtension()) {
                    case "text", "pl", "txt", "el" -> {
                        var stringBuilder = new StringBuilder();
                        try (var inputStream = document.openInputStream(this)) {
                            if (inputStream != null) {
                                var inputStreamReader = new InputStreamReader(inputStream);
                                var bufferedReader = new BufferedReader(inputStreamReader);
                                var receiveString = "";
                                while ((receiveString = bufferedReader.readLine()) != null) {
                                    stringBuilder.append("\n").append(receiveString);
                                }
                            }
                        } catch (FileNotFoundException e) {
                            doShowErrorDialog(getString(R.string.notFoundFile) + "\n" + e, ErrorType.EXCEPTION);
                        } catch (IOException e) {
                            doShowErrorDialog(getString(R.string.notReadFile) + "\n" + e, ErrorType.EXCEPTION);
                        }
                        postTextInDialogFrags(stringBuilder.toString());
                    }
                }
            }
            return null;
        });

        if (gameViewModel.checkNativePlugin()) {
            if (savedInstanceState != null) {
                initControls();
                setActiveTab(savedInstanceState.getInt("savedActiveTab"));
            } else {
                initServices();
                initControls();
                initGame();
            }
        } else {
            doShowErrorDialog("Plugin not connected!", ErrorType.EXCEPTION);
        }

        gameViewModel.actEmit.observe(this, new Events.EventObserver(eventNavigation -> {
            if (eventNavigation instanceof GameFragmentNavigation.StartRWSave rwSave) {
                startReadOrWriteSave(rwSave.slotAction);
            }
            if (eventNavigation instanceof GameFragmentNavigation.FinishActivity) {
                finish();
            }
            if (eventNavigation instanceof GameFragmentNavigation.WarnUser user) {
                warnUser(user.tabId);
            }
            if (eventNavigation instanceof GameFragmentNavigation.ShowDialog dialog) {
                var buildDialog = dialog.buildDialog;
                if (buildDialog != null) {
                    showDialog(buildDialog);
                }
            }
            if (eventNavigation instanceof GameFragmentNavigation.ShowPopup popup) {
                showPopup(popup.popupType);
            }
        }));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("savedActiveTab", activeTab);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        storageHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        storageHelper.getStorage().onActivityResult(requestCode, resultCode, data);
    }

    private void postTextInDialogFrags(String text) {
        var manager = getSupportFragmentManager();
        if (!manager.isDestroyed()) {
            for (var fragment : manager.getFragments()) {
                if (Objects.equals(fragment.getTag(), "executorDialogFragment")
                        || Objects.equals(fragment.getTag(), "inputDialogFragment")) {
                    var bundle = new Bundle();
                    bundle.putString("template", text);
                    fragment.setArguments(bundle);
                }
            }
        }
    }

    private void initControls() {
        setSupportActionBar(activityGameBinding.gameToolbar);
        actionBar = getSupportActionBar();
        activityGameBinding.gameToolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24);
        activityGameBinding.gameToolbar.setNavigationOnClickListener(v -> doShowCloseDialog());
    }

    private void initServices() {
        gameViewModel.startAudio();
        gameViewModel.initNativePlugin()
                .thenAccept(aBoolean -> {
                    if (!aBoolean) {
                        throw new CompletionException(new Exception("Plugin is not init!"));
                    }
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> doShowErrorDialog(throwable.toString(), ErrorType.EXCEPTION));
                    return null;
                });
    }

    private void initGame() {
        var intent = getIntent();
        var gameId = intent.getLongExtra("gameId", 0L);
        var gameTitle = intent.getStringExtra("gameTitle");
        var gameDirUri = Uri.parse(intent.getStringExtra("gameDirUri"));
        var gameFileUri = Uri.parse(intent.getStringExtra("gameFileUri"));

        gameViewModel.runGameIntoNativeLib(gameId, gameTitle, gameDirUri, gameFileUri);
    }

    private void setActiveTab(int tab) {
        if (activeTab == tab) {
            return;
        }

        switch (tab) {
            case TAB_MAIN_DESC_AND_ACTIONS -> {
                pager2.setCurrentItem(0, false);
                var badge = bottomNavigationView.getBadge(R.id.menu_mainDesc);
                if (badge != null) bottomNavigationView.removeBadge(R.id.menu_mainDesc);
                setTitle(getString(R.string.mainDescFullTitle));
            }
            case TAB_OBJECTS -> {
                pager2.setCurrentItem(1, false);
                var badge = bottomNavigationView.getBadge(R.id.menu_inventory);
                if (badge != null) bottomNavigationView.removeBadge(R.id.menu_inventory);
                setTitle(getString(R.string.inventoryTitle));
            }
            case TAB_VARS_DESC -> {
                pager2.setCurrentItem(2, false);
                var badge = bottomNavigationView.getBadge(R.id.menu_varsDesc);
                if (badge != null) bottomNavigationView.removeBadge(R.id.menu_varsDesc);
                if (!settingsController.language.equals("ru")) {
                    setTitle(getString(R.string.varsDescFullTitle));
                } else {
                    setTitle(getString(R.string.varsDescTitle));
                }
            }
        }

        activeTab = tab;
    }

    private void setTitle(String title) {
        actionBar.setTitle(title);
    }

    public void warnUser(int id) {
        if (!isMainThread()) {
            runOnUiThread(() -> warnUser(id));
        } else {
            var currItem = pager2.getCurrentItem();

            switch (id) {
                case TAB_MAIN_DESC_AND_ACTIONS -> {
                    if (currItem != 0)
                        bottomNavigationView.getOrCreateBadge(R.id.menu_mainDesc);
                }
                case TAB_OBJECTS -> {
                    if (currItem != 1)
                        bottomNavigationView.getOrCreateBadge(R.id.menu_inventory);
                }
                case TAB_VARS_DESC -> {
                    if (currItem != 2)
                        bottomNavigationView.getOrCreateBadge(R.id.menu_varsDesc);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceManager
                .getDefaultSharedPreferences(getApplication())
                .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onPause() {
        gameViewModel.pauseAudio();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        settingsController = gameViewModel.getSettingsController();

        if (gameViewModel.isGameRunning()) {
            gameViewModel.resumeAudio();
        }
    }

    public void showPopup(GamePopupType popupType) {
        if (isFinishing()) return;
        if (isDestroyed()) return;

        runOnUiThread(() -> {
            if (popupType == GamePopupType.SAVE_POPUP) {
                mainMenu.performIdentifierAction(R.id.menu_saveGame, 0);
            }
        });
    }

    private void doShowCloseDialog() {
        var dialogFragment = new GameDialogFrags(GameDialogType.CLOSE_DIALOG);
        dialogFragment.setCancelable(false);
        showDialog(dialogFragment);
    }

    private String getErrorMessage(String inputString, @NonNull ErrorType errorType) {
        return switch (errorType) {
            case IMAGE_ERROR -> ContextCompat.getString(getApplication(), R.string.notFoundImage) + "\n" + inputString;
            case SOUND_ERROR -> ContextCompat.getString(getApplication(), R.string.notFoundSound) + "\n" + inputString;
            case WAITING_ERROR -> ContextCompat.getString(getApplication(), R.string.waitingError) + "\n" + inputString;
            case WAITING_INPUT_ERROR -> ContextCompat.getString(getApplication(), R.string.waitingInputError) + "\n" + inputString;
            case EXCEPTION -> ContextCompat.getString(getApplication(), R.string.error) + "\n" + inputString;
            default -> "";
        };
    }

    private void doShowErrorDialog(String errorStr, ErrorType errorType) {
        var dialogFragment = new GameDialogFrags(GameDialogType.ERROR_DIALOG_WSEND);

        if (errorType == null) {
            dialogFragment.setMessage(errorStr);
        } else {
            dialogFragment.setMessage(getErrorMessage(errorStr, errorType));
        }

        showDialog(dialogFragment);
    }

    public void showDialog(GameDialogFrags buildDialog) {
        if (isFinishing()) return;
        if (isDestroyed()) return;
        assertNonUiThread();

        var manager = getSupportFragmentManager();
        if (manager.isDestroyed()) return;

        switch (buildDialog.getDialogType()) {
            case ERROR_DIALOG_WSEND -> buildDialog.show(manager, "errorDialogFragment");
            case ERROR_DIALOG_WOSEND -> buildDialog.show(manager, "loadErrorGameDialogFragment");
            case CLOSE_DIALOG -> buildDialog.show(manager, "closeDialogFragment");
            case IMAGE_DIALOG -> buildDialog.show(manager, "imageDialogFragment");
            case INPUT_DIALOG -> buildDialog.show(manager, "inputDialogFragment");
            case MENU_DIALOG -> buildDialog.show(manager, "menuDialogFragment");
            case MESSAGE_DIALOG -> buildDialog.show(manager, "messageDialogFragment");
            case EXECUTOR_DIALOG -> buildDialog.show(manager, "executorDialogFragment");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            doShowCloseDialog();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mainMenu = menu;
        getMenuInflater()
                .inflate(R.menu.menu_game, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        var gameRunning = gameViewModel.isGameRunning();
        menu.setGroupVisible(R.id.menuGroup_running, gameRunning);
        if (gameRunning) {
            var loadItem = menu.findItem(R.id.menu_loadGame);
            addSaveSlotsSubMenu(loadItem, LOAD);
            var saveItem = menu.findItem(R.id.menu_saveGame);
            addSaveSlotsSubMenu(saveItem, SAVE);
        }
        return true;
    }

    private void addSaveSlotsSubMenu(MenuItem parent, int action) {
        if (parent == null) return;

        final var savesDir = gameViewModel.getSavesDir();
        if (!isWritableDir(this, savesDir)) return;

        final var id = parent.getItemId();
        mainMenu.removeItem(id);

        final var order = action == LOAD ? 2 : 3;
        final var subMenu = mainMenu.addSubMenu(
                R.id.menuGroup_running,
                id,
                order,
                parent.getTitle()
        );
        subMenu.setHeaderTitle(getString(R.string.selectSlot));

        MenuItem item;
        for (int i = 0; i <= MAX_SAVE_SLOTS; i++) {
            final var filename = getSaveSlotFilename(i);
            final var loadFile = fromRelPath(this, filename, savesDir, false);
            var title = "";

            if (loadFile != null) {
                var lastMod = DateFormat.format(
                        "yyyy-MM-dd HH:mm:ss",
                        loadFile.lastModified()
                ).toString();
                title = getString(R.string.slotPresent, i + 1, lastMod);
            } else {
                title = getString(R.string.slotEmpty, i + 1);
            }

            item = subMenu.add(title);
            item.setOnMenuItemClickListener(menuItem -> {
                switch (action) {
                    case LOAD -> {
                        if (loadFile == null) return true;
                        gameViewModel.requestForNativeLib(LibGameRequest.LOAD_FILE, loadFile.getUri());
                    }
                    case SAVE -> {
                        var saveFile = findOrCreateFile(this, savesDir, filename, MimeType.TEXT);
                        if (saveFile == null) return true;
                        gameViewModel.requestForNativeLib(LibGameRequest.SAVE_FILE, saveFile.getUri());
                    }
                }
                return true;
            });
        }

        switch (action) {
            case LOAD -> {
                item = subMenu.add(getString(R.string.loadFrom));
                item.setOnMenuItemClickListener(item12 -> {
                    startReadOrWriteSave(LOAD);
                    return true;
                });
            }
            case SAVE -> {
                item = subMenu.add(getString(R.string.saveTo));
                item.setOnMenuItemClickListener(item1 -> {
                    startReadOrWriteSave(SAVE);
                    return true;
                });
            }
        }
    }

    public void startReadOrWriteSave(int slotAction) {
        Intent mIntent;
        switch (slotAction) {
            case LOAD -> {
                mIntent = new Intent(Intent.ACTION_GET_CONTENT);
                mIntent.putExtra(Intent.ACTION_GET_CONTENT, true);
                mIntent.setType("application/octet-stream");
                this.slotAction = slotAction;
                saveResultLaunch.launch(mIntent);
            }
            case SAVE -> {
                final var gameDir = gameViewModel.getCurGameDir();
                if (!isWritableDir(getApplication(), gameDir)) return;
                final var gameDirName = gameDir.getName();

                mIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                var extraValue = "";
                if (isNotEmptyOrBlank(gameDirName)) {
                    extraValue = gameDirName + ".sav";
                } else {
                    extraValue = ThreadLocalRandom.current().nextInt() + ".sav";
                }
                mIntent.putExtra(Intent.EXTRA_TITLE, extraValue);
                mIntent.setType("application/octet-stream");
                this.slotAction = slotAction;
                saveResultLaunch.launch(mIntent);
            }
        }
    }

    @NonNull
    @Contract(pure = true)
    private String getSaveSlotFilename(int slot) {
        return (slot + 1) + ".sav";
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.menu_userInput -> {
                if (settingsController.isUseExecString) {
                    gameViewModel.requestForNativeLib(LibGameRequest.USE_EXECUTOR);
                } else {
                    gameViewModel.requestForNativeLib(LibGameRequest.USE_INPUT);
                }
                return true;
            }
            case R.id.menu_options -> {
                startActivity(new Intent().setClass(this, SettingsActivity.class));
                return true;
            }
            case R.id.menu_newGame -> {
                gameViewModel.requestForNativeLib(LibGameRequest.RESTART_GAME);
                setActiveTab(TAB_MAIN_DESC_AND_ACTIONS);
                return true;
            }
            default -> {
                return itemId == R.id.menu_loadGame || itemId == R.id.menu_saveGame;
            }
        }
    }
}
