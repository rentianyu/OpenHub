package com.thirtydegreesray.openhub.http;

import com.thirtydegreesray.openhub.mvp.model.graphql.GraphqlRequest;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphqlResponse;
import com.thirtydegreesray.openhub.mvp.model.graphql.MutationData;
import com.thirtydegreesray.openhub.mvp.model.graphql.StarListItemsData;
import com.thirtydegreesray.openhub.mvp.model.graphql.StarListsData;

import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import rx.Observable;

/**
 * GitHub GraphQL API v4
 * 星标目录（Lists）功能只开放了 GraphQL 接口，REST API 无对应端点。
 *
 * baseUrl 为 https://api.github.com/，POST graphql
 */
public interface GraphqlService {

    @Headers("Cache-Control: no-cache")
    @POST("graphql")
    Observable<Response<GraphqlResponse<StarListsData>>> getStarLists(
            @Body GraphqlRequest request
    );

    @Headers("Cache-Control: no-cache")
    @POST("graphql")
    Observable<Response<GraphqlResponse<StarListItemsData>>> getStarListItems(
            @Body GraphqlRequest request
    );

    @Headers("Cache-Control: no-cache")
    @POST("graphql")
    Observable<Response<GraphqlResponse<MutationData>>> postMutation(
            @Body GraphqlRequest request
    );
}
