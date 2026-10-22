

package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.model.Branch;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.model.StarList;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/8/9 21:40:25
 */

public interface IRepositoryContract {

    interface View extends IBaseContract.View{
        void showRepo(Repository repo);
        void showBranchesAndTags(ArrayList<Branch> list, Branch curBranch);
        void invalidateOptionsMenu();
        void showStarWishes();

        /**
         * 星标目录加载完成，selectedIds 为当前仓库已所属的目录
         */
        void showRepoStarLists(ArrayList<StarList> starLists, ArrayList<String> selectedIds);
    }

    interface Presenter extends IBaseContract.Presenter<IRepositoryContract.View>{
        void loadBranchesAndTags();
        void starRepo(boolean star);
        void watchRepo(boolean watch);
        void createFork();
        boolean isForkEnable();
        boolean isBookmarked();
        void bookmark(boolean bookmark);

        /**
         * 加载星标目录及当前仓库的归属
         */
        void loadRepoStarLists();

        /**
         * 保存当前仓库所属目录（覆盖式）
         */
        void updateRepoStarLists(ArrayList<String> listIds);
    }

}
