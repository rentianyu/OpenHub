

package com.thirtydegreesray.openhub.mvp.contract;

import android.support.annotation.NonNull;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.model.RepoEditInfo;
import com.thirtydegreesray.openhub.mvp.model.Repository;

/**
 * Create / edit / delete a repository owned by the authenticated user.
 */
public interface IEditReposContract {

    interface View extends IBaseContract.View {
        /** repository created or updated, close the page and tell the caller */
        void showSaveSuccess(@NonNull Repository repository, boolean addMode);

        /** repository deleted, close the page */
        void showDeleteSuccess(@NonNull String fullName);

        /** show / hide the "delete repository" section */
        void showDeleteSection(boolean visible);
    }

    interface Presenter extends IBaseContract.Presenter<IEditReposContract.View> {
        void saveRepo(@NonNull RepoEditInfo info);

        void deleteRepo(@NonNull String confirmName);

        boolean isAddMode();

        Repository getRepository();

        String getOwner();

        String getRepoName();

        /** full name of the repo, used by the delete confirmation dialog */
        String getFullName();
    }

}
