package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.openhub.AppConfig;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.ITopicsContract;
import com.thirtydegreesray.openhub.mvp.model.Topic;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;
import com.orhanobut.logger.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by ThirtyDegreesRay on 2017/12/29 11:10:27
 */

public class TopicsPresenter extends BasePresenter<ITopicsContract.View>
        implements ITopicsContract.Presenter {

    private ArrayList<Topic> topics;

    @Inject
    public TopicsPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadTopics(false);
    }

    @Override
    public void loadTopics(boolean isReload) {
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

        generalRxHttpExecute(forceNetWork -> getGitHubWebPageService().getTopics(forceNetWork),
                httpObserver, !isReload);

    }

    private void parsePageData(String page) {
        Observable.just(page)
                .map(s -> {
                    ArrayList<Topic> topics = new ArrayList<>();
                    try {
                        Document doc = Jsoup.parse(s, AppConfig.GITHUB_BASE_URL);
                        //top three topics
                        topics.addAll(getTopTopics(doc));
                        topics.addAll(getFeaturedTopics(doc));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return topics;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(results -> {
                    if(mView == null) return;
                    if(results.size() != 0){
                        topics = results;
                        mView.hideLoading();
                        mView.showTopics(topics);
                    } else {
                        String errorTip = String.format(getString(R.string.github_page_parse_error),
                                getString(R.string.topics));
                        mView.showLoadError(errorTip);
                        mView.hideLoading();
                    }
                });
    }

    private ArrayList<Topic> getTopTopics(Document doc) throws Exception{
        ArrayList<Topic> topTopics = new ArrayList<>();
        // current github layout (2026): <li class="col-12 ... tmp-mb-4"><div class="topic-box">
        Elements elements = doc.select("div.topic-box");
        if(elements.isEmpty()){
            // legacy layout fallback
            elements = doc.getElementsByClass("col-12 col-sm-6 col-md-4 mb-4");
        }
        for (Element element : elements) {
            // isolate every card so one broken card never kills the whole page
            try {
                Element idElement = element.select("a[href]").first();
                if(idElement == null) continue;
                Elements paragraphs = element.select("p");
                if(paragraphs.size() < 1) continue;
                Element imageElement = element.select("img").first();

                String id = idElement.attr("href");
                if(!id.contains("/topics/")) continue;
                id = id.substring(id.lastIndexOf("/") + 1);
                String name = paragraphs.get(0).text().trim();
                String desc = paragraphs.size() > 1 ? paragraphs.get(1).text().trim() : "";
                String image = imageElement == null ? null : imageElement.attr("src");
                if(name.isEmpty()) continue;

                topTopics.add(new Topic()
                        .setId(id)
                        .setName(name)
                        .setDesc(desc)
                        .setImage(image));
            } catch (Exception e) {
                Logger.w("parse topic card error", e);
            }
        }
        return topTopics;
    }

    private ArrayList<Topic> getFeaturedTopics(Document doc) throws Exception{
        ArrayList<Topic> featuredTopics = new ArrayList<>();
        // current github layout (2026): <div class="tmp-py-4 border-bottom ...">
        Elements topElements = doc.select("div.tmp-py-4.border-bottom");
        if(topElements.isEmpty()){
            // legacy layout fallback
            topElements = doc.getElementsByClass("py-4 border-bottom");
        }
        for (Element element : topElements) {
            try {
                // content link is the anchor that wraps the paragraphs
                Element contentElement = null;
                for (Element a : element.select("a[href]")) {
                    if(a.select("p").size() > 0){
                        contentElement = a;
                        break;
                    }
                }
                if(contentElement == null) continue;
                Elements paragraphs = contentElement.select("p");
                if(paragraphs.size() < 1) continue;
                Element imageElement = element.select("img").first();

                String id = contentElement.attr("href");
                if(!id.contains("/topics/")) continue;
                id = id.substring(id.lastIndexOf("/") + 1);
                String name = paragraphs.get(0).text().trim();
                String desc = paragraphs.size() > 1 ? paragraphs.get(1).text().trim() : "";
                String image = imageElement == null ? null : imageElement.attr("src");
                if(name.isEmpty()) continue;

                featuredTopics.add(new Topic()
                        .setId(id)
                        .setName(name)
                        .setDesc(desc)
                        .setImage(image));
            } catch (Exception e) {
                Logger.w("parse featured topic error", e);
            }
        }
        return featuredTopics;
    }

}
