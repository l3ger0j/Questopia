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

        stockViewModel.getGameLiveData().observe(getViewLifecycleOwner(), data -> {
            var gameDataObserver = new GameDataObserver();
            gameDataObserver.authorObserver.set(
                    data.author.isEmpty()
                            ? data.author
                            : getString(R.string.author).replace("-AUTHOR-" , data.author)
            );
            gameDataObserver.portedByObserver.set(
                    data.portedBy.isEmpty()
                            ? data.portedBy
                            : getString(R.string.ported_by).replace("-PORTED_BY-" , data.portedBy)
            );
            gameDataObserver.versionObserver.set(
                    data.version.isEmpty()
                            ? data.version
                            : getString(R.string.version).replace("-VERSION-" , data.version)
            );
            gameDataObserver.fileExtObserver.set(
                    data.fileExt.isEmpty()
                            ? data.fileExt
                            : data.fileExt.equals("aqsp")
                                ? getString(R.string.fileType).replace("-TYPE-" , data.fileExt) + " " + getString(R.string.experimental)
                                : getString(R.string.fileType).replace("-TYPE-" , data.fileExt)
            );
            gameDataObserver.fileSizeObserver.set(
                    data.fileSize.isEmpty()
                            ? data.fileSize
                            : getString(R.string.fileSize).replace("-SIZE-" , data.fileSize)
            );
            gameDataObserver.pubDateObserver.set(
                    data.pubDate.isEmpty()
                            ? data.pubDate
                            : getString(R.string.pub_data).replace("-PUB_DATA-" , data.pubDate)
            );
            gameDataObserver.modDateObserver.set(
                    data.modDate.isEmpty()
                            ? data.modDate
                            : getString(R.string.mod_data).replace("-MOD_DATA-" , data.pubDate)
            );
            gameDataObserver.titleObserver.set(data.title);
            gameDataObserver.langObserver.set(data.lang);
            gameDataObserver.playerObserver.set(data.player);
            gameDataObserver.iconPathObserver.set(data.icon);
            gameDataObserver.fileUrlObserver.set(data.fileUrl);
            gameDataObserver.descUrlObserver.set(data.descUrl);
            fragmentStockGameBinding.setGameData(gameDataObserver);
        });

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
        // TODO: 19.07.2023 Release this
        // fragmentStockGameBinding.downloadButton.setOnClickListener(view3 -> listener.onClickDownloadButton());
    }
}
