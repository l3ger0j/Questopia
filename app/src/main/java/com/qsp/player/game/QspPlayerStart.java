package com.qsp.player.game;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.JniResult;
import com.qsp.player.R;
import com.qsp.player.settings.Settings;
import com.qsp.player.stock.QspGameStock;
import com.qsp.player.util.FileUtil;
import com.qsp.player.util.HtmlUtil;
import com.qsp.player.util.ViewUtil;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import static com.qsp.player.util.FileUtil.MIME_TYPE_BINARY;

public class QspPlayerStart extends AppCompatActivity {

    private static final String TAG = QspPlayerStart.class.getName();
    private static final int REQUEST_CODE_SELECT_GAME = 1;
    private static final int MAX_SAVE_SLOTS = 5;

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

    private final Context uiContext = this;
    private final ReentrantLock libQspLock = new ReentrantLock();
    private final Handler counterHandler = new Handler();
    private final ArrayList<QspMenuItem> qspMenuItems = new ArrayList<>();
    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final QspImageGetter imageGetter = new QspImageGetter(this);

    private final Runnable counterTask = new Runnable() {
        @Override
        public void run() {
            executeOnLibQspThread(new Runnable() {
                @Override
                public void run() {
                    if (!QSPExecCounter(true)) {
                        showLastLibQspError();
                    }
                }
            });
            counterHandler.postDelayed(this, timerInterval);
        }
    };

    private SharedPreferences settings;
    private boolean varsDescUnread;
    private int objectsBackground, varsDescBackground;
    private int activeTab;
    private String currentLanguage = Locale.getDefault().getLanguage();
    private volatile long gameStartTime;
    private volatile int timerInterval;
    private boolean gameRunning;
    private String pageTemplate;
    private volatile Handler libQspHandler;
    private DocumentFile gameDir;
    private DocumentFile gameFile;

    private View mainView;
    private WebView mainDescView;
    private WebView varsDescView;
    private ListView actionsView;
    private ListView objectsView;
    private Menu mainMenu;

    private String typeface = "";
    private String fontSize = "";
    private int backColor;
    private int textColor;
    private int linkColor;

    private volatile boolean useHtml;
    private volatile String mainDesc = "";
    private volatile String varsDesc = "";
    private volatile int qspFSize;
    private volatile int qspBColor;
    private volatile int qspFColor;
    private volatile int qspLColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        settings = PreferenceManager.getDefaultSharedPreferences(uiContext);

        loadLocale();

        mainView = findViewById(R.id.main);

