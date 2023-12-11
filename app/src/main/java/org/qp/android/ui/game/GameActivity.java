package org.qp.android.ui.game;

import static androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener;
import static org.qp.android.helpers.utils.FileUtil.createFindDFile;
import static org.qp.android.helpers.utils.FileUtil.createFindDFolder;
import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.helpers.utils.ThreadUtil.isMainThread;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.anggrayudi.storage.SimpleStorageHelper;
import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.MimeType;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.Contract;
import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.databinding.ActivityGameBinding;
import org.qp.android.model.libQP.LibQpProxy;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.ui.dialogs.GameDialogFrags;
import org.qp.android.ui.dialogs.GameDialogType;
import org.qp.android.ui.dialogs.GamePatternDialogFrags;
import org.qp.android.ui.settings.SettingsActivity;
import org.qp.android.ui.settings.SettingsController;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public class GameActivity extends AppCompatActivity implements
        GamePatternDialogFrags.GamePatternDialogList {

    private final String TAG = this.getClass().getSimpleName();

    private static final int MAX_SAVE_SLOTS = 5;
    public static final int TAB_MAIN_DESC_AND_ACTIONS = 0;
    public static final int TAB_OBJECTS = 1;
    public static final int TAB_VARS_DESC = 2;
    private static final int LOAD = 0;
    private static final int SAVE = 1;

    private SettingsController settingsController;
    private int activeTab;

    private ActionBar actionBar;
    private Menu mainMenu;
    private NavController navController;
    private BottomNavigationView navigationView;

    private HtmlProcessor htmlProcessor;
    private LibQpProxy libQpProxy;
    private AudioPlayer audioPlayer;

    private int slotAction = 0;
    private GameViewModel gameViewModel;
    private ActivityGameBinding activityGameBinding;
    private final SimpleStorageHelper storageHelper = new SimpleStorageHelper(this);
    private ActivityResultLauncher<Intent> saveResultLaunch;
    private View mDecorView;

    public SettingsController getSettingsController() {
        return settingsController;
    }

    @Nullable
    private FragmentManager getFragManager() {
        var fragmentManager = getSupportFragmentManager();
        if (fragmentManager.isDestroyed()) {
            return null;
        } else {
            return fragmentManager;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityGameBinding = ActivityGameBinding.inflate(getLayoutInflater());
        gameViewModel = new ViewModelProvider(this).get(GameViewModel.class);
        gameViewModel.gameActivityObservableField.set(this);
        settingsController = gameViewModel.getSettingsController();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Prevent jumping of the player on devices with cutout
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        mDecorView = getWindow().getDecorView();
        if (settingsController.isUseImmersiveMode) {
            hideSystemUI();
        } else {
            showSystemUI();
        }

        setContentView(activityGameBinding.getRoot());
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        var navFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.gameFragHost);
        if (navFragment != null) {
            navController = navFragment.getNavController();
        }

        navigationView = activityGameBinding.bottomNavigationView;
        navigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_mainDesc) {
                setActiveTab(TAB_MAIN_DESC_AND_ACTIONS);
            } else if (itemId == R.id.menu_varsDesc) {
                setActiveTab(TAB_VARS_DESC);
            } else if (itemId == R.id.menu_inventory) {
                setActiveTab(TAB_OBJECTS);
            }
            return true;
        });
        setOnApplyWindowInsetsListener(navigationView , null);

        saveResultLaunch = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri uri;
                    Intent data = result.getData();
                    if (result.getResultCode() == RESULT_OK) {
                        switch (slotAction) {
                            case LOAD -> {
                                if (data != null) {
                                    uri = data.getData();
                                    gameViewModel.doWithCounterDisabled(() ->
                                            libQpProxy.loadGameState(uri));
                                }
                            }
                            case SAVE -> {
                                if (data != null) {
                                    uri = data.getData();
                                    libQpProxy.saveGameState(uri);
                                }
                            }
                        }
                    }
                }
        );

        storageHelper.setOnFileSelected((integer , documentFiles) -> {
            for (DocumentFile documentFile : documentFiles) {
                var document = documentWrap(documentFile);
                switch (document.getExtension()) {
                    case "text" , "pl" , "txt" , "el" -> {
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
                            showSimpleDialog("File not found: " + "\n" + e ,
                                    GameDialogType.ERROR_DIALOG);
                        } catch (IOException e) {
                            showSimpleDialog("Can not read file: " + "\n" + e ,
                                    GameDialogType.ERROR_DIALOG);
                        }
                        postTextInDialogFrags(stringBuilder.toString());
                    }
                }
            }
            return null;
        });

        if (savedInstanceState != null) {
            restartServices();
            initControls();
            setActiveTab(savedInstanceState.getInt("savedActiveTab"));
        } else {
            initServices();
            initControls();
            initGame();
        }

        Log.i(TAG, "Game created");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("savedActiveTab" , activeTab);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode ,
                                           @NonNull String[] permissions ,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode , permissions , grantResults);
        storageHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode ,
                                    int resultCode ,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode , resultCode , data);
        storageHelper.getStorage().onActivityResult(requestCode , resultCode , data);
    }

    private void postTextInDialogFrags (String text) {
        var manager = getFragManager();
        if (manager != null) {
            for (var fragment : manager.getFragments()) {
                if (Objects.equals(fragment.getTag() , "executorDialogFragment")
                        || Objects.equals(fragment.getTag() , "inputDialogFragment")) {
                    var bundle = new Bundle();
                    bundle.putString("template" , text);
                    fragment.setArguments(bundle);
                }
            }
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var windowInsetsController =
                    WindowCompat.getInsetsController(getWindow() , activityGameBinding.getRoot());
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            mDecorView.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                if (windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars())
                        || windowInsets.isVisible(WindowInsetsCompat.Type.statusBars())) {
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
                }
                return view.onApplyWindowInsets(windowInsets);
            });
        } else {
            mDecorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

    private void showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var windowInsetsController =
                    WindowCompat.getInsetsController(getWindow() , activityGameBinding.getRoot());
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            mDecorView.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                if (!windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars())
                        || !windowInsets.isVisible(WindowInsetsCompat.Type.statusBars())) {
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
                }
                return view.onApplyWindowInsets(windowInsets);
            });
        } else {
            mDecorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (settingsController.isUseImmersiveMode) {
                mDecorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                mDecorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
        }
    }

    private void initControls() {
        setSupportActionBar(activityGameBinding.toolbar);
        actionBar = getSupportActionBar();
    }

    private void initServices() {
        gameViewModel.startAudio();
        gameViewModel.startLibQsp();
        htmlProcessor = gameViewModel.getHtmlProcessor();
        audioPlayer = gameViewModel.getAudioPlayer();
        libQpProxy = gameViewModel.getLibQspProxy();
    }

    private void restartServices() {
        htmlProcessor = gameViewModel.getHtmlProcessor();
        audioPlayer = gameViewModel.getAudioPlayer();
        libQpProxy = gameViewModel.getLibQspProxy();
    }

    private void initGame() {
        var intent = getIntent();
        var gameId = intent.getStringExtra("gameId");
        var gameTitle = intent.getStringExtra("gameTitle");
        var gameDirUri = Uri.parse(intent.getStringExtra("gameDirUri"));
        var gameDir =  DocumentFileCompat.fromUri(this , gameDirUri);
        var gameFileUri = Uri.parse(intent.getStringExtra("gameFileUri"));
        var gameFile = DocumentFileCompat.fromUri(this , gameFileUri);

        libQpProxy.runGame(gameId, gameTitle, gameDir, gameFile);
    }

    private void setActiveTab(int tab) {
        switch (tab) {
            case TAB_MAIN_DESC_AND_ACTIONS -> {
                navController.navigate(R.id.gameMainFragment);
                var badge = navigationView.getBadge(R.id.menu_mainDesc);
                if (badge != null) navigationView.removeBadge(R.id.menu_mainDesc);
                setTitle(getString(R.string.mainDescTitle));
            }
            case TAB_OBJECTS -> {
                navController.navigate(R.id.gameObjectFragment);
                var badge = navigationView.getBadge(R.id.menu_inventory);
                if (badge != null) navigationView.removeBadge(R.id.menu_inventory);
                setTitle(getString(R.string.inventoryTitle));
            }
            case TAB_VARS_DESC -> {
                navController.navigate(R.id.gameVarsFragment);
                var badge = navigationView.getBadge(R.id.menu_varsDesc);
                if (badge != null) navigationView.removeBadge(R.id.menu_varsDesc);
                setTitle(getString(R.string.varsDescTitle));
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
            var label = navController.getCurrentDestination().getLabel();
            if (label != null) {
                switch (id) {
                    case TAB_MAIN_DESC_AND_ACTIONS -> {
                        if (!label.equals("GameMainFragment"))
                            navigationView.getOrCreateBadge(R.id.menu_mainDesc);
                    }
                    case TAB_OBJECTS -> {
                        if (!label.equals("GameObjectFragment"))
                            navigationView.getOrCreateBadge(R.id.menu_inventory);
                    }
                    case TAB_VARS_DESC -> {
                        if (!label.equals("GameVarsFragment"))
                            navigationView.getOrCreateBadge(R.id.menu_varsDesc);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        libQpProxy.setGameInterface(null);
        gameViewModel.removeCallback();
        super.onDestroy();
        Log.i(TAG,"Game destroyed");
    }

    @Override
    public void onPause() {
        audioPlayer.pause();
        gameViewModel.removeCallback();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        settingsController = gameViewModel.getSettingsController();
        applySettings();
        if (libQpProxy.getGameState().gameRunning) {
            audioPlayer.setSoundEnabled(settingsController.isSoundEnabled);
            audioPlayer.resume();
            gameViewModel.setCallback();
        }
    }

    public void applySettings() {
        if (!isMainThread()) {
            runOnUiThread(this::applySettings);
        } else {
            if (settingsController.language.equals("ru")) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"));
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
            }
            htmlProcessor = gameViewModel.getHtmlProcessor();
        }
    }

    private void promptCloseGame() {
        if (!isMainThread()) {
            runOnUiThread(this::promptCloseGame);
        } else {
            var dialogFragment = new GameDialogFrags();
            dialogFragment.setDialogType(GameDialogType.CLOSE_DIALOG);
            dialogFragment.setCancelable(false);
            var manager = getFragManager();
            if (manager == null) return;
            dialogFragment.show(manager , "closeGameDialogFragment");
        }
    }

    public void showSavePopup() {
        if (!isMainThread()) {
            runOnUiThread(this::showSavePopup);
        } else {
            mainMenu.performIdentifierAction(R.id.menu_saveGame , 0);
        }
    }

    public void showSimpleDialog(@NonNull String inputString ,
                                 @NonNull GameDialogType dialogType) {
        if (!isMainThread()) {
            runOnUiThread(() -> showSimpleDialog(inputString , dialogType));
        } else {
            var manager = getFragManager();
            if (manager == null) return;

            switch (dialogType) {
                case ERROR_DIALOG -> {
                    var dialogFragment = new GameDialogFrags();
                    dialogFragment.setDialogType(GameDialogType.ERROR_DIALOG);
                    dialogFragment.setMessage(inputString);
                    dialogFragment.show(manager , "errorDialogFragment");
                }
                case IMAGE_DIALOG -> {
                    var dialogFragment = new GameDialogFrags();
                    dialogFragment.setDialogType(GameDialogType.IMAGE_DIALOG);
                    dialogFragment.pathToImage.set(inputString);
                    dialogFragment.show(manager , "imageDialogFragment");
                }
                case LOAD_DIALOG -> {
                    var dialogFragment = new GameDialogFrags();
                    dialogFragment.setDialogType(GameDialogType.LOAD_DIALOG);
                    dialogFragment.setCancelable(true);
                    dialogFragment.show(manager , "loadGameDialogFragment");
                }
            }
        }
    }

    public void showMessageDialog (@Nullable String inputString ,
                                   @NonNull CountDownLatch latch) {
        if (!isMainThread()) {
            runOnUiThread(() -> showMessageDialog(inputString, latch));
        } else {
            var manager = getFragManager();
            if (manager == null) return;

            var config = libQpProxy.getGameState().interfaceConfig;
            var processedMsg = config.useHtml ? htmlProcessor.removeHTMLTags(inputString) : inputString;
            if (processedMsg == null) {
                processedMsg = "";
            }
            if (gameViewModel.outputBooleanObserver.hasObservers()) {
                gameViewModel.outputBooleanObserver = new MutableLiveData<>();
            }
            var dialogFragment = new GameDialogFrags();
            dialogFragment.setDialogType(GameDialogType.MESSAGE_DIALOG);
            dialogFragment.setProcessedMsg(processedMsg);
            dialogFragment.setCancelable(false);
            dialogFragment.show(manager , "showMessageDialogFragment");
            gameViewModel.outputBooleanObserver.observeForever(aBoolean -> {
                if (aBoolean) {
                    latch.countDown();
                }
            });
        }
    }

    public void showInputDialog (@Nullable String inputString ,
                                 @NonNull ArrayBlockingQueue<String> inputQueue) {
        if (!isMainThread()) {
            runOnUiThread(() -> showInputDialog(inputString, inputQueue));
        } else {
            var manager = getFragManager();
            if (manager == null) return;

            var config = libQpProxy.getGameState().interfaceConfig;
            var message = config.useHtml ? htmlProcessor.removeHTMLTags(inputString) : inputString;
            if (message == null) {
                message = "";
            }
            if (gameViewModel.outputTextObserver.hasObservers()) {
                gameViewModel.outputTextObserver = new MutableLiveData<>();
            }
            var dialogFragment = new GameDialogFrags();
            dialogFragment.setDialogType(GameDialogType.INPUT_DIALOG);
            if (message.equals("userInputTitle")) {
                dialogFragment.setMessage(getString(R.string.userInputTitle));
            } else {
                dialogFragment.setMessage(message);
            }
            dialogFragment.setCancelable(false);
            dialogFragment.show(manager , "inputDialogFragment");
            gameViewModel.outputTextObserver.observeForever(inputQueue::add);
        }
    }

    public void showExecutorDialog (@Nullable String inputString ,
                                    @NonNull ArrayBlockingQueue<String> inputQueue) {
        if (!isMainThread()) {
            runOnUiThread(() -> showExecutorDialog(inputString, inputQueue));
        } else {
            var manager = getFragManager();
            if (manager == null) return;

            var config = libQpProxy.getGameState().interfaceConfig;
            var message = config.useHtml ? htmlProcessor.removeHTMLTags(inputString) : inputString;
            if (message == null) {
                message = "";
            }
            if (gameViewModel.outputTextObserver.hasObservers()) {
                gameViewModel.outputTextObserver = new MutableLiveData<>();
            }
            var dialogFragment = new GameDialogFrags();
            dialogFragment.setDialogType(GameDialogType.EXECUTOR_DIALOG);
            if (message.equals("execStringTitle")) {
                dialogFragment.setMessage(getString(R.string.execStringTitle));
            } else {
                dialogFragment.setMessage(message);
            }
            dialogFragment.setCancelable(false);
            dialogFragment.show(manager , "executorDialogFragment");
            gameViewModel.outputTextObserver.observeForever(inputQueue::add);
        }
    }

    public void showMenuDialog (@NonNull ArrayList<String> items ,
                                @NonNull ArrayBlockingQueue<Integer> resultQueue) {
        if (!isMainThread()) {
            runOnUiThread(() -> showMenuDialog(items, resultQueue));
        } else {
            var manager = getFragManager();
            if (manager == null) return;

            if (gameViewModel.outputIntObserver.hasObservers()) {
                gameViewModel.outputIntObserver = new MutableLiveData<>();
            }
            var dialogFragment = new GameDialogFrags();
            dialogFragment.setDialogType(GameDialogType.MENU_DIALOG);
            dialogFragment.setItems(items);
            dialogFragment.setCancelable(false);
            dialogFragment.show(manager , "showMenuDialogFragment");
            gameViewModel.outputIntObserver.observeForever(resultQueue::add);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            promptCloseGame();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mainMenu = menu;
        getMenuInflater()
                .inflate(R.menu.menu_game , menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        var gameRunning = libQpProxy.getGameState().gameRunning;
        menu.setGroupVisible(R.id.menuGroup_running , gameRunning);
        if (gameRunning) {
            var loadItem = menu.findItem(R.id.menu_loadGame);
            addSaveSlotsSubMenu(loadItem, LOAD);
            var saveItem = menu.findItem(R.id.menu_saveGame);
            addSaveSlotsSubMenu(saveItem, SAVE);
        }
        return true;
    }

    private void addSaveSlotsSubMenu(MenuItem parent, int action) {
        if (parent == null) {
            return;
        }

        final var proxy = libQpProxy;
        final var application = (QuestPlayerApplication) getApplication();
        final var savesDir = createFindDFolder(application.getCurrentGameDir() , "saves");
        if (savesDir == null) return;

        var id = parent.getItemId();
        mainMenu.removeItem(id);

        var order = action == LOAD ? 2 : 3;
        var subMenu = mainMenu.addSubMenu(
                R.id.menuGroup_running ,
                id ,
                order ,
                parent.getTitle()
        );
        subMenu.setHeaderTitle(getString(R.string.selectSlot));

        MenuItem item;
        for (int i = 0; i <= MAX_SAVE_SLOTS; i++) {
            final var filename = getSaveSlotFilename(i);
            final var loadFile = findFileOrDirectory(savesDir, filename);
            var title = "";

            if (loadFile != null) {
                var lastMod = DateFormat.format(
                        "yyyy-MM-dd HH:mm:ss" ,
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
                        if (loadFile != null) {
                            gameViewModel.doWithCounterDisabled(() ->
                                    proxy.loadGameState(loadFile.getUri()));
                        }
                    }
                    case SAVE -> {
                        var saveFile = createFindDFile(
                                savesDir ,
                                MimeType.TEXT ,
                                filename
                        );
                        if (saveFile != null) {
                            proxy.saveGameState(saveFile.getUri());
                        }
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

    private void startReadOrWriteSave (int slotAction) {
        Intent mIntent;
        switch (slotAction) {
            case LOAD -> {
                mIntent = new Intent(Intent.ACTION_GET_CONTENT);
                mIntent.putExtra(Intent.ACTION_GET_CONTENT , true);
                mIntent.setType("application/octet-stream");
                this.slotAction = slotAction;
                saveResultLaunch.launch(mIntent);
            }
            case SAVE -> {
                mIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                var extraValue = libQpProxy.getGameState().gameFile + ".sav";
                mIntent.putExtra(Intent.EXTRA_TITLE , extraValue);
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
                    libQpProxy.onUseExecutorString();
                } else {
                    libQpProxy.onInputAreaClicked();
                }
                return true;
            }
            case R.id.menu_gameStock -> {
                promptCloseGame();
                return true;
            }
            case R.id.menu_options -> {
                startActivity(new Intent().setClass(this , SettingsActivity.class));
                return true;
            }
            case R.id.menu_newGame -> {
                libQpProxy.restartGame();
                setActiveTab(TAB_MAIN_DESC_AND_ACTIONS);
                return true;
            }
            default -> {
                return itemId == R.id.menu_loadGame || itemId == R.id.menu_saveGame;
            }
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        if (dialog == null) return;
        var dialogTag = dialog.getTag();
        if (dialogTag != null) {
            switch (dialogTag) {
                case "closeGameDialogFragment" -> {
                    gameViewModel.stopAudio();
                    gameViewModel.stopLibQsp();
                    gameViewModel.removeCallback();
                    finish();
                }
                case "inputDialogFragment" , "executorDialogFragment" -> {
                    var inputBox = dialog.requireDialog().getWindow().findViewById(R.id.inputBox_edit);
                    if (inputBox != null) {
                        var editText = (TextInputLayout) inputBox;
                        if (editText.getEditText() != null) {
                            var outputText = editText.getEditText().getText().toString();
                            if (Objects.equals(outputText , "")) {
                                gameViewModel.outputTextObserver.setValue("");
                            } else {
                                gameViewModel.outputTextObserver.setValue(outputText);
                            }
                        }
                    }
                }
                case "loadGameDialogFragment" -> startReadOrWriteSave(LOAD);
                case "showMessageDialogFragment" -> gameViewModel.outputBooleanObserver.setValue(true);
            }
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        if (dialog.getTag() != null) {
            if (dialog.getTag().equals("showMenuDialogFragment")) {
                gameViewModel.outputIntObserver.setValue(-1);
            }
        }
    }

    @Override
    public void onDialogNeutralClick(DialogFragment dialog) {
        if (dialog.getTag() != null) {
            if (dialog.getTag().equals("inputDialogFragment")
                    || dialog.getTag().equals("executorDialogFragment")) {
                storageHelper.openFilePicker("text/plain");
            }
        }
    }

    @Override
    public void onDialogListClick(DialogFragment dialog , int which) {
        if (dialog != null) {
            if (Objects.equals(dialog.getTag() , "showMenuDialogFragment")) {
                gameViewModel.outputIntObserver.setValue(which);
            }
        }
    }
}
