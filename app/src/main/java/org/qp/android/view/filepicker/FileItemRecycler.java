package org.qp.android.view.filepicker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.databinding.ListItemFileBinding;
import org.qp.android.dto.FileInfo;

import java.util.ArrayList;
import java.util.Objects;

public class FileItemRecycler extends RecyclerView.Adapter<FileItemRecycler.ViewHolder> {
    private Context context;

    private final AsyncListDiffer<FileInfo> differ =
            new AsyncListDiffer<>(this , DIFF_CALLBACK);

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    private static final DiffUtil.ItemCallback<FileInfo> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull FileInfo oldItem , @NonNull FileInfo newItem) {
                    return Objects.equals(oldItem.name , newItem.name)
                            && Objects.equals(oldItem.countObject , newItem.countObject);
                }

                @Override
                public boolean areContentsTheSame(@NonNull FileInfo oldItem , @NonNull FileInfo newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public FileItemRecycler(Context context,
                            ArrayList<FileInfo> fileInfo) {
        this.context = context;
        differ.submitList(fileInfo);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent , int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        ListItemFileBinding listItemFileBinding =
                DataBindingUtil.inflate(inflater , R.layout.list_item_file , parent , false);
        return new FileItemRecycler.ViewHolder(listItemFileBinding);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder , int position) {
        holder.listItemFileBinding(differ.getCurrentList().get(position));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ListItemFileBinding listItemFileBinding;

        ViewHolder(ListItemFileBinding binding) {
            super(binding.getRoot());
            this.listItemFileBinding = binding;
        }

        public void listItemFileBinding(FileInfo fileInfo) {
            listItemFileBinding.setFileInfo(fileInfo);
            listItemFileBinding.executePendingBindings();
        }
    }
}