package org.qp.android.view.stock;

import static org.qp.android.utils.DirUtil.doesDirectoryContainGameFiles;
import static org.qp.android.utils.FileUtil.deleteDirectory;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ActionMode;
import androidx.cardview.widget.CardView;
import androidx.core.os.LocaleListCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.zhpan.bannerview.BannerViewPager;
import com.zhpan.bannerview.BaseBannerAdapter;
import com.zhpan.bannerview.BaseViewHolder;

import org.qp.android.R;
import org.qp.android.databinding.ActivityStockBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.view.settings.SettingsActivity;
import org.qp.android.view.settings.SettingsController;
import org.qp.android.view.stock.fragment.dialogs.StockDialogFrags;
import org.qp.android.view.stock.fragment.dialogs.StockDialogType;
import org.qp.android.view.stock.fragment.dialogs.StockPatternDialogFrags;
import org.qp.android.view.stock.fragment.StockGameFragment;
import org.qp.android.view.stock.fragment.StockPatternFragment;
import org.qp.android.view.stock.fragment.StockRecyclerFragment;
import org.qp.android.viewModel.ActivityStock;
import org.qp.android.viewModel.FragmentStock;
import org.qp.android.viewModel.FragmentStockGame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

public class StockActivity extends AppCompatActivity implements StockPatternDialogFrags.StockPatternDialogList, StockPatternFragment.StockPatternFragmentList {
    private final String TAG = this.getClass().getSimpleName();

    private HashMap<String, GameData> gamesMap = new HashMap<>();

    public SettingsController settingsController;
    private ActivityStock activityStock;
    private FragmentStock localStockViewModel;

    private ActionMode actionMode;
    protected ActivityStockBinding activityStockBinding;
    public ActivityResultLauncher<Intent>
            resultInstallLauncher, resultInstallDir, resultGetImageLauncher, resultSetPath, resultSetMod;

    private boolean isEnable = false;

    private FloatingActionButton mFAB;
    private RecyclerView mRecyclerView;
    private ArrayList<GameData> tempList;
    private final ArrayList<GameData> selectList = new ArrayList<>();

    public String getGameIdByPosition(int position) {
        localStockViewModel.getGameData().observe(this, gameDataArrayList -> {
            if (!gameDataArrayList.isEmpty() && gameDataArrayList.size() > position) {
                activityStock.setTempGameData(gameDataArrayList.get(position));
            }
        });
        return activityStock.getTempGameData().id;
    }

    @NonNull
    public ArrayList<GameData> getSortedGames() {
        var unsortedGameData = gamesMap.values();
        var gameData = new ArrayList<>(unsortedGameData);

        if (gameData.size() < 2) {
            return gameData;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Collections.sort(gameData , Comparator.comparing(game -> game.title.toLowerCase()));
        } else {
            Collections.sort(gameData , (first, second) -> first.title.toLowerCase()
                    .compareTo(second.title.toLowerCase()));
        }

        return gameData;
    }

    public void setRecyclerView(RecyclerView mRecyclerView) {
        this.mRecyclerView = mRecyclerView;
    }

