package org.qp.android.ui.game;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.databinding.FragmentGameMainBinding;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;
import org.qp.android.questopiabundle.lib.LibTypeDialog;

public class GameMainFragment extends Fragment {

    private final GameItemAdapter adapter = new GameItemAdapter();
    private GameViewModel viewModel;
    private ConstraintLayout layoutTop;
    private WebView mainDescView;
    private final Runnable onAutoScroll = () -> {
        if (!isAdded()) return;
        if (mainDescView.getContentHeight()
                * getResources().getDisplayMetrics().density
                > mainDescView.getScrollY()) {
            mainDescView.scrollBy(0, mainDescView.getHeight());
        }
    };
    private View separatorView;
    private RecyclerView actionsView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        var gameMainBinding = FragmentGameMainBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
        gameMainBinding.setGameViewModel(viewModel);

        layoutTop = gameMainBinding.layoutTop;
        layoutTop.setBackgroundColor(viewModel.getBackgroundColor());
        var constraintSet = new ConstraintSet();
        constraintSet.clone(layoutTop);
        constraintSet.setVerticalWeight(R.id.gameMainDesc, 1.0f - (viewModel.getSettingsController().actionsHeightRatio));
        constraintSet.setVerticalWeight(R.id.gameMainActions, (viewModel.getSettingsController().actionsHeightRatio));
        constraintSet.applyTo(layoutTop);

        separatorView = gameMainBinding.separator;
        if (viewModel.getSettingsController().isUseSeparator) {
            var defSepColor = requireContext().getColor(R.color.materialcolorpicker__grey);
            separatorView.setBackgroundColor(defSepColor);
        } else {
            separatorView.setBackgroundColor(viewModel.getBackgroundColor());
        }

        // WebView
        mainDescView = viewModel.getDefaultWebClient(gameMainBinding.gameMainDesc);
        mainDescView.setBackgroundColor(viewModel.getBackgroundColor());
        mainDescView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onClickImage(String src) {
                if (src == null) return;
                var imageUri = viewModel.getImageUriFromPath(src);
                if (imageUri.equals(Uri.EMPTY)) return;
                var pathToPic = String.valueOf(imageUri);
                viewModel.showLibDialog(LibTypeDialog.DIALOG_PICTURE, pathToPic);
            }
        }, "img");
        if (viewModel.getSettingsController().isUseAutoscroll) {
            mainDescView.postDelayed(onAutoScroll, 300);
        }
        viewModel.getMainDescObserver().observe(getViewLifecycleOwner(), desc -> {
            mainDescView.loadDataWithBaseURL(
                    "file:///",
                    desc,
                    "text/html",
                    "UTF-8",
                    "");
        });

        // RecyclerView
        actionsView = gameMainBinding.gameMainActions;
        var manager = (LinearLayoutManager) actionsView.getLayoutManager();
        var dividerItemDecoration = new DividerItemDecoration(
                actionsView.getContext(),
                manager.getOrientation());
        actionsView.addItemDecoration(dividerItemDecoration);
        actionsView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        actionsView.setBackgroundColor(viewModel.getBackgroundColor());
        adapter.setStateRestorationPolicy(
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        actionsView.setAdapter(adapter);

        viewModel.actsListLiveData.observe(getViewLifecycleOwner(), actions -> {
            actionsView.setBackgroundColor(viewModel.getBackgroundColor());
            adapter.typeface = viewModel.getSettingsController().getTypeface();
            adapter.textSize = viewModel.getFontSize();
            adapter.textColor = viewModel.getTextColor();
            adapter.linkTextColor = viewModel.getLinkColor();
            adapter.backgroundColor = viewModel.getBackgroundColor();
            adapter.submitList(actions);
        });

        // Settings
        viewModel.getControllerObserver().observe(getViewLifecycleOwner(), settingsController -> {
            if (settingsController.isUseSeparator) {
                var defSepColor = requireContext().getColor(R.color.materialcolorpicker__grey);
                separatorView.setBackgroundColor(defSepColor);
            } else {
                separatorView.setBackgroundColor(viewModel.getBackgroundColor());
            }

            constraintSet.clone(layoutTop);
            constraintSet.setVerticalWeight(R.id.gameMainDesc, 1.0f - (settingsController.actionsHeightRatio));
            constraintSet.setVerticalWeight(R.id.gameMainActions, (settingsController.actionsHeightRatio));
            constraintSet.applyTo(layoutTop);

            layoutTop.setBackgroundColor(viewModel.getBackgroundColor());
            mainDescView.setBackgroundColor(viewModel.getBackgroundColor());
            actionsView.setBackgroundColor(viewModel.getBackgroundColor());
            gameMainBinding.getRoot().refreshDrawableState();
        });

        return gameMainBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        actionsView.addOnItemTouchListener(new RecyclerItemClickListener(
                requireContext(),
                actionsView,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        viewModel.onActionClicked(position);
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {

                    }
                }
        ));
    }
}
