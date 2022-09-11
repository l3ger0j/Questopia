package com.qsp.player.view.activities;

import static com.qsp.player.utils.ColorUtil.convertRgbaToBgra;
import static com.qsp.player.utils.ColorUtil.getHexColor;
import static com.qsp.player.utils.FileUtil.findFileOrDirectory;
import static com.qsp.player.utils.FileUtil.getOrCreateDirectory;
import static com.qsp.player.utils.FileUtil.getOrCreateFile;
import static com.qsp.player.utils.LanguageUtil.setLocale;
import static com.qsp.player.utils.ThreadUtil.isMainThread;
import static com.qsp.player.utils.ViewUtil.getFontStyle;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.GestureDetectorCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.qsp.player.QuestPlayerApplication;
import com.qsp.player.R;
import com.qsp.player.databinding.ActivityGameBinding;
import com.qsp.player.model.libQSP.InterfaceConfiguration;
import com.qsp.player.model.libQSP.LibQspProxy;
import com.qsp.player.model.libQSP.QspListItem;
import com.qsp.player.model.libQSP.QspMenuItem;
import com.qsp.player.model.libQSP.RefreshInterfaceRequest;
import com.qsp.player.model.libQSP.WindowType;
import com.qsp.player.model.service.AudioPlayer;
import com.qsp.player.model.service.GameContentResolver;
import com.qsp.player.model.service.HtmlProcessor;
import com.qsp.player.utils.ViewUtil;
import com.qsp.player.view.adapters.SettingsAdapter;
import com.qsp.player.viewModel.viewModels.GameActivityVM;

import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

@SuppressLint("ClickableViewAccessibility")
public class GameActivity extends AppCompatActivity implements GameInterface, GestureDetector.OnGestureListener {

    private static final int MAX_SAVE_SLOTS = 5;
    private static final int TAB_MAIN_DESC_AND_ACTIONS = 0;
    private static final int TAB_OBJECTS = 1;
    private static final int TAB_VARS_DESC = 2;
    private static final int LOAD = 0;
    private static final int SAVE = 1;

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

    private SettingsAdapter settingsAdapter;
    private String currentLanguage = Locale.getDefault().getLanguage();
    private int activeTab;
    private String pageTemplate = "";
    private boolean showActions = true;
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

    private int slotAction = 0;
    private GameActivityVM gameActivityVM;
    private ActivityGameBinding activityGameBinding;
    private ActivityResultLauncher<Intent> resultLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityGameBinding = DataBindingUtil.setContentView(this, R.layout.activity_game);

        gameActivityVM = new ViewModelProvider(this).get(GameActivityVM.class);
        activityGameBinding.setGameViewModel(gameActivityVM);
        settingsAdapter = gameActivityVM.loadSettings(this);
        gameActivityVM.setLogger(logger);

