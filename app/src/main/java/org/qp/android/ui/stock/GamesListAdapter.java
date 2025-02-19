package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.FileUtil.formatFileSize;
import static org.qp.android.ui.stock.StockViewModel.DISABLE_CALC_SIZE;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.qp.android.R;
import org.qp.android.databinding.ListItemGameBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.ui.settings.SettingsController;

import java.util.List;

public class GamesListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int DIVIDER = 1;
    private static final int ITEM = 2;

    private final Context context;
    private final AsyncListDiffer<GameData> differ =
            new AsyncListDiffer<>(this , DIFF_CALLBACK);

    public GameData getItem(int position) {
        return differ.getCurrentList().get(position);
    }

    public List<GameData> getGameData() {
        return differ.getCurrentList();
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    @Override
    public int getItemViewType(int position) {
        var currGameData = getItem(position);

        if (getItemCount() > position + 1) {
            var nextGameData = getItem(position + 1);

            if (currGameData.listId.equals("0")
                    && nextGameData.listId.equals("1")) {
                return DIVIDER;
            }
        }

        return ITEM;
    }

    private static final DiffUtil.ItemCallback<GameData> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull GameData oldItem , @NonNull GameData newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull GameData oldItem , @NonNull GameData newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public GamesListAdapter submitList(List<GameData> gameData){
        differ.submitList(gameData);
        return this;
    }

    public GamesListAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        ListItemGameBinding listItemGameBinding =
                DataBindingUtil.inflate(inflater, R.layout.list_item_game, parent, false);
        return new GameHolder(listItemGameBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof GameHolder gameHolder) {
            gameHolder.listItemGameBinding(getGameData().get(position));
            var gameData = getItem(position);

            if (gameData.icon != null) {
                Picasso.get()
                        .load(Uri.parse(gameData.icon))
                        .fit()
                        .error(R.drawable.baseline_broken_image_24)
                        .into(gameHolder.listItemGameBinding.gameIcon);
            }

            var fileSize = gameData.fileSize;
            if (fileSize == DISABLE_CALC_SIZE) return;

            var currBinPref = SettingsController.newInstance(context).binaryPrefixes;
            var sizeWithPref = formatFileSize(fileSize, currBinPref);

            var elementSize = gameHolder.listItemGameBinding.gameSize;
            var fileSizeString = context.getString(R.string.fileSize);
            elementSize.setText(fileSizeString.replace("-SIZE-", sizeWithPref));
        }
    }

    public static class GameHolder extends RecyclerView.ViewHolder {
        ListItemGameBinding listItemGameBinding;

        GameHolder(ListItemGameBinding listItemGameBinding){
            super(listItemGameBinding.getRoot());
            this.listItemGameBinding = listItemGameBinding;
        }

        public void listItemGameBinding(GameData gameData) {
            listItemGameBinding.setGameData(gameData);
            listItemGameBinding.executePendingBindings();
        }
    }
}

