

package com.thirtydegreesray.openhub.mvp.presenter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.AppData;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.Bookmark;
import com.thirtydegreesray.openhub.dao.BookmarkDao;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.dao.LocalRepo;
import com.thirtydegreesray.openhub.dao.Trace;
import com.thirtydegreesray.openhub.dao.TraceDao;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpProgressSubscriber;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.IRepositoryContract;
import com.thirtydegreesray.openhub.mvp.model.Branch;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.model.StarList;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphqlQueries;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphqlRepo;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphqlRequest;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphqlResponse;
import com.thirtydegreesray.openhub.mvp.model.graphql.MutationData;
import com.thirtydegreesray.openhub.mvp.model.graphql.StarListItemsData;
import com.thirtydegreesray.openhub.mvp.model.graphql.StarListsData;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;
import com.thirtydegreesray.openhub.ui.activity.RepositoryActivity;
import com.thirtydegreesray.openhub.util.StarListCache;
import com.thirtydegreesray.openhub.util.StarWishesHelper;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by ThirtyDegreesRay on 2017/8/9 21:42:47
 */

public class RepositoryPresenter extends BasePresenter<IRepositoryContract.View>
        implements IRepositoryContract.Presenter {

    @AutoAccess(dataName = "repository") Repository repository;

    @AutoAccess String owner;
    @AutoAccess String repoName;

    private ArrayList<Branch> branches;
    @AutoAccess Branch curBranch;
    private boolean starred;
    private boolean watched;

    private boolean isStatusChecked = false;
    @AutoAccess boolean isTraceSaved = false;

    private boolean isBookmarkQueried = false;
    private boolean bookmarked = false;

    @Inject
    public RepositoryPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        if (repository != null) {
            owner = repository.getOwner().getLogin();
            repoName = repository.getName();
            initCurBranch();
            mView.showRepo(repository);
            getRepoInfo(false);
            checkStatus();
        } else {
            getRepoInfo(true);
        }
    }

    @Override
    public void loadBranchesAndTags() {
        if (branches != null) {
            mView.showBranchesAndTags(branches, curBranch);
            return;
        }
        HttpProgressSubscriber<ArrayList<Branch>> httpProgressSubscriber =
                new HttpProgressSubscriber<>(
                        mView.getProgressDialog(getLoadTip()),
                        new HttpObserver<ArrayList<Branch>>() {
                            @Override
                            public void onError(Throwable error) {
                                mView.showErrorToast(getErrorTip(error));
                            }

                            @Override
                            public void onSuccess(HttpResponse<ArrayList<Branch>> response) {
                                setTags(response.body());
                                branches.addAll(response.body());
                                mView.showBranchesAndTags(branches, curBranch);
                            }
                        }
                );
        Observable<Response<ArrayList<Branch>>> observable = getRepoService().getBranches(owner, repoName, 100)
                .flatMap(new Func1<Response<ArrayList<Branch>>, Observable<Response<ArrayList<Branch>>>>() {
                    @Override
                    public Observable<Response<ArrayList<Branch>>> call(
                            Response<ArrayList<Branch>> arrayListResponse) {
                        branches = arrayListResponse.body();
                        return getRepoService().getTags(owner, repoName, 100);
                    }
                });
        generalRxHttpExecute(observable, httpProgressSubscriber);
    }

    @Override
    public void starRepo(boolean star) {
        starred = star;
        Observable<Response<ResponseBody>> observable = starred ?
                getRepoService().starRepo(owner, repoName) :
                getRepoService().unstarRepo(owner, repoName);
        executeSimpleRequest(observable);
    }

    @Override
    public void watchRepo(boolean watch) {
        watched = watch;
        Observable<Response<ResponseBody>> observable = watched ?
                getRepoService().watchRepo(owner, repoName) :
                getRepoService().unwatchRepo(owner, repoName);
        executeSimpleRequest(observable);
    }

    @Override
    public void createFork() {
        mView.getProgressDialog(getLoadTip()).show();
        HttpObserver<Repository> httpObserver = new HttpObserver<Repository>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
                mView.getProgressDialog(getLoadTip()).dismiss();
            }

            @Override
            public void onSuccess(HttpResponse<Repository> response) {
                if(response.body() != null) {
                    mView.showSuccessToast(getString(R.string.forked));
                    RepositoryActivity.show(getContext(), response.body());
                } else {
                    mView.showErrorToast(getString(R.string.fork_failed));
                }
                mView.getProgressDialog(getLoadTip()).dismiss();
            }
        };
        generalRxHttpExecute(new IObservableCreator<Repository>() {
            @Override
            public Observable<Response<Repository>> createObservable(boolean forceNetWork) {
                return getRepoService().createFork(owner, repoName);
            }
        }, httpObserver);
    }

    @Override
    public boolean isForkEnable() {
        if(repository != null && !repository.isFork() &&
                !repository.getOwner().getLogin().equals(AppData.INSTANCE.getLoggedUser().getLogin())){
            return true;
        }
        return false;
    }

    private void setTags(ArrayList<Branch> list) {
        for (Branch branch : list) {
            branch.setBranch(false);
        }
    }

    private void getRepoInfo(final boolean isShowLoading) {
        if (isShowLoading) mView.showLoading();
        HttpObserver<Repository> httpObserver =
                new HttpObserver<Repository>() {
                    @Override
                    public void onError(Throwable error) {
                        if (isShowLoading) mView.hideLoading();
                        mView.showErrorToast(getErrorTip(error));
                    }

                    @Override
                    public void onSuccess(HttpResponse<Repository> response) {
                        if (isShowLoading) mView.hideLoading();
                        repository = response.body();
                        initCurBranch();
                        mView.showRepo(repository);
                        checkStatus();
                        saveTrace();
                    }
                };

        generalRxHttpExecute(new IObservableCreator<Repository>() {
            @Override
            public Observable<Response<Repository>> createObservable(boolean forceNetWork) {
                return getRepoService().getRepoInfo(forceNetWork, owner, repoName);
            }
        }, httpObserver, true);
    }

    private void checkStatus(){
        if(isStatusChecked) return;
        isStatusChecked = true;
        checkStarred();
        checkWatched();
    }


    private void checkStarred() {
        checkStatus(
                getRepoService().checkRepoStarred(owner, repoName),
                new CheckStatusCallback() {
                    @Override
                    public void onChecked(boolean status) {
                        starred = status;
                        mView.invalidateOptionsMenu();
                        starWishes();
                    }
                }
        );
    }

    private void checkWatched() {
        checkStatus(
                getRepoService().checkRepoWatched(owner, repoName),
                new CheckStatusCallback() {
                    @Override
                    public void onChecked(boolean status) {
                        watched = status;
                        mView.invalidateOptionsMenu();
                    }
                }
        );
    }

    private void initCurBranch(){
        curBranch = new Branch(repository.getDefaultBranch());
        curBranch.setZipballUrl("https://github.com/".concat(owner).concat("/")
                .concat(repoName).concat("/archive/")
                .concat(curBranch.getName()).concat(".zip"));
        curBranch.setTarballUrl("https://github.com/".concat(owner).concat("/")
                .concat(repoName).concat("/archive/")
                .concat(curBranch.getName()).concat(".tar.gz"));
    }

    public Repository getRepository() {
        return repository;
    }

    public boolean isFork() {
        return repository != null && repository.isFork();
    }

    public boolean isStarred() {
        return starred;
    }

    public boolean isWatched() {
        return watched;
    }

    public String getZipSourceUrl(){
        return curBranch.getZipballUrl();
    }

    public String getZipSourceName(){
        return repoName.concat("-").concat(curBranch.getName()).concat(".zip");
    }

    public String getTarSourceUrl(){
        return curBranch.getTarballUrl();
    }

    public String getTarSourceName(){
        return repoName.concat("-").concat(curBranch.getName()).concat(".tar.gz");
    }

    public void setCurBranch(Branch curBranch) {
        this.curBranch = curBranch;
    }

    public String getRepoName() {
        return repository == null ? repoName : repository.getName();
    }

    private void starWishes(){
        if(!starred && getString(R.string.author_login_id).equals(owner)
                && getString(R.string.app_name).equals(repoName)
                && StarWishesHelper.isStarWishesTipable()){
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!starred && mView != null){
                        mView.showStarWishes();
                    }
                }
            }, 3000);
        }
    }

    @Override
    public boolean isBookmarked() {
        if(!isBookmarkQueried && repository != null){
            bookmarked = daoSession.getBookmarkDao().queryBuilder()
                    .where(BookmarkDao.Properties.RepoId.eq(repository.getId()))
                    .unique() != null;
            isBookmarkQueried = true;
        }
        return bookmarked;
    }

    @Override
    public void bookmark(boolean bookmark) {
        if(repository == null) return;
        bookmarked = bookmark;
        Bookmark bookmarkModel = daoSession.getBookmarkDao().queryBuilder()
                .where(BookmarkDao.Properties.RepoId.eq(repository.getId()))
                .unique();
        if(bookmark && bookmarkModel == null){
            bookmarkModel = new Bookmark(UUID.randomUUID().toString());
            bookmarkModel.setType("repo");
            bookmarkModel.setRepoId((long) repository.getId());
            bookmarkModel.setMarkTime(new Date());
            daoSession.getBookmarkDao().insert(bookmarkModel);
        } else if(!bookmark && bookmarkModel != null){
            daoSession.getBookmarkDao().delete(bookmarkModel);
        }
    }

    private void saveTrace(){
        daoSession.runInTx(() ->{
            if(!isTraceSaved){
                Trace trace = daoSession.getTraceDao().queryBuilder()
                        .where(TraceDao.Properties.RepoId.eq(repository.getId()))
                        .unique();

                if(trace == null){
                    trace = new Trace(UUID.randomUUID().toString());
                    trace.setType("repo");
                    trace.setRepoId((long) repository.getId());
                    Date curDate = new Date();
                    trace.setStartTime(curDate);
                    trace.setLatestTime(curDate);
                    trace.setTraceNum(1);
                    daoSession.getTraceDao().insert(trace);
                } else {
                    trace.setTraceNum(trace.getTraceNum() + 1);
                    trace.setLatestTime(new Date());
                    daoSession.getTraceDao().update(trace);
                }
            }

            LocalRepo localRepo = daoSession.getLocalRepoDao().load((long) repository.getId());
            LocalRepo updateRepo = repository.toLocalRepo();
            if(localRepo == null){
                daoSession.getLocalRepoDao().insert(updateRepo);
            } else {
                daoSession.getLocalRepoDao().update(updateRepo);
            }
        });
        isTraceSaved = true;
    }

    //========================== 星标目录（GitHub Lists） ==========================

    @Override
    public void loadRepoStarLists() {
        final StarListCache cache = StarListCache.getInstance();
        if (cache.isValid() && cache.hasStarLists() && cache.isMembershipLoaded()) {
            if (mView != null) {
                mView.showRepoStarLists(cache.getStarLists(), buildRepoListIds(cache));
            }
            return;
        }
        if (cache.hasStarLists()) {
            cache.startMemberships();
            loadRepoMembership(cache, 0);
            return;
        }
        generalRxHttpExecute(
                new IObservableCreator<GraphqlResponse<StarListsData>>() {
                    @Nullable
                    @Override
                    public Observable<Response<GraphqlResponse<StarListsData>>> createObservable(boolean forceNetWork) {
                        return getGraphqlService()
                                .getStarLists(new GraphqlRequest(GraphqlQueries.QUERY_STAR_LISTS));
                    }
                },
                new HttpObserver<GraphqlResponse<StarListsData>>() {
                    @Override
                    public void onError(@NonNull Throwable error) {
                        if (mView != null) {
                            mView.showErrorToast(getErrorTip(error));
                        }
                    }

                    @Override
                    public void onSuccess(@NonNull HttpResponse<GraphqlResponse<StarListsData>> response) {
                        GraphqlResponse<StarListsData> body = response.body();
                        if (body == null || body.hasErrors() || body.getData() == null
                                || body.getData().getViewer() == null
                                || body.getData().getViewer().getLists() == null) {
                            if (mView != null) {
                                mView.showErrorToast(body == null ? "" : body.getErrorMessages());
                            }
                            return;
                        }
                        ArrayList<StarList> lists = body.getData().getViewer().getLists().getNodes();
                        if (lists == null) {
                            lists = new ArrayList<>();
                        }
                        cache.setStarLists(lists);
                        cache.startMemberships();
                        loadRepoMembership(cache, 0);
                    }
                }, false);
    }

    private ArrayList<String> buildRepoListIds(StarListCache cache) {
        ArrayList<String> listIds = new ArrayList<>();
        if (repository == null || repository.getFullName() == null) {
            return listIds;
        }
        ArrayList<StarList> lists = cache.getMember(repository.getFullName());
        if (lists != null) {
            for (StarList list : lists) {
                listIds.add(list.getId());
            }
        }
        return listIds;
    }

    private void loadRepoMembership(final StarListCache cache, final int index) {
        final ArrayList<StarList> lists = cache.getStarLists();
        if (lists == null || index >= lists.size()) {
            if (mView != null) {
                mView.showRepoStarLists(lists, buildRepoListIds(cache));
            }
            return;
        }
        final StarList starList = lists.get(index);
        generalRxHttpExecute(
                new IObservableCreator<GraphqlResponse<StarListItemsData>>() {
                    @Nullable
                    @Override
                    public Observable<Response<GraphqlResponse<StarListItemsData>>> createObservable(boolean forceNetWork) {
                        return getGraphqlService().getStarListItems(
                                GraphqlRequest.withVariable(GraphqlQueries.QUERY_STAR_LIST_MEMBERS,
                                        "id", starList.getId()));
                    }
                },
                new HttpObserver<GraphqlResponse<StarListItemsData>>() {
                    @Override
                    public void onError(@NonNull Throwable error) {
                        loadRepoMembership(cache, index + 1);
                    }

                    @Override
                    public void onSuccess(@NonNull HttpResponse<GraphqlResponse<StarListItemsData>> response) {
                        GraphqlResponse<StarListItemsData> body = response.body();
                        if (body != null && !body.hasErrors() && body.getData() != null
                                && body.getData().getNode() != null
                                && body.getData().getNode().getItems() != null) {
                            ArrayList<GraphqlRepo> nodes = body.getData().getNode().getItems().getNodes();
                            if (nodes != null) {
                                for (GraphqlRepo node : nodes) {
                                    cache.addMember(node.getNameWithOwner(), starList);
                                }
                            }
                        }
                        loadRepoMembership(cache, index + 1);
                    }
                }, false);
    }

    @Override
    public void updateRepoStarLists(ArrayList<String> listIds) {
        if (repository == null || StringUtils.isBlank(repository.getNodeId())) {
            if (mView != null) {
                mView.showErrorToast(getString(R.string.star_list_missing_repo_id));
            }
            return;
        }
        final Map<String, Object> variables = new HashMap<>();
        variables.put("itemId", repository.getNodeId());
        variables.put("listIds", listIds == null ? new ArrayList<String>() : listIds);
        generalRxHttpExecute(
                new IObservableCreator<GraphqlResponse<MutationData>>() {
                    @Nullable
                    @Override
                    public Observable<Response<GraphqlResponse<MutationData>>> createObservable(boolean forceNetWork) {
                        return getGraphqlService().postMutation(
                                new GraphqlRequest(GraphqlQueries.MUTATION_UPDATE_REPO_LISTS, variables));
                    }
                },
                new HttpObserver<GraphqlResponse<MutationData>>() {
                    @Override
                    public void onError(@NonNull Throwable error) {
                        if (mView != null) {
                            mView.showErrorToast(getErrorTip(error));
                        }
                    }

                    @Override
                    public void onSuccess(@NonNull HttpResponse<GraphqlResponse<MutationData>> response) {
                        GraphqlResponse<MutationData> body = response.body();
                        if (body != null && body.hasErrors()) {
                            if (mView != null) {
                                mView.showErrorToast(body.getErrorMessages());
                            }
                            return;
                        }
                        StarListCache.getInstance().invalidate();
                        if (mView != null) {
                            mView.showSuccessToast(getString(R.string.repo_lists_updated));
                        }
                    }
                }, false);
    }

}
