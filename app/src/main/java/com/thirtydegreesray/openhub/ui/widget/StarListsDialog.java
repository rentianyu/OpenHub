package com.thirtydegreesray.openhub.ui.widget;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.mvp.model.StarList;
import com.thirtydegreesray.openhub.mvp.presenter.RepositoriesPresenter;

import java.util.ArrayList;

/**
 * 星标目录（GitHub Lists）管理对话框：重命名 / 删除
 */
public class StarListsDialog {

    public static void showManage(final Activity activity, final RepositoriesPresenter presenter) {
        if (activity == null || presenter == null) {
            return;
        }
        final ArrayList<StarList> lists = presenter.getStarLists();
        if (lists == null || lists.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setMessage(R.string.star_list_no_data)
                    .setPositiveButton(R.string.cancel, null)
                    .show();
            return;
        }
        final String[] names = new String[lists.size()];
        for (int i = 0; i < lists.size(); i++) {
            StarList starList = lists.get(i);
            names[i] = starList.getName() + " (" + starList.getCount() + ")";
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.star_list_manage)
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which >= 0 && which < lists.size()) {
                            showItemActions(activity, presenter, lists.get(which));
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void showItemActions(final Activity activity,
                                        final RepositoriesPresenter presenter,
                                        final StarList starList) {
        final String[] actions = new String[]{
                activity.getString(R.string.star_list_rename),
                activity.getString(R.string.star_list_delete)
        };
        new AlertDialog.Builder(activity)
                .setTitle(starList.getName())
                .setItems(actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showRenameDialog(activity, presenter, starList);
                        } else {
                            showDeleteConfirm(activity, presenter, starList);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void showRenameDialog(final Activity activity,
                                         final RepositoriesPresenter presenter,
                                         final StarList starList) {
        final EditText editText = new EditText(activity);
        editText.setSingleLine(true);
        editText.setText(starList.getName());
        if (editText.getText() != null) {
            editText.setSelection(editText.getText().length());
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.star_list_rename)
                .setView(editText)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = editText.getText().toString().trim();
                        if (name.length() == 0) {
                            return;
                        }
                        presenter.renameStarList(starList, name);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void showDeleteConfirm(final Activity activity,
                                          final RepositoriesPresenter presenter,
                                          final StarList starList) {
        new AlertDialog.Builder(activity)
                .setMessage(activity.getString(R.string.star_list_delete_confirm, starList.getName()))
                .setPositiveButton(R.string.star_list_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.deleteStarList(starList);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
