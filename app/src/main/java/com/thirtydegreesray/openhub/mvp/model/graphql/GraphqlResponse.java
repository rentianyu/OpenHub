package com.thirtydegreesray.openhub.mvp.model.graphql;

import java.util.ArrayList;

/**
 * GitHub GraphQL 响应外层结构
 */
public class GraphqlResponse<T> {

    private T data;
    private ArrayList<GraphqlError> errors;

    public T getData() {
        return data;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public String getErrorMessages() {
        if (!hasErrors()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (GraphqlError error : errors) {
            if (error == null || error.getMessage() == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(error.getMessage());
        }
        return builder.toString();
    }

    public static class GraphqlError {
        private String message;
        private String type;

        public String getMessage() {
            return message;
        }

        public String getType() {
            return type;
        }
    }
}
