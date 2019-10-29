package com.arbytek.trips.models;

/**
 * User model
 */

public class User {
    private String mUserName;
    private String mUserEmail;

    public User(String mUserName, String mUserEmail) {
        this.mUserName = mUserName;
        this.mUserEmail = mUserEmail;
    }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String mUserName) {
        this.mUserName = mUserName;
    }

    public String getUserEmail() {
        return mUserEmail;
    }

    public void setUserEmail(String mUserEmail) {
        this.mUserEmail = mUserEmail;
    }
}
