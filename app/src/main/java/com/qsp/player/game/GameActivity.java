package com.qsp.player.game;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import androidx.core.view.GestureDetectorCompat;

import com.qsp.player.QuestPlayerApplication;
import com.qsp.player.R;
import com.qsp.player.game.libqsp.LibQspProxy;
import com.qsp.player.settings.SettingsActivity;
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

import static com.qsp.player.util.FileUtil.findFileOrDirectory;
import static com.qsp.player.util.FileUtil.findFileRecursively;
import static com.qsp.player.util.FileUtil.getExtension;
import static com.qsp.player.util.FileUtil.getOrCreateDirectory;
import static com.qsp.player.util.FileUtil.getOrCreateFile;
import static com.qsp.player.util.FileUtil.normalizePath;

@SuppressLint("ClickableViewAccessibility")
public class GameActivity extends AppCompatActivity implements PlayerView, GestureDetector.OnGestureListener {
    private static final int MAX_SAVE_SLOTS = 5;
    private static final String SHOW_ADVANCED_EXTRA_NAME = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ?
            "android.content.extra.SHOW_ADVANCED" :
            "android.provider.extra.SHOW_ADVANCED";

    private static final int REQUEST_CODE_SELECT_GAME = 1;
    private static final int REQUEST_CODE_LOAD_FROM_FILE = 2;
    private static final int REQUEST_CODE_SAVE_TO_FILE = 3;

    private static final int TAB_OBJECTS = 0;
    private static final int TAB_MAIN_DESC = 1;
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

    private static final int SWIPE_THRESHOLD = 100;

    private static final Logger logger = LoggerFactory.getLogger(GameActivity.class);

    private final Context context = this;

    private ImageProvider imageProvider;
    private HtmlProcessor htmlProcessor;
    private LibQspProxy libQspProxy;
    private SharedPreferences settings;
    private String currentLanguage = Locale.getDefault().getLanguage();
    private int activeTab;
    private String pageTemplate = "";
    private boolean selectingGame;
    private PlayerViewState viewState;
    private GestureDetectorCompat gestureDetector;
    private boolean showActions = true;

    private ActionBar actionBar;
    private View layoutTop;
    private WebView mainDescView;
    private WebView varsDescView;
    private View separatorView;
    private ListView actionsView;
    private ListView objectsView;
    private Menu mainMenu;

    private String typeface = "";
    private String fontSize = "";
    private boolean useGameFont;
    private int backColor;
    private int textColor;
    private int linkColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        initDependenices();

        settings = PreferenceManager.getDefaultSharedPreferences(context);

        loadLocale();
        initActionBar();
        initLayoutTopView();
        initMainDescView();
        initVarsDescView();
        initSeparatorView();
        initActionsView();
        initObjectsView();
        setActiveTab(TAB_MAIN_DESC);

