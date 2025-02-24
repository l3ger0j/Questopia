package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.AccessibilityUtil.customAccessibilityDelegate;
import static org.qp.android.helpers.utils.FileUtil.formatFileSize;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;
import static org.qp.android.ui.stock.StockViewModel.DISABLE_CALC_SIZE;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.qp.android.R;
import org.qp.android.databinding.ListItemGameBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.ui.settings.SettingsController;

import java.util.List;

public class RemoteGamesListAdapter extends RecyclerView.Adapter<RemoteGamesListAdapter.RemoteGameHolder> {

    private static final DiffUtil.ItemCallback<GameData> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull GameData oldItem, @NonNull GameData newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull GameData oldItem, @NonNull GameData newItem) {
                    return oldItem.equals(newItem);
                }
            };
    private final AsyncListDiffer<GameData> differ =
            new AsyncListDiffer<>(this, DIFF_CALLBACK);
    private Context context;

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public GameData getItem(int position) {
        return differ.getCurrentList().get(position);
    }

    public void submitList(List<GameData> gameData) {
        differ.submitList(gameData);
    }

    @NonNull
    @Override
    public RemoteGameHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        var inflater = LayoutInflater.from(parent.getContext());
        var listItemGameBinding = ListItemGameBinding.inflate(inflater, parent, false);
        listItemGameBinding.relativeLayout.setAccessibilityDelegate(customAccessibilityDelegate());
        return new RemoteGameHolder(listItemGameBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RemoteGameHolder holder, int position) {
        var gameData = getItem(position);

        final var icon = holder.listItemGameBinding.gameIcon;
        if (isNotEmptyOrBlank(gameData.icon)) {
            Picasso.get()
                    .load(Uri.parse(gameData.icon))
                    .fit()
                    .error(R.drawable.baseline_broken_image_24)
                    .into(icon);
        } else {
            var drawable = AppCompatResources.getDrawable(
                    context,
                    R.drawable.baseline_broken_image_24
            );
            icon.setImageDrawable(drawable);
        }

        final var text = holder.listItemGameBinding.gameTitle;
        if (isNotEmptyOrBlank(gameData.title)) {
            text.setText(gameData.title);
            text.setTextColor(0xFFFFD700);
        }

        var fileSize = gameData.fileSize;
        if (fileSize == DISABLE_CALC_SIZE) return;

        var currBinPref = SettingsController.newInstance(context).binaryPrefixes;
        var sizeWithPref = formatFileSize(fileSize, currBinPref);

        var elementSize = holder.listItemGameBinding.gameSize;
        var fileSizeString = ContextCompat.getString(context, R.string.fileSize);
        elementSize.setText(fileSizeString.replace("-SIZE-", sizeWithPref));
    }

    public static class RemoteGameHolder extends RecyclerView.ViewHolder {
        ListItemGameBinding listItemGameBinding;

        RemoteGameHolder(ListItemGameBinding listItemGameBinding) {
            super(listItemGameBinding.getRoot());
            this.listItemGameBinding = listItemGameBinding;
        }
    }
}
