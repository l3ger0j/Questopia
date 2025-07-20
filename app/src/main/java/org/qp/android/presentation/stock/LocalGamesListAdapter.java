package org.qp.android.presentation.stock;

import static org.qp.android.helpers.utils.AccessibilityUtil.customAccessibilityDelegate;
import static org.qp.android.helpers.utils.FileUtil.formatFileSize;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.data.source.database.model.Game;
import org.qp.android.databinding.ListItemLocalGameBinding;
import org.qp.android.presentation.settings.SettingsController;

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
    private final AsyncListDiffer<Game> differ = new AsyncListDiffer<>(this, DIFF_CALLBACK);
    private SelectionTracker<Long> tracker;
    private Context context;

    private int unselectColor;
    private int selectColor;

    public Game getGameEntry(int position) {
        return differ.getCurrentList().get(position);
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public void setTracker(SelectionTracker<Long> tracker) {
        this.tracker = tracker;
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

        unselectColor = android.R.attr.selectableItemBackground;
        selectColor = ContextCompat.getColor(context, R.color.md_theme_primaryContainer);

        return new LocalGameHolder(listItemLocalGameBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull LocalGameHolder holder, int position) {
        holder.listItemGameBinding(getGameEntry(position));
        var gameEntry = getGameEntry(position);

        var fileSize = gameEntry.fileSize;
        if (fileSize == 0) return;

        var currBinPref = SettingsController.getInstance(context).binaryPrefixes;
        var sizeWithPref = formatFileSize(fileSize, currBinPref);

        var elementSize = holder.listItemLocalGameBinding.gameSize;
        var fileSizeString = context.getString(R.string.fileSize);
        elementSize.setText(fileSizeString.replace("-SIZE-", sizeWithPref));

        if (tracker.isSelected(gameEntry.id)) {
            holder.listItemLocalGameBinding.relativeLayout.setBackgroundColor(selectColor);
        } else {
            holder.listItemLocalGameBinding.relativeLayout.setBackgroundColor(unselectColor);
        }
    }

    static final class LocalGamesDetailsLookup extends ItemDetailsLookup<Long> {
        private final RecyclerView mRecyclerView;

        LocalGamesDetailsLookup(RecyclerView mRecyclerView) {
            this.mRecyclerView = mRecyclerView;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            var view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            if (view != null) {
                return ((LocalGameHolder) mRecyclerView.getChildViewHolder(view)).getItemDetails();
            } else {
                return null;
            }
        }
    }

    static final class LocalGamesIdsProvider extends ItemKeyProvider<Long> {
        private final LocalGamesListAdapter adapter;

        LocalGamesIdsProvider(LocalGamesListAdapter adapter) {
            super(SCOPE_CACHED);
            this.adapter = adapter;
        }

        @Override
        public Long getKey(int position) {
            return adapter.getGameEntry(position).id;
        }

        @Override
        public int getPosition(@NonNull Long key) {
            for (var item : adapter.differ.getCurrentList()) {
                if (item.id == key) {
                    return adapter.differ.getCurrentList().indexOf(item);
                }
            }
            return -1;
        }
    }

    public static class LocalGameHolder extends RecyclerView.ViewHolder {
        ListItemLocalGameBinding listItemLocalGameBinding;

        LocalGameHolder(ListItemLocalGameBinding listItemLocalGameBinding) {
            super(listItemLocalGameBinding.getRoot());
            this.listItemLocalGameBinding = listItemLocalGameBinding;
        }

        public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            return new ItemDetailsLookup.ItemDetails<>() {
                @Override
                public int getPosition() {
                    return LocalGameHolder.this.getBindingAdapterPosition();
                }

                @Override
                public Long getSelectionKey() {
                    var adapter = ((LocalGamesListAdapter) LocalGameHolder.this.getBindingAdapter());
                    if (adapter == null) return null;
                    var adapterPosition = LocalGameHolder.this.getBindingAdapterPosition();
                    return adapter.getGameEntry(adapterPosition).id;
                }
            };
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

