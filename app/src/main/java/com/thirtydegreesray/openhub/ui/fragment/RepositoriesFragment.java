

package com.thirtydegreesray.openhub.ui.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.EditText;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerFragmentComponent;
import com.thirtydegreesray.openhub.inject.module.FragmentModule;
import com.thirtydegreesray.openhub.mvp.contract.IRepositoriesContract;
import com.thirtydegreesray.openhub.mvp.model.Collection;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.model.SearchModel;
import com.thirtydegreesray.openhub.mvp.model.StarList;
import com.thirtydegreesray.openhub.mvp.model.Topic;
import com.thirtydegreesray.openhub.mvp.model.TrendingLanguage;
import com.thirtydegreesray.openhub.mvp.model.filter.RepositoriesFilter;
import com.thirtydegreesray.openhub.mvp.model.filter.TrendingSince;
import com.thirtydegreesray.openhub.mvp.presenter.RepositoriesPresenter;
import com.thirtydegreesray.openhub.ui.activity.RepositoryActivity;
import com.thirtydegreesray.openhub.ui.activity.TrendingActivity;
import com.thirtydegreesray.openhub.ui.adapter.RepositoriesAdapter;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.ui.fragment.base.OnDrawerSelectedListener;
import com.thirtydegreesray.openhub.ui.widget.StarListsDialog;
import com.thirtydegreesray.openhub.util.BundleHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 2017/7/18.
 *
 * @author ThirtyDegreesRay
 */

