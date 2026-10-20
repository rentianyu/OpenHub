package com.thirtydegreesray.openhub.http.core;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.thirtydegreesray.openhub.AppApplication;
import com.thirtydegreesray.openhub.AppConfig;
import com.thirtydegreesray.openhub.AppData;
import com.thirtydegreesray.openhub.BuildConfig;
import com.thirtydegreesray.openhub.util.FileUtil;
import com.thirtydegreesray.openhub.util.NetHelper;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

/**
 * Retrofit 网络请求
 * Created by ThirtyDegreesRay on 2016/7/15 11:39
 */
public enum  AppRetrofit {
    INSTANCE;

    private final String TAG = "AppRetrofit";

    private final ConcurrentHashMap<String, Retrofit> retrofitMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> tokenMap = new ConcurrentHashMap<>();
    private volatile OkHttpClient okHttpClient;

    private OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            synchronized (this) {
                if (okHttpClient == null) {
                    int timeOut = AppConfig.HTTP_TIME_OUT;
                    OkHttpClient.Builder builder = new OkHttpClient.Builder()
                            .connectTimeout(timeOut, TimeUnit.MILLISECONDS)
                            .readTimeout(timeOut, TimeUnit.MILLISECONDS)
                            .writeTimeout(timeOut, TimeUnit.MILLISECONDS)
                            .retryOnConnectionFailure(true)
                            .addInterceptor(new BaseInterceptor())
                            .addNetworkInterceptor(new NetworkBaseInterceptor());
                    File cacheDir = FileUtil.getHttpImageCacheDir(AppApplication.get());
                    if (cacheDir != null) {
                        builder.cache(new Cache(cacheDir, AppConfig.HTTP_MAX_CACHE_SIZE));
                    }
                    okHttpClient = builder.build();
                }
            }
        }
        return okHttpClient;
    }

    private void createRetrofit(@NonNull String baseUrl, boolean isJson) {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(getOkHttpClient());

        if(isJson){
            builder.addConverterFactory(GsonConverterFactory.create());
        } else {
            builder.addConverterFactory(SimpleXmlConverterFactory.createNonStrict());
        }

        retrofitMap.put(baseUrl + "-" + isJson, builder.build());
    }

    public Retrofit getRetrofit(@NonNull String baseUrl, @Nullable String token, boolean isJson) {
        HttpUrl url = HttpUrl.parse(baseUrl);
        if (url != null) {
            if (StringUtils.isBlank(token)) {
                tokenMap.remove(url.host());
            } else {
                tokenMap.put(url.host(), token);
            }
        }
        String key = baseUrl + "-" + isJson;
        Retrofit retrofit = retrofitMap.get(key);
        if (retrofit == null) {
            createRetrofit(baseUrl, isJson);
            retrofit = retrofitMap.get(key);
        }
        return retrofit;
    }

    public Retrofit getRetrofit(@NonNull String baseUrl, @Nullable String token) {
        return getRetrofit(baseUrl, token, true);
    }

    public void clearTokens() {
        tokenMap.clear();
    }

    /**
     * 拦截器
     */
    private class BaseInterceptor implements Interceptor {
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request request = chain.request();

            //add unique login id in url to differentiate caches
            if(AppData.INSTANCE.getLoggedUser() != null
                    && !AppConfig.isCommonPageUrl(request.url().toString())){
                HttpUrl url = request.url().newBuilder()
                        .addQueryParameter("uniqueLoginId",
                                AppData.INSTANCE.getLoggedUser().getLogin())
                        .build();
                request = request.newBuilder()
                        .url(url)
                        .build();
            }

            //add access token for the target host
            String token = tokenMap.get(request.url().host());
            if(!StringUtils.isBlank(token)){
                String auth = token.startsWith("Basic") ? token : "token " + token;
                request = request.newBuilder()
                        .addHeader("Authorization", auth)
                        .build();
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, request.url().toString());
            }

            //第二次请求，强制使用网络请求
            String forceNetWork = request.header("forceNetWork");
            //有forceNetWork且无网络状态下取从缓存中取
            if (!StringUtils.isBlank(forceNetWork) &&
                    !NetHelper.INSTANCE.getNetEnabled()) {
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .build();
            } else if("true".equals(forceNetWork)){
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_NETWORK)
                        .build();
            }

            return chain.proceed(request);
        }
    }

    /**
     * 网络请求拦截器
     */
    private class NetworkBaseInterceptor implements Interceptor {
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {

            Request request = chain.request();
            Response originalResponse = chain.proceed(request);

            String requestCacheControl = request.cacheControl().toString();

            //有forceNetWork时，强制更改缓存策略
            String forceNetWork = request.header("forceNetWork");
            if(!StringUtils.isBlank(forceNetWork)){
                requestCacheControl = getCacheString();
            }

            if (StringUtils.isBlank(requestCacheControl)) {
                return originalResponse;
            }
            //设置缓存策略
            else {
                Response res = originalResponse.newBuilder()
                        .header("Cache-Control", requestCacheControl)
                        //纠正服务器时间，服务器时间出错时可能会导致缓存处理出错
//                        .header("Date", getGMTTime())
                        .removeHeader("Pragma")
                        .build();
                return res;
            }
        }
    }

    private static String getGMTTime(){
        Date date = new Date();
        DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        format.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        String gmtTime = format.format(date);
        return gmtTime;
    }

    public static String getCacheString(){
        return "public, max-age=" + AppConfig.CACHE_MAX_AGE;
    }

}
