package com.arbytek.trips;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.arbytek.trips.models.Trip;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * Displays trips information
 */

public class TripDetailsActivity extends AppCompatActivity {

    TextView mNameTextView;
    TextView mDestinationTextView;
    TextView mTypeTextView;
    TextView mPriceTextView;
    TextView mStartDateTextView;
    TextView mEndDateTextView;
    RatingBar mRatingBar;
    ImageView mTripImage;
    FrameLayout mNoImageLayout;
    FrameLayout mWithImageLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_details);
        setTitle(R.string.trip_details);

        mNameTextView = findViewById(R.id.trip_name);
        mDestinationTextView = findViewById(R.id.trip_destination);
        mTypeTextView = findViewById(R.id.trip_type);
        mPriceTextView = findViewById(R.id.trip_price);
        mStartDateTextView = findViewById(R.id.trip_start_date);
        mEndDateTextView = findViewById(R.id.trip_end_date);
        mRatingBar = findViewById(R.id.trip_rating);
        mTripImage = findViewById(R.id.trip_image);
        mNoImageLayout = findViewById(R.id.no_image_layout);
        mWithImageLayout = findViewById(R.id.with_image_layout);

        // Get trip from Intent
        Trip trip = (Trip) getIntent().getSerializableExtra("trip");

        // Set fields based on trip information
        mNameTextView.setText(trip.getName());
        mDestinationTextView.setText(trip.getDestination());

        int type = trip.getType();
        switch (type) {
            case 0:
                mTypeTextView.setText(getString(R.string.city_trip));
                break;
            case 1:
                mTypeTextView.setText(getString(R.string.sea_trip));
                break;
            case 2:
                mTypeTextView.setText(getString(R.string.mountain_trip));
                break;
            default:
                mTypeTextView.setText(getString(R.string.no_type));
        }

        mPriceTextView.setText(getString(R.string.price, (int) trip.getPrice()));

        if (trip.getStartDate().equals(getString(R.string.date_format))) {
            mStartDateTextView.setText(getString(R.string.no_start_date));
        } else {
            mStartDateTextView.setText(trip.getStartDate());
        }
        if (trip.getEndDate().equals(getString(R.string.date_format))) {
            mEndDateTextView.setText(getString(R.string.no_end_date));
        } else {
            mEndDateTextView.setText(trip.getEndDate());
        }

        mRatingBar.setRating((float) trip.getRating());

        // Set trip image, making it round
        if (trip.getImage() != null) {
            Uri mImage = Uri.parse(trip.getImage());
            Picasso.get()
                    .load(mImage)
                    .fit()
                    .centerCrop()
                    .placeholder(R.drawable.placeholder)
                    .into(mTripImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap imageBitmap = ((BitmapDrawable) mTripImage.getDrawable()).getBitmap();
                            RoundedBitmapDrawable imageDrawable =
                                    RoundedBitmapDrawableFactory.create(getResources(), imageBitmap);
                            imageDrawable.setCircular(true);
                            imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(),
                                    imageBitmap.getHeight()) / 2.0f);
                            mTripImage.setImageDrawable(imageDrawable);
                        }

                        @Override
                        public void onError(Exception e) {
                            // OnError, display layout for trip with no image
                            mWithImageLayout.setVisibility(View.GONE);
                            mNoImageLayout.setVisibility(View.VISIBLE);
                        }
                    });
        } else {
            // If no image attached, display layout for trip with no image
            mWithImageLayout.setVisibility(View.GONE);
            mNoImageLayout.setVisibility(View.VISIBLE);
        }
    }
}