package org.qp.android.ui.game;

import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;

import android.graphics.Typeface;
import android.net.Uri;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.libqsp.jni.QSPLib;

import org.qp.android.databinding.ListGameItemBinding;

import java.util.List;
import java.util.Objects;

public class GameItemRecycler extends RecyclerView.Adapter<GameItemRecycler.ViewHolder> {

    private static final DiffUtil.ItemCallback<QSPLib.ListItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull QSPLib.ListItem oldItem, @NonNull QSPLib.ListItem newItem) {
                    return Objects.equals(oldItem.image(), newItem.image()) && Objects.equals(oldItem.name(), newItem.name());
                }

                @Override
                public boolean areContentsTheSame(@NonNull QSPLib.ListItem oldItem, @NonNull QSPLib.ListItem newItem) {
                    return oldItem.equals(newItem);
                }
            };
    private final AsyncListDiffer<QSPLib.ListItem> differ =
            new AsyncListDiffer<>(this, DIFF_CALLBACK);
    public Typeface typeface;
    public int textSize;
    public int backgroundColor;
    public int textColor;
    public int linkTextColor;

    public QSPLib.ListItem getItem(int position) {
        return differ.getCurrentList().get(position);
    }

    public List<QSPLib.ListItem> getGameData() {
        return differ.getCurrentList();
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public void submitList(List<QSPLib.ListItem> gameData) {
        differ.submitList(gameData);
    }

    @NonNull
    @Override
    public GameItemRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                          int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        var listGameItemBinding = ListGameItemBinding.inflate(inflater, parent, false);
        return new GameItemRecycler.ViewHolder(listGameItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull GameItemRecycler.ViewHolder holder, int position) {
        var qpListItem = getItem(position);

        final var itemImage = holder.listGameItemBinding.itemIcon;
        if (isNotEmptyOrBlank(qpListItem.image())) {
            itemImage.setVisibility(ViewGroup.VISIBLE);
            itemImage.setImageURI(Uri.parse(qpListItem.image()));
        } else {
            itemImage.setVisibility(ViewGroup.GONE);
        }

        final var itemText = holder.listGameItemBinding.itemText;
        if (isNotEmptyOrBlank(qpListItem.name())) {
            itemText.setVisibility(ViewGroup.VISIBLE);
            itemText.setTypeface(typeface);
            itemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            itemText.setBackgroundColor(backgroundColor);
            itemText.setTextColor(textColor);
            itemText.setLinkTextColor(linkTextColor);
            itemText.setText(Html.fromHtml(qpListItem.name(), Html.FROM_HTML_MODE_LEGACY));
        } else {
            itemText.setVisibility(ViewGroup.GONE);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ListGameItemBinding listGameItemBinding;

        ViewHolder(ListGameItemBinding binding) {
            super(binding.getRoot());
            this.listGameItemBinding = binding;
        }
    }
}
