

package com.thirtydegreesray.openhub.mvp.presenter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.AppConfig;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.common.Event;
import com.thirtydegreesray.openhub.dao.BookMarkRepo;
import com.thirtydegreesray.openhub.dao.BookMarkRepoDao;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.dao.TraceRepo;
import com.thirtydegreesray.openhub.dao.TraceRepoDao;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.http.error.HttpPageNoFoundError;
import com.thirtydegreesray.openhub.mvp.contract.IRepositoriesContract;
import com.thirtydegreesray.openhub.mvp.model.Collection;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.model.SearchModel;
import com.thirtydegreesray.openhub.mvp.model.SearchResult;
import com.thirtydegreesray.openhub.mvp.model.StarList;
import com.thirtydegreesray.openhub.mvp.model.Topic;
import com.thirtydegreesray.openhub.mvp.model.TrendingLanguage;
import com.thirtydegreesray.openhub.mvp.model.User;
import com.thirtydegreesray.openhub.mvp.model.filter.RepositoriesFilter;
import com.thirtydegreesray.openhub.mvp.model.filter.TrendingSince;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphqlQueries;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphqlRepo;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphqlRequest;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphqlResponse;
import com.thirtydegreesray.openhub.mvp.model.graphql.MutationData;
import com.thirtydegreesray.openhub.mvp.model.graphql.StarListItemsData;
import com.thirtydegreesray.openhub.mvp.model.graphql.StarListsData;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePagerPresenter;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;
import com.thirtydegreesray.openhub.util.StarListCache;
import com.thirtydegreesray.openhub.util.StringUtils;

import org.greenrobot.eventbus.Subscribe;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created on 2017/7/18.
 *
 * @author ThirtyDegreesRay
 */

