package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.AccessibilityUtil.customAccessibilityDelegate;
import static org.qp.android.helpers.utils.FileUtil.formatFileSize;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;

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
import org.qp.android.ui.settings.SettingsController;

import java.util.List;
import java.util.Objects;

public class LocalGamesListAdapter extends RecyclerView.Adapter<LocalGamesListAdapter.LocalGameHolder> {

    private final static boolean DEFAULT_VALUE = true;
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

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public void submitList(List<Game> gameEntriesList) {
        differ.submitList(gameEntriesList);
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
        holder.listItemGameBinding(getGameEntry(position));
        var gameEntry = getGameEntry(position);

        var fileSize = gameEntry.fileSize;
        if (!isNotEmptyOrBlank(fileSize)) return;

        var currBinPref = SettingsController.newInstance(context).binaryPrefixes;
        var sizeWithPref = formatFileSize(Long.parseLong(fileSize), currBinPref);

        var elementSize = holder.listItemLocalGameBinding.gameSize;
        var fileSizeString = context.getString(R.string.fileSize);
        elementSize.setText(fileSizeString.replace("-SIZE-", sizeWithPref));
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
            gameDataObserver.isGameInstalled.set(DEFAULT_VALUE);
            gameDataObserver.iconUriObserver.set(gameEntry.gameIconUri);
            listItemLocalGameBinding.setData(gameDataObserver);
            listItemLocalGameBinding.executePendingBindings();
        }
    }
}

