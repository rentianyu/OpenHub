package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * GraphQL mutation 的统一响应体，只关心是否成功，具体字段不需要解析
 */
public class MutationData {

    private String clientMutationId;

    public String getClientMutationId() {
        return clientMutationId;
    }
}