public class RepositoriesPresenter extends BasePagerPresenter<IRepositoriesContract.View>
        implements IRepositoriesContract.Presenter {

    private ArrayList<Repository> repos;

    @AutoAccess RepositoriesFragment.RepositoriesType type;
    @AutoAccess String user;
    @AutoAccess String repo;

    @AutoAccess SearchModel searchModel;
    @AutoAccess TrendingSince since;

    @AutoAccess RepositoriesFilter filter;

    @AutoAccess TrendingLanguage language;

    @AutoAccess Collection collection;
    @AutoAccess Topic topic;

    @AutoAccess String starListId;
    @AutoAccess String starListName;

    private ArrayList<StarList> starLists;
    private boolean starListsLoaded = false;

    @Inject
    public RepositoriesPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        if (type.equals(RepositoriesFragment.RepositoriesType.SEARCH)) {
            setEventSubscriber(true);
        }
    }

    @Override
    protected void loadData() {
        if (RepositoriesFragment.RepositoriesType.SEARCH.equals(type)) {
            if (searchModel != null) searchRepos(1);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.TRACE.equals(type)) {
            loadTrace(1);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.BOOKMARK.equals(type)) {
            loadBookmarks(1);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.COLLECTION.equals(type)) {
            loadCollection(false);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.TOPIC.equals(type)) {
            initSearchModelForTopic();
            searchRepos(1);
            return;
        }
        if(RepositoriesFragment.RepositoriesType.TRENDING.equals(type)){
            loadTrending(false);
            return;
        }
        if(RepositoriesFragment.RepositoriesType.STARRED.equals(type)){
            loadStarLists();
        }
        loadRepositories(false, 1);
    }

    @Override
    public void loadRepositories(final boolean isReLoad, final int page) {
        filter = getFilter();
        if (RepositoriesFragment.RepositoriesType.STARRED.equals(type) && starListId != null) {
            // 目录视图一次性加载全部数据，忽略翻页请求
            if (page <= 1) {
                loadStarListRepos(isReLoad);
            }
            return;
        }
        if (type.equals(RepositoriesFragment.RepositoriesType.SEARCH)) {
            searchRepos(page);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.TRACE.equals(type)) {
            loadTrace(page);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.BOOKMARK.equals(type)) {
            loadBookmarks(page);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.COLLECTION.equals(type)) {
            loadCollection(isReLoad);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.TOPIC.equals(type)) {
            initSearchModelForTopic();
            searchRepos(page);
            return;
        }
        if(RepositoriesFragment.RepositoriesType.TRENDING.equals(type)){
            loadTrending(isReLoad);
            return;
        }
        mView.showLoading();
        final boolean readCacheFirst = !isReLoad && page == 1;

        HttpObserver<ArrayList<Repository>> httpObserver = new HttpObserver<ArrayList<Repository>>() {
            @Override
            public void onError(@NonNull Throwable error) {
                mView.hideLoading();
                handleError(error);
            }

            @Override
            public void onSuccess(@NonNull HttpResponse<ArrayList<Repository>> response) {
                mView.hideLoading();
                if (isReLoad || readCacheFirst || repos == null || page == 1) {
                    repos = response.body();
                } else {
                    repos.addAll(response.body());
                }
                if (response.body().size() == 0 && repos.size() != 0) {
                    mView.setCanLoadMore(false);
                } else {
                    mView.showRepositories(repos);
                }
            }
        };

        generalRxHttpExecute(new IObservableCreator<ArrayList<Repository>>() {
            @Nullable
            @Override
            public Observable<Response<ArrayList<Repository>>> createObservable(boolean forceNetWork) {
                return getObservable(forceNetWork, page);
            }
        }, httpObserver, readCacheFirst);

    }

    @Override
    public void loadRepositories(RepositoriesFilter filter) {
        this.filter = filter;
        loadRepositories(false, 1);
    }

    private Observable<Response<ArrayList<Repository>>> getObservable(boolean forceNetWork, int page) {
        switch (type) {
            case OWNED:
                return getRepoService().getUserRepos(forceNetWork, page, filter.getType(),
                        filter.getSort(), filter.getSortDirection());
            case PUBLIC:
                return getRepoService().getUserPublicRepos(forceNetWork, user, page,
                        filter.getType(), filter.getSort(), filter.getSortDirection());
            case STARRED:
                return getRepoService().getStarredRepos(forceNetWork, user, page,
                        filter.getSort(), filter.getSortDirection());
            case FORKS:
                return getRepoService().getForks(forceNetWork, user, repo, page);
            default:
                return null;
        }
    }

    private void searchRepos(final int page) {
        mView.showLoading();

        HttpObserver<SearchResult<Repository>> httpObserver =
                new HttpObserver<SearchResult<Repository>>() {
                    @Override
                    public void onError(@NonNull Throwable error) {
                        mView.hideLoading();
                        handleError(error);
                    }

                    @Override
                    public void onSuccess(@NonNull HttpResponse<SearchResult<Repository>> response) {
                        mView.hideLoading();
                        if (repos == null || page == 1) {
                            repos = response.body().getItems();
                        } else {
                            repos.addAll(response.body().getItems());
                        }
                        if (response.body().getItems().size() == 0 && repos.size() != 0) {
                            mView.setCanLoadMore(false);
                        } else {
                            mView.showRepositories(repos);
                        }
                    }
                };
        generalRxHttpExecute(new IObservableCreator<SearchResult<Repository>>() {
            @Nullable
            @Override
            public Observable<Response<SearchResult<Repository>>> createObservable(boolean forceNetWork) {
                return getSearchService().searchRepos(searchModel.getQuery(), searchModel.getSort(),
                        searchModel.getOrder(), page);
            }
        }, httpObserver);
    }

    @Subscribe
    public void onSearchEvent(@NonNull Event.SearchEvent searchEvent) {
        if (!searchEvent.searchModel.getType().equals(SearchModel.SearchType.Repository)) return;
        setLoaded(false);
        this.searchModel = searchEvent.searchModel;
        prepareLoadData();
    }

    private void handleError(Throwable error) {
        if (!StringUtils.isBlankList(repos)) {
            mView.showErrorToast(getErrorTip(error));
        } else if (error instanceof HttpPageNoFoundError) {
            mView.showRepositories(new ArrayList<Repository>());
        } else {
            mView.showLoadError(getErrorTip(error));
        }
    }

    public String getUser() {
        return user;
    }

    public RepositoriesFragment.RepositoriesType getType() {
        return type;
    }

    public RepositoriesFilter getFilter() {
        if (filter == null) {
            filter = RepositoriesFragment.RepositoriesType.STARRED.equals(type) ?
                    RepositoriesFilter.DEFAULT_STARRED_REPO : RepositoriesFilter.DEFAULT;
        }
        return filter;
    }

    private void loadTrace(int page) {
        long start = System.currentTimeMillis();

        List<TraceRepo> traceRepos = daoSession.getTraceRepoDao().queryBuilder()
                .orderDesc(TraceRepoDao.Properties.LatestTime)
                .offset((page - 1) * 30)
                .limit(page * 30)
                .list();

        ArrayList<Repository> queryRepos = new ArrayList<>();
        for (TraceRepo traceRepo : traceRepos) {
            queryRepos.add(Repository.generateFromTrace(traceRepo));
        }
        Logger.t("loadTrace").d(System.currentTimeMillis() - start);
        showQueryRepos(queryRepos, page);
    }

    private void loadBookmarks(int page) {
        List<BookMarkRepo> bookMarkRepos = daoSession.getBookMarkRepoDao().queryBuilder()
                .orderDesc(BookMarkRepoDao.Properties.MarkTime)
                .offset((page - 1) * 30)
                .limit(page * 30)
                .list();

        ArrayList<Repository> queryRepos = new ArrayList<>();
        for (BookMarkRepo bookMarkRepo : bookMarkRepos) {
            queryRepos.add(Repository.generateFromBookmark(bookMarkRepo));
        }
        showQueryRepos(queryRepos, page);
    }

    private void showQueryRepos(ArrayList<Repository> queryRepos, int page){
        if(repos == null || page == 1){
            repos = queryRepos;
        } else {
            repos.addAll(queryRepos);
        }

        mView.showRepositories(repos);
        mView.hideLoading();
    }

    public void setLanguage(TrendingLanguage language) {
        this.language = language;
    }

    private void loadCollection(boolean isReload){
        mView.showLoading();
        HttpObserver<ResponseBody> httpObserver = new HttpObserver<ResponseBody>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                mView.showLoadError(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<ResponseBody> response) {
                try {
                    parseCollectionsPageData(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        generalRxHttpExecute(forceNetWork -> getGitHubWebPageService()
                        .getCollectionInfo(forceNetWork, collection.getId()),
                httpObserver, !isReload);

    }

    private void parseCollectionsPageData(String page){
        Observable.just(page)
                .map(s -> {
                    ArrayList<Repository> repos = new ArrayList<>();
                    try {
                        Document doc = Jsoup.parse(s, AppConfig.GITHUB_BASE_URL);
                        Elements elements = doc.getElementsByTag("article");
                        for (Element element : elements) {
                            //maybe a user or an org, so add catch
                            try{
                                repos.add(parseCollectionsRepositoryData(element));
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    return repos;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(results -> {
                    if(mView == null) return;
                    if(results.size() != 0){
                        repos = results;
                        mView.hideLoading();
                        mView.showRepositories(repos);
                    } else {
                        String errorTip = String.format(getString(R.string.github_page_parse_error),
                                getString(R.string.repo_collections));
                        mView.showLoadError(errorTip);
                        mView.hideLoading();
                    }
                });
    }

    private Repository parseCollectionsRepositoryData(Element element) throws Exception{
        // current github layout (2026): <article class="height-full border ...">
        //   <h1 class="h3"><a href="/owner/repo">..</a></h1>
        //   <div class="color-fg-muted mb-2 ws-normal">desc</div>
        //   <div class="d-flex f6"><a href="..stargazers">..</a><a href="..forks">..</a>
        //     <span itemprop="programmingLanguage">..</span>
        Element linkElement = element.select("h1 a[href], h2 a[href]").first();
        if(linkElement == null){
            throw new Exception("repo link not found in collection article");
        }
        String fullName = linkElement.attr("href");
        if(fullName.startsWith("/")){
            fullName = fullName.substring(1);
        }
        int slashIndex = fullName.lastIndexOf("/");
        if(slashIndex <= 0 || slashIndex == fullName.length() - 1){
            throw new Exception("invalid repo full name: " + fullName);
        }
        String owner = fullName.substring(0, slashIndex);
        String repoName = fullName.substring(slashIndex + 1);

        String ownerAvatar = "";
        String desc = "";
        String starNumStr = "0";
        String forkNumStr = "0";
        String language = "";

        try{
            Element descElement = element.select("div.color-fg-muted.mb-2, div.ws-normal").first();
            if(descElement != null){
                desc = descElement.text().trim();
            }
            starNumStr = parseTrendingNumber(element.select("a[href$='/stargazers']").first());
            forkNumStr = parseTrendingNumber(element.select("a[href$='/forks']").first());
            Element languageElement = element.select("[itemprop=programmingLanguage]").first();
            if(languageElement != null){
                language = languageElement.text().trim();
            }
        }catch (Exception e){
            Logger.e("Collection repo meta parse error.", e);
        }

        Repository repo = new Repository();
        repo.setFullName(fullName);
        repo.setName(repoName);
        User user = new User();
        user.setLogin(owner);
        user.setAvatarUrl(ownerAvatar);
        repo.setOwner(user);

        repo.setDescription(desc);
        repo.setStargazersCount(parseIntSafe(starNumStr));
        repo.setForksCount(parseIntSafe(forkNumStr));
        repo.setLanguage(language);

        return repo;
    }

    private void initSearchModelForTopic(){
        if(searchModel == null){
            searchModel = new SearchModel(SearchModel.SearchType.Repository);
            searchModel.setQuery("topic:" + topic.getId());
        }
    }

    private void loadTrending(boolean isReload){
        mView.showLoading();
        HttpObserver<ResponseBody> httpObserver = new HttpObserver<ResponseBody>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                mView.showLoadError(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<ResponseBody> response) {
                try {
                    parseTrendingPageData(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        generalRxHttpExecute(forceNetWork -> getGitHubWebPageService()
                        .getTrendingRepos(forceNetWork, language.getSlug(), since.name()),
                httpObserver, !isReload);
    }

    private void parseTrendingPageData(String page){
        Observable.just(page)
                .map(s -> {
                    ArrayList<Repository> repos = new ArrayList<>();
                    try {
                        Document doc = Jsoup.parse(s, AppConfig.GITHUB_BASE_URL);
                        Elements elements = doc.getElementsByClass("Box-row");
                        if(elements.size() != 0){
                            for (Element element : elements) {
                                try{
                                    repos.add(parseTrendingRepositoryData(element));
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        repos = null;
                    }

                    return repos;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(results -> {
                    if(mView == null) return;
                    if(results != null){
                        repos = results;
                        mView.hideLoading();
                        mView.showRepositories(repos);
                    } else {
                        String errorTip = String.format(getString(R.string.github_page_parse_error),
                                getString(R.string.trending));
                        mView.showLoadError(errorTip);
                        mView.hideLoading();
                    }
                });
    }

    private Repository parseTrendingRepositoryData(Element element) throws Exception{
        Element linkElement = element.select("h2 > a, h1 > a").first();
        if(linkElement == null){
            throw new Exception("trending repo link element not found");
        }
        String fullName = linkElement.attr("href");
        if(fullName.startsWith("/")){
            fullName = fullName.substring(1);
        }
        String owner = fullName.substring(0, fullName.lastIndexOf("/"));
        String repoName = fullName.substring(fullName.lastIndexOf("/") + 1);

        StringBuilder desc = new StringBuilder("");
        String language = "unknown";
        String starNumStr = "0";
        String forkNumStr = "0";
        String periodNumStr = "0";

        try{
            Element descElement = element.select("p").first();
            if(null != descElement){
                desc.append(descElement.text());
            }

            Element numElement = element.select("div.f6").first();
            if(null != numElement){
                Element languageElement = numElement.select("[itemprop=programmingLanguage]").first();
                if(languageElement == null){
                    Elements languageElements = numElement.select("span > span");
                    if(languageElements.size() > 1){
                        languageElement = languageElements.get(1);
                    }
                }
                if(languageElement != null && !StringUtils.isBlank(languageElement.text())){
                    language = languageElement.text().trim();
                }
                starNumStr = parseTrendingNumber(numElement.select("a[href$='/stargazers']").first());
                forkNumStr = parseTrendingNumber(numElement.select("a[href$='/forks']").first());
                Element periodElement = numElement.select("span.d-inline-block.float-sm-right").first();
                if(periodElement != null){
                    periodNumStr = parseTrendingNumber(periodElement);
                }
            }
        }catch (Exception e){
            Logger.e("Trending repo desc or num info parse error.", e);
        }

        Repository repo = new Repository();
        repo.setFullName(fullName);
        repo.setName(repoName);
        User user = new User();
        user.setLogin(owner);
        user.setAvatarUrl("https://github.com/".concat(owner).concat(".png"));
        repo.setOwner(user);

        repo.setDescription(desc.toString().trim()
                .replaceAll("\n", ""));
        repo.setStargazersCount(parseIntSafe(starNumStr));
        repo.setForksCount(parseIntSafe(forkNumStr));
        repo.setSinceStargazersCount(parseIntSafe(periodNumStr));
        repo.setLanguage(language);
        repo.setSince(since);

        return repo;
    }

    /**
     * extract a plain number from trending page fragments like "59,497" or "3,897 stars today"
     */
    private String parseTrendingNumber(Element element){
        if(element == null) return "0";
        String digits = element.text().replaceAll("[^0-9]", "");
        return digits.isEmpty() ? "0" : digits;
    }

    private int parseIntSafe(String numStr){
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public TrendingLanguage getLanguage() {
        return language;
    }

    //========================== 星标目录（GitHub Lists） ==========================

    @Override
    public void loadStarLists() {
        if (!RepositoriesFragment.RepositoriesType.STARRED.equals(type)) {
            return;
        }
        if (starListsLoaded && starLists != null) {
            if (mView != null) {
                mView.showStarLists(starLists);
            }
            loadMemberships();
            return;
        }
        StarListCache cache = StarListCache.getInstance();
        if (cache.isValid() && cache.hasStarLists()) {
            starLists = cache.getStarLists();
            starListsLoaded = true;
            syncCurStarList();
            if (mView != null) {
                mView.showStarLists(starLists);
            }
            loadMemberships();
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
                        Logger.e("load star lists failed", error);
                    }

                    @Override
                    public void onSuccess(@NonNull HttpResponse<GraphqlResponse<StarListsData>> response) {
                        ArrayList<StarList> lists = parseStarLists(response.body());
                        if (lists == null) {
                            return;
                        }
                        starLists = lists;
                        starListsLoaded = true;
                        StarListCache.getInstance().setStarLists(lists);
                        syncCurStarList();
                        if (mView != null) {
                            mView.showStarLists(lists);
                        }
                        loadMemberships();
                    }
                }, false);
    }

    @Nullable
    private ArrayList<StarList> parseStarLists(GraphqlResponse<StarListsData> response) {
        if (response == null || response.hasErrors() || response.getData() == null
                || response.getData().getViewer() == null
                || response.getData().getViewer().getLists() == null) {
            return null;
        }
        ArrayList<StarList> lists = response.getData().getViewer().getLists().getNodes();
        return lists == null ? new ArrayList<StarList>() : lists;
    }

    private void syncCurStarList() {
        if (starListId == null || starLists == null) {
            return;
        }
        for (StarList list : starLists) {
            if (starListId.equals(list.getId())) {
                starListName = list.getName();
                return;
            }
        }
        // 当前目录已不存在（被删除），回退到「全部」
        starListId = null;
        starListName = null;
    }

    @Override
    public void setStarList(@Nullable StarList starList) {
        if (starList == null) {
            starListId = null;
            starListName = null;
        } else {
            starListId = starList.getId();
            starListName = starList.getName();
        }
        setLoaded(false);
        loadRepositories(true, 1);
    }

    @Nullable
    @Override
    public StarList getStarList() {
        if (starListId == null || starLists == null) {
            return null;
        }
        for (StarList list : starLists) {
            if (starListId.equals(list.getId())) {
                return list;
            }
        }
        return null;
    }

    @Override
    public ArrayList<StarList> getStarLists() {
        return starLists;
    }

    @Nullable
    public String getStarListName() {
        return starListName;
    }

    public boolean hasStarListFilter() {
        return starListId != null;
    }

    /**
     * 仓库所属目录的展示文案，如「安卓」或「安卓 +2」，无归属返回 null
     */
    @Nullable
    public String getStarListTag(String fullName) {
        ArrayList<StarList> lists = StarListCache.getInstance().getMember(fullName);
        if (lists == null || lists.isEmpty()) {
            return null;
        }
        if (lists.size() == 1) {
            return lists.get(0).getName();
        }
        return lists.get(0).getName() + " +" + (lists.size() - 1);
    }

    /**
     * 依次拉取每个目录的成员，构建「仓库 -> 目录」映射，用于列表标签与详情页预选
     */
    private void loadMemberships() {
        StarListCache cache = StarListCache.getInstance();
        if (cache.isMembershipLoaded() || !cache.hasStarLists()) {
            return;
        }
        cache.startMemberships();
        loadMembershipOfList(0);
    }

    private void loadMembershipOfList(final int index) {
        final StarListCache cache = StarListCache.getInstance();
        final ArrayList<StarList> lists = cache.getStarLists();
        if (lists == null || index >= lists.size()) {
            if (mView != null && repos != null) {
                mView.showRepositories(repos);
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
                        loadMembershipOfList(index + 1);
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
                        loadMembershipOfList(index + 1);
                    }
                }, false);
    }

    private final static int MAX_STAR_LIST_PAGES = 10;

    private void loadStarListRepos(boolean isReLoad) {
        if (mView != null) {
            mView.showLoading();
        }
        loadStarListReposPage(starListId, null, new ArrayList<Repository>(), 1);
    }

    private void loadStarListReposPage(final String listId, final String after,
                                       final ArrayList<Repository> result, final int page) {
        generalRxHttpExecute(
                new IObservableCreator<GraphqlResponse<StarListItemsData>>() {
                    @Nullable
                    @Override
                    public Observable<Response<GraphqlResponse<StarListItemsData>>> createObservable(boolean forceNetWork) {
                        Map<String, Object> variables = new HashMap<>();
                        variables.put("id", listId);
                        variables.put("after", after);
                        return getGraphqlService().getStarListItems(
                                new GraphqlRequest(GraphqlQueries.QUERY_STAR_LIST_ITEMS, variables));
                    }
                },
                new HttpObserver<GraphqlResponse<StarListItemsData>>() {
                    @Override
                    public void onError(@NonNull Throwable error) {
                        if (result.isEmpty()) {
                            if (mView != null) {
                                mView.hideLoading();
                                mView.showLoadError(getErrorTip(error));
                            }
                        } else {
                            showStarListRepos(result);
                        }
                    }

                    @Override
                    public void onSuccess(@NonNull HttpResponse<GraphqlResponse<StarListItemsData>> response) {
                        GraphqlResponse<StarListItemsData> body = response.body();
                        if (body == null || body.hasErrors()) {
                            onError(new IOException("graphql error: "
                                    + (body == null ? "empty body" : body.getErrorMessages())));
                            return;
                        }
                        StarListItemsData.Node node = body.getData() == null ? null : body.getData().getNode();
                        StarListItemsData.Items items = node == null ? null : node.getItems();
                        if (items == null) {
                            showStarListRepos(result);
                            return;
                        }
                        if (items.getNodes() != null) {
                            for (GraphqlRepo graphqlRepo : items.getNodes()) {
                                result.add(graphqlRepo.toRepository());
                            }
                        }
                        StarListItemsData.PageInfo pageInfo = items.getPageInfo();
                        if (pageInfo != null && pageInfo.isHasNextPage()
                                && pageInfo.getEndCursor() != null
                                && page < MAX_STAR_LIST_PAGES) {
                            loadStarListReposPage(listId, pageInfo.getEndCursor(), result, page + 1);
                        } else {
                            showStarListRepos(result);
                        }
                    }
                }, false);
    }

    private void showStarListRepos(ArrayList<Repository> list) {
        sortStarListRepos(list);
        repos = list;
        if (mView != null) {
            mView.setCanLoadMore(false);
            mView.showRepositories(repos);
            mView.hideLoading();
        }
    }

    /**
     * 目录内仓库的本地排序。GitHub 目录接口不返回仓库加入目录的时间，
     * 因此「星标时间」两种排序保持目录默认顺序，其余排序本地完成。
     */
    private void sortStarListRepos(ArrayList<Repository> list) {
        final RepositoriesFilter currentFilter = getFilter();
        final String sort = currentFilter.getSort();
        final boolean desc = "desc".equals(currentFilter.getSortDirection());
        if (!"updated".equals(sort) && !"pushed".equals(sort) && !"full_name".equals(sort)) {
            return;
        }
        Collections.sort(list, new Comparator<Repository>() {
            @Override
            public int compare(Repository left, Repository right) {
                int result;
                if ("updated".equals(sort)) {
                    result = compareTime(left.getUpdatedAt(), right.getUpdatedAt());
                } else if ("pushed".equals(sort)) {
                    result = compareTime(left.getPushedAt(), right.getPushedAt());
                } else {
                    result = compareString(left.getFullName(), right.getFullName());
                }
                return desc ? -result : result;
            }
        });
    }

    private int compareTime(Date left, Date right) {
        long leftTime = left == null ? 0 : left.getTime();
        long rightTime = right == null ? 0 : right.getTime();
        if (leftTime == rightTime) {
            return 0;
        }
        return leftTime < rightTime ? -1 : 1;
    }

    private int compareString(String left, String right) {
        String leftValue = left == null ? "" : left;
        String rightValue = right == null ? "" : right;
        return leftValue.compareToIgnoreCase(rightValue);
    }

    /**
     * 新建星标目录
     */
    public void createStarList(final String name) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        postStarListMutation(GraphqlQueries.MUTATION_CREATE_LIST, variables);
    }

    /**
     * 重命名星标目录
     */
    public void renameStarList(@Nullable StarList starList, final String newName) {
        if (starList == null) {
            return;
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("listId", starList.getId());
        variables.put("name", newName);
        postStarListMutation(GraphqlQueries.MUTATION_UPDATE_LIST, variables);
    }

    /**
     * 删除星标目录（目录内仓库不会被取消星标）
     */
    public void deleteStarList(@Nullable StarList starList) {
        if (starList == null) {
            return;
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("listId", starList.getId());
        postStarListMutation(GraphqlQueries.MUTATION_DELETE_LIST, variables);
    }

    private void postStarListMutation(final String query, final Map<String, Object> variables) {
        generalRxHttpExecute(
                new IObservableCreator<GraphqlResponse<MutationData>>() {
                    @Nullable
                    @Override
                    public Observable<Response<GraphqlResponse<MutationData>>> createObservable(boolean forceNetWork) {
                        return getGraphqlService().postMutation(new GraphqlRequest(query, variables));
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
                        refreshStarLists();
                    }
                }, false);
    }

    private void refreshStarLists() {
        StarListCache.getInstance().invalidate();
        starLists = null;
        starListsLoaded = false;
        if (mView != null) {
            mView.showSuccessToast(getString(R.string.star_list_updated));
        }
        loadStarLists();
    }
}
