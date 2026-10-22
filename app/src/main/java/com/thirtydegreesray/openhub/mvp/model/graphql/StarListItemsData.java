package com.thirtydegreesray.openhub.mvp.model.graphql;

import java.util.ArrayList;

/**
 * GraphQL: query { node(id: $id) { ... on UserList { items { nodes { ... on Repository {} } } } } }
 */
public class StarListItemsData {

    private Node node;

    public Node getNode() {
        return node;
    }

    public static class Node {
        private Items items;

        public Items getItems() {
            return items;
        }
    }

    public static class Items {
        private int totalCount;
        private PageInfo pageInfo;
        private ArrayList<GraphqlRepo> nodes;

        public int getTotalCount() {
            return totalCount;
        }

        public PageInfo getPageInfo() {
            return pageInfo;
        }

        public ArrayList<GraphqlRepo> getNodes() {
            return nodes;
        }
    }

    public static class PageInfo {
        private boolean hasNextPage;
        private String endCursor;

        public boolean isHasNextPage() {
            return hasNextPage;
        }

        public String getEndCursor() {
            return endCursor;
        }
    }
}