        setContentView(activityGameBinding.getRoot());
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri uri;
                    Intent data = result.getData();
                    if (result.getResultCode() == RESULT_OK) {
                        switch (slotAction) {
                            case LOAD:
                                if (data != null) {
                                    uri = data.getData();
                                    doWithCounterDisabled(() -> libQspProxy.loadGameState(uri));
                                } else {
                                    break;
                                }
                                break;
                            case SAVE:
                                if (data != null) {
                                    uri = data.getData();
                                    libQspProxy.saveGameState(uri);
                                } else {
                                    break;
                                }
                                break;
                        }
                    }
                }
        );

        initServices();
        initControls();
        settingsAdapter = gameActivityVM.loadSettings(this);
        currentLanguage = gameActivityVM.loadLocale(this, settingsAdapter);
        initGame();
        setActiveTab(TAB_MAIN_DESC_AND_ACTIONS);

        logger.info("GameActivity created");
    }

    private void initControls() {
        layoutTop = activityGameBinding.layoutTop;
        separatorView = activityGameBinding.separator;

        initActionBar();
        initMainDescView();
        initActionsView();
        initObjectsView();
        initVarsDescView();
    }

    private void initActionBar() {
        setSupportActionBar(activityGameBinding.toolbar);
        actionBar = getSupportActionBar();
    }

    private void initMainDescView() {
        mainDescView = activityGameBinding.mainDesc;
        mainDescView.setOnTouchListener(this::handleTouchEvent);
    }

    private boolean handleTouchEvent(View v, MotionEvent event) {
        if (settingsAdapter.useGestures) {
            return gestureDetector.onTouchEvent(event);
        } else {
            return false;
        }
    }

    private void initActionsView() {
        actionsView = activityGameBinding.actions;
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
        objectsView = activityGameBinding.objects;
        objectsView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        objectsView.setOnItemClickListener((parent, view, position, id) -> libQspProxy.onObjectSelected(position));
        objectsView.setOnTouchListener(this::handleTouchEvent);
    }

    private void initVarsDescView() {
        varsDescView = activityGameBinding.varsDesc;
        varsDescView.setOnTouchListener(this::handleTouchEvent);
    }

    private void initServices() {
        gestureDetector = new GestureDetectorCompat(this, this);

        QuestPlayerApplication application = (QuestPlayerApplication) getApplication();

        GameContentResolver gameContentResolver = application.getGameContentResolver();
        gameActivityVM.setGameContentResolver(gameContentResolver);
        htmlProcessor = application.getHtmlProcessor();

        audioPlayer = application.getAudioPlayer();
        audioPlayer.start();

        libQspProxy = application.getLibQspProxy();
        libQspProxy.setGameInterface(this);
        libQspProxy.start();
        gameActivityVM.setLibQspProxy(libQspProxy);
    }

    private void initGame() {
        Intent intent = getIntent();

        String gameId = intent.getStringExtra("gameId");
        String gameTitle = intent.getStringExtra("gameTitle");

        String gameDirUri = intent.getStringExtra("gameDirUri");
        File gameDir = new File(gameDirUri);

        String gameFileUri = intent.getStringExtra("gameFileUri");
        File gameFile = new File(gameFileUri);

        libQspProxy.runGame(gameId, gameTitle, gameDir, gameFile);
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
        boolean shouldShowActions = show && showActions;
        activityGameBinding.mainDesc.setVisibility(show ? View.VISIBLE : View.GONE);
        activityGameBinding.separator.setVisibility(shouldShowActions ? View.VISIBLE : View.GONE);
        activityGameBinding.actions.setVisibility(shouldShowActions ? View.VISIBLE : View.GONE);
    }

    private void toggleObjects(boolean show) {
        activityGameBinding.objects.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void toggleVarsDesc(boolean show) {
        activityGameBinding.varsDesc.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setTitle(String title) {
        actionBar.setTitle(title);
    }

    private void updateTabIcons() {
        if (mainMenu == null) return;
        mainMenu.findItem(R.id.menu_inventory).setIcon(activeTab == TAB_OBJECTS ? R.drawable.tab_object : R.drawable.tab_object_alt);
        mainMenu.findItem(R.id.menu_maindesc).setIcon(activeTab == TAB_MAIN_DESC_AND_ACTIONS ? R.drawable.tab_main : R.drawable.tab_main_alt);
        mainMenu.findItem(R.id.menu_varsdesc).setIcon(activeTab == TAB_VARS_DESC ? R.drawable.tab_vars : R.drawable.tab_vars_alt);
    }

    @Override
    protected void onDestroy() {
        audioPlayer.stop();
        libQspProxy.stop();
        libQspProxy.setGameInterface(null);
        counterHandler.removeCallbacks(counterTask);
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

        settingsAdapter = gameActivityVM.loadSettings(this);
        updateLocale();
        applySettings();

        if (libQspProxy.getGameState().gameRunning) {
            applyGameState();

            audioPlayer.setSoundEnabled(settingsAdapter.soundEnabled);
            audioPlayer.resume();

            counterHandler.postDelayed(counterTask, counterInterval);
        }
    }

    private void updateLocale() {
        if (currentLanguage.equals(settingsAdapter.language)) return;

        setLocale(this, settingsAdapter.language);
        setTitle(R.string.appName);
        invalidateOptionsMenu();
        setActiveTab(activeTab);

        currentLanguage = settingsAdapter.language;
    }

    private void applySettings() {
        applyActionsHeightRatio();

        if (settingsAdapter.isSeparator) {
            separatorView.setBackgroundColor(getBackgroundColor());
        } else {
            separatorView.setBackgroundColor(getResources().getColor(R.color.materialcolorpicker__grey));
        }

        int backColor = getBackgroundColor();
        layoutTop.setBackgroundColor(backColor);
        mainDescView.setBackgroundColor(backColor);
        varsDescView.setBackgroundColor(backColor);
        actionsView.setBackgroundColor(backColor);
        objectsView.setBackgroundColor(backColor);

        updatePageTemplate();
    }

    private int getBackgroundColor() {
        InterfaceConfiguration config = libQspProxy.getGameState().interfaceConfig;
        return config.backColor != 0 ? convertRgbaToBgra(config.backColor) : settingsAdapter.backColor;
    }

    private void applyActionsHeightRatio() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(layoutTop);
        constraintSet.setVerticalWeight(R.id.main_desc, 1.0f - settingsAdapter.actionsHeightRatio);
        constraintSet.setVerticalWeight(R.id.actions, settingsAdapter.actionsHeightRatio);
        constraintSet.applyTo(layoutTop);
    }

    private void updatePageTemplate() {
        String pageHeadTemplate = PAGE_HEAD_TEMPLATE
                .replace("QSPTEXTCOLOR", getHexColor(getTextColor()))
                .replace("QSPBACKCOLOR", getHexColor(getBackgroundColor()))
                .replace("QSPLINKCOLOR", getHexColor(getLinkColor()))
                .replace("QSPFONTSTYLE", getFontStyle(settingsAdapter.typeface))
                .replace("QSPFONTSIZE", Integer.toString(getFontSize()));

        pageTemplate = pageHeadTemplate + PAGE_BODY_TEMPLATE;
    }

    private int getTextColor() {
        InterfaceConfiguration config = libQspProxy.getGameState().interfaceConfig;
        return config.fontColor != 0 ? convertRgbaToBgra(config.fontColor) : settingsAdapter.textColor;
    }

    private int getLinkColor() {
        InterfaceConfiguration config = libQspProxy.getGameState().interfaceConfig;
        return config.linkColor != 0 ? convertRgbaToBgra(config.linkColor) : settingsAdapter.linkColor;
    }

    private int getFontSize() {
        InterfaceConfiguration config = libQspProxy.getGameState().interfaceConfig;
        return settingsAdapter.useGameFont && config.fontSize != 0 ? config.fontSize : settingsAdapter.fontSize;
    }

    private void applyGameState() {
        refreshMainDesc();
        refreshVarsDesc();
        refreshActions();
        refreshObjects();
    }

    private void refreshMainDesc() {
        String mainDesc = getHtml(libQspProxy.getGameState().mainDesc);

        mainDescView.loadDataWithBaseURL(
                "file:///",
                pageTemplate.replace("REPLACETEXT", mainDesc),
                "text/html",
                "UTF-8",
                "");
    }

    private String getHtml(String str) {
        InterfaceConfiguration config = libQspProxy.getGameState().interfaceConfig;

        return config.useHtml ?
                htmlProcessor.convertQspHtmlToWebViewHtml(str) :
                htmlProcessor.convertQspStringToWebViewHtml(str);
    }

    private void refreshVarsDesc() {
        String varsDesc = getHtml(libQspProxy.getGameState().varsDesc);

        varsDescView.loadDataWithBaseURL(
                "file:///",
                pageTemplate.replace("REPLACETEXT", varsDesc),
                "text/html",
                "UTF-8",
                "");
    }

    private void refreshActions() {
        ArrayList<QspListItem> actions = libQspProxy.getGameState().actions;
        actionsView.setAdapter(new QspItemAdapter(this, R.layout.list_item_action, actions));
        refreshActionsVisibility();
    }

    private void refreshObjects() {
        ArrayList<QspListItem> objects = libQspProxy.getGameState().objects;
        objectsView.setAdapter(new QspItemAdapter(this, R.layout.list_item_object, objects));
    }

    private void promptCloseGame() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.promptCloseGame)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> finish())
                .setNegativeButton(android.R.string.no, (dialog, which) -> { })
                .setCancelable(false)
                .create()
                .show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            promptCloseGame();
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
        boolean gameRunning = libQspProxy.getGameState().gameRunning;
        menu.setGroupVisible(R.id.menugroup_running, gameRunning);

        if (gameRunning) {
            MenuItem loadItem = menu.findItem(R.id.menu_loadgame);
            addSaveSlotsSubMenu(loadItem, LOAD);

            MenuItem saveItem = menu.findItem(R.id.menu_savegame);
            addSaveSlotsSubMenu(saveItem, SAVE);
        }

        return true;
    }

    private void addSaveSlotsSubMenu(MenuItem parent, int action) {
        int id = parent.getItemId();
        mainMenu.removeItem(id);

        int order = action == LOAD ? 2 : 3;
        SubMenu subMenu = mainMenu.addSubMenu(R.id.menugroup_running, id, order, parent.getTitle());
        subMenu.setHeaderTitle(getString(R.string.selectSlot));

        MenuItem item;
        final File savesDir = getOrCreateDirectory(libQspProxy.getGameState().gameDir, "saves");
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
                    startReadOrWriteSave(LOAD);
                    return true;
                });
                break;
            case SAVE:
                item = subMenu.add(getString(R.string.saveTo));
                item.setOnMenuItemClickListener(item1 -> {
                    startReadOrWriteSave(SAVE);
                    return true;
                });
                break;
        }
    }

    private void startReadOrWriteSave (int slotAction) {
        Intent mIntent;
        switch (slotAction) {
            case LOAD:
                mIntent = new Intent(Intent.ACTION_GET_CONTENT);
                mIntent.putExtra(Intent.ACTION_GET_CONTENT, true);
                mIntent.setType("application/octet-stream");
                this.slotAction = slotAction;
                resultLauncher.launch(mIntent);
                break;
            case SAVE:
                mIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                mIntent.putExtra(Intent.EXTRA_TITLE, libQspProxy.getGameState().gameFile + ".sav");
                mIntent.setType("application/octet-stream");
                this.slotAction = slotAction;
                resultLauncher.launch(mIntent);
                break;
        }
    }

    @NonNull
    @Contract(pure = true)
    private String getSaveSlotFilename(int slot) {
        return (slot + 1) + ".sav";
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_maindesc) {
            setActiveTab(TAB_MAIN_DESC_AND_ACTIONS);
            return true;
        } else if (i == R.id.menu_inventory) {
            setActiveTab(TAB_OBJECTS);
            return true;
        } else if (i == R.id.menu_varsdesc) {
            setActiveTab(TAB_VARS_DESC);
            return true;
        } else if (i == R.id.menu_userinput) {
            libQspProxy.onInputAreaClicked();
            return true;
        } else if (i == R.id.menu_gamestock) {
            promptCloseGame();
            return true;
        } else if (i == R.id.menu_options) {
            Intent intent = new Intent();
            intent.setClass(this , SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (i == R.id.menu_newgame) {
            libQspProxy.restartGame();
            setActiveTab(TAB_MAIN_DESC_AND_ACTIONS);
            return true;
        } else return i == R.id.menu_loadgame || i == R.id.menu_savegame;
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
            if (request.interfaceConfigChanged) {
                applySettings();
            }
            if (request.interfaceConfigChanged || request.mainDescChanged) {
                refreshMainDesc();
            }
            if (request.actionsChanged) {
                refreshActions();
            }
            if (request.objectsChanged) {
                refreshObjects();
            }
            if (request.interfaceConfigChanged || request.varsDescChanged) {
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
            intent.putExtra("gameDirUri", libQspProxy.getGameState().gameDir.getAbsolutePath());
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
            InterfaceConfiguration config = libQspProxy.getGameState().interfaceConfig;
            String processedMsg = config.useHtml ? htmlProcessor.removeHtmlTags(message) : message;
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

            InterfaceConfiguration config = libQspProxy.getGameState().interfaceConfig;
            String message = config.useHtml ? htmlProcessor.removeHtmlTags(prompt) : prompt;
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

        for (QspMenuItem item : libQspProxy.getGameState().menuItems) {
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
        runOnUiThread(() -> mainMenu.performIdentifierAction(R.id.menu_savegame, 0));
    }

    @Override
    public void showWindow(WindowType type, final boolean show) {
        if (type == WindowType.ACTIONS) {
            showActions = show;
            if (activeTab == TAB_MAIN_DESC_AND_ACTIONS) {
                runOnUiThread(this::refreshActionsVisibility);
            }
        } else {
            logger.debug("Unsupported window type: " + type);
        }
    }

    private void refreshActionsVisibility() {
        int count = actionsView.getAdapter().getCount();
        boolean show = showActions && count > 0;
        separatorView.setVisibility(show ? View.VISIBLE : View.GONE);
        actionsView.setVisibility(show ? View.VISIBLE : View.GONE);
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
            switch (settingsAdapter.typeface) {
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
