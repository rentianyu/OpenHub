package com.thirtydegreesray.openhub.ui.widget;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.mvp.model.StarList;

import java.util.ArrayList;

/**
 * 仓库归属的星标目录多选对话框
 */
public class RepoStarListsDialog {

    public interface OnConfirmListener {
        void onConfirm(ArrayList<String> listIds);
    }

    public static void show(final Activity activity, final ArrayList<StarList> starLists,
                            ArrayList<String> selectedIds, final OnConfirmListener listener) {
        if (activity == null || starLists == null || starLists.isEmpty()) {
            return;
        }
        final String[] names = new String[starLists.size()];
        final boolean[] checked = new boolean[starLists.size()];
        for (int i = 0; i < starLists.size(); i++) {
            StarList starList = starLists.get(i);
            names[i] = starList.getName() + " (" + starList.getCount() + ")";
            checked[i] = selectedIds != null && selectedIds.contains(starList.getId());
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.repo_lists_title)
                .setMultiChoiceItems(names, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (which >= 0 && which < checked.length) {
                            checked[which] = isChecked;
                        }
                    }
                })
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<String> listIds = new ArrayList<>();
                        for (int i = 0; i < checked.length; i++) {
                            if (checked[i]) {
                                listIds.add(starLists.get(i).getId());
                            }
                        }
                        if (listener != null) {
                            listener.onConfirm(listIds);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
