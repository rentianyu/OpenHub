package com.thirtydegreesray.openhub.util;

import com.thirtydegreesray.openhub.mvp.model.StarList;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 星标目录（GitHub Lists）内存缓存
 *
 * 目录列表与「仓库 -> 所属目录」映射都需要多次 GraphQL 请求，
 * 在进程内缓存 10 分钟，星标页与仓库详情页共用，避免重复拉取。
 */
public class StarListCache {

    private static final long VALID_TIME = 10 * 60 * 1000L;

    private static final StarListCache INSTANCE = new StarListCache();

    private ArrayList<StarList> starLists;
    private HashMap<String, ArrayList<StarList>> memberships;
    private long updateTime = 0;

    public static StarListCache getInstance() {
        return INSTANCE;
    }

    public boolean hasStarLists() {
        return starLists != null && !starLists.isEmpty();
    }

    public boolean isValid() {
        return starLists != null && System.currentTimeMillis() - updateTime < VALID_TIME;
    }

    public boolean isMembershipLoaded() {
        return memberships != null;
    }

    public ArrayList<StarList> getStarLists() {
        return starLists;
    }

    public HashMap<String, ArrayList<StarList>> getMemberships() {
        return memberships;
    }

    public void setStarLists(ArrayList<StarList> lists) {
        this.starLists = lists;
        this.memberships = null;
        this.updateTime = System.currentTimeMillis();
    }

    public void startMemberships() {
        this.memberships = new HashMap<>();
        this.updateTime = System.currentTimeMillis();
    }

    public void addMember(String fullName, StarList starList) {
        if (fullName == null || starList == null) {
            return;
        }
        if (memberships == null) {
            memberships = new HashMap<>();
        }
        ArrayList<StarList> lists = memberships.get(fullName);
        if (lists == null) {
            lists = new ArrayList<>();
            memberships.put(fullName, lists);
        }
        for (StarList exist : lists) {
            if (exist.sameAs(starList)) {
                return;
            }
        }
        lists.add(starList);
    }

    public ArrayList<StarList> getMember(String fullName) {
        if (memberships == null || fullName == null) {
            return null;
        }
        return memberships.get(fullName);
    }

    public int getMemberCount(String fullName) {
        ArrayList<StarList> lists = getMember(fullName);
        return lists == null ? 0 : lists.size();
    }

    public void invalidate() {
        updateTime = 0;
        memberships = null;
    }

    public void clear() {
        starLists = null;
        memberships = null;
        updateTime = 0;
    }
}