        initMainDescView();
        initVarsDescView();
        initActionsView();
        initObjectsView();
        setActiveTab(TAB_MAIN_DESC);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        startLibQspThread();
        startSelectGame();
    }

    private void loadLocale() {
        String language = settings.getString("lang", "ru");
        ViewUtil.setLocale(uiContext, language);
        currentLanguage = language;
    }

    private void initMainDescView() {
        mainDescView = findViewById(R.id.main_desc);
        mainDescView.setWebViewClient(new QspWebViewClient());
    }

    private void initVarsDescView() {
        varsDescView = findViewById(R.id.vars_desc);
        varsDescView.setWebViewClient(new QspWebViewClient());
    }

    private void initActionsView() {
        actionsView = findViewById(R.id.acts);
        actionsView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        actionsView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                executeAction(position);
            }
        });

        actionsView.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                executeOnLibQspThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!QSPSetSelActionIndex(position, true)) {
                            showLastLibQspError();
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void executeOnLibQspThread(final Runnable runnable) {
        if (libQspLock.isLocked()) {
            return;
        }
        libQspHandler.post(new Runnable() {
            @Override
            public void run() {
                libQspLock.lock();
                try {
                    runnable.run();
                } finally {
                    libQspLock.unlock();
                }
            }
        });
    }

    private void executeAction(final int position) {
        executeOnLibQspThread(new Runnable() {
            @Override
            public void run() {
                if (!QSPSetSelActionIndex(position, false)) {
                    showLastLibQspError();
                }
                if (!QSPExecuteSelActionCode(true)) {
                    showLastLibQspError();
                }
            }
        });
    }

    private void showLastLibQspError() {
        JniResult errorResult = (JniResult) QSPGetLastErrorData();

        String locName = (errorResult.str1 == null) ? "" : errorResult.str1;
        int action = errorResult.int2;
        int line = errorResult.int3;
        int errorNumber = errorResult.int1;

        String desc = QSPGetErrorDesc(errorResult.int1);
        if (desc == null) {
            desc = "";
        }

        final String message = String.format(
                "Location: %s\nAction: %d\nLine: %d\nError number: %d\nDescription: %s",
                locName,
                action,
                line,
                errorNumber,
                desc);

        Log.e(TAG, message);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewUtil.showErrorDialog(uiContext, message);
            }
        });
    }

    private void initObjectsView() {
        objectsView = findViewById(R.id.inv);
        objectsView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        objectsView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                executeOnLibQspThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!QSPSetSelObjectIndex(position, true)) {
                            showLastLibQspError();
                        }
                    }
                });
            }
        });
    }

    private void setActiveTab(int tab) {
        switch (tab) {
            case TAB_OBJECTS:
                toggleObjects(true);
                toggleMainDesc(false);
                toggleVarsDesc(false);
                objectsBackground = 0;
                setTitle(getString(R.string.inventory));
                break;

            case TAB_MAIN_DESC:
                toggleObjects(false);
                toggleMainDesc(true);
                toggleVarsDesc(false);
                setTitle(getString(R.string.mainDesc));
                break;

            case TAB_VARS_DESC:
                toggleObjects(false);
                toggleMainDesc(false);
                toggleVarsDesc(true);
                varsDescUnread = false;
                varsDescBackground = 0;
                setTitle(getString(R.string.varsDesc));
                break;
        }

        activeTab = tab;
    }

    private void setTitle(String second) {
        TextView winTitle = findViewById(R.id.title_text);
        winTitle.setText(second);
        updateTitle();
    }

    private void updateTitle() {
        ImageButton objectsBtn = findViewById(R.id.title_button_1);
        objectsBtn.clearAnimation();

        ImageButton varsDescBtn = findViewById(R.id.title_button_2);
        varsDescBtn.clearAnimation();

        if (varsDescUnread) {
            varsDescBtn.setBackgroundResource(varsDescBackground = R.drawable.btn_bg_pressed);
            varsDescUnread = false;
        }
    }

    private void toggleObjects(boolean show) {
        findViewById(R.id.inv).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.title_button_1).setBackgroundResource(show ? R.drawable.btn_bg_active : objectsBackground);
    }

    private void toggleMainDesc(boolean show) {
        findViewById(R.id.main_tab).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.title_home_button).setBackgroundResource(show ? R.drawable.btn_bg_active : 0);
    }

    private void toggleVarsDesc(boolean show) {
        findViewById(R.id.vars_tab).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.title_button_2).setBackgroundResource(show ? R.drawable.btn_bg_active : varsDescBackground);
    }

    private void startLibQspThread() {
        new Thread() {
            @Override
            public void run() {
                QSPInit();
                Looper.prepare();
                libQspHandler = new Handler();
                Looper.loop();
                QSPDeInit();
            }
        }
                .start();

        Log.i(TAG, "QSP library thread started");
    }

    private void startSelectGame() {
        Intent intent = new Intent(this, QspGameStock.class);
        intent.putExtra("gameRunning", gameRunning);
        startActivityForResult(intent, REQUEST_CODE_SELECT_GAME);
    }

    @Override
    public void onDestroy() {
        if (gameRunning) {
            stopGame();
        }
        stopLibraryThread();
        super.onDestroy();
    }

    private void stopGame() {
        if (!gameRunning) {
            return;
        }
        counterHandler.removeCallbacks(counterTask);
        audioPlayer.stopAll();
        gameRunning = false;
    }

    private void stopLibraryThread() {
        Handler handler = libQspHandler;
        if (handler != null) {
            handler.getLooper().quitSafely();
        }
        Log.i(TAG, "QSP library thread stopped");
    }

    @Override
    public void onResume() {
        super.onResume();

        updateLanguage();
        loadTextSettings();
        applyTextSettings();
        refreshMainDesc();
        refreshVarsDesc();
        refreshActions();
        refreshObjects();

        audioPlayer.setSoundEnabled(settings.getBoolean("sound", true));

        if (gameRunning) {
            counterHandler.postDelayed(counterTask, timerInterval);
            audioPlayer.resumeAll();
        }
    }

    private void updateLanguage() {
        String language = settings.getString("lang", "ru");
        if (currentLanguage.equals(language)) {
            return;
        }
        ViewUtil.setLocale(uiContext, currentLanguage);
        setTitle(R.string.appName);
        invalidateOptionsMenu();
        setActiveTab(activeTab);
        currentLanguage = language;
    }

    private void loadTextSettings() {
        typeface = settings.getString("typeface", "0");
        fontSize = settings.getString("fontSize", "16");
        backColor = settings.getInt("backColor", Color.parseColor("#e0e0e0"));
        textColor = settings.getInt("textColor", Color.parseColor("#000000"));
        linkColor = settings.getInt("linkColor", Color.parseColor("#0000ff"));
    }

    private void applyTextSettings() {
        int backColor = getBackgroundColor();

        mainView.setBackgroundColor(backColor);
        mainDescView.setBackgroundColor(backColor);
        varsDescView.setBackgroundColor(backColor);
        actionsView.setBackgroundColor(backColor);
        objectsView.setBackgroundColor(backColor);

        updatePageTemplate();
    }

    private int getBackgroundColor() {
        return qspBColor != 0 ? qspBColor : backColor;
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
        return qspFColor != 0 ? qspFColor : textColor;
    }

    private int getLinkColor() {
        return qspLColor != 0 ? qspLColor : linkColor;
    }

    private String getFontSize() {
        return qspFSize != 0 ? Integer.toString(qspFSize) : fontSize;
    }

    private String getHexColor(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    private void refreshMainDesc() {
        String text = getHtml(mainDesc);
        mainDescView.loadDataWithBaseURL(
                "file:///",
                pageTemplate.replace("REPLACETEXT", text),
                "text/html",
                "UTF-8",
                "");
    }

    private String getHtml(String str) {
        return useHtml ? HtmlUtil.preprocessQspHtml(str) : HtmlUtil.convertQspStringToHtml(str);
    }

    private void refreshVarsDesc() {
        String text = getHtml(varsDesc);
        varsDescView.loadDataWithBaseURL(
                "file:///",
                pageTemplate.replace("REPLACETEXT", text),
                "text/html",
                "UTF-8",
                "");
    }

    private void refreshActions() {
        QspItemAdapter adapter = (QspItemAdapter) actionsView.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void refreshObjects() {
        QspItemAdapter adapter = (QspItemAdapter) objectsView.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPause() {
        if (gameRunning) {
            counterHandler.removeCallbacks(counterTask);
            audioPlayer.pauseAll();
        }
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

        new AlertDialog.Builder(uiContext)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            startSelectGame();
                        } else if (which == 1) {
                            moveTaskToBack(true);
                        }
                    }
                })
                .create()
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mainMenu = menu;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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

        SubMenu subMenu = mainMenu.addSubMenu(R.id.menugroup_running, id, Menu.NONE, parent.getTitle());
        subMenu.setHeaderTitle(getString(R.string.selectSlot));

        DocumentFile dir = getOrCreateSavesDirectory();

        for (int i = 0; i < MAX_SAVE_SLOTS; ++i) {
            final String filename = getSaveSlotFilename(i);
            final DocumentFile file = dir.findFile(filename);
            String title = String.format(
                    getString(file != null ? R.string.slotPresent : R.string.slotEmpty),
                    i + 1);

            MenuItem item = subMenu.add(title);
            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (action) {
                        case LOAD:
                            if (file != null) {
                                doLoadGame(filename);
                            }
                            break;
                        case SAVE:
                            doSaveGame(filename);
                            break;
                    }

                    return true;
                }
            });
        }
    }

    private DocumentFile getOrCreateSavesDirectory() {
        DocumentFile dir = gameDir.findFile("saves");
        if (dir == null) {
            dir = gameDir.createDirectory("saves");
        }

        return dir;
    }

    private String getSaveSlotFilename(int slot) {
        return (slot + 1) + ".sav";
    }

    private void doLoadGame(String filename) {
        DocumentFile dir = getOrCreateSavesDirectory();
        DocumentFile file = dir.findFile(filename);
        if (file == null) {
            Log.e(TAG, "Save file not found: " + filename);
            return;
        }
        final byte[] gameData;
        try (InputStream in = uiContext.getContentResolver().openInputStream(file.getUri())) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] b = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(b)) > 0) {
                    out.write(b, 0, bytesRead);
                }
                gameData = out.toByteArray();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed reading from the save file", e);
            return;
        }
        counterHandler.removeCallbacks(counterTask);
        audioPlayer.stopAll();

        executeOnLibQspThread(new Runnable() {
            @Override
            public void run() {
                if (!QSPOpenSavedGameFromData(gameData, gameData.length, true)) {
                    showLastLibQspError();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        counterHandler.postDelayed(counterTask, timerInterval);
                    }
                });
            }
        });
    }

    private void doSaveGame(final String filename) {
        final DocumentFile dir = getOrCreateSavesDirectory();
        executeOnLibQspThread(new Runnable() {
            @Override
            public void run() {
                DocumentFile file = dir.findFile(filename);
                if (file == null) {
                    file = dir.createFile(MIME_TYPE_BINARY, filename);
                }
                byte[] gameData = QSPSaveGameAsData(false);
                if (gameData == null) {
                    return;
                }
                try (OutputStream out = uiContext.getContentResolver().openOutputStream(file.getUri(), "w")) {
                    out.write(gameData);
                } catch (IOException e) {
                    Log.e(TAG, "Failed writing to a save file", e);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_gamestock:
                startSelectGame();
                return true;

            case R.id.menu_exit:
                moveTaskToBack(true);
                return true;

            case R.id.menu_options:
                Intent intent = new Intent();
                intent.setClass(this, Settings.class);
                startActivity(intent);
                return true;

            case R.id.menu_about:
                showAboutDialog();
                return true;

            case R.id.menu_newgame:
                stopGame();
                runGame();
                return true;

            case R.id.menu_loadgame:
            case R.id.menu_savegame:
                return true;
        }

        return false;
    }

    private void showAboutDialog() {
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        new AlertDialog.Builder(uiContext)
                .setIcon(R.drawable.icon)
                .setTitle(R.string.appName)
                .setView(messageView)
                .create()
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_SELECT_GAME) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        stopGame();

        String gameDirUri = data.getStringExtra("gameDirUri");
        DocumentFile newGameDir = FileUtil.getDirectory(uiContext, gameDirUri);
        imageGetter.setGameDirectory(newGameDir);
        audioPlayer.setGameDirectory(newGameDir);
        gameDir = newGameDir;

        String gameFileUri = data.getStringExtra("gameFileUri");
        gameFile = FileUtil.getFile(uiContext, gameFileUri);

        runGame();
    }

    public void onTitleClick(View v) {
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

    public void onMainDescTabClick(View v) {
        setActiveTab(TAB_MAIN_DESC);
    }

    public void onObjectsTabClick(View v) {
        setActiveTab(TAB_OBJECTS);
    }

    public void onVarsDescTabClick(View v) {
        setActiveTab(TAB_VARS_DESC);
    }

    public void onInputButtonClick(View v) {
        View inputButton = findViewById(R.id.title_button_3);
        inputButton.clearAnimation();

        executeOnLibQspThread(new Runnable() {
            @Override
            public void run() {
                String input = InputBox(getString(R.string.inputArea));
                QSPSetInputStrText(input);

                if (!QSPExecUserInput(true)) {
                    showLastLibQspError();
                }
            }
        });
    }

    private void runGame() {
        executeOnLibQspThread(new Runnable() {
            @Override
            public void run() {
                gameStartTime = System.currentTimeMillis();
                qspFSize = 0;
                qspBColor = 0;
                qspFColor = 0;
                qspLColor = 0;
                if (!loadGameWorld()) {
                    return;
                }
                if (!QSPRestartGame(true)) {
                    showLastLibQspError();
                }
                startCounter();
                gameRunning = true;
            }
        });

        setActiveTab(TAB_MAIN_DESC);
    }

    private boolean loadGameWorld() {
        byte[] gameData;
        try (InputStream in = uiContext.getContentResolver().openInputStream(gameFile.getUri())) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] b = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(b)) > 0) {
                    out.write(b, 0, bytesRead);
                }
                gameData = out.toByteArray();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed loading the game world", e);
            return false;
        }

        if (!QSPLoadGameWorldFromData(gameData, gameData.length, gameFile.getUri().toString())) {
            showLastLibQspError();
            return false;
        }

        return true;
    }

    private void startCounter() {
        timerInterval = 500;
        counterHandler.postDelayed(counterTask, timerInterval);
    }

    private void RefreshInt() {
        final boolean settingsChanged = loadGameUISettings();

        final boolean mainDescChanged = QSPIsMainDescChanged();
        if (mainDescChanged) {
            mainDesc = QSPGetMainDesc();
        }

        final boolean actionsChanged = QSPIsActionsChanged();
        final ArrayList<QspListItem> actions;
        if (actionsChanged) {
            int actionsCount = QSPGetActionsCount();
            actions = new ArrayList<>();
            for (int i = 0; i < actionsCount; ++i) {
                JniResult actionResult = (JniResult) QSPGetActionData(i);
                QspListItem action = new QspListItem();
                action.icon = imageGetter.getDrawable(FileUtil.normalizePath(actionResult.str2));
                action.text = useHtml ? HtmlUtil.removeHtmlTags(actionResult.str1) : actionResult.str1;
                actions.add(action);
            }
        } else {
            actions = null;
        }

        final boolean objectsChanged = QSPIsObjectsChanged();
        final ArrayList<QspListItem> objects;
        if (objectsChanged) {
            int objectsCount = QSPGetObjectsCount();
            objects = new ArrayList<>();
            for (int i = 0; i < objectsCount; i++) {
                JniResult objectResult = (JniResult) QSPGetObjectData(i);
                QspListItem object = new QspListItem();
                object.icon = imageGetter.getDrawable(FileUtil.normalizePath(objectResult.str2));
                object.text = useHtml ? HtmlUtil.removeHtmlTags(objectResult.str1) : objectResult.str1;
                objects.add(object);
            }
        } else {
            objects = null;
        }

        final boolean varsDescChanged = QSPIsVarsDescChanged();
        if (varsDescChanged) {
            varsDesc = QSPGetVarsDesc();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (settingsChanged) {
                    applyTextSettings();
                }
                if (settingsChanged || mainDescChanged) {
                    refreshMainDesc();
                }
                if (actionsChanged) {
                    QspItemAdapter adapter = new QspItemAdapter(uiContext, R.layout.act_item, actions);
                    actionsView.setAdapter(adapter);
                    adjustListViewHeight(actionsView);
                }
                if (objectsChanged) {
                    if (activeTab != TAB_OBJECTS) {
                        updateTitle();
                    }
                    QspItemAdapter adapter = new QspItemAdapter(uiContext, R.layout.obj_item, objects);
                    objectsView.setAdapter(adapter);
                }
                if (settingsChanged || varsDescChanged) {
                    if (activeTab != TAB_VARS_DESC) {
                        varsDescUnread = true;
                        updateTitle();
                    }
                    refreshVarsDesc();
                }
            }
        });
    }

    /**
     * Loads user interface settings from the current game.
     *
     * @return <code>true</code> if settings changed, <code>false</code> otherwise
     */
    private boolean loadGameUISettings() {
        boolean settingsChanged = false;

        JniResult htmlResult = (JniResult) QSPGetVarValues("USEHTML", 0);
        if (htmlResult.success) {
            boolean newUseHtml = htmlResult.int1 != 0;
            if (useHtml != newUseHtml) {
                useHtml = newUseHtml;
                settingsChanged = true;
            }
        }

        JniResult fSizeResult = (JniResult) QSPGetVarValues("FSIZE", 0);
        if (fSizeResult.success) {
            int newFSize = fSizeResult.int1;
            if (qspFSize != newFSize) {
                qspFSize = newFSize;
                settingsChanged = true;
            }
        }

        JniResult bColorResult = (JniResult) QSPGetVarValues("BCOLOR", 0);
        if (bColorResult.success) {
            int newBColor = bColorResult.int1;
            if (qspBColor != newBColor) {
                qspBColor = newBColor;
                settingsChanged = true;
            }
        }

        JniResult fColorResult = (JniResult) QSPGetVarValues("FCOLOR", 0);
        if (fColorResult.success) {
            int newFColor = fColorResult.int1;
            if (qspFColor != newFColor) {
                qspFColor = newFColor;
                settingsChanged = true;
            }
        }

        JniResult lColorResult = (JniResult) QSPGetVarValues("LCOLOR", 0);
        if (lColorResult.success) {
            int newLColor = lColorResult.int1;
            if (qspLColor != newLColor) {
                qspLColor = newLColor;
                settingsChanged = true;
            }
        }

        return settingsChanged;
    }

    private void adjustListViewHeight(ListView view) {
        ListAdapter adapter = view.getAdapter();
        if (adapter == null) {
            return;
        }

        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.AT_MOST);
        for (int i = 0; i < adapter.getCount(); i++) {
            View item = adapter.getView(i, null, view);
            item.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += item.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = totalHeight + (view.getDividerHeight() * (adapter.getCount() - 1));
        view.setLayoutParams(params);
        view.requestLayout();
    }

    private void ShowPicture(final String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(uiContext, QspImageBox.class);
                intent.putExtra("gameDirUri", gameDir.getUri().toString());
                intent.putExtra("imagePath", FileUtil.normalizePath(path));
                startActivity(intent);
            }
        });
    }

    private void SetTimer(int msecs) {
        final int timeMsecs = msecs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                timerInterval = timeMsecs;
            }
        });
    }

    private void ShowMessage(final String message) {
        final CountDownLatch latch = new CountDownLatch(1);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(uiContext)
                        .setMessage(useHtml ? HtmlUtil.removeHtmlTags(message) : message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                latch.countDown();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void PlayFile(final String path, final int volume) {
        if (path == null || path.isEmpty()) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                audioPlayer.play(FileUtil.normalizePath(path), volume);
            }
        });
    }

    private boolean IsPlayingFile(final String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return audioPlayer.isPlaying(FileUtil.normalizePath(path));
    }

    private void CloseFile(final String path) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (path == null || path.isEmpty()) {
                    audioPlayer.stopAll();
                } else {
                    audioPlayer.stop(FileUtil.normalizePath(path));
                }
            }
        });
    }

    private void OpenGame(String file) {
        doLoadGame(file);
    }

    private void SaveGame(String file) {
        doSaveGame(file);
    }

    private String InputBox(final String prompt) {
        final ArrayBlockingQueue<String> inputQueue = new ArrayBlockingQueue<>(1);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View view = getLayoutInflater().inflate(R.layout.inputbox, null);
                String title = prompt != null ? prompt : "";

                new AlertDialog.Builder(uiContext)
                        .setView(view)
                        .setMessage(title)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                EditText editView = view.findViewById(R.id.inputbox_edit);
                                inputQueue.add(editView.getText().toString());
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            }
        });

        try {
            return inputQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int GetMSCount() {
        return (int) (System.currentTimeMillis() - gameStartTime);
    }

    private void AddMenuItem(String name, String imgPath) {
        QspMenuItem item = new QspMenuItem();
        item.imgPath = FileUtil.normalizePath(imgPath);
        item.name = name;
        qspMenuItems.add(item);
    }

    private void ShowMenu() {
        final ArrayBlockingQueue<Integer> resultQueue = new ArrayBlockingQueue<>(1);
        final ArrayList<String> items = new ArrayList<>();

        for (QspMenuItem item : qspMenuItems) {
            items.add(item.name);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(uiContext)
                        .setItems(items.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                resultQueue.add(which);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                resultQueue.add(-1);
                            }
                        })
                        .create()
                        .show();
            }
        });

        try {
            int result = resultQueue.take();
            if (result != -1) {
                QSPSelectMenuItem(result);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void DeleteMenu() {
        qspMenuItems.clear();
    }

    private void Wait(int msecs) {
        try {
            Thread.sleep(msecs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class QspWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, final String href) {
            if (href.toLowerCase().startsWith("exec:")) {
                executeOnLibQspThread(new Runnable() {
                    @Override
                    public void run() {
                        String code = HtmlUtil.decodeExec(href.substring(5));
                        if (!QSPExecString(code, true)) {
                            showLastLibQspError();
                        }
                    }
                });
            }

            return true;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (url.startsWith("file:///")) {
                String path = FileUtil.normalizePath(url.substring(8));
                DocumentFile file = FileUtil.findFileByPath(gameDir, path);
                if (file == null) {
                    return null;
                }
                try {
                    InputStream in = uiContext.getContentResolver().openInputStream(file.getUri());
                    return new WebResourceResponse(file.getType(), null, in);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found", e);
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
                    textView.setBackgroundColor(getBackgroundColor());
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

    private class QspListItem {
        Drawable icon;
        CharSequence text;
    }

    private class QspMenuItem {
        String name;
        String imgPath;
    }

    private enum SlotAction {
        LOAD,
        SAVE
    }

    public native void QSPInit();

    public native void QSPDeInit();

    public native boolean QSPIsInCallBack();

    public native void QSPEnableDebugMode(boolean isDebug);

    public native Object QSPGetCurStateData();//!!!STUB

    public native String QSPGetVersion();

    public native int QSPGetFullRefreshCount();

    public native String QSPGetQstFullPath();

    public native String QSPGetCurLoc();

    public native String QSPGetMainDesc();

    public native boolean QSPIsMainDescChanged();

    public native String QSPGetVarsDesc();

    public native boolean QSPIsVarsDescChanged();

    public native Object QSPGetExprValue();//!!!STUB

    public native void QSPSetInputStrText(String val);

    public native int QSPGetActionsCount();

    public native Object QSPGetActionData(int ind);//!!!STUB

    public native boolean QSPExecuteSelActionCode(boolean isRefresh);

    public native boolean QSPSetSelActionIndex(int ind, boolean isRefresh);

    public native int QSPGetSelActionIndex();

    public native boolean QSPIsActionsChanged();

    public native int QSPGetObjectsCount();

    public native Object QSPGetObjectData(int ind);//!!!STUB

    public native boolean QSPSetSelObjectIndex(int ind, boolean isRefresh);

    public native int QSPGetSelObjectIndex();

    public native boolean QSPIsObjectsChanged();

    public native void QSPShowWindow(int type, boolean isShow);

    public native Object QSPGetVarValuesCount(String name);

    public native Object QSPGetVarValues(String name, int ind);//!!!STUB

    public native int QSPGetMaxVarsCount();

    public native Object QSPGetVarNameByIndex(int index);//!!!STUB

    public native boolean QSPExecString(String s, boolean isRefresh);

    public native boolean QSPExecLocationCode(String name, boolean isRefresh);

    public native boolean QSPExecCounter(boolean isRefresh);

    public native boolean QSPExecUserInput(boolean isRefresh);

    public native Object QSPGetLastErrorData();

    public native String QSPGetErrorDesc(int errorNum);

    public native boolean QSPLoadGameWorld(String fileName);

    public native boolean QSPLoadGameWorldFromData(byte data[], int dataSize, String fileName);

    public native boolean QSPSaveGame(String fileName, boolean isRefresh);

    public native byte[] QSPSaveGameAsData(boolean isRefresh);

    public native boolean QSPOpenSavedGame(String fileName, boolean isRefresh);

    public native boolean QSPOpenSavedGameFromData(byte data[], int dataSize, boolean isRefresh);

    public native boolean QSPRestartGame(boolean isRefresh);

    public native void QSPSelectMenuItem(int index);
    //public native void QSPSetCallBack(int type, QSP_CALLBACK func)

    static {
        System.loadLibrary("ndkqsp");
    }
}
