package org.qp.android.ui.game;

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

import org.libndkqsp.jni.NDKLib;
import org.qp.android.R;
import org.qp.android.databinding.ListGameItemBinding;

import java.util.List;
import java.util.Objects;

public class GameItemRecycler extends RecyclerView.Adapter<GameItemRecycler.ViewHolder> {

    private final AsyncListDiffer<NDKLib.ListItem> differ =
            new AsyncListDiffer<>(this , DIFF_CALLBACK);

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

    private static final DiffUtil.ItemCallback<NDKLib.ListItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull NDKLib.ListItem oldItem , @NonNull NDKLib.ListItem newItem) {
                    return Objects.equals(oldItem.image(), newItem.image()) && Objects.equals(oldItem.text(), newItem.text());
                }

                @Override
                public boolean areContentsTheSame(@NonNull NDKLib.ListItem oldItem , @NonNull NDKLib.ListItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public void submitList(List<NDKLib.ListItem> gameData){
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
        holder.listItemActionObjectBinding(getItem(position));
        var qpListItem = getItem(position);

        if (isNotEmptyOrBlank(qpListItem.image())) {
            Picasso.get()
                    .load(qpListItem.image())
                    .error(R.drawable.baseline_broken_image_24)
                    .fit()
                    .into(holder.listGameItemBinding.itemIcon);
        }

        if (isNotEmptyOrBlank(qpListItem.text())) {
            var itemText = holder.listGameItemBinding.itemText;
            itemText.setTypeface(typeface);
            itemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            itemText.setBackgroundColor(backgroundColor);
            itemText.setTextColor(textColor);
            itemText.setLinkTextColor(linkTextColor);
            itemText.setText(Html.fromHtml(qpListItem.text(), Html.FROM_HTML_MODE_LEGACY));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ListGameItemBinding listGameItemBinding;

        ViewHolder(ListGameItemBinding binding){
            super(binding.getRoot());
            this.listGameItemBinding = binding;
        }

        public void listItemActionObjectBinding(NDKLib.ListItem libListItem) {
            listGameItemBinding.setListItem(libListItem);
            listGameItemBinding.executePendingBindings();
        }
    }
}
