package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.AccessibilityUtil.customAccessibilityDelegate;
import static org.qp.android.ui.stock.StockViewModel.DISABLE_CALCULATE_DIR;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.data.db.Game;
import org.qp.android.databinding.ListItemLocalGameBinding;

import java.util.List;
import java.util.Objects;

public class LocalGamesListAdapter extends RecyclerView.Adapter<LocalGamesListAdapter.LocalGameHolder> {

    private static final DiffUtil.ItemCallback<Game> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull Game oldItem, @NonNull Game newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Game oldItem, @NonNull Game newItem) {
                    return Objects.equals(oldItem, newItem);
                }
            };
    private final AsyncListDiffer<Game> differ =
            new AsyncListDiffer<>(this, DIFF_CALLBACK);
    private Context context;

    public Game getGameEntry(int position) {
        return differ.getCurrentList().get(position);
    }

    public List<Game> getGameEntries() {
        return differ.getCurrentList();
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public LocalGamesListAdapter submitList(List<Game> gameEntriesList) {
        differ.submitList(gameEntriesList);
        return this;
    }

    @NonNull
    @Override
    public LocalGameHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        var inflater = LayoutInflater.from(parent.getContext());
        ListItemLocalGameBinding listItemLocalGameBinding =
                DataBindingUtil.inflate(inflater, R.layout.list_item_local_game, parent, false);
        listItemLocalGameBinding.relativeLayout.setAccessibilityDelegate(customAccessibilityDelegate());
        return new LocalGameHolder(listItemLocalGameBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull LocalGameHolder holder, int position) {
        holder.listItemGameBinding(getGameEntries().get(position));
        var gameData = getGameEntry(position);

        var fileSize = gameData.fileSize;
        if (fileSize == null || fileSize.isEmpty() || fileSize.isBlank()) return;
        if (fileSize.equals(DISABLE_CALCULATE_DIR)) return;

        var elementSize = holder.listItemLocalGameBinding.gameSize;
        var fileSizeString = context.getString(R.string.fileSize);

        elementSize.setText(fileSizeString.replace("-SIZE-", fileSize));
    }

    public static class LocalGameHolder extends RecyclerView.ViewHolder {
        ListItemLocalGameBinding listItemLocalGameBinding;

        LocalGameHolder(ListItemLocalGameBinding listItemLocalGameBinding) {
            super(listItemLocalGameBinding.getRoot());
            this.listItemLocalGameBinding = listItemLocalGameBinding;
        }

        public void listItemGameBinding(Game gameEntry) {
            var gameDataObserver = new GameDataObserver();
            gameDataObserver.titleObserver.set(gameEntry.title);
            gameDataObserver.isGameInstalled.set(gameEntry.listId == 0);
            gameDataObserver.iconUriObserver.set(gameEntry.gameIconUri);
            listItemLocalGameBinding.setData(gameDataObserver);
            listItemLocalGameBinding.executePendingBindings();
        }
    }
}

