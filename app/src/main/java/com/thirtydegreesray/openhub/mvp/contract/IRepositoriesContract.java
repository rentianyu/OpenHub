

package com.thirtydegreesray.openhub.mvp.contract;

import android.support.annotation.Nullable;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseListContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBasePagerContract;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.model.StarList;
import com.thirtydegreesray.openhub.mvp.model.filter.RepositoriesFilter;

import java.util.ArrayList;

/**
 * Created on 2017/7/18.
 *
 * @author ThirtyDegreesRay
 */

public interface IRepositoriesContract {

    interface View extends IBaseContract.View, IBasePagerContract.View, IBaseListContract.View {

        void showRepositories(ArrayList<Repository> repositoryList);

        /**
         * 星标目录加载完成（仅星标页使用）
         */
        void showStarLists(ArrayList<StarList> starLists);

    }

    interface Presenter extends IBasePagerContract.Presenter<IRepositoriesContract.View> {
        void loadRepositories(boolean isReLoad, int page);
        void loadRepositories(RepositoriesFilter filter);

        /**
         * 加载星标目录列表
         */
        void loadStarLists();

        /**
         * 切换当前星标目录，null 表示「全部」
         */
        void setStarList(@Nullable StarList starList);

        @Nullable
        StarList getStarList();

        ArrayList<StarList> getStarLists();
    }

}
