package com.thirtydegreesray.openhub.mvp.model.graphql;

import com.thirtydegreesray.openhub.mvp.model.StarList;

import java.util.ArrayList;

/**
 * GraphQL: query { viewer { lists { nodes { id name slug items { totalCount } } } } }
 */
public class StarListsData {

    private Viewer viewer;

    public Viewer getViewer() {
        return viewer;
    }

    public static class Viewer {
        private Lists lists;

        public Lists getLists() {
            return lists;
        }
    }

    public static class Lists {
        private int totalCount;
        private ArrayList<StarList> nodes;

        public int getTotalCount() {
            return totalCount;
        }

        public ArrayList<StarList> getNodes() {
            return nodes;
        }
    }
}
