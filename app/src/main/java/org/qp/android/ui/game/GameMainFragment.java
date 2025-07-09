package org.qp.android.ui.game;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.databinding.FragmentGameMainBinding;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;
import org.qp.android.ui.dialogs.GameDialogType;

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
    private GameActionRecyclerView actionsView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        var gameMainBinding = FragmentGameMainBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);

        // ConstraintLayout
        layoutTop = gameMainBinding.layoutTop;
        layoutTop.setBackgroundColor(viewModel.getBackgroundColor());

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
                viewModel.showLibDialog(pathToPic, GameDialogType.IMAGE_DIALOG);
            }
        }, "img");
        viewModel.mainDescLiveData.observe(getViewLifecycleOwner(), desc -> {
            if (viewModel.getSettingsController().isUseAutoscroll) {
                mainDescView.postDelayed(onAutoScroll, 500);
            }
            mainDescView.loadDataWithBaseURL(
                    "file:///",
                    desc,
                    "text/html",
                    "UTF-8",
                    null
            );
        });

        // RecyclerView
        actionsView = gameMainBinding.gameMainActions;
        actionsView.setMaxVisibleItems(viewModel.getSettingsController().countActsVis);
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

        // FAB
        gameMainBinding.gameActionVisibility.setOnClickListener(v -> {
            var shortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
            if (actionsView.getVisibility() == View.GONE) {
                actionsView.animate()
                        .alpha(1f)
                        .setDuration(shortAnimationDuration)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                actionsView.setVisibility(View.VISIBLE);
                            }
                        });
            } else {
                actionsView.animate()
                        .alpha(0f)
                        .setDuration(shortAnimationDuration)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                actionsView.setVisibility(View.GONE);
                            }
                        });
            }
        });

        viewModel.actsListLiveData.observe(getViewLifecycleOwner(), actions -> {
            actionsView.setBackgroundColor(viewModel.getBackgroundColor());
            viewModel.getDefaultItemAdapter(adapter).submitList(actions);
        });

        viewModel.actsVisibility.observe(getViewLifecycleOwner(), gameMainBinding.gameActionVisibility::setEnabled);

        // Settings
        viewModel.controllerObserver.observe(getViewLifecycleOwner(), settingsController -> {
            actionsView.setMaxVisibleItems(settingsController.countActsVis);
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
