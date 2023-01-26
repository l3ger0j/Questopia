package org.qp.android.viewModel;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import org.qp.android.R;
import org.qp.android.dto.stock.GameData;
import org.qp.android.view.stock.fragment.StockGameFragment;

public class FragmentStockGame extends ViewModel {
    private GameData gameData;

    public ObservableField<StockGameFragment> fragmentObservableField =
            new ObservableField<>();

    public void setGameData(GameData gameData) {
        this.gameData = gameData;
    }

    public GameData getGameData() {
        return gameData;
    }

    public String getGameAuthor () {
        if (gameData.author.length() > 0 && fragmentObservableField.get() != null) {
            return fragmentObservableField.get().getString(R.string.author).replace("-AUTHOR-" , gameData.author);
        } else {
            return "";
        }
    }

    public String getGameVersion () {
        if (gameData.version.length() > 0 && fragmentObservableField.get() != null) {
            return fragmentObservableField.get().getString(R.string.version).replace("-VERSION-" , gameData.version);
        } else {
            return "";
        }
    }

    public String getGameType () {
        if (gameData.fileExt.length() > 0 && fragmentObservableField.get() != null) {
            if (gameData.fileExt.equals("aqsp")) {
                return fragmentObservableField.get().getString(R.string.fileType).replace("-TYPE-", gameData.fileExt)
                        + " " + fragmentObservableField.get().getString(R.string.experimental);
            }
            return fragmentObservableField.get().getString(R.string.fileType).replace("-TYPE-", gameData.fileExt);
        } else {
            return "";
        }
    }

    public String getGameSize () {
        if (gameData.getFileSize() != null  && fragmentObservableField.get() != null) {
            return fragmentObservableField.get().getString(R.string.fileSize).replace("-SIZE-" , gameData.getFileSize());
        } else {
            return "";
        }
    }
}
