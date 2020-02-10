package com.qsp.player.game;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
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

import com.qsp.player.QuestPlayerApplication;
import com.qsp.player.R;
import com.qsp.player.settings.SettingsActivity;
import com.qsp.player.stock.GameStockActivity;
import com.qsp.player.util.FileUtil;
import com.qsp.player.util.HtmlUtil;
import com.qsp.player.util.ViewUtil;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity implements PlayerView {

    private static final String TAG = MainActivity.class.getName();
    private static final int MAX_SAVE_SLOTS = 5;

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

    private final Context uiContext = this;
    private final ImageProvider imageProvider = new ImageProvider(this);

    private LibQspProxy libQspProxy;
    private SharedPreferences settings;
    private String currentLanguage = Locale.getDefault().getLanguage();
    private int activeTab;
    private boolean varsDescUnread;
    private int varsDescBackground;
    private String pageTemplate = "";
    private boolean selectingGame;
    private PlayerViewState viewState;

    private View mainView;
    private WebView mainDescView;
    private WebView varsDescView;
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

        libQspProxy = ((QuestPlayerApplication) getApplication()).getLibQspProxy();
        libQspProxy.setPlayerView(this);

        setContentView(R.layout.main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        settings = PreferenceManager.getDefaultSharedPreferences(uiContext);

        loadLocale();

        mainView = findViewById(R.id.main);

        initMainDescView();
        initVarsDescView();
        initActionsView();
        initObjectsView();
        setActiveTab(TAB_MAIN_DESC);
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
                libQspProxy.onActionClicked(position);
            }
        });

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
        objectsView = findViewById(R.id.inv);
        objectsView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        objectsView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                libQspProxy.onObjectSelected(position);
            }
        });
    }

    private void setActiveTab(int tab) {
        switch (tab) {
            case TAB_OBJECTS:
                toggleObjects(true);
                toggleMainDesc(false);
                toggleVarsDesc(false);
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

    private void toggleObjects(boolean show) {
        findViewById(R.id.inv).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.title_button_1).setBackgroundResource(show ? R.drawable.btn_bg_active : 0);
    }

    private void toggleMainDesc(boolean show) {
        findViewById(R.id.main_tab).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.title_home_button).setBackgroundResource(show ? R.drawable.btn_bg_active : 0);
    }

    private void toggleVarsDesc(boolean show) {
        findViewById(R.id.vars_tab).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.title_button_2).setBackgroundResource(show ? R.drawable.btn_bg_active : varsDescBackground);
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
        if (viewState.gameRunning) {
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
        ViewUtil.setLocale(uiContext, language);
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

        mainView.setBackgroundColor(backColor);
        mainDescView.setBackgroundColor(backColor);
        varsDescView.setBackgroundColor(backColor);
        actionsView.setBackgroundColor(backColor);
        objectsView.setBackgroundColor(backColor);

        updatePageTemplate();
    }

    private int getBackgroundColor() {
        return viewState != null && viewState.backColor != 0 ?
                reverseColor(viewState.backColor) :
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
        return viewState != null && viewState.fontColor != 0 ?
                reverseColor(viewState.fontColor) :
                textColor;
    }

    private int getLinkColor() {
        return viewState != null && viewState.linkColor != 0 ?
                reverseColor(viewState.linkColor) :
                linkColor;
    }

    private String getFontSize() {
        return useGameFont && viewState != null && viewState.fontSize != 0 ?
                Integer.toString(viewState.fontSize) :
                fontSize;
    }

    private String getHexColor(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    private void applyViewState() {
        imageProvider.setGameDirectory(viewState.gameDir);

        applyTextSettings();
        refreshMainDesc();
        refreshVarsDesc();
        refreshActions();
        refreshObjects();
    }

    private void refreshMainDesc() {
        String text = getHtml(viewState.mainDesc);
        mainDescView.loadDataWithBaseURL(
                "file:///",
                pageTemplate.replace("REPLACETEXT", text),
                "text/html",
                "UTF-8",
                "");
    }

    private String getHtml(String str) {
        return viewState.useHtml ?
                HtmlUtil.preprocessQspHtml(str) :
                HtmlUtil.convertQspStringToHtml(str);
    }

    private void refreshVarsDesc() {
        String text = getHtml(viewState.varsDesc);
        varsDescView.loadDataWithBaseURL(
                "file:///",
                pageTemplate.replace("REPLACETEXT", text),
                "text/html",
                "UTF-8",
                "");
    }

    private void refreshActions() {
        actionsView.setAdapter(new QspItemAdapter(uiContext, R.layout.act_item, viewState.actions));
        adjustListViewHeight(actionsView);
    }

    private void refreshObjects() {
        objectsView.setAdapter(new QspItemAdapter(uiContext, R.layout.obj_item, viewState.objects));
        adjustListViewHeight(objectsView);
    }

    private void startSelectGame() {
        selectingGame = true;
        Intent intent = new Intent(this, GameStockActivity.class);
        intent.putExtra("gameRunning", viewState.gameRunning);
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

        new AlertDialog.Builder(uiContext)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            startSelectGame();
                        } else if (which == 1) {
                            System.exit(0);
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
        boolean gameRunning = viewState.gameRunning;
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

        MenuItem item;
        final DocumentFile savesDir = FileUtil.getOrCreateDirectory(viewState.gameDir, "saves");
        final LibQspProxy proxy = libQspProxy;

        for (int i = 0; i < MAX_SAVE_SLOTS; ++i) {
            final String filename = getSaveSlotFilename(i);
            final DocumentFile file = savesDir.findFile(filename);
            String title = String.format(
                    getString(file != null ? R.string.slotPresent : R.string.slotEmpty),
                    i + 1);

            item = subMenu.add(title);
            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (action) {
                        case LOAD:
                            if (file != null) {
                                proxy.loadGameState(file.getUri());
                            }
                            break;
                        case SAVE:
                            DocumentFile file = FileUtil.getOrCreateBinaryFile(savesDir, filename);
                            proxy.saveGameState(file.getUri());
                            break;
                    }

                    return true;
                }
            });
        }

        switch (action) {
            case LOAD:
                item = subMenu.add(getString(R.string.loadFrom));
                item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        startLoadFromFile();
                        return true;
                    }
                });
                break;
            case SAVE:
                item = subMenu.add(getString(R.string.saveTo));
                item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        startSaveToFile();
                        return true;
                    }
                });
                break;
        }
    }

    private void startLoadFromFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/octet-stream");
        startActivityForResult(intent, REQUEST_CODE_LOAD_FROM_FILE);
    }

    private void startSaveToFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, viewState.gameTitle + ".sav");
        startActivityForResult(intent, REQUEST_CODE_SAVE_TO_FILE);
    }

    private String getSaveSlotFilename(int slot) {
        return (slot + 1) + ".sav";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
        String gameTitle = data.getStringExtra("gameTitle");

        String gameDirUri = data.getStringExtra("gameDirUri");
        DocumentFile gameDir = FileUtil.getDirectory(uiContext, gameDirUri);
        imageProvider.setGameDirectory(gameDir);

        String gameFileUri = data.getStringExtra("gameFileUri");
        DocumentFile gameFile = FileUtil.getFile(uiContext, gameFileUri);

        libQspProxy.runGame(gameTitle, gameDir, gameFile);
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
        libQspProxy.onInputAreaClicked();
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

    // Begin PlayerView implementation

    @Override
    public void refreshGameView(
            final boolean confChanged,
            final boolean mainDescChanged,
            final boolean actionsChanged,
            final boolean objectsChanged,
            final boolean varsDescChanged) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
                if (varsDescChanged && activeTab != TAB_VARS_DESC) {
                    varsDescUnread = true;
                    updateTitle();
                }
                if (confChanged || varsDescChanged) {
                    refreshVarsDesc();
                }
            }
        });
    }

    @Override
    public void showError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewUtil.showErrorDialog(uiContext, message);
            }
        });
    }

    @Override
    public void showPicture(final String path) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(uiContext, ImageBoxActivity.class);
                intent.putExtra("gameDirUri", viewState.gameDir.getUri().toString());
                intent.putExtra("imagePath", FileUtil.normalizePath(path));
                startActivity(intent);
            }
        });
    }

    @Override
    public void showMessage(final String message) {
        final CountDownLatch latch = new CountDownLatch(1);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String processedMsg = viewState.useHtml ? HtmlUtil.removeHtmlTags(message) : message;
                if (processedMsg == null) {
                    processedMsg = "";
                }

                new AlertDialog.Builder(uiContext)
                        .setMessage(processedMsg)
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
            Log.e(TAG, "Wait failed", e);
        }
    }

    @Override
    public String showInputBox(final String prompt) {
        final ArrayBlockingQueue<String> inputQueue = new ArrayBlockingQueue<>(1);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View view = getLayoutInflater().inflate(R.layout.inputbox, null);

                String message = viewState.useHtml ? HtmlUtil.removeHtmlTags(prompt) : prompt;
                if (message == null) {
                    message = "";
                }

                new AlertDialog.Builder(uiContext)
                        .setView(view)
                        .setMessage(message)
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
            Log.e(TAG, "Wait for input failed", e);
            return null;
        }
    }

    @Override
    public int showMenu() {
        final ArrayBlockingQueue<Integer> resultQueue = new ArrayBlockingQueue<>(1);
        final ArrayList<String> items = new ArrayList<>();

        for (QspMenuItem item : viewState.menuItems) {
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
            return resultQueue.take();
        } catch (InterruptedException e) {
            Log.e(TAG, "Wait failed", e);
            return -1;
        }
    }

    // End PlayerView implementation

    private enum SlotAction {
        LOAD,
        SAVE
    }

    private class QspWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, final String href) {
            if (href.toLowerCase().startsWith("exec:")) {
                final String code = HtmlUtil.decodeExec(href.substring(5));
                libQspProxy.execute(code);
            }

            return true;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (url.startsWith("file:///")) {
                String path = FileUtil.normalizePath(Uri.decode(url.substring(8)));
                DocumentFile file = FileUtil.findFileByPath(viewState.gameDir, path);
                if (file == null) {
                    Log.e(TAG, "File not found: " + path);
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
}
