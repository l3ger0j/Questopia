package org.qp.android.ui.stock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.qp.android.R;
import org.qp.android.databinding.FragmentItemGameBinding;
import org.qp.android.ui.dialogs.StockDialogType;

public class StockGameFragment extends Fragment {

    private FragmentItemGameBinding fragmentStockGameBinding;
    private StockViewModel stockViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater , @Nullable ViewGroup container , @Nullable Bundle savedInstanceState) {
        var appCompatActivity = ((AppCompatActivity) requireActivity());
        if (appCompatActivity.getSupportActionBar() != null) {
            appCompatActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        fragmentStockGameBinding = FragmentItemGameBinding.inflate(getLayoutInflater());
        stockViewModel = new ViewModelProvider(requireActivity())
                .get(StockViewModel.class);
        fragmentStockGameBinding.setViewModel(stockViewModel);
        return fragmentStockGameBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view , savedInstanceState);
        fragmentStockGameBinding.editButton.setOnClickListener(view1 ->
                stockViewModel.showDialogFragment(
                        getParentFragmentManager() ,
                        StockDialogType.EDIT_DIALOG ,
                        null
                )
        );
        fragmentStockGameBinding.playButton.setOnClickListener(view2 -> {
            var optIntent = stockViewModel.createPlayGameIntent();
            if (optIntent.isEmpty()) {
                stockViewModel.showDialogFragment(
                        getParentFragmentManager() ,
                        StockDialogType.ERROR_DIALOG ,
                        getString(R.string.gamesFolderError)
                );
                return;
            }
            var intent = optIntent.get();
            switch (stockViewModel.getCountGameFiles()) {
                case 0 ->
                        stockViewModel.showDialogFragment(
                                getParentFragmentManager() ,
                                StockDialogType.ERROR_DIALOG ,
                                getString(R.string.gameFolderEmpty)
                        );
                case 1 -> {
                    var chosenGameFile = stockViewModel.getGameFile(0);
                    if (chosenGameFile == null) return;
                    intent.putExtra("gameFileUri" ,  String.valueOf(chosenGameFile.getUri()));
                    requireActivity().startActivity(intent);
                }
                default -> {
                    stockViewModel.showDialogFragment(
                            getParentFragmentManager() ,
                            StockDialogType.SELECT_DIALOG ,
                            null
                    );
                    stockViewModel.outputIntObserver.observe(getViewLifecycleOwner() , integer -> {
                        var chosenGameFile = stockViewModel.getGameFile(integer);
                        if (chosenGameFile == null) return;
                        intent.putExtra("gameFileUri" , String.valueOf(chosenGameFile.getUri()));
                        requireActivity().startActivity(intent);
                    });
                }
            }
        });
         fragmentStockGameBinding.downloadButton.setOnClickListener(view3 ->
                 stockViewModel.getCurrGameData().ifPresent(gameData ->
                         stockViewModel.startFileDownload(gameData)
                 ));
    }
}
