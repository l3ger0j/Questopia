package org.qp.android.ui.game;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.qp.android.helpers.utils.AccessibilityUtil.customAccessibilityDelegate;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;

import android.graphics.Typeface;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.qp.android.R;
import org.qp.android.databinding.ListGameItemBinding;
import org.qp.android.questopiabundle.dto.LibListItem;

import java.util.List;
import java.util.Objects;

public class GameItemAdapter extends RecyclerView.Adapter<GameItemAdapter.ViewHolder> {

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
    public Typeface typeface;
    public int textSize;
    public int backgroundColor;
    public int textColor;
    public int linkTextColor;

    public LibListItem getItem(int position) {
        return differ.getCurrentList().get(position);
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
    public GameItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                         int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        var listGameItemBinding = ListGameItemBinding.inflate(inflater, parent, false);
        listGameItemBinding.relativeLayout.setAccessibilityDelegate(customAccessibilityDelegate());
        return new GameItemAdapter.ViewHolder(listGameItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull GameItemAdapter.ViewHolder holder, int position) {
        var qpListItem = getItem(position);

        var itemIcon = holder.listGameItemBinding.itemIcon;
        if (!isNotEmptyOrBlank(qpListItem.pathToImage)) {
            itemIcon.setVisibility(GONE);
        } else {
            itemIcon.setVisibility(VISIBLE);
            Glide.with(holder.listGameItemBinding.itemIcon)
                    .load(qpListItem.pathToImage)
                    .error(R.drawable.baseline_broken_image_24)
                    .centerCrop()
                    .into(itemIcon);
        }

        var itemText = holder.listGameItemBinding.itemText;
        if (!isNotEmptyOrBlank(qpListItem.text)) {
            itemText.setVisibility(GONE);
        } else {
            itemText.setVisibility(VISIBLE);
            itemText.setTypeface(typeface);
            itemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            itemText.setBackgroundColor(backgroundColor);
            itemText.setTextColor(textColor);
            itemText.setLinkTextColor(linkTextColor);
            itemText.setText(Html.fromHtml(qpListItem.text, Html.FROM_HTML_MODE_LEGACY));
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
