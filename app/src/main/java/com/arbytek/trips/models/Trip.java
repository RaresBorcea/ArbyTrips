package com.arbytek.trips.models;

import java.io.Serializable;

/**
 * Trip model
 */

public class Trip implements Serializable {
    private String mName;
    private String mDestination;
    private int mType; // 0 - city break, 1 - sea side, 2 - mountains
    private double mPrice, mRating;
    private String mStartDate, mEndDate;
    private String mImage;
    private String mId;
    private boolean mFavourite;

    public Trip() {
    }

    public Trip(String mName, String mId, String mDestination, int mType, double mPrice,
                String mStartDate, String mEndDate, double mRating, String mImage, boolean mFavourite) {
        this.mName = mName;
        this.mId = mId;
        this.mDestination = mDestination;
        this.mType = mType;
        this.mPrice = mPrice;
        this.mStartDate = mStartDate;
        this.mEndDate = mEndDate;
        this.mRating = mRating;
        this.mImage = mImage;
        this.mFavourite = mFavourite;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getDestination() {
        return mDestination;
    }

    public void setDestination(String mDestination) {
        this.mDestination = mDestination;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    public double getPrice() {
        return mPrice;
    }

    public void setPrice(double mPrice) {
        this.mPrice = mPrice;
    }

    public String getStartDate() {
        return mStartDate;
    }

    public void setStartDate(String mStartDate) {
        this.mStartDate = mStartDate;
    }

    public String getEndDate() {
        return mEndDate;
    }

    public void setEndDate(String mEndDate) {
        this.mEndDate = mEndDate;
    }

    public double getRating() {
        return mRating;
    }

    public void setRating(double mRating) {
        this.mRating = mRating;
    }

    public String getImage() {
        return mImage;
    }

    public void setImage(String mImage) {
        this.mImage = mImage;
    }

    public boolean getFavStatus() {
        return mFavourite;
    }

    public void setFavStatus(boolean mFavourite) {
        this.mFavourite = mFavourite;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }
}