package com.thirtydegreesray.openhub.mvp.model;

/**
 * GitHub 星标目录（Lists）
 * 对应 GraphQL 的 UserList 类型
 */
public class StarList {

    private String id;
    private String name;
    private String slug;
    private Count items;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public int getCount() {
        return items == null ? 0 : items.getTotalCount();
    }

    public boolean sameAs(StarList other) {
        return other != null && id != null && id.equals(other.id);
    }

    public static class Count {
        private int totalCount;

        public int getTotalCount() {
            return totalCount;
        }
    }
}
