

package com.thirtydegreesray.openhub.mvp.model;

import android.os.Parcel;
import com.google.gson.annotations.JsonAdapter;
import android.os.Parcelable;

/**
 * Created by ThirtyDegreesRay on 2017/11/6 20:15:41
 */

public class NotificationSubject implements Parcelable {

    @JsonAdapter(TypeGsonAdapter.class)
    public enum Type{
        Issue, PullRequest, Commit, Discussion, Release,
        unknown;
    }

    /** tolerate unknown values from github instead of failing the whole payload */
        public static class TypeGsonAdapter implements
                com.google.gson.JsonDeserializer<Type>, com.google.gson.JsonSerializer<Type> {
            @Override
            public Type deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT,
                    com.google.gson.JsonDeserializationContext context) {
                try {
                    return Type.valueOf(json.getAsString());
                } catch (Exception e) {
                    return Type.unknown;
                }
            }
            @Override
            public com.google.gson.JsonElement serialize(Type src, java.lang.reflect.Type typeOfT,
                    com.google.gson.JsonSerializationContext context) {
                return new com.google.gson.JsonPrimitive(src == null ? "" : src.name());
            }
        }


    private String title;
    private String url;
    private Type type;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.title);
        dest.writeString(this.url);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
    }

    public NotificationSubject() {
    }

    protected NotificationSubject(Parcel in) {
        this.title = in.readString();
        this.url = in.readString();
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : Type.values()[tmpType];
    }

    public static final Parcelable.Creator<NotificationSubject> CREATOR = new Parcelable.Creator<NotificationSubject>() {
        @Override
        public NotificationSubject createFromParcel(Parcel source) {
            return new NotificationSubject(source);
        }

        @Override
        public NotificationSubject[] newArray(int size) {
            return new NotificationSubject[size];
        }
    };

}
