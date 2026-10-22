package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * 星标目录相关的 GraphQL 语句
 */
public class GraphqlQueries {

    /** Repository 常用字段，供目录内仓库列表使用 */
    private static final String REPO_FIELDS = "databaseId name nameWithOwner description "
            + "stargazerCount forkCount isPrivate updatedAt pushedAt createdAt url "
            + "primaryLanguage { name } owner { login avatarUrl }";

    /** 我的全部星标目录 */
    public static final String QUERY_STAR_LISTS = "query { viewer { lists(first: 100) { "
            + "totalCount nodes { id name slug items(first: 1) { totalCount } } } } }";

    /** 某个目录下的仓库（分页） */
    public static final String QUERY_STAR_LIST_ITEMS = "query($id: ID!, $after: String) { "
            + "node(id: $id) { ... on UserList { items(first: 100, after: $after) { "
            + "totalCount pageInfo { hasNextPage endCursor } "
            + "nodes { ... on Repository { " + REPO_FIELDS + " } } } } } }";

    /** 某个目录下所有仓库的 fullName（用于生成本地 仓库-目录 归属映射） */
    public static final String QUERY_STAR_LIST_MEMBERS = "query($id: ID!) { "
            + "node(id: $id) { ... on UserList { items(first: 100) { "
            + "nodes { ... on Repository { nameWithOwner } } } } } }";

    /** 设置某个仓库所属的目录（覆盖式，listIds 为空数组表示移出全部目录） */
    public static final String MUTATION_UPDATE_REPO_LISTS = "mutation($itemId: ID!, $listIds: [ID!]!) { "
            + "updateUserListsForItem(input: {itemId: $itemId, listIds: $listIds}) { clientMutationId } }";

    /** 新建目录 */
    public static final String MUTATION_CREATE_LIST = "mutation($name: String!) { "
            + "createUserList(input: {name: $name}) { clientMutationId } }";

    /** 重命名目录 */
    public static final String MUTATION_UPDATE_LIST = "mutation($listId: ID!, $name: String!) { "
            + "updateUserList(input: {listId: $listId, name: $name}) { clientMutationId } }";

    /** 删除目录 */
    public static final String MUTATION_DELETE_LIST = "mutation($listId: ID!) { "
            + "deleteUserList(input: {listId: $listId}) { clientMutationId } }";
}
