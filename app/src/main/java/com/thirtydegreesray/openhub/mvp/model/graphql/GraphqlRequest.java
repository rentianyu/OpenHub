package com.thirtydegreesray.openhub.mvp.model.graphql;

import java.util.HashMap;
import java.util.Map;

/**
 * GitHub GraphQL 请求体
 * GitHub 的星标目录（Lists）功能只开放了 GraphQL API，REST API 没有对应端点。
 */
public class GraphqlRequest {

    private String query;
    private Map<String, Object> variables;

    public GraphqlRequest(String query) {
        this.query = query;
    }

    public GraphqlRequest(String query, Map<String, Object> variables) {
        this.query = query;
        this.variables = variables;
    }

    public static GraphqlRequest withVariable(String query, String key, Object value) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(key, value);
        return new GraphqlRequest(query, variables);
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }
}
