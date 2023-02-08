package org.qp.android.view.filepicker;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.qp.android.databinding.FragmentFilePickerBinding;
import org.qp.android.dto.FileInfo;
import org.qp.android.view.adapters.RecyclerItemClickListener;
import org.qp.android.viewModel.FilePickerViewModel;

import java.util.ArrayList;

public class FilePickerDialogFragment extends FilePatternDialogFrag {
    private FilePickerViewModel filePickerViewModel;
    private FragmentFilePickerBinding binding;
    private RecyclerView mRecyclerView;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        var builder = new MaterialAlertDialogBuilder(requireContext());
        binding = FragmentFilePickerBinding.inflate(getLayoutInflater());
        mRecyclerView = binding.list;
        filePickerViewModel = new ViewModelProvider(requireActivity())
                .get(FilePickerViewModel.class);
        filePickerViewModel.getFileInfoList().observe(getViewLifecycleOwner() , fileInfo);
        builder.setView(binding.getRoot());
        return builder.create();
    }

    Observer<ArrayList<FileInfo>> fileInfo = new Observer<>() {
        @Override
        public void onChanged(ArrayList<FileInfo> fileInfo) {
            var adapter =
                    new FileItemRecycler(requireContext() , fileInfo);
            mRecyclerView.setAdapter(adapter);
        }
    };

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                requireContext() ,
                mRecyclerView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view , int position) {
                        listener.onDialogListClick(getTag(), position);
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {

                    }
                }
        ));
    }
}