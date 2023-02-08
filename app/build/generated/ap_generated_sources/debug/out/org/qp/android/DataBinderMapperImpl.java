package org.qp.android;

import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import androidx.databinding.DataBinderMapper;
import androidx.databinding.DataBindingComponent;
import androidx.databinding.ViewDataBinding;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.qp.android.databinding.ActivityGameBindingImpl;
import org.qp.android.databinding.ActivityStockBindingImpl;
import org.qp.android.databinding.DialogEditBindingImpl;
import org.qp.android.databinding.DialogImageBindingImpl;
import org.qp.android.databinding.DialogInstallBindingImpl;
import org.qp.android.databinding.FragmentPluginBindingImpl;
import org.qp.android.databinding.FragmentStockGameBindingImpl;
import org.qp.android.databinding.ListGameItemBindingImpl;
import org.qp.android.databinding.ListItemGameBindingImpl;
import org.qp.android.databinding.ListItemPluginBindingImpl;

public class DataBinderMapperImpl extends DataBinderMapper {
  private static final int LAYOUT_ACTIVITYGAME = 1;

  private static final int LAYOUT_ACTIVITYSTOCK = 2;

  private static final int LAYOUT_DIALOGEDIT = 3;

  private static final int LAYOUT_DIALOGIMAGE = 4;

  private static final int LAYOUT_DIALOGINSTALL = 5;

  private static final int LAYOUT_FRAGMENTPLUGIN = 6;

  private static final int LAYOUT_FRAGMENTSTOCKGAME = 7;

  private static final int LAYOUT_LISTGAMEITEM = 8;

  private static final int LAYOUT_LISTITEMGAME = 9;

  private static final int LAYOUT_LISTITEMPLUGIN = 10;

  private static final SparseIntArray INTERNAL_LAYOUT_ID_LOOKUP = new SparseIntArray(10);

  static {
    INTERNAL_LAYOUT_ID_LOOKUP.put(org.qp.android.R.layout.activity_game, LAYOUT_ACTIVITYGAME);
    INTERNAL_LAYOUT_ID_LOOKUP.put(org.qp.android.R.layout.activity_stock, LAYOUT_ACTIVITYSTOCK);
    INTERNAL_LAYOUT_ID_LOOKUP.put(org.qp.android.R.layout.dialog_edit, LAYOUT_DIALOGEDIT);
    INTERNAL_LAYOUT_ID_LOOKUP.put(org.qp.android.R.layout.dialog_image, LAYOUT_DIALOGIMAGE);
    INTERNAL_LAYOUT_ID_LOOKUP.put(org.qp.android.R.layout.dialog_install, LAYOUT_DIALOGINSTALL);
    INTERNAL_LAYOUT_ID_LOOKUP.put(org.qp.android.R.layout.fragment_plugin, LAYOUT_FRAGMENTPLUGIN);
    INTERNAL_LAYOUT_ID_LOOKUP.put(org.qp.android.R.layout.fragment_stock_game, LAYOUT_FRAGMENTSTOCKGAME);
    INTERNAL_LAYOUT_ID_LOOKUP.put(org.qp.android.R.layout.list_game_item, LAYOUT_LISTGAMEITEM);
    INTERNAL_LAYOUT_ID_LOOKUP.put(org.qp.android.R.layout.list_item_game, LAYOUT_LISTITEMGAME);
    INTERNAL_LAYOUT_ID_LOOKUP.put(org.qp.android.R.layout.list_item_plugin, LAYOUT_LISTITEMPLUGIN);
  }

