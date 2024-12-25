package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var appCompatActivity = ((AppCompatActivity) requireActivity());

        if (appCompatActivity.getSupportActionBar() != null) {
            appCompatActivity.getSupportActionBar().hide();
        }

        fragmentStockGameBinding = FragmentItemGameBinding.inflate(inflater, container, false);
        stockViewModel = new ViewModelProvider(requireActivity())
                .get(StockViewModel.class);
        fragmentStockGameBinding.setViewModel(stockViewModel);

        fragmentStockGameBinding.itemToolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24);
        fragmentStockGameBinding.itemToolbar.setNavigationOnClickListener(v ->
                appCompatActivity.onSupportNavigateUp());

        stockViewModel.gameEntryLiveData.observe(getViewLifecycleOwner(), dataEntry -> {
            var gameDataObserver = new GameDataObserver();
            gameDataObserver.authorObserver.set(
                    dataEntry.author.isEmpty()
                            ? dataEntry.author
                            : getString(R.string.author).replace("-AUTHOR-", dataEntry.author)
            );
            gameDataObserver.portedByObserver.set(
                    dataEntry.portedBy.isEmpty()
                            ? dataEntry.portedBy
                            : getString(R.string.ported_by).replace("-PORTED_BY-", dataEntry.portedBy)
            );
            gameDataObserver.versionObserver.set(
                    dataEntry.version.isEmpty()
                            ? dataEntry.version
                            : getString(R.string.version).replace("-VERSION-", dataEntry.version)
            );
            gameDataObserver.fileExtObserver.set(
                    dataEntry.fileExt.isEmpty()
                            ? dataEntry.fileExt
                            : dataEntry.fileExt.equals("aqsp")
                            ? getString(R.string.fileType).replace("-TYPE-", dataEntry.fileExt) + " " + getString(R.string.experimental)
                            : getString(R.string.fileType).replace("-TYPE-", dataEntry.fileExt)
            );
            gameDataObserver.fileSizeObserver.set(
                    dataEntry.fileSize.isEmpty()
                            ? dataEntry.fileSize
                            : getString(R.string.fileSize).replace("-SIZE-", dataEntry.fileSize)
            );
            gameDataObserver.pubDateObserver.set(
                    dataEntry.pubDate.isEmpty()
                            ? dataEntry.pubDate
                            : getString(R.string.pub_data).replace("-PUB_DATA-", dataEntry.pubDate)
            );
            gameDataObserver.modDateObserver.set(
                    dataEntry.modDate.isEmpty()
                            ? dataEntry.modDate
                            : getString(R.string.mod_data).replace("-MOD_DATA-", dataEntry.modDate)
            );

            gameDataObserver.titleObserver.set(dataEntry.title);
            gameDataObserver.langObserver.set(dataEntry.lang);
            gameDataObserver.playerObserver.set(dataEntry.player);
            gameDataObserver.iconUriObserver.set(dataEntry.gameIconUri);
            gameDataObserver.fileUrlObserver.set(dataEntry.fileUrl);
            fragmentStockGameBinding.setData(gameDataObserver);
        });

        return fragmentStockGameBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragmentStockGameBinding.editButton.setOnClickListener(view1 ->
                stockViewModel.showDialogFragment(
                        getParentFragmentManager(),
                        StockDialogType.EDIT_DIALOG,
                        null
                )
        );
        fragmentStockGameBinding.playButton.setOnClickListener(view2 -> {
            var intent = stockViewModel.createPlayGameIntent();
            if (intent == null) {
                stockViewModel.showDialogFragment(
                        getParentFragmentManager(),
                        StockDialogType.ERROR_DIALOG,
                        getString(R.string.gamesFolderError)
                );
                return;
            }
            switch (stockViewModel.getCountGameFiles()) {
                case 0 -> stockViewModel.showDialogFragment(
                        getParentFragmentManager(),
                        StockDialogType.ERROR_DIALOG,
                        getString(R.string.gameFolderEmpty)
                );
                case 1 -> {
                    var chosenGameUri = stockViewModel.getGameFile(0);
                    if (chosenGameUri == null) return;
                    var convert = String.valueOf(chosenGameUri);
                    if (!isNotEmptyOrBlank(convert)) return;

                    intent.putExtra("gameFileUri", convert);
                    requireActivity().startActivity(intent);
                }
                default -> {
                    stockViewModel.showDialogFragment(
                            getParentFragmentManager(),
                            StockDialogType.SELECT_DIALOG,
                            null
                    );
                    stockViewModel.outputIntObserver.observe(getViewLifecycleOwner(), integer -> {
                        var chosenGameUri = stockViewModel.getGameFile(integer);
                        if (chosenGameUri == null) return;
                        var convert = String.valueOf(chosenGameUri);
                        if (!isNotEmptyOrBlank(convert)) return;

                        intent.putExtra("gameFileUri", convert);
                        requireActivity().startActivity(intent);
                    });
                }
            }
        });
        fragmentStockGameBinding.downloadButton.setOnClickListener(view3 -> {
            var data = stockViewModel.currGameEntry;
            if (data == null) return;
            stockViewModel.startFileDownload(data);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        fragmentStockGameBinding = null;

        var appCompatActivity = ((AppCompatActivity) requireActivity());

        if (appCompatActivity.getSupportActionBar() != null) {
            appCompatActivity.getSupportActionBar().show();
        }
    }
}
