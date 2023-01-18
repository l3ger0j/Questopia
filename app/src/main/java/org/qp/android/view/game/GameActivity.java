package org.qp.android.view.game;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static org.qp.android.utils.ColorUtil.getHexColor;
import static org.qp.android.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.utils.FileUtil.getOrCreateDirectory;
import static org.qp.android.utils.FileUtil.getOrCreateFile;
import static org.qp.android.utils.ThreadUtil.isMainThread;
import static org.qp.android.utils.ViewUtil.getFontStyle;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.databinding.DataBindingUtil;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.Contract;
import org.qp.android.R;
import org.qp.android.databinding.ActivityGameBinding;
import org.qp.android.model.libQSP.LibQpProxy;
import org.qp.android.model.libQSP.QpMenuItem;
import org.qp.android.model.libQSP.RefreshInterfaceRequest;
import org.qp.android.model.libQSP.WindowType;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.view.adapters.RecyclerItemClickListener;
import org.qp.android.view.game.dialogs.GameDialogFrags;
import org.qp.android.view.game.dialogs.GameDialogType;
import org.qp.android.view.game.dialogs.GamePatternDialogFrags;
import org.qp.android.view.settings.SettingsActivity;
import org.qp.android.view.settings.SettingsController;
import org.qp.android.viewModel.viewModels.ActivityGame;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public class GameActivity extends AppCompatActivity implements GameInterface,
        GamePatternDialogFrags.GamePatternDialogList {
    private final String TAG = this.getClass().getSimpleName();

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

    private SettingsController settingsController;
    private int activeTab;
    private String pageTemplate = "";
    private boolean showActions = true;

    private ActionBar actionBar;
    private Menu mainMenu;
    private ConstraintLayout layoutTop;
    private WebView mainDescView;
    private WebView varsDescView;
    private View separatorView;
    private RecyclerView actionsView, objectsView;

    private HtmlProcessor htmlProcessor;
    private LibQpProxy libQpProxy;
    private AudioPlayer audioPlayer;

    private final Handler counterHandler = new Handler();

    private int counterInterval = 500;

    private final Runnable counterTask = new Runnable() {
        @Override
        public void run() {
            libQpProxy.executeCounter();
            counterHandler.postDelayed(this, counterInterval);
        }
    };

    private int slotAction = 0;
    private ActivityGame activityGame;
    private ActivityGameBinding activityGameBinding;
    private ActivityResultLauncher<Intent> resultLauncher, templateLauncher;

    private final Runnable onScroll = new Runnable() {
        @Override
        public void run() {
            if (mainDescView.getContentHeight()
                    * getResources().getDisplayMetrics().density
                    >= mainDescView.getScrollY() ){
                mainDescView.scrollBy(0, mainDescView.getHeight());
            }
        }
    };

    private String tempIdGame;
    private View mDecorView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityGameBinding = DataBindingUtil.setContentView(this, R.layout.activity_game);
        activityGame = new ViewModelProvider(this).get(ActivityGame.class);
        activityGameBinding.setGameViewModel(activityGame);
        activityGame.gameActivityObservableField.set(this);
        settingsController = activityGame.getSettingsController();

        mDecorView = getWindow().getDecorView();
        if (settingsController.isUseImmersiveMode) {
            hideSystemUI();
        } else {
            showSystemUI();
        }

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
                                    doWithCounterDisabled(() -> libQpProxy.loadGameState(uri));
                                } else {
                                    break;
                                }
                                break;
                            case SAVE:
                                if (data != null) {
                                    uri = data.getData();
                                    libQpProxy.saveGameState(uri);
                                } else {
                                    break;
                                }
                                break;
                        }
                    }
                }
        );

        templateLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri uri;
                    DocumentFile file;
                    if (result.getResultCode() == RESULT_OK) {
                        if ((uri = Objects.requireNonNull(result.getData()).getData()) == null) {
                            showError("File is not selected");
                        }
                        file = DocumentFile.fromSingleUri(this, Objects.requireNonNull(uri));
                        assert file != null;
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(file.getUri());
                            if ( inputStream != null ) {
                                var inputStreamReader = new InputStreamReader(inputStream);
                                var bufferedReader = new BufferedReader(inputStreamReader);
                                String receiveString;
                                var stringBuilder = new StringBuilder();
                                while ((receiveString = bufferedReader.readLine()) != null) {
                                    stringBuilder.append("\n").append(receiveString);
                                }
                                inputStream.close();
                                var manager = getSupportFragmentManager();
                                var fragment = manager.findFragmentByTag("executorDialogFragment");
                                var bundle = new Bundle();
                                bundle.putString("template", stringBuilder.toString());
                                Objects.requireNonNull(fragment).setArguments(bundle);
                            }
                        }
                        catch (FileNotFoundException e) {
                            showError("File not found: "+"\n"+e);
                        } catch (IOException e) {
                            showError("Can not read file: "+"\n"+e);
                        }
                    }
                }
        );

        tempIdGame = getIntent().getStringExtra("gameId");
        if (savedInstanceState != null && savedInstanceState.containsKey("tempGameId")) {
            var gameId = savedInstanceState.getString("tempGameId");
            if (!gameId.equals(tempIdGame)) {
                initServices();
                initControls();
                initGame();
            } else {
                restartServices();
                initControls();
            }
        } else {
            initServices();
            initControls();
            initGame();
        }

        Log.i(TAG, "Game created");
    }

    private void hideSystemUI() {
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void showSystemUI() {
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            mDecorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tempGameId", tempIdGame);
    }

    private void initControls() {
        htmlProcessor.setController(settingsController);
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
        if (settingsController.isUseAutoscroll) {
            mainDescView.postDelayed(onScroll, 300);
        }
    }

    private void initActionsView() {
        actionsView = activityGameBinding.actions;
        var actions = libQpProxy.getGameState().actions;
        var recycler = new GameItemRecycler(this);
        recycler.setTypeface(activityGame.getSettingsController().getTypeface());
        recycler.setTextSize(activityGame.getFontSize());
        recycler.setBackgroundColor(activityGame.getBackgroundColor());
        recycler.setTextColor(activityGame.getTextColor());
        recycler.setLinkTextColor(activityGame.getLinkColor());
        recycler.submitList(actions);
        actionsView.setAdapter(recycler);
        actionsView.addOnItemTouchListener(new RecyclerItemClickListener(
                this ,
                actionsView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view , int position) {
                        libQpProxy.onActionClicked(position);
                        libQpProxy.onActionSelected(position);
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {

                    }
                }
        ));
    }

    private void initObjectsView() {
        objectsView = activityGameBinding.objects;
        var objects = libQpProxy.getGameState().objects;
        var recycler = new GameItemRecycler(this);
        recycler.setTypeface(activityGame.getSettingsController().getTypeface());
        recycler.setTextSize(activityGame.getFontSize());
        recycler.setBackgroundColor(activityGame.getBackgroundColor());
        recycler.setTextColor(activityGame.getTextColor());
        recycler.setLinkTextColor(activityGame.getLinkColor());
        recycler.submitList(objects);
        objectsView.setAdapter(recycler);
        objectsView.addOnItemTouchListener(new RecyclerItemClickListener(
                this ,
                objectsView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view , int position) {
                        libQpProxy.onObjectSelected(position);
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {

                    }
                }
        ));
    }

    private void initVarsDescView() {
        varsDescView = activityGameBinding.varsDesc;
    }

    private void initServices() {
        htmlProcessor = activityGame.getHtmlProcessor();
        audioPlayer = activityGame.startAudio();
        libQpProxy = activityGame.startLibQsp(this);
    }

    private void restartServices() {
        htmlProcessor = activityGame.getHtmlProcessor();
        audioPlayer = activityGame.getAudioPlayer();
        libQpProxy = activityGame.getLibQspProxy();
        libQpProxy.setGameInterface(this);
    }

    private void initGame() {
        var intent = getIntent();
        var gameId = intent.getStringExtra("gameId");
        var gameTitle = intent.getStringExtra("gameTitle");
        var gameDirUri = intent.getStringExtra("gameDirUri");
        var gameDir = new File(gameDirUri);
        var gameFileUri = intent.getStringExtra("gameFileUri");
        var gameFile = new File(gameFileUri);
        libQpProxy.runGame(gameId, gameTitle, gameDir, gameFile);
    }

    private void setActiveTab(int tab) {
        switch (tab) {
            case TAB_MAIN_DESC_AND_ACTIONS:
                toggleMainDescAndActions(true);
                toggleObjects(false);
                toggleVarsDesc(false);
                setTitle(getString(R.string.mainDescTitle));
                break;

            case TAB_OBJECTS:
                toggleMainDescAndActions(false);
                toggleObjects(true);
                toggleVarsDesc(false);
                setTitle(getString(R.string.inventoryTitle));
                break;

            case TAB_VARS_DESC:
                toggleMainDescAndActions(false);
                toggleObjects(false);
                toggleVarsDesc(true);
                setTitle(getString(R.string.varsDescTitle));
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
        mainMenu.findItem(R.id.menu_mainDesc).setIcon(activeTab == TAB_MAIN_DESC_AND_ACTIONS ? R.drawable.tab_main : R.drawable.tab_main_alt);
        mainMenu.findItem(R.id.menu_varsDesc).setIcon(activeTab == TAB_VARS_DESC ? R.drawable.tab_vars : R.drawable.tab_vars_alt);
    }

    @Override
    protected void onDestroy() {
        libQpProxy.setGameInterface(null);
        counterHandler.removeCallbacks(counterTask);
        super.onDestroy();
        Log.i(TAG,"Game destroyed");
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
        settingsController = SettingsController.newInstance().loadSettings(this);
        applySettings();
        if (libQpProxy.getGameState().gameRunning) {
            applyGameState();
            audioPlayer.setSoundEnabled(settingsController.isSoundEnabled);
            audioPlayer.resume();
            counterHandler.postDelayed(counterTask, counterInterval);
        }
    }

    private void applySettings() {
        applyActionsHeightRatio();
        applyGameState();

        if (settingsController.isUseSeparator) {
            separatorView.setBackgroundColor(activityGame.getBackgroundColor());
        } else {
            separatorView.setBackgroundColor(getResources().getColor(R.color.materialcolorpicker__grey));
        }

        htmlProcessor.setController(settingsController);

        var backColor =
                activityGame.getBackgroundColor();
        layoutTop.setBackgroundColor(backColor);
        mainDescView.setBackgroundColor(backColor);
        varsDescView.setBackgroundColor(backColor);
        actionsView.setBackgroundColor(backColor);
        objectsView.setBackgroundColor(backColor);

        updatePageTemplate();
    }

    private void applyActionsHeightRatio() {
        var constraintSet = new ConstraintSet();
        constraintSet.clone(layoutTop);
        constraintSet.setVerticalWeight(R.id.main_desc, 1.0f - settingsController.actionsHeightRatio);
        constraintSet.setVerticalWeight(R.id.actions, settingsController.actionsHeightRatio);
        constraintSet.applyTo(layoutTop);
    }

    private void updatePageTemplate() {
        var pageHeadTemplate = PAGE_HEAD_TEMPLATE
                .replace("QSPTEXTCOLOR", getHexColor(activityGame.getTextColor()))
                .replace("QSPBACKCOLOR", getHexColor(activityGame.getBackgroundColor()))
                .replace("QSPLINKCOLOR", getHexColor(activityGame.getLinkColor()))
                .replace("QSPFONTSTYLE", getFontStyle(settingsController.getTypeface()))
                .replace("QSPFONTSIZE", Integer.toString(activityGame.getFontSize()));
        pageTemplate = pageHeadTemplate + PAGE_BODY_TEMPLATE;
        Log.d(TAG, getHexColor(activityGame.getBackgroundColor()));
    }

    private void applyGameState() {
        refreshMainDesc();
        refreshVarsDesc();
        refreshActions();
        refreshObjects();
    }

    private void refreshMainDesc() {
        var mainDesc = getHtml(libQpProxy.getGameState().mainDesc);

        if (settingsController.isUseAutoscroll) {
            mainDescView.postDelayed(onScroll, 300);
        }

        mainDescView.loadDataWithBaseURL(
                "file:///",
                pageTemplate.replace("REPLACETEXT", mainDesc),
                "text/html",
                "UTF-8",
                null);
    }

    private String getHtml(String str) {
        var config = libQpProxy.getGameState().interfaceConfig;
        return config.useHtml ?
                htmlProcessor.convertQspHtmlToWebViewHtml(str) :
                htmlProcessor.convertQspStringToWebViewHtml(str);
    }

    private void refreshVarsDesc() {
        var varsDesc = getHtml(libQpProxy.getGameState().varsDesc);
        varsDescView.loadDataWithBaseURL(
                "file:///",
                pageTemplate.replace("REPLACETEXT", varsDesc),
                "text/html",
                "UTF-8",
                null);
    }

    private void refreshActions() {
        var actions = libQpProxy.getGameState().actions;
        var recycler = new GameItemRecycler(this);
        recycler.setTypeface(activityGame.getSettingsController().getTypeface());
        recycler.setTextSize(activityGame.getFontSize());
        recycler.setBackgroundColor(activityGame.getBackgroundColor());
        recycler.setTextColor(activityGame.getTextColor());
        recycler.setLinkTextColor(activityGame.getLinkColor());
        recycler.submitList(actions);
        actionsView.setAdapter(recycler);
        refreshActionsVisibility();
    }

    private void refreshObjects() {
        var objects = libQpProxy.getGameState().objects;
        var recycler = new GameItemRecycler(this);
        recycler.setTypeface(activityGame.getSettingsController().getTypeface());
        recycler.setTextSize(activityGame.getFontSize());
        recycler.setBackgroundColor(activityGame.getBackgroundColor());
        recycler.setTextColor(activityGame.getTextColor());
        recycler.setLinkTextColor(activityGame.getLinkColor());
        recycler.submitList(objects);
        objectsView.setAdapter(recycler);
    }

    private void promptCloseGame() {
        var dialogFragment = new GameDialogFrags();
        dialogFragment.setDialogType(GameDialogType.CLOSE_DIALOG);
        dialogFragment.setCancelable(false);
        dialogFragment.show(getSupportFragmentManager(), "closeGameDialogFragment");
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
        var inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_game, menu);
        updateTabIcons();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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
        var id = parent.getItemId();
        mainMenu.removeItem(id);

        var order = action == LOAD ? 2 : 3;
        var subMenu = mainMenu.addSubMenu(R.id.menuGroup_running , id, order, parent.getTitle());
        subMenu.setHeaderTitle(getString(R.string.selectSlot));

        MenuItem item;
        final var savesDir = getOrCreateDirectory(libQpProxy.getGameState().gameDir, "saves");
        final var proxy = libQpProxy;

        for (int i = 0; i < MAX_SAVE_SLOTS; ++i) {
            final var filename = getSaveSlotFilename(i);
            final var file = findFileOrDirectory(savesDir, filename);
            String title;

            if (file != null) {
                var lastMod =
                        DateFormat.format("yyyy-MM-dd HH:mm:ss",
                                file.lastModified()).toString();
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
                        var file1 = getOrCreateFile(savesDir, filename);
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
                mIntent.putExtra(Intent.EXTRA_TITLE, libQpProxy.getGameState().gameFile + ".sav");
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
        if (i == R.id.menu_mainDesc) {
            setActiveTab(TAB_MAIN_DESC_AND_ACTIONS);
            return true;
        } else if (i == R.id.menu_inventory) {
            setActiveTab(TAB_OBJECTS);
            return true;
        } else if (i == R.id.menu_varsDesc) {
            setActiveTab(TAB_VARS_DESC);
            return true;
        } else if (i == R.id.menu_userInput) {
            if (settingsController.isUseExecString) {
                libQpProxy.onUseExecutorString();
            } else {
                libQpProxy.onInputAreaClicked();
            }
            return true;
        } else if (i == R.id.menu_gameStock) {
            promptCloseGame();
            return true;
        } else if (i == R.id.menu_options) {
            var intent = new Intent();
            intent.setClass(this , SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (i == R.id.menu_newGame) {
            libQpProxy.restartGame();
            setActiveTab(TAB_MAIN_DESC_AND_ACTIONS);
            return true;
        } else return i == R.id.menu_loadGame || i == R.id.menu_saveGame;
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
        runOnUiThread(() -> {
            var dialogFragment = new GameDialogFrags();
            dialogFragment.setDialogType(GameDialogType.ERROR_DIALOG);
            dialogFragment.setMessage(message);
            dialogFragment.show(getSupportFragmentManager(), "errorDialogFragment");
        });
    }

    @Override
    public void showPicture(final String pathToImg) {
        runOnUiThread(() -> {
            var dialogFragment = new GameDialogFrags();
            dialogFragment.setDialogType(GameDialogType.IMAGE_DIALOG);
            dialogFragment.pathToImage.set(pathToImg);
            dialogFragment.show(getSupportFragmentManager(), "imageDialogFragment");
        });
    }

    @Override
    public void showMessage(final String message) {
        if (isMainThread()) {
            throw new RuntimeException("Must not be called on the main thread");
        }
        final var latch = new CountDownLatch(1);

        runOnUiThread(() -> {
            var config = libQpProxy.getGameState().interfaceConfig;
            var processedMsg = config.useHtml ? htmlProcessor.removeHTMLTags(message) : message;
            if (processedMsg == null) {
                processedMsg = "";
            }

            if (activityGame.outputBooleanObserver.hasObservers()) {
                activityGame.outputBooleanObserver = new MutableLiveData<>();
            }

            var dialogFragment = new GameDialogFrags();
            dialogFragment.setDialogType(GameDialogType.MESSAGE_DIALOG);
            dialogFragment.setProcessedMsg(processedMsg);
            dialogFragment.setCancelable(false);
            dialogFragment.show(getSupportFragmentManager(), "showMessageDialogFragment");
            activityGame.outputBooleanObserver.observeForever(aBoolean -> {
                if (aBoolean) {
                    latch.countDown();
                }
            });
        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            showError("Wait failed"+"\n"+ex);
        }
    }

    @Override
    public String showInputDialog(final String prompt) {
        if (isMainThread()) {
            throw new RuntimeException("Must not be called on the main thread");
        }
        final ArrayBlockingQueue<String> inputQueue = new ArrayBlockingQueue<>(1);

        runOnUiThread(() -> {
            var config = libQpProxy.getGameState().interfaceConfig;
            var message = config.useHtml ? htmlProcessor.removeHTMLTags(prompt) : prompt;
            if (message == null) {
                message = "";
            }

            if (activityGame.outputTextObserver.hasObservers()) {
                activityGame.outputTextObserver = new MutableLiveData<>();
            }

            var dialogFragment = new GameDialogFrags();
            dialogFragment.setDialogType(GameDialogType.INPUT_DIALOG);
            dialogFragment.setMessage(message);
            dialogFragment.setCancelable(false);
            dialogFragment.show(getSupportFragmentManager(), "inputDialogFragment");
            activityGame.outputTextObserver.observeForever(inputQueue::add);
        });

        try {
            return inputQueue.take();
        } catch (InterruptedException ex) {
            showError("Wait for input failed"+"\n"+ex);
            return "";
        }
    }

    @Override
    public String showExecutorDialog(final String text) {
        if (isMainThread()) {
            throw new RuntimeException("Must not be called on the main thread");
        }
        final ArrayBlockingQueue<String> inputQueue = new ArrayBlockingQueue<>(1);

        runOnUiThread(() -> {
            var config = libQpProxy.getGameState().interfaceConfig;
            var message = config.useHtml ? htmlProcessor.removeHTMLTags(text) : text;
            if (message == null) {
                message = "";
            }

            if (activityGame.outputTextObserver.hasObservers()) {
                activityGame.outputTextObserver = new MutableLiveData<>();
            }

            var dialogFragment = new GameDialogFrags();
            dialogFragment.setDialogType(GameDialogType.EXECUTOR_DIALOG);
            dialogFragment.setMessage(message);
            dialogFragment.setCancelable(false);
            dialogFragment.show(getSupportFragmentManager(), "executorDialogFragment");
            activityGame.outputTextObserver.observeForever(inputQueue::add);
        });

        try {
            return inputQueue.take();
        } catch (InterruptedException ex) {
            showError("Wait for input failed"+ex);
            return "";
        }
    }

    @Override
    public int showMenu() {
        if (isMainThread()) {
            throw new RuntimeException("Must not be called on the main thread");
        }
        final var resultQueue = new ArrayBlockingQueue<Integer>(1);
        final var items = new ArrayList<String>();

        for (QpMenuItem item : libQpProxy.getGameState().menuItems) {
            items.add(item.name);
        }

        runOnUiThread(() -> {
            if (activityGame.outputIntObserver.hasObservers()) {
                activityGame.outputIntObserver = new MutableLiveData<>();
            }

            var dialogFragment = new GameDialogFrags();
            dialogFragment.setDialogType(GameDialogType.MENU_DIALOG);
            dialogFragment.setItems(items);
            dialogFragment.setCancelable(false);
            dialogFragment.show(getSupportFragmentManager(), "showMenuDialogFragment");
            activityGame.outputIntObserver.observeForever(resultQueue::add);
        });

        try {
            return resultQueue.take();
        } catch (InterruptedException ex) {
            showError("Wait failed"+"\n"+ex);
            return -1;
        }
    }

    @Override
    public void showLoadGamePopup() {
        runOnUiThread(() -> {
            var dialogFragment = new GameDialogFrags();
            dialogFragment.setDialogType(GameDialogType.LOAD_DIALOG);
            dialogFragment.setCancelable(true);
            dialogFragment.show(getSupportFragmentManager(), "loadGameDialogFragment");
        });
    }

    @Override
    public void showSaveGamePopup(String filename) {
        runOnUiThread(() -> mainMenu.performIdentifierAction(R.id.menu_saveGame , 0));
    }

    @Override
    public void showWindow(WindowType type, final boolean show) {
        if (type == WindowType.ACTIONS) {
            showActions = show;
            if (activeTab == TAB_MAIN_DESC_AND_ACTIONS) {
                runOnUiThread(this::refreshActionsVisibility);
            }
        }
    }

    private void refreshActionsVisibility() {
        if (actionsView.getAdapter() != null) {
            int count = actionsView.getAdapter().getItemCount();
            boolean show = showActions && count > 0;
            separatorView.setVisibility(show ? View.VISIBLE : View.GONE);
            actionsView.setVisibility(show ? View.VISIBLE : View.GONE);
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

    @Override
    public void onDialogPositiveClick(@NonNull DialogFragment dialog) {
        if (Objects.equals(dialog.getTag() , "closeGameDialogFragment")) {
            activityGame.stopAudio();
            activityGame.stopLibQsp();
            counterHandler.removeCallbacks(counterTask);
            finish();
        } else if (Objects.equals(dialog.getTag() , "inputDialogFragment") ||
                Objects.equals(dialog.getTag() , "executorDialogFragment")) {
            if (dialog.requireDialog().getWindow().findViewById(R.id.inputBox_edit) != null) {
                TextInputLayout editText = dialog.requireDialog().getWindow()
                        .findViewById(R.id.inputBox_edit);
                if (editText.getEditText() != null) {
                    var outputText = editText.getEditText().getText().toString();
                    if (Objects.equals(outputText , "")) {
                        activityGame.outputTextObserver.setValue("");
                    } else {
                        activityGame.outputTextObserver.setValue(outputText);
                    }
                }
            }
        } else if (Objects.equals(dialog.getTag() , "loadGameDialogFragment")) {
            startReadOrWriteSave(LOAD);
        } else if (Objects.equals(dialog.getTag() , "showMessageDialogFragment")) {
            activityGame.outputBooleanObserver.setValue(true);
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        if (dialog == null) {
            showError("Dialog is null");
        } else {
            if (Objects.equals(dialog.getTag() , "showMenuDialogFragment")) {
                activityGame.outputIntObserver.setValue(-1);
            } else if (Objects.equals(dialog.getTag() , "executorDialogFragment")) {
                Intent intentInstall = new Intent(ACTION_OPEN_DOCUMENT);
                intentInstall.addCategory(Intent.CATEGORY_OPENABLE);
                intentInstall.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intentInstall.setType("text/plain");
                try {
                    templateLauncher.launch(intentInstall);
                } catch (ActivityNotFoundException e) {
                    showError("Error: "+"\n"+e);
                }
            }
        }
    }

    @Override
    public void onDialogListClick(DialogFragment dialog , int which) {
        if (dialog == null) {
            showError("Dialog is null");
        } else {
            if (Objects.equals(dialog.getTag() , "showMenuDialogFragment")) {
                activityGame.outputIntObserver.setValue(which);
            }
        }
    }

    // endregion GameInterface
}