public class RepositoriesFragment extends ListFragment<RepositoriesPresenter, RepositoriesAdapter>
            implements IRepositoriesContract.View, OnDrawerSelectedListener,
        TrendingActivity.LanguageUpdateListener{

    public enum RepositoriesType{
        OWNED, PUBLIC, STARRED, TRENDING, SEARCH, FORKS, TRACE, BOOKMARK, COLLECTION, TOPIC
    }

    public static RepositoriesFragment create(@NonNull RepositoriesType type,
                                              @NonNull String user){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder().put("type", type)
                .put("user", user).build());
        return fragment;
    }

    public static RepositoriesFragment createForCollection(@NonNull Collection collection){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder()
                .put("type", RepositoriesType.COLLECTION)
                .put("collection", collection)
                .build());
        return fragment;
    }

    public static RepositoriesFragment createForTopic(@NonNull Topic topic){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder()
                .put("type", RepositoriesType.TOPIC)
                .put("topic", topic)
                .build());
        return fragment;
    }

    public static RepositoriesFragment createForForks(@NonNull String user, @NonNull String repo){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder()
                .put("type", RepositoriesType.FORKS)
                .put("user", user)
                .put("repo", repo)
                .build());
        return fragment;
    }

    public static RepositoriesFragment createForSearch(@NonNull SearchModel searchModel){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(
                BundleHelper.builder()
                        .put("type", RepositoriesType.SEARCH)
                        .put("searchModel", searchModel)
                        .build()
        );
        return fragment;
    }

    public static RepositoriesFragment createForTrending(@NonNull TrendingSince since){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(
                BundleHelper.builder()
                        .put("type", RepositoriesType.TRENDING)
                        .put("since", since)
                        .build()
        );
        return fragment;
    }

    public static RepositoriesFragment createForTrace(){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder().put("type", RepositoriesType.TRACE).build());
        return fragment;
    }

    public static RepositoriesFragment createForBookmark(){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder().put("type", RepositoriesType.BOOKMARK).build());
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void showRepositories(ArrayList<Repository> repositoryList) {
        adapter.setData(repositoryList);
        adapter.setStarListTags(buildStarListTags(repositoryList));
        postNotifyDataSetChanged();
    }

    /**
     * 星标页为仓库生成「所属目录」标签，其他页面返回空
     */
    @NonNull
    private Map<String, String> buildStarListTags(@Nullable ArrayList<Repository> repositoryList) {
        Map<String, String> tags = new HashMap<>();
        if (repositoryList == null || !RepositoriesType.STARRED.equals(mPresenter.getType())) {
            return tags;
        }
        for (Repository repository : repositoryList) {
            if (repository == null || repository.getFullName() == null) {
                continue;
            }
            String tag = mPresenter.getStarListTag(repository.getFullName());
            if (tag != null) {
                tags.put(repository.getFullName(), tag);
            }
        }
        return tags;
    }

    //====================== 星标目录（GitHub Lists） ======================

    private final static int MENU_ID_STAR_LISTS = 90001;
    private final static int MENU_ID_STAR_LIST_NEW = 90002;
    private final static int MENU_ID_STAR_LIST_MANAGE = 90003;
    private final static int MENU_ID_STAR_LIST_ITEM_BASE = 90100;

    @Override
    public void showStarLists(ArrayList<StarList> starLists) {
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
        if (adapter != null && adapter.getData() != null) {
            adapter.setStarListTags(buildStarListTags(adapter.getData()));
            postNotifyDataSetChanged();
        }
    }

    @NonNull
    private String buildStarListsMenuTitle() {
        String title = getString(R.string.star_lists);
        if (mPresenter.hasStarListFilter()) {
            String name = mPresenter.getStarListName();
            if (name != null) {
                String shortName = name.length() > 10 ? name.substring(0, 10) + "…" : name;
                title = title + " · " + shortName;
            }
        }
        return title;
    }

    private void showNewStarListDialog() {
        if (getActivity() == null) {
            return;
        }
        final EditText editText = new EditText(getActivity());
        editText.setSingleLine(true);
        editText.setHint(R.string.star_list_name_hint);
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.star_list_new)
                .setView(editText)
                .setPositiveButton(R.string.star_list_create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = editText.getText().toString().trim();
                        if (name.length() == 0) {
                            showToast(getString(R.string.star_list_name_empty));
                            return;
                        }
                        mPresenter.createStarList(name);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_list;
    }

    @Override
    protected void setupFragmentComponent(AppComponent appComponent) {
        DaggerFragmentComponent.builder()
                .appComponent(appComponent)
                .fragmentModule(new FragmentModule(this))
                .build()
                .inject(this);
    }

    @Override
    protected void initFragment(Bundle savedInstanceState){
        super.initFragment(savedInstanceState);
        setLoadMoreEnable(!RepositoriesType.COLLECTION.equals(mPresenter.getType()));
    }

    @Override
    protected void onReLoadData() {
        mPresenter.loadRepositories(true, 1);
    }

    @Override
    protected String getEmptyTip() {
        if(RepositoriesType.TRENDING.equals(mPresenter.getType())){
            return String.format(getString(R.string.no_trending_repos), mPresenter.getLanguage().getName());
        }
        return getString(R.string.no_repository);
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        if(RepositoriesType.TRENDING.equals(mPresenter.getType())
                || RepositoriesType.TRACE.equals(mPresenter.getType())
                || RepositoriesType.BOOKMARK.equals(mPresenter.getType())
                || RepositoriesType.COLLECTION.equals(mPresenter.getType())){
            RepositoryActivity.show(getActivity(), adapter.getData().get(position).getOwner().getLogin(),
                    adapter.getData().get(position).getName());
        } else {
            RepositoryActivity.show(getActivity(), adapter.getData().get(position));
        }
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadRepositories(false, page);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mPresenter == null || !RepositoriesType.STARRED.equals(mPresenter.getType())
                || isHidden()) {
            return;
        }
        final ArrayList<StarList> starLists = mPresenter.getStarLists();
        if (starLists == null) {
            return;
        }
        SubMenu subMenu = menu.addSubMenu(Menu.NONE, MENU_ID_STAR_LISTS, 90, buildStarListsMenuTitle());
        subMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        MenuItem allItem = subMenu.add(Menu.NONE, MENU_ID_STAR_LIST_ITEM_BASE, 0,
                getString(R.string.star_lists_all));
        allItem.setCheckable(true);
        allItem.setChecked(!mPresenter.hasStarListFilter());

        for (int i = 0; i < starLists.size(); i++) {
            StarList starList = starLists.get(i);
            MenuItem item = subMenu.add(Menu.NONE, MENU_ID_STAR_LIST_ITEM_BASE + 1 + i, i + 1,
                    starList.getName() + " (" + starList.getCount() + ")");
            item.setCheckable(true);
            item.setChecked(starList.sameAs(mPresenter.getStarList()));
        }

        subMenu.add(Menu.NONE, MENU_ID_STAR_LIST_NEW, 900, getString(R.string.star_list_new));
        subMenu.add(Menu.NONE, MENU_ID_STAR_LIST_MANAGE, 901, getString(R.string.star_list_manage));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == MENU_ID_STAR_LISTS) {
            return true;
        }
        if (itemId == MENU_ID_STAR_LIST_NEW) {
            showNewStarListDialog();
            return true;
        }
        if (itemId == MENU_ID_STAR_LIST_MANAGE) {
            StarListsDialog.showManage(getActivity(), mPresenter);
            return true;
        }
        if (itemId >= MENU_ID_STAR_LIST_ITEM_BASE) {
            switchStarListItem(itemId - MENU_ID_STAR_LIST_ITEM_BASE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void switchStarListItem(int index) {
        ArrayList<StarList> starLists = mPresenter.getStarLists();
        if (index == 0) {
            mPresenter.setStarList(null);
        } else if (starLists != null && index - 1 < starLists.size() && index - 1 >= 0) {
            mPresenter.setStarList(starLists.get(index - 1));
        }
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onFragmentShowed() {
        super.onFragmentShowed();
        if(mPresenter != null) mPresenter.prepareLoadData();
    }

    @Override
    public void onDrawerSelected(@NonNull NavigationView navView, @NonNull MenuItem item) {
        RepositoriesFilter filter = RepositoriesFilter.generateFromDrawer(navView);
        mPresenter.loadRepositories(filter);
    }

    @Override
    public void onLanguageUpdate(TrendingLanguage language) {
        if(mPresenter != null){
            mPresenter.setLanguage(language);
            mPresenter.setLoaded(false);
            mPresenter.prepareLoadData();
        } else {
            getArguments().putParcelable("language", language);
        }
    }

}
