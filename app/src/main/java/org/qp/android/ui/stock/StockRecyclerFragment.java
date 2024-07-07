package org.qp.android.ui.stock;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.qp.android.databinding.FragmentRecyclerBinding;
import org.qp.android.dto.stock.GameDataList;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;
import org.qp.android.model.repository.RemoteGame;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockRecyclerFragment extends Fragment {

    private StockViewModel stockViewModel;
    private RecyclerView mRecyclerView;

    private int pageNumber;

    @NonNull
    public static StockRecyclerFragment newInstance(int numberPage) {
        var args = new Bundle();
        args.putInt("numPage", numberPage);
        var fragment = new StockRecyclerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageNumber = getArguments() != null ? getArguments().getInt("numPage") : 0;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        var recyclerBinding = FragmentRecyclerBinding.inflate(inflater);
        mRecyclerView = recyclerBinding.shareRecyclerView;
        mRecyclerView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        stockViewModel = new ViewModelProvider(requireActivity()).get(StockViewModel.class);

        if (pageNumber == 0) {
            stockViewModel.getGameDataList().observe(getViewLifecycleOwner(), gameData -> {
                var adapter = new StockGamesRecycler(requireActivity());
                adapter.submitList(gameData);
                mRecyclerView.setAdapter(adapter);
            });
        } else {
            var remoteGame = new RemoteGame();
            remoteGame.getRemoteGameData(new Callback<>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    var mapper = new XmlMapper();
                    try (var body = response.body()){
                        if (body == null) return;
                        var string = body.string();
                        var value = mapper.readValue(string, GameDataList.class);
                        var adapter = new StockGamesRecycler(requireActivity());
                        adapter.submitTList(value.game);
                        mRecyclerView.setAdapter(adapter);
                    } catch (IOException e) {
                        Log.e(getTag(), "Error:", e);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                    Log.e(getTag(), "Error:", throwable);
                }
            });
        }


        stockViewModel.activityObserver.observe(getViewLifecycleOwner() , stockActivity ->
                stockActivity.setRecyclerView(mRecyclerView));
        return recyclerBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                requireContext() ,
                mRecyclerView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view , int position) {
                        stockViewModel.doOnShowGameFragment(position);
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {
                        stockViewModel.doOnShowActionMode();
                    }
                }));
        mRecyclerView.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public boolean performAccessibilityAction(@NonNull View host ,
                                                      int action ,
                                                      @Nullable Bundle args) {
                switch (action) {
                    case AccessibilityNodeInfo.ACTION_CLICK -> host.performClick();
                    case AccessibilityNodeInfo.ACTION_LONG_CLICK -> host.performLongClick();
                }
                return super.performAccessibilityAction(host , action , args);
            }
        });
    }
}
