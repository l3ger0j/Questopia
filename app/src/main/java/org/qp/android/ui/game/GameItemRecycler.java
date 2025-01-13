package org.qp.android.ui.game;

import static org.qp.android.helpers.utils.AccessibilityUtil.customAccessibilityDelegate;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;

import android.graphics.Typeface;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.qp.android.R;
import org.qp.android.databinding.ListGameItemBinding;
import org.qp.android.questopiabundle.dto.LibListItem;

import java.util.List;
import java.util.Objects;

public class GameItemRecycler extends RecyclerView.Adapter<GameItemRecycler.ViewHolder> {

    private static final DiffUtil.ItemCallback<LibListItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull LibListItem oldItem, @NonNull LibListItem newItem) {
                    return Objects.equals(oldItem.pathToImage, newItem.pathToImage) && Objects.equals(oldItem.text, newItem.text);
                }

                @Override
                public boolean areContentsTheSame(@NonNull LibListItem oldItem, @NonNull LibListItem newItem) {
                    return oldItem.equals(newItem);
                }
            };
    private final AsyncListDiffer<LibListItem> differ =
            new AsyncListDiffer<>(this, DIFF_CALLBACK);
    private Typeface typeface;
    private int textSize;
    private int backgroundColor;
    private int textColor;
    private int linkTextColor;

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setLinkTextColor(int linkTextColor) {
        this.linkTextColor = linkTextColor;
    }

    public LibListItem getItem(int position) {
        return differ.getCurrentList().get(position);
    }

    public List<LibListItem> getGameData() {
        return differ.getCurrentList();
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public void submitList(List<LibListItem> gameData) {
        differ.submitList(gameData);
    }

    @NonNull
    @Override
    public GameItemRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                          int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        ListGameItemBinding listGameItemBinding =
                DataBindingUtil.inflate(inflater, R.layout.list_game_item, parent, false);
        listGameItemBinding.relativeLayout.setAccessibilityDelegate(customAccessibilityDelegate());
        return new GameItemRecycler.ViewHolder(listGameItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull GameItemRecycler.ViewHolder holder, int position) {
        holder.listItemActionObjectBinding(getGameData().get(position));
        var qpListItem = getItem(position);

        if (isNotEmptyOrBlank(qpListItem.pathToImage)) {
            Picasso.get()
                    .load(qpListItem.pathToImage)
                    .error(R.drawable.baseline_broken_image_24)
                    .fit()
                    .into(holder.listGameItemBinding.itemIcon);
        }

        if (isNotEmptyOrBlank(qpListItem.text)) {
            var itemText = holder.listGameItemBinding.itemText;
            itemText.setTypeface(typeface);
            itemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            itemText.setBackgroundColor(backgroundColor);
            itemText.setTextColor(textColor);
            itemText.setLinkTextColor(linkTextColor);
            // TODO: 27.06.2023 rewrite this!
            itemText.setText(Html.fromHtml(String.valueOf(qpListItem.text), Html.FROM_HTML_MODE_LEGACY));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ListGameItemBinding listGameItemBinding;

        ViewHolder(ListGameItemBinding binding) {
            super(binding.getRoot());
            this.listGameItemBinding = binding;
        }

        public void listItemActionObjectBinding(LibListItem libListItem) {
            listGameItemBinding.setLibListItem(libListItem);
            listGameItemBinding.executePendingBindings();
        }
    }
}
