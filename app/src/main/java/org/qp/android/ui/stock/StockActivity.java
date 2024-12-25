package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFile;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_IMAGE_FILE;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_MOD_FILE;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_PATH_FILE;
import static org.qp.android.ui.stock.StockViewModel.EXT_GAME_LIST_NAME;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.anggrayudi.storage.SimpleStorageHelper;
import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.FileUtils;
import com.anggrayudi.storage.file.MimeType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.qp.android.BuildConfig;
import org.qp.android.R;
import org.qp.android.data.db.Game;
import org.qp.android.data.db.GameDao;
import org.qp.android.data.db.GameDatabase;
import org.qp.android.databinding.ActivityStockBinding;
import org.qp.android.dto.stock.RemoteDataList;
import org.qp.android.helpers.utils.ViewUtil;
import org.qp.android.model.repository.RemoteGame;
import org.qp.android.ui.dialogs.StockDialogType;
import org.qp.android.ui.settings.SettingsActivity;
import org.qp.android.ui.settings.SettingsController;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class StockActivity extends AppCompatActivity {

    private static final int READ_EXTERNAL_STORAGE_CODE = 200;
    private static final int MANAGE_EXTERNAL_STORAGE_CODE = 201;
    private static final int POST_NOTIFICATION_CODE = 203;

    public final SimpleDateFormat timeDateRemGamesList = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.ROOT);
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final SimpleStorageHelper storageHelper = new SimpleStorageHelper(this);
    protected ActivityStockBinding activityStockBinding;
    @Inject
    public GameDatabase gameDatabase;
    @Inject
    public GameDao gameDao;
    private StockViewModel stockViewModel;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = (sharedPreferences , key) -> {
        if (key == null) return;
        switch (key) {
            case "binPref" -> stockViewModel.loadGameDataFromDB();
            case "lang" -> {
                if (sharedPreferences.getString("lang", "ru").equals("ru")) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"));
                } else {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
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
    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                // var downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                stockViewModel.postProcessingDownload();
            }
        }
    };
    private NavController navController;
    private ActionMode deleteMode;
    private FloatingActionButton mFAB;
    private ActivityResultLauncher<Intent> rootFolderLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        var splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> {
            switch (SettingsController.newInstance(this).theme) {
                case "auto" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                case "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                case "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            return false;
        });
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        PreferenceManager
                .getDefaultSharedPreferences(getApplication())
                .registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Prevent jumping of the player on devices with cutout
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        activityStockBinding = ActivityStockBinding.inflate(getLayoutInflater());
        stockViewModel = new ViewModelProvider(this).get(StockViewModel.class);
        var searchToolbar = activityStockBinding.stockSearchBar;
        setSupportActionBar(activityStockBinding.stockSearchBar);

        var searchView = activityStockBinding.stockSearchView;
        searchView
                .getEditText()
                .setOnEditorActionListener((v, actionId, event) -> {
                    searchToolbar.setText(searchView.getText());
                    var gameList = stockViewModel.getListGames();
                    var filteredList = new ArrayList<Game>();
                    gameList.forEach(game -> {
                        var title = game.title.toLowerCase(Locale.getDefault());
                        var searchTitle = searchView.getText().toString().toLowerCase(Locale.getDefault());
                        if (title.contains(searchTitle)) {
                            filteredList.add(game);
                        }
                    });
                    if (!filteredList.isEmpty()) {
                        stockViewModel.gameEntriesLiveData.setValue(filteredList);
                    }
                    searchView.hide();
                    return false;
                });
        searchView.setupWithSearchBar(searchToolbar);

        mFAB = activityStockBinding.stockFAB;
        stockViewModel.doIsHideFAB.observe(this, aBoolean -> {
            if (aBoolean) {
                mFAB.hide();
            } else {
                mFAB.show();
            }
        });
        mFAB.setOnClickListener(view -> showDirPickerDialog());

        ViewCompat.setOnApplyWindowInsetsListener(mFAB, (v, windowInsets) -> {
            var insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            var mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            var margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom + margin;
            mlp.rightMargin = insets.right + margin;
            v.setLayoutParams(mlp);

            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            return WindowInsetsCompat.CONSUMED;
        });

        searchView.addTransitionListener((searchView1 , previousState , newState) -> {
            if (newState.name().equalsIgnoreCase("SHOWING")) {
                stockViewModel.doIsHideFAB.setValue(true);
            }
            if (newState.name().equalsIgnoreCase("HIDING")) {
                stockViewModel.doIsHideFAB.setValue(false);
            }
        });

        storageHelper.setOnFileSelected((integer, documentFiles) -> {
            if (documentFiles == null) {
                showErrorDialog("File is not selected");
                return null;
            }

            switch (integer) {
                case CODE_PICK_IMAGE_FILE -> documentFiles.forEach(documentFile -> {
                    switch (documentWrap(documentFile).getExtension()) {
                        case "png" , "jpg" , "jpeg" -> {
                            getContentResolver().takePersistableUriPermission(documentFile.getUri() ,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            stockViewModel.setTempImageFile(documentFile);
                        }
                    }
                });
                case CODE_PICK_PATH_FILE -> documentFiles.forEach(documentFile -> {
                    switch (documentWrap(documentFile).getExtension()) {
                        case "qsp" , "gam" -> stockViewModel.setTempPathFile(documentFile);
                    }
                });
                case CODE_PICK_MOD_FILE -> documentFiles.forEach(documentFile -> {
                    if ("qsp".equals(documentWrap(documentFile).getExtension())) {
                        stockViewModel.setTempModFile(documentFile);
                    }
                });
            }
            return null;
        });

        rootFolderLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult() , result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                var data = result.getData();
                if (data == null) return;
                var uri = data.getData();
                if (uri == null) return;

                getContentResolver().takePersistableUriPermission(uri ,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                var rootFolder = DocumentFileCompat.fromUri(this , uri);
                stockViewModel.showAddDialogFragment(
                        getSupportFragmentManager(),
                        rootFolder
                );
                stockViewModel.outputIntObserver.observe(this, integer -> {
                    if (integer == 1) {
                        stockViewModel.createEntryInDBFromFile(rootFolder);
                        stockViewModel.loadGameDataFromDB();
                    }
                });
            }
        });

        checkCacheRemoteRepo();
        loadPermission();
        stockViewModel.loadGameDataFromDB();

        setContentView(activityStockBinding.getRoot());

        var navFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.stockFragHost);
        if (navFragment != null) {
            navController = navFragment.getNavController();
        }
        if (savedInstanceState == null) {
            checkMigration();
            navController.navigate(R.id.stockViewPagerFragment);
        }

        stockViewModel.emitter.observe(this , eventNavigation -> {
            if (eventNavigation instanceof StockFragmentNavigation.ShowErrorDialog errorDialog) {
                switch (errorDialog.getErrorType()) {
                    case FOLDER_ERROR -> showErrorDialog(getString(R.string.gamesFolderError));
                    case EXCEPTION -> showErrorDialog(getString(R.string.error)
                            + ": " + errorDialog.getErrorMessage());
                }
            }
            if (eventNavigation instanceof StockFragmentNavigation.ShowGameFragment gameFragment) {
                onListItemClick(gameFragment.getPosition());
            }
            if (eventNavigation instanceof StockFragmentNavigation.ShowActionMode) {
                onLongListItemClick();
            }
            if (eventNavigation instanceof StockFragmentNavigation.ShowFilePicker filePicker) {
                showFilePickerActivity(filePicker.getRequestCode() , filePicker.getMimeTypes());
            }
        });

        new AppUpdater(this)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo("l3ger0j" , "Questopia")
                .start();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navController.getCurrentDestination() == null) finish();
                var currDestLabel = navController.getCurrentDestination().getLabel();
                if (Objects.equals(currDestLabel, "StockViewPagerFragment")) {
                    finish();
                } else {
                    stockViewModel.doIsHideFAB.setValue(false);
                    navController.popBackStack();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode ,
                                    int resultCode ,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode , resultCode , data);
        storageHelper.getStorage().onActivityResult(requestCode , resultCode , data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode ,
                                           @NonNull String[] permissions ,
                                           @NonNull int[] grantResults) {
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
            case POST_NOTIFICATION_CODE -> {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Success");
                } else {
                    ViewUtil.showSnackBar(findViewById(android.R.id.content) , "Permission denied to post notification");
                }
            }
        }
        storageHelper.onRequestPermissionsResult(requestCode , permissions , grantResults);
    }

    private void checkMigration() {
        var cache = getExternalCacheDir();
        var listDirsFile = new File(cache , "tempListDir");

        if (listDirsFile.exists()) {
            stockViewModel.showDialogFragment(
                    getSupportFragmentManager(), StockDialogType.MIGRATION_DIALOG, null
            );
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        var value = stockViewModel.currPageNumber.getValue();
        if (value == null) {
            navController.popBackStack();
            return true;
        }

        if (value == 0) {
            stockViewModel.doIsHideFAB.setValue(false);
            navController.popBackStack();
        }

        if (value == 1) {
            navController.popBackStack();
        }

        return true;
    }

    private boolean isCacheFileHasExpired() {
        var cache = getExternalCacheDir();
        if (cache == null) return true;
        var listNames = cache.list();
        if (listNames == null) return true;

        for (var name : listNames) {
            if (!name.equalsIgnoreCase(EXT_GAME_LIST_NAME)) {
                try {
                    var cacheDate = timeDateRemGamesList.parse(name);
                    if (cacheDate == null) return true;

                    var isDateCurr = cacheDate.equals(Calendar.getInstance().getTime());
                    var isDateBefore = cacheDate.before(Calendar.getInstance().getTime());

                    return isDateCurr || isDateBefore;
                } catch (ParseException e) {
                    showErrorDialog(getString(R.string.error)
                            + ": " + e.getMessage());
                }
            }
        }

        return true;
    }

    private String getCurrentTimestamp() {
        var calendarInstance = Calendar.getInstance();
        calendarInstance.add(Calendar.DATE, 3);
        return timeDateRemGamesList.format(calendarInstance.getTime());
    }

    private void checkCacheRemoteRepo() {
        if (!isCacheFileHasExpired()) return;

        var remoteGame = new RemoteGame();
        var cache = getApplication().getExternalCacheDir();
        if (cache == null) return;
        var listFiles = cache.listFiles();
        if (listFiles == null) return;

        for (var item : listFiles) {
            if (item.getName().isEmpty()) continue;
            if (!item.getName().equalsIgnoreCase(EXT_GAME_LIST_NAME)) {
                FileUtils.forceDelete(item);
            }
        }

        remoteGame.getRemoteGameData(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull Response<ResponseBody> response) {
                var mapper = new XmlMapper();
                try (var body = response.body()){
                    if (body == null) return;
                    var string = body.string();
                    var value = mapper.readValue(string, RemoteDataList.class);
                    var remoteGamesList = findOrCreateFile(
                            StockActivity.this,
                            cache,
                            getCurrentTimestamp(),
                            MimeType.TEXT
                    );
                    mapper.writeValue(remoteGamesList, value.game);
                } catch (IOException e) {
                    showErrorDialog(getString(R.string.error)
                            + ": " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call,
                                  @NonNull Throwable throwable) {
                showErrorDialog(getString(R.string.error)
                        + ": " + throwable.getMessage());
            }
        });
    }

    private void loadPermission() {
        ActivityCompat.registerReceiver(
                this,
                downloadReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            var postNotification = ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS);
            if (postNotification != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{ Manifest.permission.POST_NOTIFICATIONS },
                        POST_NOTIFICATION_CODE
                );
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    var uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                    var intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                    startActivity(intent);
                } catch (Exception ex){
                    var intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else {
            var readStorage = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            var writeStorage = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (readStorage != PackageManager.PERMISSION_GRANTED && writeStorage != PackageManager.PERMISSION_GRANTED) {
                var requestPerm = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE ,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this , requestPerm , READ_EXTERNAL_STORAGE_CODE);
            }
        }
    }

    public void showErrorDialog(String errorMessage) {
        stockViewModel.showDialogFragment(getSupportFragmentManager() ,
                StockDialogType.ERROR_DIALOG , errorMessage);
    }

    public void showFilePickerActivity(int requestCode , String[] mimeTypes) {
        storageHelper.openFilePicker(requestCode , false , mimeTypes);
    }

    public void showDirPickerDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var intentQ = new Intent();
            var sm = (StorageManager) getSystemService(Activity.STORAGE_SERVICE);
            var sv = sm.getPrimaryStorageVolume();
            intentQ = sv.createOpenDocumentTreeIntent();
            intentQ.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intentQ.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intentQ.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            rootFolderLauncher.launch(intentQ);
        } else {
            var intentLQ = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            var flags = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION;
            intentLQ.addFlags(flags);
            rootFolderLauncher.launch(intentLQ);
        }
    }

    public void onListItemClick(int position) {
        if (stockViewModel.isEnableDeleteMode) {
            var currGamesMapValues = stockViewModel.getGamesMap().values();
            for (var gameData : currGamesMapValues) {
                if (!stockViewModel.isGameInstalled()) continue;
                stockViewModel.currInstalledGamesList.add(gameData);
            }
        } else {
            stockViewModel.gameEntriesLiveData.observe(this , gameEntries -> {
                if (!gameEntries.isEmpty() && gameEntries.size() > position) {
                    stockViewModel.setCurrGameData(gameEntries.get(position));
                }
            });

            navController.navigate(R.id.action_stockViewPagerFragment_to_stockGameFragment);
            stockViewModel.doIsHideFAB.setValue(true);
        }
    }

    public void onLongListItemClick() {
        if (stockViewModel.isEnableDeleteMode) return;

        var pageNumber = stockViewModel.currPageNumber.getValue();
        if (pageNumber == null) return;
        if (pageNumber == 1) return;

        var callback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode , Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_delete , menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode , Menu menu) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().hide();
                }
                stockViewModel.currInstalledGamesList = stockViewModel.getListGames();
                stockViewModel.isEnableDeleteMode = true;
                return true;
            }

            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onActionItemClicked(ActionMode mode , MenuItem item) {
                var gamesSelList = stockViewModel.selGameEntriesList;
                var gameEntriesList = stockViewModel.currInstalledGamesList;

                switch (item.getItemId()) {
                    case R.id.delete_game -> {
                        stockViewModel.showDialogFragment(
                                getSupportFragmentManager(),
                                StockDialogType.DELETE_DIALOG,
                                String.valueOf(gamesSelList.size())
                        );
                        stockViewModel.outputIntObserver.observe(StockActivity.this, integer -> {
                            if (integer == 1) {
                                stockViewModel.removeEntryAndDirFromDB(gamesSelList);
                                deleteMode.finish();
                            } else {
                                stockViewModel.removeEntryFromDB(gamesSelList);
                                deleteMode.finish();
                            }
                        });
                    }
                    case R.id.select_all -> {
                        if (gamesSelList.size() == gameEntriesList.size()) {
                            gamesSelList.clear();
                            stockViewModel.doOnUnselectAllElements();
                        } else {
                            gamesSelList.clear();
                            gamesSelList.addAll(gameEntriesList);
                            stockViewModel.doOnSelectAllElements();
                        }
                    }
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().show();
                }

                stockViewModel.doOnUnselectAllElements();
                deleteMode = null;
                stockViewModel.isEnableDeleteMode = false;
                stockViewModel.selGameEntriesList.clear();
                stockViewModel.currInstalledGamesList.clear();
                mFAB.show();
            }
        };

        mFAB.hide();
        deleteMode = startSupportActionMode(callback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceManager
                .getDefaultSharedPreferences(getApplication())
                .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);

    }

    @Override
    public void onResume() {
        super.onResume();

        loadPermission();

        stockViewModel.doIsHideFAB.setValue(false);
        navController.navigate(R.id.stockViewPagerFragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        var inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stock, menu);
        return true;
    }

    @Override
    @SuppressLint("NonConstantResourceId")
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_options) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return false;
    }

}
