package com.arbytek.trips;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.arbytek.trips.datePicker.EndDateFragment;
import com.arbytek.trips.datePicker.StartDateFragment;
import com.arbytek.trips.models.Trip;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import id.zelory.compressor.Compressor;

/**
 * Activity used to add and edit trips
 */

public class ManageTripActivity extends AppCompatActivity implements
        StartDateFragment.StartDateDialogListener, EndDateFragment.EndDateDialogListener {

    // Image selection request codes
    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_GALLERY = 101;

    // Permissions request code
    private static final int PERMISSIONS_REQ_CODE = 1200;
    private String[] mCameraPermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // Activity fields
    private ImageView mDestinationImageView;
    private ProgressBar mProgressBar;
    private Button mBtnImageGallery;
    private Button mBtnImageCapture;
    private Button mBtnStartDate;
    private Button mBtnEndDate;
    private Button mBtnSave;
    private Bitmap mBitmapCamera;
    private EditText mNameEditText;
    private EditText mDestinationEditText;
    private RadioButton mCityButton;
    private RadioButton mSeaButton;
    private RadioButton mMountainButton;
    private TextView mPriceTextView;
    private SeekBar mPriceBar;
    private RatingBar mRatingBar;
    private int mType = -1; // -1 = no type selected
    private double mPrice;
    private Uri mImage;
    private String mStartDate = null;
    private String mEndDate = null;

    // Static fields modified by DateFragments
    private static String sStartDate = "DD/MM/YYYY";
    private static String sEndDate = "DD/MM/YYYY";

    // User all and favourite trips lists
    private CollectionReference mAllTripListRef;
    private CollectionReference mFavTripListRef;

    private String mUserEmail;
    private boolean mIsGallerySelected;
    private Trip mTrip = null;
    private String mTripId = null;
    private static final String TAG = "ManageTripActivity";

    // Firebase instance variables
    private FirebaseFirestore mFirestore;
    private FirebaseStorage mStorage;

    // Images storage reference
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_trip);
        setTitle(R.string.manage_trip);

        // Obtain fields for editing
        mNameEditText = findViewById(R.id.trip_name);
        mDestinationEditText = findViewById(R.id.trip_destination);
        mCityButton = findViewById(R.id.city_break);
        mSeaButton = findViewById(R.id.sea_side);
        mMountainButton = findViewById(R.id.mountains);
        mPriceBar = findViewById(R.id.price_bar);
        mBtnStartDate = findViewById(R.id.btn_start_date);
        mBtnEndDate = findViewById(R.id.btn_end_date);
        mRatingBar = findViewById(R.id.rating_bar);
        mPriceTextView = findViewById(R.id.textview_price);
        mBtnSave = findViewById(R.id.btn_save);
        mBtnImageCapture = findViewById(R.id.btn_take_picture);
        mBtnImageGallery = findViewById(R.id.btn_select_picture);
        mDestinationImageView = findViewById(R.id.imageview_destination);
        mProgressBar = findViewById(R.id.progress_circular);

        // Get Firebase instances
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();

        // Extract data from intent bundle
        Bundle bundle = getIntent().getExtras();
        mUserEmail = bundle.getString("userEmail");
        mAllTripListRef = mFirestore.collection("users").document(mUserEmail)
                .collection("allTripLists");
        mFavTripListRef = mFirestore.collection("users").document(mUserEmail)
                .collection("favTripLists");

        // Get Storage reference
        storageRef = mStorage.getReference().child("images").child(mUserEmail);

        // If editing existing trip, populate fields with trip details
        if (!bundle.getBoolean("isNewTrip")) {
            setTitle(R.string.edit_trip);

            mTrip = (Trip) getIntent().getSerializableExtra("trip");
            mTripId = bundle.getString("tripId");
            mNameEditText.setText(mTrip.getName());
            mDestinationEditText.setText(mTrip.getDestination());

            int type = mTrip.getType();
            switch (type) {
                case 0:
                    mCityButton.setChecked(true);
                    break;
                case 1:
                    mSeaButton.setChecked(true);
                    break;
                case 2:
                    mMountainButton.setChecked(true);
                    break;
                default:
                    mCityButton.setChecked(false);
                    mSeaButton.setChecked(false);
                    mMountainButton.setChecked(false);
            }

            mPriceBar.setProgress((int) mTrip.getPrice());
            mPrice = mTrip.getPrice();
            mPriceTextView.setText(getResources().getString(R.string.price, (int) mPrice));

            if (mTrip.getStartDate() != null) {
                sStartDate = mTrip.getStartDate();
            }
            if (mTrip.getEndDate() != null) {
                sEndDate = mTrip.getEndDate();
            }
            mBtnStartDate.setText(sStartDate);
            mBtnEndDate.setText(sEndDate);

            mRatingBar.setRating((float) mTrip.getRating());
            if (mTrip.getImage() != null) {
                mImage = Uri.parse(mTrip.getImage());
                Picasso.get().load(mImage).fit().centerCrop()
                        .placeholder(R.drawable.placeholder).into(mDestinationImageView);
            } else {
                Picasso.get().load(R.drawable.placeholder).into(mDestinationImageView);
            }
        }

        // Start date listener
        mBtnStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new StartDateFragment();
                newFragment.show(getSupportFragmentManager(), "DatePicker");
            }
        });

        // End date listener
        mBtnEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new EndDateFragment();
                newFragment.show(getSupportFragmentManager(), "DatePicker");
            }
        });

        // PriceBar listener
        mPriceBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mPrice = progress;
                mPriceTextView.setText(getResources().getString(R.string.price, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Trip name changedListener
        mNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence name, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence name, int start, int before, int count) {
            }

            // Make sure user provides a name for the trip
            @Override
            public void afterTextChanged(Editable name) {
                validateNameText(name);
            }
        });

        // Display warning if user skips entering a trip name
        mNameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View editText, boolean hasFocus) {
                if (!hasFocus) {
                    validateNameText(((EditText) editText).getText());
                }
            }
        });

        // Save button listener
        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Save only if user provided a trip name
                if (validateNameText(mNameEditText.getText())) {
                    // While uploading trip to Firestore, display a ProgressBar
                    mDestinationImageView.setVisibility(View.GONE);
                    mProgressBar.setVisibility(View.VISIBLE);
                    // Disable Save button while uploading
                    mBtnSave.setEnabled(false);
                    mBtnSave.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

                    // Reset static fields for dates DialogFragments buttons
                    mStartDate = sStartDate;
                    mEndDate = sEndDate;
                    sStartDate = "DD/MM/YYYY";
                    sEndDate = "DD/MM/YYYY";

                    Uri oldImage = null;

                    // Get uri from initial trip image
                    if (mTrip != null) {
                        if (mTrip.getImage() != null) {
                            oldImage = Uri.parse(mTrip.getImage());
                        }
                    }

                    // If edited trip with modified image, then delete old image
                    if (oldImage != null) {
                        StorageReference imageRef = mStorage.getReferenceFromUrl(mTrip.getImage());
                        if (mImage != null) {
                            if (!mImage.toString().equals(oldImage.toString())) {
                                imageRef.delete();
                            }
                        } else {
                            imageRef.delete();
                        }
                    }

                    // If edited trip with modified image OR new trip, then upload new image
                    boolean upload = false;
                    if (mImage != null) {
                        if (oldImage != null) {
                            if (!mImage.toString().equals(oldImage.toString())) {
                                upload = true;
                            }
                        } else {
                            upload = true;
                        }
                    }
                    if (upload) {
                        File imageFile;

                        if (mIsGallerySelected) {
                            // Get real path for Gallery selected image
                            imageFile = new File(getPathFromUri(mImage));
                        } else {
                            imageFile = new File(mImage.getPath());
                        }

                        File compressedImage = null;

                        // Compress image before uploading
                        try {
                            compressedImage = new Compressor(getApplicationContext())
                                    .setMaxWidth(640)
                                    .setQuality(75)
                                    .compressToFile(imageFile);
                        } catch (IOException e) {
                            Log.d(TAG, "Image compression failed");
                            e.printStackTrace();
                        }

                        if (compressedImage != null) {
                            // Generate unique UUID as Firebase Storage file name
                            String uuid = UUID.randomUUID().toString();
                            StorageReference imageRef = storageRef.child(uuid);

                            // Upload compressed file to Firebase Storage
                            imageRef.putFile(Uri.fromFile(compressedImage))
                                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                            // Store downloadUrl as trip field in database
                                            Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                                            while (!urlTask.isSuccessful()) ;
                                            Uri downloadUrl = urlTask.getResult();
                                            String imageUri = downloadUrl.toString();

                                            // Upload trip to Firestore
                                            saveTrip(imageUri);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d(TAG, "Image upload failed");
                                            e.printStackTrace();
                                        }
                                    });
                        }

                    } else if (mImage != null && oldImage != null) {
                        if (mImage.toString().equals(oldImage.toString())) {
                            // Edited trip with original image
                            saveTrip(oldImage.toString());
                        }

                    } else {
                        // Edited trip with no image OR new trip with no image
                        saveTrip(null);
                    }

                    // After saving, go back to MainActivity
                    Intent intent = new Intent(ManageTripActivity.this, MainActivity.class);
                    // Include extra for Snackbar confirmation
                    if (mTrip != null) {
                        intent.putExtra("tripModified", true);
                    } else {
                        intent.putExtra("tripAdded", true);
                    }
                    startActivity(intent);
                }
            }
        });

        // Start CameraActivity to take a picture
        mBtnImageCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsGallerySelected = false;
                if (checkAndRequestPermissions()) {
                    Intent intent = new Intent(ManageTripActivity.this, CameraActivity.class);
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        // Select existing image from Gallery
        mBtnImageGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsGallerySelected = true;
                if (checkAndRequestPermissions()) {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    // Ensure only components of type image are selected
                    intent.setType("image/*");
                    // Ensure only components with certain MIME types are targeted
                    String[] mimeTypes = {"image/jpeg", "image/png"};
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                    startActivityForResult(intent, REQUEST_IMAGE_GALLERY);
                }
            }
        });
    }

    /**
     * Reset static fields for dates DialogFragments buttons if no mTrip is saved
     */
    @Override
    public void onBackPressed() {
        sStartDate = "DD/MM/YYYY";
        sEndDate = "DD/MM/YYYY";
        super.onBackPressed();
    }

    public boolean checkAndRequestPermissions() {
        // Check which permissions are granted
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : mCameraPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }

        // Ask for non-granted permissions
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    PERMISSIONS_REQ_CODE);
            return false;
        }

        // All permissions are granted
        return true;
    }

    /**
     * Dialog for permissions asking
     */
    public AlertDialog showDialog(String title, String msg, String positiveLabel,
                                  DialogInterface.OnClickListener positiveOnClick,
                                  String negativeLabel, DialogInterface.OnClickListener negativeOnClick,
                                  boolean isCancelAble) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(isCancelAble);
        builder.setMessage(msg);
        builder.setPositiveButton(positiveLabel, positiveOnClick);
        builder.setNegativeButton(negativeLabel, negativeOnClick);

        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQ_CODE) {
            HashMap<String, Integer> permissionResults = new HashMap<>();
            int deniedCount = 0;

            // Gather permissions grant results
            for (int i = 0; i < grantResults.length; i++) {
                // Add only denied permissions
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }

            // Check if all permissions are granted
            if (deniedCount != 0) {
                for (Map.Entry<String, Integer> entry : permissionResults.entrySet()) {
                    String permName = entry.getKey();

                    // Permission denied (this is the first time, when 'never ask again' isn't checked)
                    // Ask again and explain permission usage
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permName)) {
                        showDialog("", getResources().getString(R.string.explain_perm_usage),
                                getResources().getString(R.string.grant_perm),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        checkAndRequestPermissions();
                                    }
                                },
                                getResources().getString(R.string.deny_perm),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                }, false);

                    } else {
                        // Permission denied and 'never ask again' checked
                        // Ask user to go to settings app and manually allow permissions
                        showDialog("",
                                getResources().getString(R.string.some_perm_denied),
                                getResources().getString(R.string.go_to_settings),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        // Go to Settings
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", getPackageName(), null));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    }
                                },
                                getResources().getString(R.string.deny_perm),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                }, false);
                        break;
                    }
                }

            } else {
                // After granting permissions, restart intents
                if (mIsGallerySelected) {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    String[] mimeTypes = {"image/jpeg", "image/png"};
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                    startActivityForResult(intent, REQUEST_IMAGE_GALLERY);
                } else {
                    Intent intent = new Intent(ManageTripActivity.this, CameraActivity.class);
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_IMAGE_GALLERY: {
                // Image from Gallery selected
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    // data.getData return the content URI for the selected Image
                    mImage = data.getData();
                    String path = getPathFromUri(mImage);

                    // Get the correctly oriented image
                    int orientation = getOrientation(ManageTripActivity.this, mImage);
                    mBitmapCamera = BitmapFactory.decodeFile(path);
                    if (orientation > 0) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(orientation);
                        mBitmapCamera = Bitmap.createBitmap(mBitmapCamera, 0, 0, mBitmapCamera.getWidth(),
                                mBitmapCamera.getHeight(), matrix, true);
                    }

                    // Set the Image in ImageView
                    if (mBitmapCamera != null) {
                        mDestinationImageView.setImageBitmap(mBitmapCamera);
                    }
                }
                break;
            }

            case REQUEST_IMAGE_CAPTURE: {
                // Took picture with CameraActivity
                if (resultCode == RESULT_OK) {

                    mImage = Uri.parse(data.getStringExtra("image_uri"));
                    String path = mImage.getPath();

                    mBitmapCamera = createScaledBitmap(path, mDestinationImageView.getWidth(),
                            mDestinationImageView.getHeight());
                    if (mBitmapCamera != null) {
                        mDestinationImageView.setImageBitmap(mBitmapCamera);
                    }
                }
            }
            return;
        }
    }

    /**
     * Create compressed bitmap
     */
    public Bitmap createScaledBitmap(String pathName, int width, int height) {
        final BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, opt);
        opt.inSampleSize = calculateBmpSampleSize(opt, width, height);
        opt.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, opt);
    }

    public int calculateBmpSampleSize(BitmapFactory.Options opt, int width, int height) {
        final int outHeight = opt.outHeight;
        final int outWidth = opt.outWidth;
        int sampleSize = 1;

        if (outHeight > height || outWidth > width) {

            final int halfHeight = outHeight / 2;
            final int halfWidth = outWidth / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width
            while ((halfHeight / sampleSize) >= height
                    && (halfWidth / sampleSize) >= width) {
                sampleSize *= 2;
            }
        }
        return sampleSize;
    }

    /**
     * Get image correct orientation
     */
    public static int getOrientation(Context context, Uri photoUri) {
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);

        if (cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        return cursor.getInt(0);
    }

    /**
     * Get real path from Uri
     */
    public String getPathFromUri(Uri uri) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};

        Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);

        String path = cursor.getString(columnIndex);
        cursor.close();
        return path;
    }

    public void onRadioButtonClicked(View view) {
        // Check if any option is checked
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was checked
        switch (view.getId()) {
            case R.id.city_break:
                if (checked)
                    mType = 0;
                break;
            case R.id.sea_side:
                if (checked)
                    mType = 1;
                break;
            case R.id.mountains:
                if (checked)
                    mType = 2;
                break;
        }
    }

    /**
     * Update start date text on DialogFragments buttons
     */
    @Override
    public void applyStartDate(String startDate) {
        mBtnStartDate.setText(startDate);
    }

    /**
     * Update end date text on DialogFragments buttons
     */
    @Override
    public void applyEndDate(String endDate) {
        mBtnEndDate.setText(endDate);
    }

    public static String getStartDate() {
        return sStartDate;
    }

    public static String getEndDate() {
        return sEndDate;
    }

    public static void setStartDate(String mStartDate) {
        ManageTripActivity.sStartDate = mStartDate;
    }

    public static void setEndDate(String mEndDate) {
        ManageTripActivity.sEndDate = mEndDate;
    }

    /**
     * Add trip to user's AllTripList in Firestore
     */
    public void saveTrip(String imageUri) {
        // Get unique document ID
        if (mTripId == null || mTripId.isEmpty()) {
            mTripId = mAllTripListRef.document().getId();
        }

        boolean favStatus = false;
        if (mTrip != null) {
            favStatus = mTrip.getFavStatus();
        }

        // Create trip and upload to Firestore
        Trip trip = new Trip(mNameEditText.getText().toString(), mTripId,
                mDestinationEditText.getText().toString(), mType, mPrice,
                mStartDate, mEndDate, mRatingBar.getRating(), imageUri, favStatus);
        mAllTripListRef.document(mTripId).set(trip);

        // If editing fav trip, also update it in favourite trips list
        if (favStatus) {
            mFavTripListRef.document(mTripId).set(trip);
        }
    }

    /**
     * Validate data of Trip name EditText
     */
    private boolean validateNameText(Editable name) {
        TextInputLayout nameInputLayout = findViewById(R.id.inputlayout_name);
        // If trip name entered, cancel any warning
        if (!TextUtils.isEmpty(name)) {
            nameInputLayout.setError(null);
            return true;
        } else {
            // Otherwise, set error
            nameInputLayout.setError(getResources().getString(R.string.trip_name_warning));
            return false;
        }
    }
}