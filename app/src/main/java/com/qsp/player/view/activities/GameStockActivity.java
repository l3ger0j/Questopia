package com.qsp.player.view.activities;

import static com.qsp.player.utils.FileUtil.deleteDirectory;
import static com.qsp.player.utils.LanguageUtil.setLocale;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.qsp.player.R;
import com.qsp.player.databinding.ActivityStockBinding;
import com.qsp.player.dto.stock.GameData;
import com.qsp.player.view.adapters.SettingsAdapter;
import com.qsp.player.view.fragments.FragmentLocal;
import com.qsp.player.viewModel.viewModels.FragmentLocalVM;
import com.qsp.player.viewModel.viewModels.GameStockActivityVM;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class GameStockActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private HashMap<String, GameData> gamesMap = new HashMap<>();

    private SettingsAdapter settingsAdapter;
    private String currentLanguage = Locale.getDefault().getLanguage();
    private GameData gameData;

    private GameStockActivityVM gameStockActivityVM;
    private FragmentLocalVM localStockViewModel;

    private ActionMode actionMode;
    private ActivityStockBinding activityStockBinding;
    public ActivityResultLauncher<Intent> resultInstallLauncher, resultGetImageLauncher;

    private boolean isEnable = false;

    private FloatingActionButton mFAB;
    private RecyclerView mRecyclerView;
    private final ArrayList<GameData> tempList = getSortedGames();
    private final ArrayList<GameData> selectList = new ArrayList<>();

    public String getGameIdByPosition(int position) {
        localStockViewModel.getGameData().observe(this, gameDataArrayList -> {
            if (!gameDataArrayList.isEmpty() && gameDataArrayList.size() > position) {
                gameData = gameDataArrayList.get(position);
            }
        });
        return gameData.id;
    }

    @NonNull
    public ArrayList<GameData> getSortedGames() {
        Collection<GameData> unsortedGameData = gamesMap.values();
        ArrayList<GameData> gameData = new ArrayList<>(unsortedGameData);

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
        ArrayList<GameData> gameData = getSortedGames();
        ArrayList<GameData> localGameData = new ArrayList<>();

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
        mFAB = activityStockBinding.floatingActionButton;
        localStockViewModel =
                new ViewModelProvider(this).get(FragmentLocalVM.class);
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

        gameStockActivityVM = new ViewModelProvider(this).get(GameStockActivityVM.class);
        activityStockBinding.setStockVM(gameStockActivityVM);
        gameStockActivityVM.activityObservableField.set(this);
        gamesMap = gameStockActivityVM.getGamesMap();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, FragmentLocal.class, null)
                    .commit();
        }

        resultInstallLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri uri;
                    DocumentFile file;
                    if (result.getResultCode() == RESULT_OK) {
                        if ((uri = Objects.requireNonNull(result.getData()).getData()) == null) {
                            Log.e(TAG, "Archive or file is not selected");
                        }
                        try {
                            file = DocumentFile.fromSingleUri(this, Objects.requireNonNull(uri));
                        } catch (Exception e) {
                            Log.e(TAG, "Error: ", e);
                            file = DocumentFile.fromTreeUri(this, Objects.requireNonNull(uri));
                        }
                        assert file != null;
                        gameStockActivityVM.setTempInstallFile(file);
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
                            Log.e(TAG, "Archive or file is not selected");
                        }
                        assert uri != null;
                        file = DocumentFile.fromSingleUri(this, uri);
                        assert file != null;
                        gameStockActivityVM.setTempImageFile(file);
                    }
                }
        );

        loadSettings();
        loadLocale();

        Log.i(TAG,"GameStockActivity created");

        setContentView(activityStockBinding.getRoot());
    }

    private void loadSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        settingsAdapter = SettingsAdapter.from(preferences);
    }

    private void loadLocale() {
        setLocale(this, settingsAdapter.language);
        setTitle(R.string.gameStockTitle);
        currentLanguage = settingsAdapter.language;
    }

    public void onItemClick(int position) {
        if (isEnable) {
            for (GameData gameData : gamesMap.values()) {
                if (!gameData.isInstalled()) continue;
                tempList.add(gameData);
            }

            RecyclerView.ViewHolder mViewHolder =
                    mRecyclerView.findViewHolderForAdapterPosition(position);
            GameData gameData = tempList.get(Objects.requireNonNull(mViewHolder)
                    .getAdapterPosition());

            if (selectList.isEmpty() || !selectList.contains(gameData)) {
                selectList.add(gameData);
                mViewHolder.itemView.setBackgroundColor(Color.LTGRAY);
            } else {
                selectList.remove(gameData);
                mViewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
            }
        } else {
            String gameId = getGameIdByPosition(position);
            showGameInfo(gameId);
        }
    }

    public void onLongItemClick() {
        if (!isEnable) {
            ActionMode.Callback callback = new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode , Menu menu) {
                    mode.getMenuInflater().inflate(R.menu.menu_delete, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode , Menu menu) {
                    isEnable = true;
                    return true;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode , MenuItem item) {
                    int itemId = item.getItemId();
                    if (itemId == R.id.delete_game) {
                        for(GameData data : selectList)
                        {
                            tempList.remove(data);
                            try {
                                deleteDirectory(data.gameDir);
                            } catch (NullPointerException e) {
                                Log.e(TAG,e.toString());
                            }
                            gameStockActivityVM.refreshGames();
                        }
                        actionMode.finish();
                    } else if (itemId == R.id.select_all) {
                        if(selectList.size() == tempList.size()) {
                            selectList.clear();
                            for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
                                final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
                                holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                            }
                        } else {
                            selectList.clear();
                            selectList.addAll(tempList);
                            for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
                                final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
                                holder.itemView.setBackgroundColor(Color.LTGRAY);
                            }
                        }
                    }
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
                        final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
                        holder.itemView.setBackgroundColor(Color.TRANSPARENT);
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

    private void showGameInfo(String gameId) {
        final GameData gameData = gameStockActivityVM.getGamesMap().get(gameId);
        if (gameData == null) {
            Log.e(TAG,"GameData not found: " + gameId);
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
        if (gameData.getFileSize() > 0) {
            message.append('\n');
            message.append(getString(R.string.fileSize).replace("-SIZE-",
                    Integer.toString(gameData.getFileSize() / 1024)));
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
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this)
                .setMessage(message)
                .setTitle(gameData.title)
                .setIcon(R.drawable.icon)
                .setNegativeButton(getString(R.string.close), (dialog, which) -> dialog.cancel());

        if (gameData.isInstalled()) {
            alertBuilder.setNeutralButton(getString(R.string.play), (dialog, which) -> playGame(gameData));
        }
        alertBuilder.create().show();
    }

    private void playGame(final GameData gameData) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("gameId", gameData.id);
        intent.putExtra("gameTitle", gameData.title);
        intent.putExtra("gameDirUri", gameData.gameDir.getAbsolutePath());

        int gameFileCount = gameData.gameFiles.size();
        if (gameFileCount == 0) {
            Log.w(TAG, "GameData has no gameData files");
            return;
        }
        if (gameFileCount == 1) {
            intent.putExtra("gameFileUri", gameData.gameFiles.get(0).getAbsolutePath());
            startActivity(intent);
        } else {
            ArrayList<String> names = new ArrayList<>();
            for (File file : gameData.gameFiles) {
                names.add(file.getName());
            }
            new AlertDialog.Builder(GameStockActivity.this)
                    .setTitle(getString(R.string.selectGameFile))
                    .setCancelable(false)
                    .setItems(names.toArray(new String[0]), (dialog, which) -> {
                        intent.putExtra("gameFileUri", gameData.gameFiles.get(which).getAbsolutePath());
                        startActivity(intent);
                    })
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG , "GameStockActivity destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();

        loadSettings();
        updateLocale();
        gameStockActivityVM.refreshGamesDirectory();
    }

    private void updateLocale() {
        if (currentLanguage.equals(settingsAdapter.language)) return;

        setLocale(this, settingsAdapter.language);
        setTitle(getString(R.string.gameStockTitle));
        gameStockActivityVM.refreshGames();
        invalidateOptionsMenu();

        currentLanguage = settingsAdapter.language;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stock, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_options) {
            showSettings();
            return true;
        } else if (itemId == R.id.action_search) {
            androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) item.getActionView();
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
        ArrayList<GameData> gameData = getSortedGames();
        ArrayList<GameData> filteredList = new ArrayList<>();

        for (GameData item : gameData) {
            if (item.title.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        if (filteredList.isEmpty()) {
            Log.e(TAG,"No Data Found");
        } else {
            localStockViewModel.setGameDataArrayList(filteredList);
        }
    }
}