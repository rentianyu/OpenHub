package com.thirtydegreesray.openhub.mvp.model.graphql;

import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.model.User;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * GraphQL 返回的仓库节点，转换为 App 通用的 {@link Repository} 模型
 */
public class GraphqlRepo {

    private int databaseId;
    private String name;
    private String nameWithOwner;
    private String description;
    private int stargazerCount;
    private int forkCount;
    private boolean isPrivate;
    private String updatedAt;
    private String pushedAt;
    private String createdAt;
    private String url;

    private Language primaryLanguage;
    private Owner owner;

    public String getNameWithOwner() {
        return nameWithOwner;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public Repository toRepository() {
        Repository repository = new Repository();
        repository.setId(databaseId);
        repository.setName(name);
        repository.setFullName(nameWithOwner);
        repository.setDescription(description);
        repository.setStargazersCount(stargazerCount);
        repository.setForksCount(forkCount);
        repository.setRepPrivate(isPrivate);
        repository.setHtmlUrl(url);
        repository.setCreatedAt(parseDate(createdAt));
        repository.setUpdatedAt(parseDate(updatedAt));
        repository.setPushedAt(parseDate(pushedAt));
        if (primaryLanguage != null) {
            repository.setLanguage(primaryLanguage.name);
        }
        if (owner != null) {
            User user = new User();
            user.setLogin(owner.login);
            user.setAvatarUrl(owner.avatarUrl);
            repository.setOwner(user);
        }
        return repository;
    }

    private static Date parseDate(String isoDate) {
        if (isoDate == null || isoDate.trim().length() == 0) {
            return null;
        }
        String value = isoDate.trim();
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ssZ"
        };
        for (String pattern : patterns) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            ParsePosition position = new ParsePosition(0);
            Date date = format.parse(value, position);
            if (date != null && position.getIndex() == value.length()) {
                return date;
            }
        }
        return null;
    }

    private static class Language {
        private String name;
    }

    private static class Owner {
        private String login;
        private String avatarUrl;
    }
}
