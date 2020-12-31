package com.qsp.player;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.GestureDetectorCompat;

import com.qsp.player.libqsp.GameInterface;
import com.qsp.player.libqsp.LibQspProxy;
import com.qsp.player.libqsp.model.InterfaceConfiguration;
import com.qsp.player.libqsp.model.QspListItem;
import com.qsp.player.libqsp.model.QspMenuItem;
import com.qsp.player.libqsp.model.RefreshInterfaceRequest;
import com.qsp.player.libqsp.model.WindowType;
import com.qsp.player.service.AudioPlayer;
import com.qsp.player.service.GameContentResolver;
import com.qsp.player.service.HtmlProcessor;
import com.qsp.player.service.ImageProvider;
import com.qsp.player.stock.GameStockActivity;
import com.qsp.player.util.ViewUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

import static com.qsp.player.util.Base64Util.decodeBase64;
import static com.qsp.player.util.ColorUtil.convertRgbaToBgra;
import static com.qsp.player.util.ColorUtil.getHexColor;
import static com.qsp.player.util.FileUtil.findFileOrDirectory;
import static com.qsp.player.util.FileUtil.getExtension;
import static com.qsp.player.util.FileUtil.getOrCreateDirectory;
import static com.qsp.player.util.FileUtil.getOrCreateFile;
import static com.qsp.player.util.ThreadUtil.isMainThread;
import static com.qsp.player.util.ViewUtil.setLocale;

@SuppressLint("ClickableViewAccessibility")
public class GameActivity extends AppCompatActivity implements GameInterface, GestureDetector.OnGestureListener {
    private static final int MAX_SAVE_SLOTS = 5;

    private static final String SHOW_ADVANCED_EXTRA_NAME = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ?
            "android.content.extra.SHOW_ADVANCED" :
            "android.provider.extra.SHOW_ADVANCED";

    private static final int REQUEST_CODE_SELECT_GAME = 1;
    private static final int REQUEST_CODE_LOAD_FROM_FILE = 2;
    private static final int REQUEST_CODE_SAVE_TO_FILE = 3;

    private static final int TAB_MAIN_DESC_AND_ACTIONS = 0;
    private static final int TAB_OBJECTS = 1;
    private static final int TAB_VARS_DESC = 2;

    private static final String PAGE_HEAD_TEMPLATE = "<head>\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1\">\n"
            + "<style type=\"text/css\">\n"
            + "  body {\n"
            + "    margin: 0;\n"
            + "    padding: 0.5em;\n"
            + "    color: QSPTEXTCOLOR;\n"
            + "    background-color: QSPBACKCOLOR;\n"
            + "    font-size: QSPFONTSIZE;\n"
            + "    font-family: QSPFONTSTYLE;\n"
            + "  }\n"
            + "  a { color: QSPLINKCOLOR; }\n"
            + "  a:link { color: QSPLINKCOLOR; }\n"
            + "</style></head>";

    private static final String PAGE_BODY_TEMPLATE = "<body>REPLACETEXT</body>";

    private static final Logger logger = LoggerFactory.getLogger(GameActivity.class);

    private final ServiceConnection backgroundServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private Settings settings;
    private String currentLanguage = Locale.getDefault().getLanguage();
    private int activeTab;
    private String pageTemplate = "";
    private boolean showActions = true;
    private boolean selectingGame;

    // region Контролы

    private ActionBar actionBar;
    private Menu mainMenu;
    private ConstraintLayout layoutTop;
    private WebView mainDescView;
    private WebView varsDescView;
    private View separatorView;
    private ListView actionsView;
    private ListView objectsView;

    // endregion Контролы

    // region Сервисы

    private GameContentResolver gameContentResolver;
    private ImageProvider imageProvider;
    private HtmlProcessor htmlProcessor;
    private LibQspProxy libQspProxy;
    private AudioPlayer audioPlayer;
    private GestureDetectorCompat gestureDetector;

    // endregion Сервисы

