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

import org.libndkqsp.jni.NDKLib;
import org.qp.android.databinding.ListGameItemBinding;

import java.util.List;
import java.util.Objects;

public class GameItemAdapter extends RecyclerView.Adapter<GameItemAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<NDKLib.ListItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull NDKLib.ListItem oldItem, @NonNull NDKLib.ListItem newItem) {
                    return Objects.equals(oldItem.image(), newItem.image()) && Objects.equals(oldItem.text(), newItem.text());
                }

                @Override
                public boolean areContentsTheSame(@NonNull NDKLib.ListItem oldItem, @NonNull NDKLib.ListItem newItem) {
                    return oldItem.equals(newItem);
                }
            };
    private final AsyncListDiffer<NDKLib.ListItem> differ =
            new AsyncListDiffer<>(this, DIFF_CALLBACK);
    public Typeface typeface;
    public int textSize;
    public int backgroundColor;
    public int textColor;
    public int linkTextColor;

    public NDKLib.ListItem getItem(int position) {
        return differ.getCurrentList().get(position);
    }

    public List<NDKLib.ListItem> getGameData() {
        return differ.getCurrentList();
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public void submitList(List<NDKLib.ListItem> gameData) {
        differ.submitList(gameData);
    }

    @NonNull
    @Override
    public GameItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                         int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        var listGameItemBinding = ListGameItemBinding.inflate(inflater, parent, false);
        return new GameItemAdapter.ViewHolder(listGameItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull GameItemAdapter.ViewHolder holder, int position) {
        var qpListItem = getItem(position);

        final var itemImage = holder.listGameItemBinding.itemIcon;
        if (isNotEmptyOrBlank(qpListItem.image())) {
            itemImage.setVisibility(ViewGroup.VISIBLE);
            itemImage.setImageURI(Uri.parse(qpListItem.image()));
        } else {
            itemImage.setVisibility(ViewGroup.GONE);
        }

        final var itemText = holder.listGameItemBinding.itemText;
        if (isNotEmptyOrBlank(qpListItem.text())) {
            itemText.setVisibility(ViewGroup.VISIBLE);
            itemText.setTypeface(typeface);
            itemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            itemText.setBackgroundColor(backgroundColor);
            itemText.setTextColor(textColor);
            itemText.setLinkTextColor(linkTextColor);
            itemText.setText(Html.fromHtml(qpListItem.text(), Html.FROM_HTML_MODE_LEGACY));
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