    public void setRecyclerAdapter () {
        var gameData = getSortedGames();
        var localGameData = new ArrayList<GameData>();

        for (GameData data : gameData) {
            if (data.isInstalled()) {
                localGameData.add(data);
            }
        }

        localStockViewModel.setGameDataArrayList(localGameData);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityStockBinding = ActivityStockBinding.inflate(getLayoutInflater());
        mFAB = activityStockBinding.stockFAB;
        localStockViewModel =
                new ViewModelProvider(this).get(FragmentStock.class);
        ArrayList<Integer> list = new ArrayList<>();
        list.add(R.drawable.banner_1);
        list.add(R.drawable.banner_0);
        BannerViewPager<Integer> bannerViewPager;
        bannerViewPager = activityStockBinding.bannerView;
        bannerViewPager.registerLifecycleObserver(getLifecycle()).setAdapter(new BaseBannerAdapter<>() {
            @Override
            protected void bindData(BaseViewHolder<Integer> holder , Integer data , int position , int pageSize) {
                holder.setImageResource(R.id.banner_image , data);
            }

            @Override
            public int getLayoutId(int viewType) {
                return R.layout.list_item_banner;
            }
        }).create(list);
        bannerViewPager.setOnPageClickListener((clickedView , position) -> {
            switch (position) {
                case 0:
                    var intentImg0 = new Intent(Intent.ACTION_VIEW, Uri
                            .parse("https://t.me/joinchat/AAAAAFgqAMXq0SA34umFbQ"));
                    intentImg0.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intentImg0);
                    break;
                case 1:
                    var intentImg1 = new Intent(Intent.ACTION_VIEW, Uri
                            .parse("https://schoollife.fludilka.su/viewtopic.php?id=828#p63540"));
                    intentImg1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intentImg1);
                    break;
            }
        });
        localStockViewModel.activityObservableField.set(this);
        if (mRecyclerView != null) {
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView , int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE)
                    {
                        mFAB.show();
                    }
                    super.onScrollStateChanged(recyclerView, newState);
                }

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView , int dx , int dy) {
                    if (dy > 0 ||dy<0 && mFAB.isShown())
                    {
                        mFAB.hide();
                    }
                }
            });
        }

        activityStock = new ViewModelProvider(this).get(ActivityStock.class);
        activityStockBinding.setStockVM(activityStock);
        activityStock.activityObservableField.set(this);
        gamesMap = activityStock.getGamesMap();

        if (savedInstanceState == null) {
            var stockFragment = new StockRecyclerFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(activityStockBinding.stockFragContainer.getId(),
                            stockFragment, "stockRecyclerFragment")
                    .commit();
        }

        resultSetPath = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri uri;
                    DocumentFile file;
                    if (result.getResultCode() == RESULT_OK) {
                        if ((uri = Objects.requireNonNull(result.getData()).getData()) == null) {
                            showErrorDialog("Archive or file is not selected");
                        }
                        file = DocumentFile.fromSingleUri(this, Objects.requireNonNull(uri));
                        assert file != null;
                        activityStock.setTempPathFile(file);
                    }
                }
        );

        resultSetMod = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri uri;
                    DocumentFile file;
                    if (result.getResultCode() == RESULT_OK) {
                        if ((uri = Objects.requireNonNull(result.getData()).getData()) == null) {
                            showErrorDialog("Archive or file is not selected");
                        }
                        file = DocumentFile.fromSingleUri(this, Objects.requireNonNull(uri));
                        assert file != null;
                        activityStock.setTempModFile(file);
                    }
                }
        );

        resultInstallLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri uri;
                    DocumentFile file;
                    if (result.getResultCode() == RESULT_OK) {
                        if ((uri = Objects.requireNonNull(result.getData()).getData()) == null) {
                            showErrorDialog("Archive or file is not selected");
                        }
                        file = DocumentFile.fromSingleUri(this, Objects.requireNonNull(uri));
                        assert file != null;
                        activityStock.setTempInstallFile(file);
                        activityStock.isSelectArchive.set(true);
                    }
                }
        );

        resultInstallDir = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri uri;
                    DocumentFile file;
                    if (result.getResultCode() == RESULT_OK) {
                        if ((uri = Objects.requireNonNull(result.getData()).getData()) == null) {
                            showErrorDialog("Archive or file is not selected");
                        }
                        file = DocumentFile.fromTreeUri(this, Objects.requireNonNull(uri));
                        assert file != null;
                        activityStock.setTempInstallDir(file);
                        activityStock.isSelectFolder.set(true);
                    }
                }
        );

        resultGetImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri uri;
                    DocumentFile file;
                    if (result.getResultCode() == RESULT_OK) {
                        if ((uri = Objects.requireNonNull(result.getData()).getData()) == null) {
                            showErrorDialog("Archive or file is not selected");
                        }
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        assert uri != null;
                        file = DocumentFile.fromSingleUri(this, uri);
                        assert file != null;
                        activityStock.setTempImageFile(file);
                    }
                }
        );

        loadSettings();

        Log.i(TAG,"Stock Activity created");

        setContentView(activityStockBinding.getRoot());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode , @NonNull String[] permissions , @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode , permissions , grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Success");
            } else {
                showErrorDialog("Permission denied to read your External storage");
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        var stockFragment = new StockRecyclerFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.stockFragContainer, stockFragment, "stockRecyclerFragment")
                .commit();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        var manager = getSupportFragmentManager();
        var stockFragment = new StockRecyclerFragment();
        if (manager.findFragmentByTag("stockRecyclerFragment") != null) {
            super.onBackPressed();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.stockFragContainer , stockFragment , "stockRecyclerFragment")
                    .commit();
        }
    }

    private void loadSettings() {
        settingsController = SettingsController.newInstance().loadSettings(this);
        activityStock.setController(settingsController);
        if (settingsController.binaryPrefixes <= 1000) {
            activityStock.refreshGames();
        }
        if (settingsController.language.equals("ru")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"));
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
        }
    }

    public void showErrorDialog(String errorMessage) {
        var dialogFragments = new StockDialogFrags();
        dialogFragments.setDialogType(StockDialogType.ERROR_DIALOG);
        dialogFragments.setMessage(errorMessage);
        dialogFragments.show(getSupportFragmentManager(), "errorDialogFragment");
    }

    public void showEditDialogFragment (DialogFragment editDialog) {
        editDialog.show(getSupportFragmentManager(), "editDialogFragment");
    }

    public void showInstallDialogFragment (DialogFragment installDialog) {
        installDialog.show(getSupportFragmentManager(), "installDialogFragment");
    }

    public void showSelectDialogFragment (DialogFragment selectDialog) {
        selectDialog.show(getSupportFragmentManager(), "selectDialogFragment");
    }

    public void onItemClick(int position) {
        if (isEnable) {
            for (GameData gameData : gamesMap.values()) {
                if (!gameData.isInstalled()) continue;
                tempList.add(gameData);
            }
            var mViewHolder =
                    mRecyclerView.findViewHolderForAdapterPosition(position);
            var gameData = tempList.get(Objects.requireNonNull(mViewHolder)
                    .getAdapterPosition());

            if (selectList.isEmpty() || !selectList.contains(gameData)) {
                selectList.add(gameData);
                CardView cardView = mViewHolder.itemView.findViewWithTag("gameCardView");
                cardView.setCardBackgroundColor(Color.LTGRAY);
            } else {
                selectList.remove(gameData);
                CardView cardView = mViewHolder.itemView.findViewWithTag("gameCardView");
                cardView.setCardBackgroundColor(Color.DKGRAY);
            }
        } else {
            if (!getSupportFragmentManager().getFragments().isEmpty()) {
                StockGameFragment fragment = new StockGameFragment();
                FragmentStockGame fragmentStockGame = new ViewModelProvider(this).get(FragmentStockGame.class);
                fragmentStockGame.setGameData(activityStock.getGamesMap()
                        .get(getGameIdByPosition(position)));
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.stockFragContainer, fragment, "StockGameFragment")
                        .commit();
            }
            // showGameInfo(getGameIdByPosition(position));
        }
    }

    @Override
    public void onDialogDestroy(DialogFragment dialog) {
        activityStock.isShowDialog.set(false);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        if (Objects.equals(dialog.getTag() , "infoDialogFragment")) {
            activityStock.showDialogEdit();
        }
    }

    @Override
    public void onDialogNeutralClick(DialogFragment dialog) {
        if (Objects.equals(dialog.getTag() , "infoDialogFragment")) {
            activityStock.playGame();
        }
    }

    @Override
    public void onClickEditButton() {
        activityStock.showDialogEdit();
    }

    @Override
    public void onClickPlayButton() {
        activityStock.playGame();
    }

    @Override
    public void onDialogListClick(DialogFragment dialog, int which) {
        if (Objects.equals(dialog.getTag() , "selectDialogFragment")) {
            activityStock.outputIntObserver.setValue(which);
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
                    tempList = getSortedGames();
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
                            activityStock.refreshGames();
                        }
                        actionMode.finish();
                    } else if (itemId == R.id.select_all) {
                        if(selectList.size() == tempList.size()) {
                            selectList.clear();
                            for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
                                final RecyclerView.ViewHolder holder =
                                        mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
                                CardView cardView = holder.itemView.findViewWithTag("gameCardView");
                                cardView.setCardBackgroundColor(Color.DKGRAY);
                            }
                        } else {
                            selectList.clear();
                            selectList.addAll(tempList);
                            for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
                                final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
                                CardView cardView = holder.itemView.findViewWithTag("gameCardView");
                                cardView.setCardBackgroundColor(Color.LTGRAY);
                            }
                        }
                    }
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
                        final RecyclerView.ViewHolder holder =
                                mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
                        CardView cardView = holder.itemView.findViewWithTag("gameCardView");
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

    @Deprecated
    private void showGameInfo(String gameId) {
        var gameData = activityStock.getGamesMap().get(gameId);
        if (gameData == null) {
            showErrorDialog("GameData not found: " + gameId);
            return;
        }
        StringBuilder message = new StringBuilder();
        if (gameData.author.length() > 0) {
            message.append(getString(R.string.author).replace("-AUTHOR-", gameData.author));
        }
        if (gameData.portedBy.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.ported_by)
                    .replace("-PORTED_BY-", gameData.portedBy));
        }
        if (gameData.version.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.version).replace("-VERSION-", gameData.version));
        }
        if (gameData.fileSize != null) {
            message.append('\n');
            message.append(getString(R.string.fileSize).replace("-SIZE-",
                    gameData.getFileSize()));
        }
        if (gameData.fileExt.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.fileType).replace("-TYPE-", gameData.fileExt));
            if (gameData.fileExt.equals("aqsp")) {
                message.append(" ");
                message.append(getString(R.string.experimental));
            }
        }
        if (gameData.pubDate.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.pub_data).replace("-PUB_DATA-", gameData.pubDate));
        }
        if (gameData.modDate.length() > 0) {
            message.append('\n');
            message.append(getString(R.string.mod_data).replace("-MOD_DATA-", gameData.pubDate));
        }
        var dialogFrags = new StockDialogFrags();
        dialogFrags.setDialogType(StockDialogType.INFO_DIALOG);
        dialogFrags.setTitle(gameData.title);
        dialogFrags.setMessage(String.valueOf(message));
        if (gameData.isInstalled() && doesDirectoryContainGameFiles(gameData.gameDir)) {
            dialogFrags.setInstalled(true);
        }
        dialogFrags.show(getSupportFragmentManager(), "infoDialogFragment");
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
        activityStock.refreshGamesDirectory();
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
        return false;
    }

    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    private void filter(String text){
        var gameData = getSortedGames();
        var filteredList = new ArrayList<GameData>();
        for (GameData item : gameData) {
            if (item.title.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        if (!filteredList.isEmpty()) {
            localStockViewModel.setGameDataArrayList(filteredList);
        }
    }
}