    // region Локация-счётчик

    private final Handler counterHandler = new Handler();

    private int counterInterval = 500;

    private final Runnable counterTask = new Runnable() {
        @Override
        public void run() {
            libQspProxy.executeCounter();
            counterHandler.postDelayed(this, counterInterval);
        }
    };

    // endregion Локация-счётчик

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        bindBackgroundService();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        initServices();
        initControls();
        loadSettings();
        loadLocale();
        setActiveTab(TAB_MAIN_DESC_AND_ACTIONS);

        logger.info("GameActivity created");
    }

    private void bindBackgroundService() {
        Intent intent = new Intent(this, BackgroundService.class);
        bindService(intent, backgroundServiceConn, BIND_AUTO_CREATE);
    }

    private void initControls() {
        layoutTop = findViewById(R.id.layout_top);
        separatorView = findViewById(R.id.separator);

        initActionBar();
        initMainDescView();
        initActionsView();
        initObjectsView();
        initVarsDescView();
    }

    private void initActionBar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        actionBar = getSupportActionBar();
    }

    private void initMainDescView() {
        mainDescView = findViewById(R.id.main_desc);
        mainDescView.setWebViewClient(new QspWebViewClient());
        mainDescView.setOnTouchListener(this::handleTouchEvent);
    }

    private boolean handleTouchEvent(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private void initActionsView() {
        actionsView = findViewById(R.id.actions);
        actionsView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        actionsView.setOnTouchListener(this::handleTouchEvent);
        actionsView.setOnItemClickListener((parent, view, position, id) -> libQspProxy.onActionClicked(position));
        actionsView.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                libQspProxy.onActionSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initObjectsView() {
        objectsView = findViewById(R.id.objects);
        objectsView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        objectsView.setOnItemClickListener((parent, view, position, id) -> libQspProxy.onObjectSelected(position));
        objectsView.setOnTouchListener(this::handleTouchEvent);
    }

    private void initVarsDescView() {
        varsDescView = findViewById(R.id.vars_desc);
        varsDescView.setWebViewClient(new QspWebViewClient());
        varsDescView.setOnTouchListener(this::handleTouchEvent);
    }

    private void initServices() {
        gestureDetector = new GestureDetectorCompat(this, this);

        QuestPlayerApplication application = (QuestPlayerApplication) getApplication();

        gameContentResolver = application.getGameContentResolver();
        imageProvider = application.getImageProvider();
        htmlProcessor = application.getHtmlProcessor();

        audioPlayer = application.getAudioPlayer();
        audioPlayer.start();

        libQspProxy = application.getLibQspProxy();
        libQspProxy.setGameInterface(this);
    }

    private void loadLocale() {
        setLocale(this, settings.getLanguage());
        currentLanguage = settings.getLanguage();
    }

    private void setActiveTab(int tab) {
        switch (tab) {
            case TAB_MAIN_DESC_AND_ACTIONS:
                toggleMainDescAndActions(true);
                toggleObjects(false);
                toggleVarsDesc(false);
                setTitle(getString(R.string.mainDesc));
                break;

            case TAB_OBJECTS:
                toggleMainDescAndActions(false);
                toggleObjects(true);
                toggleVarsDesc(false);
                setTitle(getString(R.string.inventory));
                break;

            case TAB_VARS_DESC:
                toggleMainDescAndActions(false);
                toggleObjects(false);
                toggleVarsDesc(true);
                setTitle(getString(R.string.varsDesc));
                break;
        }

        activeTab = tab;
        updateTabIcons();
    }

    private void toggleMainDescAndActions(boolean show) {
        findViewById(R.id.main_desc).setVisibility(show ? View.VISIBLE : View.GONE);

        boolean shouldShowActions = show && showActions;
        findViewById(R.id.separator).setVisibility(shouldShowActions ? View.VISIBLE : View.GONE);
        findViewById(R.id.actions).setVisibility(shouldShowActions ? View.VISIBLE : View.GONE);
    }

    private void toggleObjects(boolean show) {
        findViewById(R.id.objects).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void toggleVarsDesc(boolean show) {
        findViewById(R.id.vars_desc).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setTitle(String title) {
        actionBar.setTitle(title);
    }

    private void updateTabIcons() {
        if (mainMenu == null) return;

        mainMenu.findItem(R.id.menu_inventory).setIcon(activeTab == TAB_OBJECTS ? R.drawable.ic_tab_objects : R.drawable.ic_tab_objects_alt);
        mainMenu.findItem(R.id.menu_maindesc).setIcon(activeTab == TAB_MAIN_DESC_AND_ACTIONS ? R.drawable.ic_tab_main : R.drawable.ic_tab_main_alt);
        mainMenu.findItem(R.id.menu_varsdesc).setIcon(activeTab == TAB_VARS_DESC ? R.drawable.ic_tab_vars : R.drawable.ic_tab_vars_alt);
    }

    @Override
    protected void onDestroy() {
        audioPlayer.stop();
        libQspProxy.setGameInterface(null);
        counterHandler.removeCallbacks(counterTask);
        unbindService(backgroundServiceConn);
        super.onDestroy();
        logger.info("GameActivity destroyed");
    }

    @Override
    public void onPause() {
        audioPlayer.pause();
        counterHandler.removeCallbacks(counterTask);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        loadSettings();
        updateLocale();
        applySettings();

        if (libQspProxy.getGameState().isGameRunning()) {
            applyGameState();
            audioPlayer.setSoundEnabled(settings.isSoundEnabled());
            audioPlayer.resume();
            counterHandler.postDelayed(counterTask, counterInterval);

        } else if (!selectingGame) {
            startSelectGame();
        }
    }

    private void loadSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        settings = Settings.from(preferences);
    }

    private void updateLocale() {
        if (currentLanguage.equals(settings.getLanguage())) return;

        setLocale(this, settings.getLanguage());
        setTitle(R.string.appName);
        invalidateOptionsMenu();
        setActiveTab(activeTab);

        currentLanguage = settings.getLanguage();
    }

    private void applySettings() {
        applyActionsHeightRatio();

        int backColor = getBackgroundColor();
        layoutTop.setBackgroundColor(backColor);
        mainDescView.setBackgroundColor(backColor);
        varsDescView.setBackgroundColor(backColor);
        actionsView.setBackgroundColor(backColor);
        objectsView.setBackgroundColor(backColor);

        updatePageTemplate();
    }

    private int getBackgroundColor() {
        InterfaceConfiguration config = libQspProxy.getGameState().getInterfaceConfig();
        return config.getBackColor() != 0 ? convertRgbaToBgra(config.getBackColor()) : settings.getBackColor();
    }

    private void applyActionsHeightRatio() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(layoutTop);
        constraintSet.setVerticalWeight(R.id.main_desc, 1.0f - settings.getActionsHeightRatio());
        constraintSet.setVerticalWeight(R.id.actions, settings.getActionsHeightRatio());
        constraintSet.applyTo(layoutTop);
    }

    private void updatePageTemplate() {
        String pageHeadTemplate = PAGE_HEAD_TEMPLATE
                .replace("QSPTEXTCOLOR", getHexColor(getTextColor()))
                .replace("QSPBACKCOLOR", getHexColor(getBackgroundColor()))
                .replace("QSPLINKCOLOR", getHexColor(getLinkColor()))
                .replace("QSPFONTSTYLE", ViewUtil.getFontStyle(settings.getTypeface()))
                .replace("QSPFONTSIZE", Integer.toString(getFontSize()));

        pageTemplate = pageHeadTemplate + PAGE_BODY_TEMPLATE;
    }

    private int getTextColor() {
        InterfaceConfiguration config = libQspProxy.getGameState().getInterfaceConfig();
        return config.getFontColor() != 0 ? convertRgbaToBgra(config.getFontColor()) : settings.getTextColor();
    }

    private int getLinkColor() {
        InterfaceConfiguration config = libQspProxy.getGameState().getInterfaceConfig();
        return config.getLinkColor() != 0 ? convertRgbaToBgra(config.getLinkColor()) : settings.getLinkColor();
    }

    private int getFontSize() {
        InterfaceConfiguration config = libQspProxy.getGameState().getInterfaceConfig();
        return settings.isUseGameFont() && config.getFontSize() != 0 ? config.getFontSize() : settings.getFontSize();
    }

    private void applyGameState() {
        imageProvider.invalidateCache();

        applySettings();
        refreshMainDesc();
        refreshVarsDesc();
        refreshActions();
        refreshObjects();
    }

    private void refreshMainDesc() {
        String mainDesc = getHtml(libQspProxy.getGameState().getMainDesc());

        mainDescView.loadDataWithBaseURL(
                "file:///",
                pageTemplate.replace("REPLACETEXT", mainDesc),
                "text/html",
                "UTF-8",
                "");
    }

    private String getHtml(String str) {
        InterfaceConfiguration config = libQspProxy.getGameState().getInterfaceConfig();

        return config.isUseHtml() ?
                htmlProcessor.convertQspHtmlToWebViewHtml(str) :
                htmlProcessor.convertQspStringToWebViewHtml(str);
    }

    private void refreshVarsDesc() {
        String varsDesc = getHtml(libQspProxy.getGameState().getVarsDesc());

        varsDescView.loadDataWithBaseURL(
                "file:///",
                pageTemplate.replace("REPLACETEXT", varsDesc),
                "text/html",
                "UTF-8",
                "");
    }

    private void refreshActions() {
        ArrayList<QspListItem> actions = libQspProxy.getGameState().getActions();
        actionsView.setAdapter(new QspItemAdapter(this, R.layout.list_item_action, actions));
    }

    private void refreshObjects() {
        ArrayList<QspListItem> objects = libQspProxy.getGameState().getObjects();
        objectsView.setAdapter(new QspItemAdapter(this, R.layout.list_item_object, objects));
    }

    private void startSelectGame() {
        selectingGame = true;
        Intent intent = new Intent(this, GameStockActivity.class);
        if (libQspProxy.getGameState().isGameRunning()) {
            intent.putExtra("gameRunning", libQspProxy.getGameState().getGameId());
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_GAME);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            startSelectGame();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mainMenu = menu;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_game, menu);

        updateTabIcons();

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean gameRunning = libQspProxy.getGameState().isGameRunning();
        menu.setGroupVisible(R.id.menugroup_running, gameRunning);

        if (gameRunning) {
            MenuItem loadItem = menu.findItem(R.id.menu_loadgame);
            addSaveSlotsSubMenu(loadItem, SlotAction.LOAD);

            MenuItem saveItem = menu.findItem(R.id.menu_savegame);
            addSaveSlotsSubMenu(saveItem, SlotAction.SAVE);
        }

        return true;
    }

    private void addSaveSlotsSubMenu(MenuItem parent, final SlotAction action) {
        int id = parent.getItemId();
        mainMenu.removeItem(id);

        int order = action == SlotAction.LOAD ? 2 : 3;
        SubMenu subMenu = mainMenu.addSubMenu(R.id.menugroup_running, id, order, parent.getTitle());
        subMenu.setHeaderTitle(getString(R.string.selectSlot));

        MenuItem item;
        final File savesDir = getOrCreateDirectory(libQspProxy.getGameState().getGameDir(), "saves");
        final LibQspProxy proxy = libQspProxy;

        for (int i = 0; i < MAX_SAVE_SLOTS; ++i) {
            final String filename = getSaveSlotFilename(i);
            final File file = findFileOrDirectory(savesDir, filename);
            String title;

            if (file != null) {
                String lastMod = DateFormat.format("yyyy-MM-dd HH:mm:ss", file.lastModified()).toString();
                title = getString(R.string.slotPresent, i + 1, lastMod);
            } else {
                title = getString(R.string.slotEmpty, i + 1);
            }

            item = subMenu.add(title);
            item.setOnMenuItemClickListener(item13 -> {
                switch (action) {
                    case LOAD:
                        if (file != null) {
                            doWithCounterDisabled(() -> proxy.loadGameState(Uri.fromFile(file)));
                        }
                        break;
                    case SAVE:
                        File file1 = getOrCreateFile(savesDir, filename);
                        proxy.saveGameState(Uri.fromFile(file1));
                        break;
                }

                return true;
            });
        }

        switch (action) {
            case LOAD:
                item = subMenu.add(getString(R.string.loadFrom));
                item.setOnMenuItemClickListener(item12 -> {
                    startLoadFromFile();
                    return true;
                });
                break;
            case SAVE:
                item = subMenu.add(getString(R.string.saveTo));
                item.setOnMenuItemClickListener(item1 -> {
                    startSaveToFile();
                    return true;
                });
                break;
        }
    }

    private void startLoadFromFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.putExtra(SHOW_ADVANCED_EXTRA_NAME, true);
        intent.setType("application/octet-stream");
        startActivityForResult(intent, REQUEST_CODE_LOAD_FROM_FILE);
    }

    private void startSaveToFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, libQspProxy.getGameState().getGameFile() + ".sav");
        startActivityForResult(intent, REQUEST_CODE_SAVE_TO_FILE);
    }

    private String getSaveSlotFilename(int slot) {
        return (slot + 1) + ".sav";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_maindesc:
                setActiveTab(TAB_MAIN_DESC_AND_ACTIONS);
                return true;

            case R.id.menu_inventory:
                setActiveTab(TAB_OBJECTS);
                return true;

            case R.id.menu_varsdesc:
                setActiveTab(TAB_VARS_DESC);
                return true;

            case R.id.menu_userinput:
                libQspProxy.onInputAreaClicked();
                return true;

            case R.id.menu_gamestock:
                startSelectGame();
                return true;

            case R.id.menu_options:
                Intent intent = new Intent();
                intent.setClass(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.menu_about:
                showAboutDialog();
                return true;

            case R.id.menu_newgame:
                libQspProxy.restartGame();
                setActiveTab(TAB_MAIN_DESC_AND_ACTIONS);
                return true;

            case R.id.menu_loadgame:
            case R.id.menu_savegame:
                return true;

            default:
                return false;
        }
    }

    private void showAboutDialog() {
        View messageView = getLayoutInflater().inflate(R.layout.dialog_about, null, false);

        new AlertDialog.Builder(this)
                .setIcon(R.drawable.icon)
                .setTitle(R.string.appName)
                .setView(messageView)
                .create()
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SELECT_GAME:
                handleSelectGame(resultCode, data);
                break;
            case REQUEST_CODE_LOAD_FROM_FILE:
                handleLoadFromFile(resultCode, data);
                break;
            case REQUEST_CODE_SAVE_TO_FILE:
                handleSaveToFile(resultCode, data);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleSelectGame(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        String gameId = data.getStringExtra("gameId");
        String gameTitle = data.getStringExtra("gameTitle");

        String gameDirUri = data.getStringExtra("gameDirUri");
        File gameDir = new File(gameDirUri);

        String gameFileUri = data.getStringExtra("gameFileUri");
        File gameFile = new File(gameFileUri);

        libQspProxy.runGame(gameId, gameTitle, gameDir, gameFile);
    }

    private void handleLoadFromFile(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        doWithCounterDisabled(() -> libQspProxy.loadGameState(uri));
    }

    private void handleSaveToFile(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        libQspProxy.saveGameState(uri);
    }

    private void selectNextTab() {
        int tab;
        switch (activeTab) {
            case TAB_VARS_DESC:
                tab = TAB_MAIN_DESC_AND_ACTIONS;
                break;
            case TAB_OBJECTS:
                tab = TAB_VARS_DESC;
                break;
            case TAB_MAIN_DESC_AND_ACTIONS:
            default:
                tab = TAB_OBJECTS;
                break;
        }
        setActiveTab(tab);
    }

    private void selectPreviousTab() {
        int tab;
        switch (activeTab) {
            case TAB_VARS_DESC:
                tab = TAB_OBJECTS;
                break;
            case TAB_OBJECTS:
                tab = TAB_MAIN_DESC_AND_ACTIONS;
                break;
            case TAB_MAIN_DESC_AND_ACTIONS:
            default:
                tab = TAB_VARS_DESC;
                break;
        }
        setActiveTab(tab);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) return true;

        return super.onTouchEvent(event);
    }

    // region GameInterface

    @Override
    public void refresh(final RefreshInterfaceRequest request) {
        runOnUiThread(() -> {
            if (request.isInterfaceConfigChanged()) {
                applySettings();
            }
            if (request.isInterfaceConfigChanged() || request.isMainDescChanged()) {
                refreshMainDesc();
            }
            if (request.isActionsChanged()) {
                refreshActions();
            }
            if (request.isObjectsChanged()) {
                refreshObjects();
            }
            if (request.isInterfaceConfigChanged() || request.isVarsDescChanged()) {
                refreshVarsDesc();
            }
        });
    }

    @Override
    public void showError(final String message) {
        runOnUiThread(() -> ViewUtil.showErrorDialog(this, message));
    }

    @Override
    public void showPicture(final String path) {
        runOnUiThread(() -> {
            Intent intent = new Intent(this, ImageBoxActivity.class);
            intent.putExtra("gameDirUri", libQspProxy.getGameState().getGameDir().getAbsolutePath());
            intent.putExtra("imagePath", path);
            startActivity(intent);
        });
    }

    @Override
    public void showMessage(final String message) {
        if (isMainThread()) {
            throw new RuntimeException("Must not be called on the main thread");
        }
        final CountDownLatch latch = new CountDownLatch(1);

        runOnUiThread(() -> {
            InterfaceConfiguration config = libQspProxy.getGameState().getInterfaceConfig();
            String processedMsg = config.isUseHtml() ? htmlProcessor.removeHtmlTags(message) : message;
            if (processedMsg == null) {
                processedMsg = "";
            }
            new AlertDialog.Builder(this)
                    .setMessage(processedMsg)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> latch.countDown())
                    .setCancelable(false)
                    .create()
                    .show();
        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            logger.error("Wait failed", ex);
        }
    }

    @Override
    public String showInputBox(final String prompt) {
        if (isMainThread()) {
            throw new RuntimeException("Must not be called on the main thread");
        }
        final ArrayBlockingQueue<String> inputQueue = new ArrayBlockingQueue<>(1);

        runOnUiThread(() -> {
            final View view = getLayoutInflater().inflate(R.layout.dialog_input, null);

            InterfaceConfiguration config = libQspProxy.getGameState().getInterfaceConfig();
            String message = config.isUseHtml() ? htmlProcessor.removeHtmlTags(prompt) : prompt;
            if (message == null) {
                message = "";
            }
            new AlertDialog.Builder(this)
                    .setView(view)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        EditText editView = view.findViewById(R.id.inputbox_edit);
                        inputQueue.add(editView.getText().toString());
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        });

        try {
            return inputQueue.take();
        } catch (InterruptedException ex) {
            logger.error("Wait for input failed", ex);
            return null;
        }
    }

    @Override
    public int showMenu() {
        if (isMainThread()) {
            throw new RuntimeException("Must not be called on the main thread");
        }
        final ArrayBlockingQueue<Integer> resultQueue = new ArrayBlockingQueue<>(1);
        final ArrayList<String> items = new ArrayList<>();

        for (QspMenuItem item : libQspProxy.getGameState().getMenuItems()) {
            items.add(item.name);
        }
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setItems(items.toArray(new CharSequence[0]), (dialog, which) -> resultQueue.add(which))
                .setOnCancelListener(dialog -> resultQueue.add(-1))
                .create()
                .show());

        try {
            return resultQueue.take();
        } catch (InterruptedException ex) {
            logger.error("Wait failed", ex);
            return -1;
        }
    }

    @Override
    public void showSaveGamePopup(String filename) {
        mainMenu.performIdentifierAction(R.id.menu_savegame, 0);
    }

    @Override
    public void showWindow(WindowType type, final boolean show) {
        if (type == WindowType.ACTIONS) {
            showActions = show;
            if (activeTab == TAB_MAIN_DESC_AND_ACTIONS) {
                runOnUiThread(() -> {
                    separatorView.setVisibility(show ? View.VISIBLE : View.GONE);
                    actionsView.setVisibility(show ? View.VISIBLE : View.GONE);
                });
            }
        } else {
            logger.debug("Unsupported window type: " + type);
        }
    }

    @Override
    public void setCounterInterval(int millis) {
        counterInterval = millis;
    }

    @Override
    public void doWithCounterDisabled(Runnable runnable) {
        counterHandler.removeCallbacks(counterTask);
        runnable.run();
        counterHandler.postDelayed(counterTask, counterInterval);
    }

    // endregion GameInterface

    // region GestureDetector.OnGestureListener

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();
            float absDiffX = Math.abs(diffX);
            float absDiffY = Math.abs(diffY);

            int screenW = layoutTop.getMeasuredWidth();

            if (absDiffX > 0.33f * screenW && absDiffX > 2.0f * absDiffY) {
                if (diffX > 0.0f) {
                    selectNextTab();
                } else {
                    selectPreviousTab();
                }
                return true;
            }
        } catch (Exception ex) {
            logger.error("Error handling fling event", ex);
        }

        return false;
    }

    // endregion GestureDetector.OnGestureListener

    private enum SlotAction {
        LOAD,
        SAVE
    }

    private class QspWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, final String href) {
            if (href.toLowerCase().startsWith("exec:")) {
                String code = decodeBase64(href.substring(5));
                libQspProxy.execute(code);
            }
            return true;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (url.startsWith("file:///")) {
                String relPath = Uri.decode(url.substring(8));
                File file = gameContentResolver.getFile(relPath);
                if (file == null) {
                    logger.error("File not found: " + relPath);
                    return null;
                }
                try {
                    String extension = getExtension(file.getName());
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    InputStream in = GameActivity.this.getContentResolver().openInputStream(Uri.fromFile(file));
                    return new WebResourceResponse(mimeType, null, in);
                } catch (FileNotFoundException ex) {
                    logger.error("File not found", ex);
                    return null;
                }
            }

            return super.shouldInterceptRequest(view, url);
        }
    }

    private class QspItemAdapter extends ArrayAdapter<QspListItem> {
        private final int resource;
        private final List<QspListItem> items;

        QspItemAdapter(Context context, int resource, List<QspListItem> items) {
            super(context, resource, items);
            this.resource = resource;
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(resource, null);
            }
            QspListItem item = items.get(position);
            if (item != null) {
                ImageView iconView = convertView.findViewById(R.id.item_icon);
                TextView textView = convertView.findViewById(R.id.item_text);
                if (iconView != null) {
                    iconView.setImageDrawable(item.icon);
                }
                if (textView != null) {
                    textView.setTypeface(getTypeface());
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getFontSize());
                    //textView.setBackgroundColor(getBackgroundColor());
                    textView.setTextColor(getTextColor());
                    textView.setLinkTextColor(getLinkColor());
                    textView.setText(item.text);
                }
            }

            return convertView;
        }

        private Typeface getTypeface() {
            switch (settings.getTypeface()) {
                case 1:
                    return Typeface.SANS_SERIF;
                case 2:
                    return Typeface.SERIF;
                case 3:
                    return Typeface.MONOSPACE;
                default:
                    return Typeface.DEFAULT;
            }
        }
    }
}