        gestureDetector = new GestureDetectorCompat(this, this);
    }

    private void initDependenices() {
        QuestPlayerApplication application = (QuestPlayerApplication) getApplication();

        imageProvider = application.getImageProvider();
        htmlProcessor = application.getHtmlProcessor();

        libQspProxy = application.getLibQspProxy();
        libQspProxy.setPlayerView(this);
    }

    private void loadLocale() {
        String language = settings.getString("lang", "ru");
        ViewUtil.setLocale(context, language);
        currentLanguage = language;
    }

    private void initActionBar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        actionBar = getSupportActionBar();
    }

    private void initLayoutTopView() {
        layoutTop = findViewById(R.id.layout_top);
    }

    private boolean handleTouchEvent(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private void initMainDescView() {
        mainDescView = findViewById(R.id.main_description);
        mainDescView.setWebViewClient(new QspWebViewClient());
        mainDescView.setOnTouchListener(this::handleTouchEvent);
    }

    private void initVarsDescView() {
        varsDescView = findViewById(R.id.vars_desc);
        varsDescView.setWebViewClient(new QspWebViewClient());
        varsDescView.setOnTouchListener(this::handleTouchEvent);
    }

    private void initSeparatorView() {
        separatorView = findViewById(R.id.separator);
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

    private void setActiveTab(int tab) {
        switch (tab) {
            case TAB_OBJECTS:
                toggleObjects(true);
                toggleMainDescAndActions(false);
                toggleVarsDesc(false);
                setTitle(getString(R.string.inventory));
                break;

            case TAB_MAIN_DESC:
                toggleObjects(false);
                toggleMainDescAndActions(true);
                toggleVarsDesc(false);
                setTitle(getString(R.string.mainDesc));
                break;

            case TAB_VARS_DESC:
                toggleObjects(false);
                toggleMainDescAndActions(false);
                toggleVarsDesc(true);
                setTitle(getString(R.string.varsDesc));
                break;
        }

        activeTab = tab;
        updateTabIcons();
    }

    private void toggleObjects(boolean show) {
        findViewById(R.id.objects).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void toggleMainDescAndActions(boolean show) {
        findViewById(R.id.main_description).setVisibility(show ? View.VISIBLE : View.GONE);
        if (showActions) {
            findViewById(R.id.separator).setVisibility(show ? View.VISIBLE : View.GONE);
            findViewById(R.id.actions).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void toggleVarsDesc(boolean show) {
        findViewById(R.id.vars_desc).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setTitle(String title) {
        actionBar.setTitle(title);
    }

    @Override
    protected void onDestroy() {
        libQspProxy.pauseGame();
        libQspProxy.setPlayerView(null);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateLocale();
        loadTextSettings();
        applyTextSettings();

        viewState = libQspProxy.getViewState();
        if (viewState.isGameRunning()) {
            applyViewState();
            libQspProxy.resumeGame();
        } else if (!selectingGame) {
            startSelectGame();
        }
    }

    private void updateLocale() {
        String language = settings.getString("lang", "ru");
        if (currentLanguage.equals(language)) {
            return;
        }
        ViewUtil.setLocale(context, language);
        setTitle(R.string.appName);
        invalidateOptionsMenu();
        setActiveTab(activeTab);
        currentLanguage = language;
    }

    private void loadTextSettings() {
        typeface = settings.getString("typeface", "0");
        fontSize = settings.getString("fontSize", "16");
        useGameFont = settings.getBoolean("useGameFont", false);
        backColor = settings.getInt("backColor", Color.parseColor("#e0e0e0"));
        textColor = settings.getInt("textColor", Color.parseColor("#000000"));
        linkColor = settings.getInt("linkColor", Color.parseColor("#0000ff"));
    }

    private void applyTextSettings() {
        int backColor = getBackgroundColor();

        layoutTop.setBackgroundColor(backColor);
        mainDescView.setBackgroundColor(backColor);
        varsDescView.setBackgroundColor(backColor);
        actionsView.setBackgroundColor(backColor);
        objectsView.setBackgroundColor(backColor);

        updatePageTemplate();
    }

    private int getBackgroundColor() {
        return viewState != null && viewState.getBackColor() != 0 ?
                reverseColor(viewState.getBackColor()) :
                backColor;
    }

    private int reverseColor(int color) {
        return 0xff000000 |
                ((color & 0x0000ff) << 16) |
                (color & 0x00ff00) |
                ((color & 0xff0000) >> 16);
    }

    private void updatePageTemplate() {
        String pageHeadTemplate = PAGE_HEAD_TEMPLATE
                .replace("QSPTEXTCOLOR", getHexColor(getTextColor()))
                .replace("QSPBACKCOLOR", getHexColor(getBackgroundColor()))
                .replace("QSPLINKCOLOR", getHexColor(getLinkColor()))
                .replace("QSPFONTSTYLE", ViewUtil.getFontStyle(typeface))
                .replace("QSPFONTSIZE", getFontSize());

        pageTemplate = pageHeadTemplate + PAGE_BODY_TEMPLATE;
    }

    private int getTextColor() {
        return viewState != null && viewState.getFontColor() != 0 ?
                reverseColor(viewState.getFontColor()) :
                textColor;
    }

    private int getLinkColor() {
        return viewState != null && viewState.getLinkColor() != 0 ?
                reverseColor(viewState.getLinkColor()) :
                linkColor;
    }

    private String getFontSize() {
        return useGameFont && viewState != null && viewState.getFontSize() != 0 ?
                Integer.toString(viewState.getFontSize()) :
                fontSize;
    }

    private String getHexColor(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    private void applyViewState() {
        imageProvider.invalidateCache();

        applyTextSettings();
        refreshMainDesc();
        refreshVarsDesc();
        refreshActions();
        refreshObjects();
    }

    private void refreshMainDesc() {
        String text = getHtml(viewState.getMainDesc());
        mainDescView.loadDataWithBaseURL(
                "file:///",
                pageTemplate.replace("REPLACETEXT", text),
                "text/html",
                "UTF-8",
                "");
    }

    private String getHtml(String str) {
        return viewState.isUseHtml() ?
                htmlProcessor.preprocessQspHtml(str) :
                htmlProcessor.convertQspStringToHtml(str);
    }

    private void refreshVarsDesc() {
        String text = getHtml(viewState.getVarsDesc());
        varsDescView.loadDataWithBaseURL(
                "file:///",
                pageTemplate.replace("REPLACETEXT", text),
                "text/html",
                "UTF-8",
                "");
    }

    private void refreshActions() {
        actionsView.setAdapter(new QspItemAdapter(context, R.layout.list_item_action, viewState.getActions()));
    }

    private void refreshObjects() {
        objectsView.setAdapter(new QspItemAdapter(context, R.layout.list_item_object, viewState.getObjects()));
    }

    private void startSelectGame() {
        selectingGame = true;
        Intent intent = new Intent(this, GameStockActivity.class);
        if (viewState.isGameRunning()) {
            intent.putExtra("gameRunning", viewState.getGameId());
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_GAME);
    }

    @Override
    public void onPause() {
        libQspProxy.pauseGame();
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            showExitDialog();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void showExitDialog() {
        final CharSequence[] items = new String[2];
        items[0] = getString(R.string.gameStock);
        items[1] = getString(R.string.closeApp);

        new AlertDialog.Builder(context)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        startSelectGame();
                    } else if (which == 1) {
                        System.exit(0);
                    }
                })
                .create()
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mainMenu = menu;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_game, menu);

        updateTabIcons();

        return true;
    }

    private void updateTabIcons() {
        if (mainMenu == null) return;

        mainMenu.findItem(R.id.menu_inventory).setIcon(activeTab == TAB_OBJECTS ? R.drawable.ic_tab_objects : R.drawable.ic_tab_objects_alt);
        mainMenu.findItem(R.id.menu_maindesc).setIcon(activeTab == TAB_MAIN_DESC ? R.drawable.ic_tab_main : R.drawable.ic_tab_main_alt);
        mainMenu.findItem(R.id.menu_varsdesc).setIcon(activeTab == TAB_VARS_DESC ? R.drawable.ic_tab_vars : R.drawable.ic_tab_vars_alt);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean gameRunning = viewState.isGameRunning();
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
        final File savesDir = getOrCreateDirectory(viewState.getGameDir(), "saves");
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
                            proxy.loadGameState(Uri.fromFile(file));
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
        intent.putExtra(Intent.EXTRA_TITLE, viewState.getGameFile() + ".sav");
        startActivityForResult(intent, REQUEST_CODE_SAVE_TO_FILE);
    }

    private String getSaveSlotFilename(int slot) {
        return (slot + 1) + ".sav";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_maindesc:
                setActiveTab(TAB_MAIN_DESC);
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

            case R.id.menu_exit:
                System.exit(0);
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
                setActiveTab(TAB_MAIN_DESC);
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

        new AlertDialog.Builder(context)
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
        libQspProxy.loadGameState(uri);
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
                tab = TAB_MAIN_DESC;
                break;
            case TAB_OBJECTS:
                tab = TAB_VARS_DESC;
                break;
            case TAB_MAIN_DESC:
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
                tab = TAB_MAIN_DESC;
                break;
            case TAB_MAIN_DESC:
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

    // region PlayerView

    @Override
    public void refreshGameView(
            final boolean confChanged,
            final boolean mainDescChanged,
            final boolean actionsChanged,
            final boolean objectsChanged,
            final boolean varsDescChanged) {

        runOnUiThread(() -> {
            viewState = libQspProxy.getViewState();

            if (confChanged) {
                applyTextSettings();
            }
            if (confChanged || mainDescChanged) {
                refreshMainDesc();
            }
            if (actionsChanged) {
                refreshActions();
            }
            if (objectsChanged) {
                refreshObjects();
            }
            if (confChanged || varsDescChanged) {
                refreshVarsDesc();
            }
        });
    }

    @Override
    public void showError(final String message) {
        runOnUiThread(() -> ViewUtil.showErrorDialog(context, message));
    }

    @Override
    public void showPicture(final String path) {
        runOnUiThread(() -> {
            Intent intent = new Intent(context, ImageBoxActivity.class);
            intent.putExtra("gameDirUri", viewState.getGameDir().getAbsolutePath());
            intent.putExtra("imagePath", normalizePath(path));
            startActivity(intent);
        });
    }

    @Override
    public void showMessage(final String message) {
        final CountDownLatch latch = new CountDownLatch(1);

        runOnUiThread(() -> {
            String processedMsg = viewState.isUseHtml() ? htmlProcessor.removeHtmlTags(message) : message;
            if (processedMsg == null) {
                processedMsg = "";
            }

            new AlertDialog.Builder(context)
                    .setMessage(processedMsg)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> latch.countDown())
                    .setCancelable(false)
                    .create()
                    .show();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Wait failed", e);
        }
    }

    @Override
    public String showInputBox(final String prompt) {
        final ArrayBlockingQueue<String> inputQueue = new ArrayBlockingQueue<>(1);

        runOnUiThread(() -> {
            final View view = getLayoutInflater().inflate(R.layout.dialog_input, null);

            String message = viewState.isUseHtml() ? htmlProcessor.removeHtmlTags(prompt) : prompt;
            if (message == null) {
                message = "";
            }

            new AlertDialog.Builder(context)
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
        } catch (InterruptedException e) {
            logger.error("Wait for input failed", e);
            return null;
        }
    }

    @Override
    public int showMenu() {
        final ArrayBlockingQueue<Integer> resultQueue = new ArrayBlockingQueue<>(1);
        final ArrayList<String> items = new ArrayList<>();

        for (QspMenuItem item : viewState.getMenuItems()) {
            items.add(item.name);
        }

        runOnUiThread(() -> new AlertDialog.Builder(context)
                .setItems(items.toArray(new CharSequence[0]), (dialog, which) -> resultQueue.add(which))
                .setOnCancelListener(dialog -> resultQueue.add(-1))
                .create()
                .show());

        try {
            return resultQueue.take();
        } catch (InterruptedException e) {
            logger.error("Wait failed", e);
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
            runOnUiThread(() -> {
                showActions = show;
                separatorView.setVisibility(show ? View.VISIBLE : View.GONE);
                actionsView.setVisibility(show ? View.VISIBLE : View.GONE);
            });
        } else {
            logger.debug("Unsupported window type: " + type);
        }
    }

    // endregion PlayerView

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
            float absVelocityX = Math.abs(velocityX);
            float absVelocityY = Math.abs(velocityY);

            if (Math.abs(diffX) > SWIPE_THRESHOLD && absVelocityX > absVelocityY) {
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
                String code = htmlProcessor.decodeExec(href.substring(5));
                libQspProxy.execute(code);
            }

            return true;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (url.startsWith("file:///")) {
                String path = normalizePath(Uri.decode(url.substring(8)));
                File file = findFileRecursively(viewState.getGameDir(), path);
                if (file == null) {
                    logger.error("File not found: " + path);
                    return null;
                }
                try {
                    String extension = getExtension(file.getName());
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    InputStream in = context.getContentResolver().openInputStream(Uri.fromFile(file));
                    return new WebResourceResponse(mimeType, null, in);
                } catch (FileNotFoundException e) {
                    logger.error("File not found", e);
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
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(getFontSize()));
                    //textView.setBackgroundColor(getBackgroundColor());
                    textView.setTextColor(getTextColor());
                    textView.setLinkTextColor(getLinkColor());
                    textView.setText(item.text);
                }
            }

            return convertView;
        }

        private Typeface getTypeface() {
            switch (Integer.parseInt(typeface)) {
                case 1:
                    return Typeface.SANS_SERIF;
                case 2:
                    return Typeface.SERIF;
                case 3:
                    return Typeface.MONOSPACE;
                case 0:
                default:
                    return Typeface.DEFAULT;
            }
        }
    }
}
