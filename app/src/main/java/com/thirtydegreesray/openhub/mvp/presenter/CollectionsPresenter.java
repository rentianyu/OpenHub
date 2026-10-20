package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.openhub.AppConfig;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.ICollectionsContract;
import com.thirtydegreesray.openhub.mvp.model.Collection;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;
import com.orhanobut.logger.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by ThirtyDegreesRay on 2017/12/25 15:16:34
 */

public class CollectionsPresenter extends BasePresenter<ICollectionsContract.View>
        implements ICollectionsContract.Presenter {

    private ArrayList<Collection> collections;

    @Inject
    public CollectionsPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadCollections(false);
    }

    @Override
    public void loadCollections(boolean isReload) {
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
                    parsePageData(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        generalRxHttpExecute(forceNetWork -> getGitHubWebPageService().getCollections(forceNetWork),
                httpObserver, !isReload);

    }

    private void parsePageData(String page) {
        Observable.just(page)
                .map(s -> {
                    ArrayList<Collection> collections = new ArrayList<>();
                    try {
                        Document doc = Jsoup.parse(s, AppConfig.GITHUB_BASE_URL);
                        collections.addAll(getTopCollections(doc));
                        collections.addAll(getBellowCollections(doc));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return collections;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(results -> {
                    if(mView == null) return;
                    if(results.size() != 0){
                        collections = results;
                        mView.hideLoading();
                        mView.showCollections(collections);
                    } else {
                        String errorTip = String.format(getString(R.string.github_page_parse_error),
                                getString(R.string.repo_collections));
                        mView.showLoadError(errorTip);
                        mView.hideLoading();
                    }
                });
    }

    private ArrayList<Collection> getTopCollections(Document doc){
        ArrayList<Collection> collections = new ArrayList<>();
        // current github layout (2026): <a class="exploregrid-item" href="/collections/...">
        Elements elements = doc.select("a.exploregrid-item");
        if(elements.isEmpty()){
            // legacy layout fallback
            elements = doc.getElementsByClass("col-12 col-sm-6 col-md-4 mb-4").select("a");
        }
        for (Element element : elements) {
            // isolate every card so one broken card never kills the whole page
            try {
                String href = element.attr("href");
                if(href == null || !href.contains("/collections/")) continue;
                String id = href.substring(href.lastIndexOf("/") + 1);
                Elements paragraphs = element.select("p");
                String title = paragraphs.size() > 0 ? paragraphs.get(0).text().trim() : "";
                String desc = paragraphs.size() > 1 ? paragraphs.get(1).text().trim() : "";
                if(title.isEmpty()) continue;
                collections.add(new Collection(id, title, desc));
            } catch (Exception e) {
                Logger.w("parse collection card error", e);
            }
        }
        return collections;
    }

    private ArrayList<Collection> getBellowCollections(Document doc){
        ArrayList<Collection> collections = new ArrayList<>();
        // current github layout (2026): <article class="d-flex border-bottom ...">
        //   <div class="col-10 ..."><h2><a href="/collections/xxx">title</a></h2> desc...</div>
        Elements elements = doc.select("article.d-flex");
        if(elements.isEmpty()){
            // legacy layout fallback
            elements = doc.getElementsByClass("d-flex border-bottom border-gray-light pb-4");
        }
        for (Element element : elements) {
            try {
                Element hrefElement = element.select("h2 a, a").first();
                if(hrefElement == null) continue;
                String href = hrefElement.attr("href");
                if(href == null || !href.contains("/collections/")) continue;
                String id = href.substring(href.lastIndexOf("/") + 1);
                String title = hrefElement.text().trim();
                if(title.isEmpty()) continue;

                StringBuilder desc = new StringBuilder();
                Element descHost = element.select("div.col-10, div.col-11, div[class*=col-10]").first();
                if(descHost != null){
                    for (org.jsoup.nodes.TextNode textNode : descHost.textNodes()) {
                        desc.append(textNode.getWholeText().trim());
                    }
                }
                collections.add(new Collection(id, title, desc.toString().trim()));
            } catch (Exception e) {
                Logger.w("parse collection article error", e);
            }
        }
        return collections;
    }

}
