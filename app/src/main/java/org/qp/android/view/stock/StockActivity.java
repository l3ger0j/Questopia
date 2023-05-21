package org.qp.android.view.stock;

import static org.qp.android.utils.FileUtil.deleteDirectory;
import static org.qp.android.utils.FileUtil.findFileRecursively;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ActionMode;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.anggrayudi.storage.FileWrapper;
import com.anggrayudi.storage.SimpleStorageHelper;
import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.DocumentFileType;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.picker.prettyfilepicker.PrettyFilePicker;

import org.qp.android.R;
import org.qp.android.databinding.ActivityStockBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.utils.ViewUtil;
import org.qp.android.view.adapters.AutoScrollRunnable;
import org.qp.android.view.settings.SettingsActivity;
import org.qp.android.view.settings.SettingsController;
import org.qp.android.view.stock.fragment.StockAdapterFragment;
import org.qp.android.view.stock.fragment.StockPatternFragment;
import org.qp.android.view.stock.fragment.dialogs.StockDialogFrags;
import org.qp.android.view.stock.fragment.dialogs.StockDialogType;
import org.qp.android.view.stock.fragment.dialogs.StockPatternDialogFrags;
import org.qp.android.viewModel.StockViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class StockActivity extends AppCompatActivity implements StockPatternDialogFrags.StockPatternDialogList, StockPatternFragment.StockPatternFragmentList {
    private static final int READ_EXTERNAL_STORAGE_CODE = 200;
    private static final int MANAGE_EXTERNAL_STORAGE_CODE = 201;
    private static final int POST_NOTIFICATION_CODE = 202;
    private final String TAG = this.getClass().getSimpleName();
    private HashMap<String, GameData> gamesMap = new HashMap<>();
    public SettingsController settingsController;
    private StockViewModel stockViewModel;

    private NavController navController;

    private ActionMode actionMode;
    protected ActivityStockBinding activityStockBinding;
    private boolean isEnable = false;
    private FloatingActionButton mFAB;
    private RecyclerView mRecyclerView;
    private ArrayList<GameData> tempList;
    private final ArrayList<GameData> selectList = new ArrayList<>();

    private final SimpleStorageHelper storageHelper = new SimpleStorageHelper(this);

    public void setRecyclerView(RecyclerView mRecyclerView) {
        this.mRecyclerView = mRecyclerView;
    }

    private AutoScrollRunnable autoScrollRunnable;
    private ViewPager2 bannerViewPager;
    private DownloadManager mgr = null;
    
    BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            var gameData = stockViewModel.getTempGameData();
            var downloadDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            var tempDownloadedFile = findFileRecursively(downloadDir ,
                    gameData.title+"."+gameData.fileExt);
            if (tempDownloadedFile != null) {
                var downloadedFile = DocumentFileCompat.fromFile(
                        context,
                        tempDownloadedFile,
                        DocumentFileType.FILE,
                        true
                );

                stockViewModel.installGame(downloadedFile, stockViewModel.getTempGameData());
                ViewUtil.showSnackBar(findViewById(android.R.id.content) ,
                        getString(R.string.textInstallNotify)
                                .replace("-GAMENAME-" , gameData.title));
            } else {
                ViewUtil.showSnackBar(findViewById(android.R.id.content) ,
                        getString(R.string.installError)
                                .replace("-GAMENAME-" , gameData.title));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        activityStockBinding = ActivityStockBinding.inflate(getLayoutInflater());

        bannerViewPager = activityStockBinding.bannerView;
        var fragmentAdapter = new StockAdapterFragment(this);
        bannerViewPager.setAdapter(fragmentAdapter);
        autoScrollRunnable = new AutoScrollRunnable(bannerViewPager, 3000, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        MANAGE_EXTERNAL_STORAGE_CODE);
            }
        }

        mgr = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        stockViewModel = new ViewModelProvider(this).get(StockViewModel.class);
        activityStockBinding.setStockVM(stockViewModel);
        stockViewModel.activityObservableField.set(this);
        gamesMap = stockViewModel.getGamesMap();

        mFAB = activityStockBinding.stockFAB;
        mFAB.setOnClickListener(v -> stockViewModel.showDialogInstall());

        storageHelper.setOnFileSelected((integer , documentFiles) -> {
            if (documentFiles != null) {
                for (var file : documentFiles) {
                    var document = new FileWrapper.Document(file);
                    switch (document.getExtension()) {
                        case "png":
                        case "jpg":
                        case "jpeg":
                            getContentResolver().takePersistableUriPermission(document.getUri(),
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            stockViewModel.setTempImageFile(document.getDocumentFile());
                            break;
                        case "qsp":
                            stockViewModel.setTempPathFile(document.getDocumentFile());
                            stockViewModel.setTempModFile(document.getDocumentFile());
                            break;
                    }
                }
            } else {
                showErrorDialog("Archive or file is not selected");
            }
            return null;
        });

        storageHelper.setOnFolderSelected((integer , documentFile) -> {
            stockViewModel.setTempInstallDir(documentFile);
            stockViewModel.isSelectFolder.set(true);
            return null;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, POST_NOTIFICATION_CODE);
            }
        }

        if (stockViewModel.getSettingsController().isUseNewFilePicker) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, MANAGE_EXTERNAL_STORAGE_CODE);
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_CODE);
                }
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode , @NonNull String[] permissions , @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode , permissions , grantResults);
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Success");
                } else {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Permission denied to read your External storage");
                }
                break;
            case MANAGE_EXTERNAL_STORAGE_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Success");
                } else {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Permission denied to manage your External storage");
                }
                break;
            case POST_NOTIFICATION_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Success");
                } else {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Permission denied to post notification");
                }
                break;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        navController.popBackStack();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        if (navController.getCurrentDestination() != null) {
            if (Objects.equals(navController.getCurrentDestination().getLabel()
                    , "StockRecyclerFragment")) {
                super.onBackPressed();
            } else {
                navController.popBackStack();
            }
        } else {
            super.onBackPressed();
        }
    }

    private void loadSettings() {
        settingsController = stockViewModel.getSettingsController();
        stockViewModel.setController(settingsController);
        if (settingsController.binaryPrefixes <= 1000) {
            stockViewModel.refreshGames();
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

    public void showEditDialogFragment (DialogFragment editDialog) {
        var fragment = getSupportFragmentManager()
                .findFragmentByTag("editDialogFragment");
        if (fragment != null && fragment.isAdded()) {
            fragment.onDestroy();
        } else {
            editDialog.show(getSupportFragmentManager() , "editDialogFragment");
        }
    }

    public void showInstallDialogFragment (DialogFragment installDialog) {
        var fragment = getSupportFragmentManager()
                .findFragmentByTag("installDialogFragment");
        if (fragment != null && fragment.isAdded()) {
            fragment.onDestroy();
        } else {
            installDialog.show(getSupportFragmentManager() , "installDialogFragment");
        }
    }

    public void showSelectDialogFragment (DialogFragment selectDialog) {
        var fragment = getSupportFragmentManager()
                .findFragmentByTag("selectDialogFragment");
        if (fragment != null && fragment.isAdded()) {
            fragment.onDestroy();
        } else {
            selectDialog.show(getSupportFragmentManager() , "selectDialogFragment");
        }
    }

    public void showFilePickerActivity(String[] mimeTypes) {
        storageHelper.openFilePicker(mimeTypes);
    }

    public void startGameActivity(Intent intent) {
        startActivity(intent);
    }

    public void showFilePickerDialog (String[] mimeTypes) {
        new PrettyFilePicker(
                this,
                "Select file",
                true,
                mimeTypes).runFilePicker(data -> {
            // stockViewModel.setTempInstallFile((DocumentFile) data);
            return null;
        });
    }

    public void showDirPickerDialog () {
        storageHelper.openFolderPicker();
    }

    public void onItemClick(int position) {
        if (isEnable) {
            for (GameData gameData : gamesMap.values()) {
                if (!gameData.isInstalled()) continue;
                tempList.add(gameData);
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
            }
        }
    }

    @Override
    public void onDialogDestroy(DialogFragment dialog) {
        stockViewModel.isShowDialog.set(false);
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
    public void onClickEditButton() {
        stockViewModel.showDialogEdit();
    }

    @Override
    public void onClickPlayButton() {
        stockViewModel.playGame();
    }

    @Override
    public void onClickDownloadButton() {
        var gameData = stockViewModel.getTempGameData();
        var uri = Uri.parse(gameData.fileUrl);
        var files = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .mkdirs();
        if (!files) {
            showErrorDialog("Files not found");
        }
        mgr.enqueue(new DownloadManager.Request(uri)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
                        | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(gameData.title+"."+gameData.fileExt)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS ,
                        gameData.title+"."+gameData.fileExt));
    }

    @Override
    public void onDialogListClick(DialogFragment dialog, int which) {
        if (Objects.equals(dialog.getTag() , "selectDialogFragment")) {
            stockViewModel.outputIntObserver.setValue(which);
        }
    }

    public void onLongItemClick() {
        if (!isEnable) {
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

                @Override
                public boolean onActionItemClicked(ActionMode mode , MenuItem item) {
                    int itemId = item.getItemId();
                    if (itemId == R.id.delete_game) {
                        for (GameData data : selectList) {
                            tempList.remove(data);
                            try {
                                deleteDirectory(data.gameDir);
                            } catch (NullPointerException e) {
                                showErrorDialog("Error: "+"\n"+e);
                            }
                            stockViewModel.refreshGames();
                        }
                        actionMode.finish();
                    } else if (itemId == R.id.select_all) {
                        if(selectList.size() == tempList.size()) {
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
    }

    @Override
    public void onScrolled(RecyclerView recyclerView , int dx , int dy) {
        if (dy > 0 || dy < 0 && mFAB.isShown()) mFAB.hide();
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView , int newState) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) mFAB.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onComplete);
        Log.i(TAG , "Stock Activity destroyed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        bannerViewPager.removeCallbacks(autoScrollRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSettings();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        stockViewModel.refreshIntGameDirectory();
        navController.navigate(R.id.stockRecyclerFragment);
        bannerViewPager.postDelayed(autoScrollRunnable, 3000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        var inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stock, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        var refresh = menu.findItem(R.id.action_refresh);
        refresh.setVisible(stockViewModel.isDownloadPlugin());
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
        } else if (itemId == R.id.action_refresh) {
            stockViewModel.startDownloadPlugin();
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
        var filteredList = new ArrayList<GameData>();
        for (var item : gameData) {
            if (item.title.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        if (!filteredList.isEmpty()) {
            stockViewModel.setGameDataList(filteredList);
        }
    }
}