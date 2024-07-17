package org.qp.android.ui.stock;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.JsonUtil.jsonToObject;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_IMAGE_FILE;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_MOD_FILE;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_PATH_FILE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anggrayudi.storage.SimpleStorageHelper;
import com.anggrayudi.storage.file.DocumentFileCompat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.qp.android.BuildConfig;
import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.databinding.ActivityStockBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.dto.stock.RemoteGameData;
import org.qp.android.helpers.utils.ViewUtil;
import org.qp.android.ui.dialogs.StockDialogType;
import org.qp.android.ui.settings.SettingsActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class StockActivity extends AppCompatActivity {

    private static final int READ_EXTERNAL_STORAGE_CODE = 200;
    private static final int MANAGE_EXTERNAL_STORAGE_CODE = 201;
    private static final int POST_NOTIFICATION_CODE = 203;

    private final String TAG = this.getClass().getSimpleName();

    private StockViewModel stockViewModel;

    private NavController navController;

    private ActionMode deleteMode;
    protected ActivityStockBinding activityStockBinding;
    private boolean isEnableDeleteMode = false;
    private FloatingActionButton mFAB;
    private RecyclerView mRecyclerView;
    private ArrayList<GameData> tempList;
    private final ArrayList<GameData> selectList = new ArrayList<>();

    private ActivityResultLauncher<Intent> rootFolderLauncher;
    private final SimpleStorageHelper storageHelper = new SimpleStorageHelper(this);

    private File listDirsFile;

    SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = (sharedPreferences , key) -> {
        if (key == null) return;
        switch (key) {
            case "binPref" -> stockViewModel.refreshGameData();
            case "lang" -> {
                if (sharedPreferences.getString("lang", "ru").equals("ru")) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"));
                } else {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
                }
            }
        }
    };

    public void setRecyclerView(RecyclerView mRecyclerView) {
        this.mRecyclerView = mRecyclerView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        var splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> false);
        super.onCreate(savedInstanceState);

        PreferenceManager
                .getDefaultSharedPreferences(getApplication())
                .registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Prevent jumping of the player on devices with cutout
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
        }

        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        activityStockBinding = ActivityStockBinding.inflate(getLayoutInflater());
        stockViewModel = new ViewModelProvider(this).get(StockViewModel.class);
        stockViewModel.activityObserver.setValue(this);

        mFAB = activityStockBinding.stockFAB;
        stockViewModel.doIsHideFAB.observe(this, aBoolean -> {
            if (aBoolean) {
                mFAB.hide();
            } else {
                mFAB.show();
            }
        });
        mFAB.setOnClickListener(view -> showDirPickerDialog());

        storageHelper.setOnFileSelected((integer , documentFiles) -> {
            var boxDocumentFiles = Optional.ofNullable(documentFiles);
            if (boxDocumentFiles.isEmpty()) {
                showErrorDialog("File is not selected");
            } else {
                var unBoxDocFiles = boxDocumentFiles.get();
                switch (integer) {
                    case CODE_PICK_IMAGE_FILE -> unBoxDocFiles.forEach(documentFile -> {
                        switch (documentWrap(documentFile).getExtension()) {
                            case "png" , "jpg" , "jpeg" -> {
                                getContentResolver().takePersistableUriPermission(documentFile.getUri() ,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                stockViewModel.setTempImageFile(documentFile);
                            }
                        }
                    });
                    case CODE_PICK_PATH_FILE -> unBoxDocFiles.forEach(documentFile -> {
                        switch (documentWrap(documentFile).getExtension()) {
                            case "qsp", "gam" ->
                                    stockViewModel.setTempPathFile(documentFile);
                        }
                    });
                    case CODE_PICK_MOD_FILE -> unBoxDocFiles.forEach(documentFile -> {
                        if ("qsp".equals(documentWrap(documentFile).getExtension())) {
                            stockViewModel.setTempModFile(documentFile);
                        }
                    });
                }
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
                        var application = (QuestPlayerApplication) getApplication();
                        if (application != null) application.setCurrentGameDir(rootFolder);
                    }
                });
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this , Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this , new String[]{Manifest.permission.POST_NOTIFICATIONS} , POST_NOTIFICATION_CODE);
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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                var permission = new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE , Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permission , READ_EXTERNAL_STORAGE_CODE);
            }
        }

        loadSettings();

        Log.i(TAG , "Stock Activity created");

        setContentView(activityStockBinding.getRoot());

        var navFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.stockFragHost);
        if (navFragment != null) {
            navController = navFragment.getNavController();
        }
        if (savedInstanceState == null) {
            navController.navigate(R.id.stockViewPagerFragment);
        }

        stockViewModel.emitter.observe(this , eventNavigation -> {
            if (eventNavigation instanceof StockFragmentNavigation.ShowErrorDialog errorDialog) {
                switch (errorDialog.getErrorType()) {
                    case FOLDER_ERROR ->
                            showErrorDialog(getString(R.string.gamesFolderError));
                    case EXCEPTION ->
                            showErrorDialog(getString(R.string.error)
                                    + ": " + errorDialog.getErrorMessage());
                }
            } else if (eventNavigation instanceof StockFragmentNavigation.ShowGameFragment gameFragment) {
                onListItemClick(gameFragment.getPosition(), gameFragment.getPageNum());
            } else if (eventNavigation instanceof StockFragmentNavigation.ShowActionMode) {
                onLongListItemClick();
            } else if (eventNavigation instanceof StockFragmentNavigation.ShowFilePicker filePicker) {
                showFilePickerActivity(filePicker.getRequestCode() , filePicker.getMimeTypes());
            }
        });

        new AppUpdater(this)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo("l3ger0j" , "Questopia")
                .start();

        getOnBackPressedDispatcher().addCallback(this , new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                }
                if (navController.getCurrentDestination() != null) {
                    if (Objects.equals(navController.getCurrentDestination().getLabel(),
                            "StockViewPagerFragment")) {
                        finish();
                    } else {
                        stockViewModel.doIsHideFAB.setValue(false);
                        navController.popBackStack();
                    }
                } else {
                    finish();
                }
            }
        });
    }

    public void restoreListDirsFromFile() {
        try {
            var ref = new TypeReference<HashMap<String , String>>() {};
            var mapFiles = jsonToObject(listDirsFile , ref);
            var listFile = new ArrayList<DocumentFile>();
            for (var value : mapFiles.values()) {
                var uri = Uri.parse(value);
                var file = DocumentFileCompat.fromUri(this , uri);
                listFile.add(file);
            }
            stockViewModel.setListGamesDir(listFile);
            stockViewModel.refreshGameData();
        } catch (IOException e) {
            Log.e(TAG , "Error: ", e);
        }
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

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        stockViewModel.doIsHideFAB.setValue(false);
        navController.popBackStack();
        return true;
    }

    private void loadSettings() {
        var cache = getExternalCacheDir();
        listDirsFile = new File(cache , "gamesListDir");

        if (listDirsFile.exists()) {
            restoreListDirsFromFile();
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

    public void onListItemClick(int position, int numPage) {
        if (!isEnableDeleteMode) {
            Observer<List<GameData>> localDataObserver = localGameData -> {
                if (!localGameData.isEmpty() && localGameData.size() > position) {
                    stockViewModel.setCurrGameData(localGameData.get(position));
                }
            };
            Observer<List<RemoteGameData>> remoteDataObserver = remoteGameData -> {
                if (!remoteGameData.isEmpty() && remoteGameData.size() > position) {
                    stockViewModel.setCurrGameData(new GameData(remoteGameData.get(position)));
                }
            };

            if (numPage == 0) {
                stockViewModel.getRemoteDataList().removeObserver(remoteDataObserver);
                stockViewModel.getGameDataList().observe(this, localDataObserver);
            } else {
                stockViewModel.getGameDataList().removeObserver(localDataObserver);
                stockViewModel.getRemoteDataList().observe(this, remoteDataObserver);
            }

            navController.navigate(R.id.stockGameFragment);
            stockViewModel.doIsHideFAB.setValue(true);
        } else {
            var mViewHolder = mRecyclerView.findViewHolderForAdapterPosition(position);
            if (mViewHolder == null) return;

            var adapterPosition = mViewHolder.getAdapterPosition();
            if (adapterPosition == NO_POSITION) return;
            if (adapterPosition < 0 || adapterPosition >= tempList.size()) return;

            var currGamesMapValues = stockViewModel.getGamesMap().values();
            for (var gameData : currGamesMapValues) {
                if (!gameData.isFileInstalled()) continue;
                tempList.add(gameData);
            }

            var gameData = tempList.get(adapterPosition);
            if (selectList.isEmpty() || !selectList.contains(gameData)) {
                selectList.add(gameData);
                var cardView = (CardView) mViewHolder.itemView.findViewWithTag("gameCardView");
                cardView.setCardBackgroundColor(Color.LTGRAY);
            } else {
                selectList.remove(gameData);
                var cardView = (CardView) mViewHolder.itemView.findViewWithTag("gameCardView");
                cardView.setCardBackgroundColor(Color.DKGRAY);
            }
        }
    }

    public void onLongListItemClick() {
        if (isEnableDeleteMode) return;

        var callback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode , Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_delete , menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode , Menu menu) {
                tempList = stockViewModel.getSortedGames();
                isEnableDeleteMode = true;
                return true;
            }

            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onActionItemClicked(ActionMode mode , MenuItem item) {
                int itemId = item.getItemId();
                switch (itemId) {
                    case R.id.delete_game -> {
                        stockViewModel.showDialogFragment(
                                getSupportFragmentManager(),
                                StockDialogType.DELETE_DIALOG,
                                String.valueOf(selectList.size())
                        );
                        stockViewModel.outputIntObserver.observe(StockActivity.this, integer -> {
                            if (integer == 1) {
                                for (var data : selectList) {
                                    stockViewModel.delEntryDirFromList(tempList, data, listDirsFile);
                                }
                                deleteMode.finish();
                            } else {
                                for (var data : selectList) {
                                    stockViewModel.delEntryFromList(tempList, data, listDirsFile);
                                }
                                deleteMode.finish();
                            }
                        });
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
                deleteMode = null;
                isEnableDeleteMode = false;
                tempList.clear();
                selectList.clear();
                mFAB.show();
            }
        };

        mFAB.hide();
        deleteMode = startSupportActionMode(callback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stockViewModel.saveListDirsIntoFile(listDirsFile);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceManager
                .getDefaultSharedPreferences(getApplication())
                .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);

        Log.i(TAG , "Stock Activity destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        stockViewModel.refreshGamesDirs();
        stockViewModel.doIsHideFAB.setValue(false);
        navController.navigate(R.id.stockViewPagerFragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        var inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stock , menu);
        return true;
    }

    @Override
    @SuppressLint("NonConstantResourceId")
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_options -> {
                startActivity(new Intent(this , SettingsActivity.class));
                return true;
            }
            case R.id.action_search ->
                    Optional.ofNullable((SearchView) item.getActionView()).ifPresent(searchView ->
                            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                                @Override
                                public boolean onQueryTextSubmit(String query) {
                                    return false;
                                }

                                @Override
                                public boolean onQueryTextChange(String newText) {
                                    var gameDataList = stockViewModel.getSortedGames();
                                    var filteredList = new ArrayList<GameData>();
                                    gameDataList.forEach(gameData -> {
                                        if (gameData.title.toLowerCase(Locale.getDefault())
                                                .contains(newText.toLowerCase(Locale.getDefault()))) {
                                            filteredList.add(gameData);
                                        }
                                    });
                                    if (!filteredList.isEmpty()) {
                                        stockViewModel.setValueGameDataList(filteredList);
                                    }
                                    return true;
                                }
                            }));
        }
        return false;
    }

}
