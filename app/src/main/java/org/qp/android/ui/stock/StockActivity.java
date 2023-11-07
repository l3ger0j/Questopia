package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.FileUtil.deleteDirectory;
import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_DIR_FILE;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_IMAGE_FILE;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_MOD_FILE;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_PATH_FILE;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_ROOT_FOLDER;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ActionMode;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.anggrayudi.storage.SimpleStorageHelper;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.qp.android.BuildConfig;
import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.databinding.ActivityStockBinding;
import org.qp.android.dto.stock.InnerGameData;
import org.qp.android.helpers.utils.ViewUtil;
import org.qp.android.ui.dialogs.StockDialogFrags;
import org.qp.android.ui.dialogs.StockDialogType;
import org.qp.android.ui.dialogs.StockPatternDialogFrags;
import org.qp.android.ui.settings.SettingsActivity;
import org.qp.android.ui.settings.SettingsController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class StockActivity extends AppCompatActivity implements
        StockPatternDialogFrags.StockPatternDialogList, StockPatternFragment.StockPatternFragmentList {

    private static final int READ_EXTERNAL_STORAGE_CODE = 200;
    private static final int MANAGE_EXTERNAL_STORAGE_CODE = 201;
    private static final int POST_NOTIFICATION = 203;
    private final String TAG = this.getClass().getSimpleName();
    private HashMap<String, InnerGameData> gamesMap = new HashMap<>();
    public SettingsController settingsController;
    private StockViewModel stockViewModel;

    private NavController navController;

    private ActionMode actionMode;
    protected ActivityStockBinding activityStockBinding;
    private boolean isEnable = false;
    private ExtendedFloatingActionButton mFAB;
    private RecyclerView mRecyclerView;
    private ArrayList<InnerGameData> tempList;
    private final ArrayList<InnerGameData> selectList = new ArrayList<>();

    private final SimpleStorageHelper storageHelper = new SimpleStorageHelper(this);

    public void setRecyclerView(RecyclerView mRecyclerView) {
        this.mRecyclerView = mRecyclerView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        var splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> false);
        super.onCreate(savedInstanceState);

        activityStockBinding = ActivityStockBinding.inflate(getLayoutInflater());
        stockViewModel = new ViewModelProvider(this).get(StockViewModel.class);
        activityStockBinding.setStockVM(stockViewModel);
        stockViewModel.activityObservableField.set(this);
        gamesMap = stockViewModel.getGamesMap();

        mFAB = activityStockBinding.stockFAB;
        mFAB.setOnClickListener(view -> showDirPickerDialog(CODE_PICK_ROOT_FOLDER));

        storageHelper.setOnFileSelected((integer , documentFiles) -> {
            if (documentFiles != null) {
                switch (integer) {
                    case CODE_PICK_IMAGE_FILE -> {
                        for (var file : documentFiles) {
                            var document = documentWrap(file);
                            switch (document.getExtension()) {
                                case "png" , "jpg" , "jpeg" -> {
                                    getContentResolver().takePersistableUriPermission(document.getUri() ,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    stockViewModel.setTempImageFile(document.getDocumentFile());
                                }
                            }
                        }
                    }
                    case CODE_PICK_PATH_FILE -> {
                        for (var file : documentFiles) {
                            var document = documentWrap(file);
                            if ("qsp".equals(document.getExtension()))
                                stockViewModel.setTempPathFile(document.getDocumentFile());
                        }
                    }
                    case CODE_PICK_MOD_FILE -> {
                        for (var file : documentFiles) {
                            var document = documentWrap(file);
                            if ("qsp".equals(document.getExtension()))
                                stockViewModel.setTempModFile(document.getDocumentFile());
                        }
                    }
                }

            } else {
                showErrorDialog("File is not selected");
            }
            return null;
        });

        storageHelper.setOnFolderSelected((integer , documentFile) -> {
            if (documentFile != null) {
                switch (integer) {
                    case CODE_PICK_DIR_FILE ->
                            stockViewModel.setTempInstallDir(documentFile);
                    case CODE_PICK_ROOT_FOLDER -> {
                        var application = (QuestPlayerApplication) getApplication();
                        if (application != null) application.setCustomRootFolder(documentFile);
                    }
                }
            }

            return null;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this , Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this , new String[]{ Manifest.permission.POST_NOTIFICATIONS } , POST_NOTIFICATION);
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                    startActivity(intent);
                } catch (Exception ex){
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                var permission = new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE , Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permission , READ_EXTERNAL_STORAGE_CODE);
            }
        }

        loadSettings();

        Log.i(TAG,"Stock Activity created");

        setContentView(activityStockBinding.getRoot());

        var navFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.stockFragHost);
        if (navFragment != null) {
            navController = navFragment.getNavController();
        }
        if (savedInstanceState == null) {
            navController.navigate(R.id.stockRecyclerFragment);
        }

        new AppUpdater(this)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo("l3ger0j", "Questopia")
                .start();

        getOnBackPressedDispatcher().addCallback(this , new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                }
                var currDestination = navController.getCurrentDestination();
                if (currDestination != null) {
                    if (Objects.equals(currDestination.getLabel() , "StockRecyclerFragment")) {
                        finish();
                    } else {
                        stockViewModel.isHideFAB.set(false);
                        navController.popBackStack();
                    }
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode , int resultCode , @Nullable Intent data) {
        super.onActivityResult(requestCode , resultCode , data);
        storageHelper.getStorage().onActivityResult(requestCode , resultCode , data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode , @NonNull String[] permissions , @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode , permissions , grantResults);
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_CODE -> {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Success");
                } else {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Permission denied to read your External storage");
                }
            }
            case MANAGE_EXTERNAL_STORAGE_CODE -> {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Success");
                } else {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Permission denied to manage your External storage");
                }
            }
            case POST_NOTIFICATION -> {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Success");
                } else {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Permission denied to post notification");
                }
            }
        }

        storageHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        stockViewModel.isHideFAB.set(false);
        navController.popBackStack();
        return true;
    }

    private void loadSettings() {
        settingsController = stockViewModel.getSettingsController();
        stockViewModel.setController(settingsController);
        if (settingsController.binaryPrefixes <= 1000) {
            stockViewModel.refreshGameData();
        }
        if (settingsController.language.equals("ru")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"));
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
        }
    }

    public void showErrorDialog(String errorMessage) {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            var dialogFragments = new StockDialogFrags();
            dialogFragments.setDialogType(StockDialogType.ERROR_DIALOG);
            dialogFragments.setMessage(errorMessage);
            var fragment = getSupportFragmentManager()
                    .findFragmentByTag("errorDialogFragment");
            if (fragment != null && fragment.isAdded()) {
                fragment.onDestroy();
            } else {
                dialogFragments.show(getSupportFragmentManager() , "errorDialogFragment");
            }
        }
    }

    public void showDialogFragment (DialogFragment dialogFragment , StockDialogType dialogType) {
        var fragment = getSupportFragmentManager()
                .findFragmentByTag(dialogFragment.getTag());
        if (fragment != null && fragment.isAdded()) {
            fragment.onDestroy();
        } else {
            switch (dialogType) {
                case EDIT_DIALOG ->
                        dialogFragment.show(getSupportFragmentManager() , "editDialogFragment");
                case INSTALL_DIALOG ->
                        dialogFragment.show(getSupportFragmentManager() , "installDialogFragment");
                case SELECT_DIALOG ->
                        dialogFragment.show(getSupportFragmentManager() , "selectDialogFragment");
            }
        }
    }

    public void showFilePickerActivity(int requestCode , String[] mimeTypes) {
        storageHelper.openFilePicker(requestCode , false , mimeTypes);
    }

    public void startGameActivity(Intent intent) {
        startActivity(intent);
    }

    public void showDirPickerDialog(int requestCode) {
        storageHelper.openFolderPicker(requestCode);
    }

    public void onItemClick(int position) {
        if (isEnable) {
            for (InnerGameData innerGameData : gamesMap.values()) {
                if (!innerGameData.isInstalled()) continue;
                tempList.add(innerGameData);
            }
            var mViewHolder =
                    mRecyclerView.findViewHolderForAdapterPosition(position);
            var gameData =
                    tempList.get(Objects.requireNonNull(mViewHolder).getAdapterPosition());
            if (selectList.isEmpty() || !selectList.contains(gameData)) {
                selectList.add(gameData);
                var cardView = (CardView) mViewHolder.itemView.findViewWithTag("gameCardView");
                cardView.setCardBackgroundColor(Color.LTGRAY);
            } else {
                selectList.remove(gameData);
                var cardView = (CardView) mViewHolder.itemView.findViewWithTag("gameCardView");
                cardView.setCardBackgroundColor(Color.DKGRAY);
            }
        } else {
            if (!getSupportFragmentManager().getFragments().isEmpty()) {
                new ViewModelProvider(this)
                        .get(StockViewModel.class)
                        .setTempGameData(stockViewModel.getGamesMap()
                        .get(stockViewModel.getGameIdByPosition(position)));
                navController.navigate(R.id.stockGameFragment);
                stockViewModel.isHideFAB.set(true);
            }
        }
    }

    @Override
    public void onDialogDestroy(DialogFragment dialog) {
        if (!Objects.equals(dialog.getTag() , "editDialogFragment")) {
            stockViewModel.isHideFAB.set(false);
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        if (Objects.equals(dialog.getTag() , "infoDialogFragment")) {
            stockViewModel.showDialogEdit();
        }
    }

    @Override
    public void onDialogNeutralClick(DialogFragment dialog) {
        if (Objects.equals(dialog.getTag() , "infoDialogFragment")) {
            stockViewModel.playGame();
        }
    }

    @Override
    public void onDialogListClick(DialogFragment dialog, int which) {
        if (Objects.equals(dialog.getTag() , "selectDialogFragment")) {
            stockViewModel.outputIntObserver.setValue(which);
        }
    }

    @Override
    public void onClickEditButton() {
        stockViewModel.showDialogEdit();
    }

    @Override
    public void onClickPlayButton() {
        stockViewModel.playGame();
    }

    @Override
    public void onClickDownloadButton() {
        // TODO: 19.07.2023 Release this
    }

    public void onLongItemClick() {
        if (isEnable) {
            return;
        }
        var callback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode , Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_delete, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode , Menu menu) {
                tempList = stockViewModel.getSortedGames();
                isEnable = true;
                return true;
            }

            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onActionItemClicked(ActionMode mode , MenuItem item) {
                int itemId = item.getItemId();
                switch (itemId) {
                    case R.id.delete_game -> {
                        var service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                        for (var data : selectList) {
                            CompletableFuture
                                    .runAsync(() -> deleteDirectory(data.gameDir) , service)
                                    .thenRun(() -> {
                                        tempList.remove(data);
                                        stockViewModel.refreshGameData();
                                    })
                                    .exceptionally(ex -> {
                                        showErrorDialog("Error: " + "\n" + ex);
                                        return null;
                                    });
                        }
                        actionMode.finish();
                    }
                    case R.id.select_all -> {
                        if (selectList.size() == tempList.size()) {
                            selectList.clear();
                            for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
                                final var holder =
                                        mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
                                var cardView = (CardView) holder.itemView.findViewWithTag("gameCardView");
                                cardView.setCardBackgroundColor(Color.DKGRAY);
                            }
                        } else {
                            selectList.clear();
                            selectList.addAll(tempList);
                            for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
                                final var holder =
                                        mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
                                var cardView = (CardView) holder.itemView.findViewWithTag("gameCardView");
                                cardView.setCardBackgroundColor(Color.LTGRAY);
                            }
                        }
                    }
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
                    final var holder =
                            mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
                    var cardView = (CardView) holder.itemView.findViewWithTag("gameCardView");
                    cardView.setCardBackgroundColor(Color.DKGRAY);
                }
                actionMode = null;
                isEnable = false;
                tempList.clear();
                selectList.clear();
                mFAB.show();
            }
        };

        mFAB.hide();
        actionMode = startSupportActionMode(callback);
    }

    @Override
    public void onScrolled(RecyclerView recyclerView , int dx , int dy) {
        // TODO: 14.09.2023 NEED TO REWORK
//        if (!isEnable) {
//            if (dy > 0 || dy < 0 && mFAB.isShown()) {
//                stockViewModel.isHideMenu.set(true);
//                mFAB.hide();
//            }
//        }
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView , int newState) {
        // TODO: 14.09.2023 NEED TO REWORK
//        if (!isEnable) {
//            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                stockViewModel.isHideMenu.set(false);
//                mFAB.show();
//            }
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG , "Stock Activity destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSettings();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        stockViewModel.refreshIntGamesDirectory();
        navController.navigate(R.id.stockRecyclerFragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        var inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stock, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        var itemId = item.getItemId();
        if (itemId == R.id.menu_options) {
            showSettings();
            return true;
        } else if (itemId == R.id.action_search) {
            var searchView = (androidx.appcompat.widget.SearchView) item.getActionView();
            if (searchView != null) {
                searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filter(newText);
                        return false;
                    }
                });
            }
        }
        return false;
    }

    private void showSettings() {
        var intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    private void filter(String text){
        var gameData = stockViewModel.getSortedGames();
        var filteredList = new ArrayList<InnerGameData>();
        for (var item : gameData) {
            if (item.title.toLowerCase(Locale.getDefault())
                    .contains(text.toLowerCase(Locale.getDefault()))) {
                filteredList.add(item);
            }
        }
        if (!filteredList.isEmpty()) {
            stockViewModel.setGameDataList(filteredList);
        }
    }
}