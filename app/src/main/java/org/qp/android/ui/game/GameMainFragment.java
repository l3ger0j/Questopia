package org.qp.android.ui.game;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.databinding.FragmentGameMainBinding;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;

public class GameMainFragment extends GamePatternFragment {
    private GameViewModel viewModel;
    private ConstraintLayout layoutTop;
    private WebView mainDescView;
    private View separatorView;
    private RecyclerView actionsView;

    private final Runnable onScroll = new Runnable() {
        @Override
        public void run() {
            if (mainDescView.getContentHeight()
                    * getResources().getDisplayMetrics().density
                    >= mainDescView.getScrollY() ){
                mainDescView.scrollBy(0, mainDescView.getHeight());
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        org.qp.android.databinding.FragmentGameMainBinding gameMainBinding =
                FragmentGameMainBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
        gameMainBinding.setGameViewModel(viewModel);

        layoutTop = gameMainBinding.layoutTop;
        layoutTop.setBackgroundColor(viewModel.getBackgroundColor());
        var constraintSet = new ConstraintSet();
        constraintSet.clone(layoutTop);
        constraintSet.setVerticalWeight(R.id.main_desc , 1.0f - (viewModel.getSettingsController().actionsHeightRatio));
        constraintSet.setVerticalWeight(R.id.actions , (viewModel.getSettingsController().actionsHeightRatio));
        constraintSet.applyTo(layoutTop);

        separatorView = gameMainBinding.separator;
        if (viewModel.getSettingsController().isUseSeparator) {
            separatorView.setBackgroundColor(viewModel.getBackgroundColor());
        } else {
            separatorView.setBackgroundColor(
                    requireContext().getColor(R.color.materialcolorpicker__grey));
        }

        // WebView
        mainDescView = gameMainBinding.mainDesc;
        mainDescView.setBackgroundColor(viewModel.getBackgroundColor());
        mainDescView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onClickImage(String src) {
                listener.showPictureDialog(viewModel.getImageAbsolutePath(src));
            }
        }, "img");
        if (viewModel.getSettingsController().isUseAutoscroll) {
            mainDescView.postDelayed(onScroll, 300);
        }
        viewModel.getMainDescObserver().observe(getViewLifecycleOwner() , desc ->
                mainDescView.loadDataWithBaseURL(
                        "file:///",
                        desc,
                        "text/html",
                        "UTF-8",
                        null));

        // RecyclerView
        actionsView = gameMainBinding.actions;
        actionsView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        actionsView.setBackgroundColor(viewModel.getBackgroundColor());
        viewModel.getActionObserver().observe(getViewLifecycleOwner() , actions -> {
            actionsView.setBackgroundColor(viewModel.getBackgroundColor());
            actionsView.setAdapter(actions);
        });

        // Settings
        viewModel.getControllerObserver().observe(getViewLifecycleOwner() , settingsController -> {
            if (settingsController.isUseSeparator) {
                separatorView.setBackgroundColor(viewModel.getBackgroundColor());
            } else {
                separatorView.setBackgroundColor(
                        requireContext().getColor(R.color.materialcolorpicker__grey));
            }

            constraintSet.clone(layoutTop);
            constraintSet.setVerticalWeight(R.id.main_desc , 1.0f - (settingsController.actionsHeightRatio));
            constraintSet.setVerticalWeight(R.id.actions , (settingsController.actionsHeightRatio));
            constraintSet.applyTo(layoutTop);

            layoutTop.setBackgroundColor(viewModel.getBackgroundColor());
            mainDescView.setBackgroundColor(viewModel.getBackgroundColor());
            actionsView.setBackgroundColor(viewModel.getBackgroundColor());
            gameMainBinding.getRoot().refreshDrawableState();
        });
        return gameMainBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        actionsView.addOnItemTouchListener(new RecyclerItemClickListener(
                requireContext() ,
                actionsView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view , int position) {
                        if (viewModel.isWaitTaskDone()) viewModel.startTimer();
                        viewModel.getLibQspProxy().onActionClicked(position);
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {

                    }
                }
        ));
    }
}
