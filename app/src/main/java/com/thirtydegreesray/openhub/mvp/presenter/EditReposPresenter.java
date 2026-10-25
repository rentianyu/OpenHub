

package com.thirtydegreesray.openhub.mvp.presenter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.AppData;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.common.Event;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.http.error.HttpError;
import com.thirtydegreesray.openhub.mvp.contract.IEditReposContract;
import com.thirtydegreesray.openhub.mvp.model.RepoEditInfo;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.model.User;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;
import com.thirtydegreesray.openhub.util.StringUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;

/**
 * Create / edit / delete a repository owned by the authenticated user.
 */
public class EditReposPresenter extends BasePresenter<IEditReposContract.View>
        implements IEditReposContract.Presenter {

    @AutoAccess boolean addMode;
    @AutoAccess Repository repository;

    @Inject
    public EditReposPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void saveRepo(@NonNull final RepoEditInfo info) {
        final boolean add = isAddMode();
        HttpObserver<Repository> httpObserver = new HttpObserver<Repository>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<Repository> response) {
                Repository saved = response.body() == null ? repository : response.body();
                String fullName = saved == null ? info.name : saved.getFullName();
                EventBus.getDefault().post(new Event.RepoChangedEvent(
                        add ? Event.RepoChangedEvent.CREATED : Event.RepoChangedEvent.UPDATED, fullName));
                mView.showSaveSuccess(saved, add);
            }
        };

        generalRxHttpExecute(new IObservableCreator<Repository>() {
            @Nullable
            @Override
            public Observable<Response<Repository>> createObservable(boolean forceNetWork) {
                return add ? getRepoService().createRepo(buildCreateParams(info))
                        : getRepoService().updateRepo(getOwner(), getRepoName(), buildUpdateParams(info));
            }
        }, httpObserver, true);
    }

    @Override
    public void deleteRepo(@NonNull String confirmName) {
        HttpObserver<ResponseBody> httpObserver = new HttpObserver<ResponseBody>() {
            @Override
            public void onError(Throwable error) {
                //deleting requires the delete_repo scope, tell the user how to fix a 403
                if (error instanceof HttpError && ((HttpError) error).getErrorCode() == 403) {
                    mView.showErrorToast(getContext().getString(R.string.repo_delete_need_scope));
                } else {
                    mView.showErrorToast(getErrorTip(error));
                }
            }

            @Override
            public void onSuccess(HttpResponse<ResponseBody> response) {
                String fullName = getFullName();
                EventBus.getDefault().post(
                        new Event.RepoChangedEvent(Event.RepoChangedEvent.DELETED, fullName));
                mView.showDeleteSuccess(fullName);
            }
        };

        generalRxHttpExecute(new IObservableCreator<ResponseBody>() {
            @Nullable
            @Override
            public Observable<Response<ResponseBody>> createObservable(boolean forceNetWork) {
                return getRepoService().deleteRepo(getOwner(), getRepoName());
            }
        }, httpObserver, true);
    }

    private Map<String, Object> buildCreateParams(@NonNull RepoEditInfo info) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", info.name);
        params.put("description", info.description == null ? "" : info.description);
        params.put("homepage", info.homepage == null ? "" : info.homepage);
        params.put("private", info.isPrivate);
        params.put("auto_init", info.autoInit);
        params.put("has_issues", info.hasIssues);
        params.put("has_wiki", info.hasWiki);
        params.put("has_projects", info.hasProjects);
        return params;
    }

    private Map<String, Object> buildUpdateParams(@NonNull RepoEditInfo info) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", info.name);
        params.put("description", info.description == null ? "" : info.description);
        params.put("homepage", info.homepage == null ? "" : info.homepage);
        params.put("private", info.isPrivate);
        params.put("archived", info.archived);
        params.put("has_issues", info.hasIssues);
        params.put("has_wiki", info.hasWiki);
        params.put("has_projects", info.hasProjects);
        return params;
    }

    @Override
    public boolean isAddMode() {
        return addMode;
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public String getOwner() {
        if (!isAddMode() && repository != null && repository.getOwner() != null
                && !StringUtils.isBlank(repository.getOwner().getLogin())) {
            return repository.getOwner().getLogin();
        }
        User user = AppData.INSTANCE.getLoggedUser();
        return user == null ? "" : user.getLogin();
    }

    @Override
    public String getRepoName() {
        if (isAddMode() || repository == null) return "";
        return repository.getName();
    }

    @Override
    public String getFullName() {
        if (isAddMode() || repository == null) return "";
        String fullName = repository.getFullName();
        if (StringUtils.isBlank(fullName)) {
            fullName = getOwner() + "/" + getRepoName();
        }
        return fullName;
    }

}
