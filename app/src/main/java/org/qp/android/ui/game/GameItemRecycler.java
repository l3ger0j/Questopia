package org.qp.android.ui.game;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.qp.android.R;
import org.qp.android.databinding.ListGameItemBinding;
import org.qp.android.dto.lib.LibListItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameItemRecycler extends RecyclerView.Adapter<GameItemRecycler.ViewHolder> {

    private final AsyncListDiffer<LibListItem> differ =
            new AsyncListDiffer<>(this , DIFF_CALLBACK);

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

    private static final DiffUtil.ItemCallback<LibListItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull LibListItem oldItem , @NonNull LibListItem newItem) {
                    return Objects.equals(oldItem.pathToImage , newItem.pathToImage) && Objects.equals(oldItem.text , newItem.text);
                }

                @Override
                public boolean areContentsTheSame(@NonNull LibListItem oldItem , @NonNull LibListItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public void submitList(ArrayList<LibListItem> gameData){
        differ.submitList(gameData);
    }

    @NonNull
    @Override
    public GameItemRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                          int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        ListGameItemBinding listGameItemBinding =
                DataBindingUtil.inflate(inflater, R.layout.list_game_item , parent, false);
        return new GameItemRecycler.ViewHolder(listGameItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull GameItemRecycler.ViewHolder holder, int position) {
        holder.listItemActionObjectBinding(getGameData().get(position));
        var qpListItem = getItem(position);

        if (qpListItem.pathToImage != null) {
            Picasso.get()
                    .load(qpListItem.pathToImage)
                    .error(R.drawable.baseline_broken_image_24)
                    .fit()
                    .into(holder.listGameItemBinding.itemIcon);
        }

        if (qpListItem.text != null) {
            var itemText = holder.listGameItemBinding.itemText;
            itemText.setTypeface(typeface);
            itemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            itemText.setBackgroundColor(backgroundColor);
            itemText.setTextColor(textColor);
            itemText.setLinkTextColor(linkTextColor);
            // TODO: 27.06.2023 rewrite this!
            itemText.setText(Html.fromHtml(String.valueOf(qpListItem.text) , Html.FROM_HTML_MODE_LEGACY));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ListGameItemBinding listGameItemBinding;

        ViewHolder(ListGameItemBinding binding){
            super(binding.getRoot());
            this.listGameItemBinding = binding;
            //Аналогично,как мы сделали для полки игр. Вообще,на мой взгляд,этот делегат можно вынести отдельно,чтобы избежать дублирования кода.
            this.listGameItemBinding.relativeLayout.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, @Nullable Bundle args) {
                    if(action== AccessibilityNodeInfo.ACTION_CLICK) return host.performClick(); else if(action==AccessibilityNodeInfo.ACTION_LONG_CLICK) return host.performLongClick();
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        public void listItemActionObjectBinding(LibListItem libListItem) {
            listGameItemBinding.setLibListItem(libListItem);
            listGameItemBinding.executePendingBindings();
        }
    }
}