  @Override
  public ViewDataBinding getDataBinder(DataBindingComponent component, View view, int layoutId) {
    int localizedLayoutId = INTERNAL_LAYOUT_ID_LOOKUP.get(layoutId);
    if(localizedLayoutId > 0) {
      final Object tag = view.getTag();
      if(tag == null) {
        throw new RuntimeException("view must have a tag");
      }
      switch(localizedLayoutId) {
        case  LAYOUT_ACTIVITYGAME: {
          if ("layout/activity_game_0".equals(tag)) {
            return new ActivityGameBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for activity_game is invalid. Received: " + tag);
        }
        case  LAYOUT_ACTIVITYSTOCK: {
          if ("layout/activity_stock_0".equals(tag)) {
            return new ActivityStockBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for activity_stock is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGEDIT: {
          if ("layout/dialog_edit_0".equals(tag)) {
            return new DialogEditBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_edit is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGIMAGE: {
          if ("layout/dialog_image_0".equals(tag)) {
            return new DialogImageBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_image is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGINSTALL: {
          if ("layout/dialog_install_0".equals(tag)) {
            return new DialogInstallBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_install is invalid. Received: " + tag);
        }
        case  LAYOUT_FRAGMENTPLUGIN: {
          if ("layout/fragment_plugin_0".equals(tag)) {
            return new FragmentPluginBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for fragment_plugin is invalid. Received: " + tag);
        }
        case  LAYOUT_FRAGMENTSTOCKGAME: {
          if ("layout/fragment_stock_game_0".equals(tag)) {
            return new FragmentStockGameBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for fragment_stock_game is invalid. Received: " + tag);
        }
        case  LAYOUT_LISTGAMEITEM: {
          if ("layout/list_game_item_0".equals(tag)) {
            return new ListGameItemBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for list_game_item is invalid. Received: " + tag);
        }
        case  LAYOUT_LISTITEMGAME: {
          if ("layout/list_item_game_0".equals(tag)) {
            return new ListItemGameBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for list_item_game is invalid. Received: " + tag);
        }
        case  LAYOUT_LISTITEMPLUGIN: {
          if ("layout/list_item_plugin_0".equals(tag)) {
            return new ListItemPluginBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for list_item_plugin is invalid. Received: " + tag);
        }
      }
    }
    return null;
  }

  @Override
  public ViewDataBinding getDataBinder(DataBindingComponent component, View[] views, int layoutId) {
    if(views == null || views.length == 0) {
      return null;
    }
    int localizedLayoutId = INTERNAL_LAYOUT_ID_LOOKUP.get(layoutId);
    if(localizedLayoutId > 0) {
      final Object tag = views[0].getTag();
      if(tag == null) {
        throw new RuntimeException("view must have a tag");
      }
      switch(localizedLayoutId) {
      }
    }
    return null;
  }

  @Override
  public int getLayoutId(String tag) {
    if (tag == null) {
      return 0;
    }
    Integer tmpVal = InnerLayoutIdLookup.sKeys.get(tag);
    return tmpVal == null ? 0 : tmpVal;
  }

  @Override
  public String convertBrIdToString(int localId) {
    String tmpVal = InnerBrLookup.sKeys.get(localId);
    return tmpVal;
  }

  @Override
  public List<DataBinderMapper> collectDependencies() {
    ArrayList<DataBinderMapper> result = new ArrayList<DataBinderMapper>(1);
    result.add(new androidx.databinding.library.baseAdapters.DataBinderMapperImpl());
    return result;
  }

  private static class InnerBrLookup {
    static final SparseArray<String> sKeys = new SparseArray<String>(9);

    static {
      sKeys.put(0, "_all");
      sKeys.put(1, "dialogFragment");
      sKeys.put(2, "gameData");
      sKeys.put(3, "gameViewModel");
      sKeys.put(4, "pluginInfo");
      sKeys.put(5, "pluginViewModel");
      sKeys.put(6, "qpListItem");
      sKeys.put(7, "stockVM");
      sKeys.put(8, "viewModel");
    }
  }

  private static class InnerLayoutIdLookup {
    static final HashMap<String, Integer> sKeys = new HashMap<String, Integer>(10);

    static {
      sKeys.put("layout/activity_game_0", org.qp.android.R.layout.activity_game);
      sKeys.put("layout/activity_stock_0", org.qp.android.R.layout.activity_stock);
      sKeys.put("layout/dialog_edit_0", org.qp.android.R.layout.dialog_edit);
      sKeys.put("layout/dialog_image_0", org.qp.android.R.layout.dialog_image);
      sKeys.put("layout/dialog_install_0", org.qp.android.R.layout.dialog_install);
      sKeys.put("layout/fragment_plugin_0", org.qp.android.R.layout.fragment_plugin);
      sKeys.put("layout/fragment_stock_game_0", org.qp.android.R.layout.fragment_stock_game);
      sKeys.put("layout/list_game_item_0", org.qp.android.R.layout.list_game_item);
      sKeys.put("layout/list_item_game_0", org.qp.android.R.layout.list_item_game);
      sKeys.put("layout/list_item_plugin_0", org.qp.android.R.layout.list_item_plugin);
    }
  }
}
