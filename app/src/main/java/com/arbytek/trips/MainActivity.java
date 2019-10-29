package com.arbytek.trips;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * NavDrawer activity
 * Contains Fragments with RecyclerView to display user trips
 */

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // User information
    private ImageView mAvatar;
    private String mUserEmail, mUserName;
    private TextView mEmailDrawer, mNameDrawer;

    // Welcome message at sign-in
    private static boolean sShowHelloMessage = true;

    private GoogleSignInClient mSignInClient;
    private DrawerLayout mDrawerLayout;
    private FloatingActionButton mFab;
    private Fragment mFragment = null;
    private Class fragmentClass = null;

    // Firebase instance variables
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mFirebaseAuth;

    // Control which list is displayed: all or favourite
    static boolean sIsFavListDisplayed = false;

    public FloatingActionButton getFab() {
        return mFab;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set the Floating Action Button
        mFab = findViewById(R.id.fab);

        // Set the Navigation Drawer toggle and SelectionListener
        mDrawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Display App Title accordingly
        if (sIsFavListDisplayed) {
            setTitle(R.string.fav_trips);
            // Needed when user edits trip from Favourite list
            navigationView.setCheckedItem(R.id.nav_favourite);
        } else {
            setTitle(R.string.all_trips);
        }

        // Set the default fragment (Home)
        fragmentClass = RecyclerFragment.class;
        try {
            // Instantiate home mFragment
            mFragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.nav_host_fragment, mFragment).commit();

        // Obtain NavDrawer header to personalize with user details
        mAvatar = navigationView.getHeaderView(0).findViewById(R.id.imageView);
        mNameDrawer = navigationView.getHeaderView(0).findViewById(R.id.userName);
        mEmailDrawer = navigationView.getHeaderView(0).findViewById(R.id.userEmail);

        // Get the signed-in account and personalize the NavDrawer header
        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (googleSignInAccount != null) {
            mUserEmail = googleSignInAccount.getEmail();
            mUserName = googleSignInAccount.getDisplayName();
            mNameDrawer.setText(mUserName);
            mEmailDrawer.setText(mUserEmail);

            // Set the user profile picture and make it round
            Picasso.get()
                    .load(googleSignInAccount.getPhotoUrl())
                    .into(mAvatar, new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap imageBitmap = ((BitmapDrawable) mAvatar.getDrawable()).getBitmap();
                            RoundedBitmapDrawable imageDrawable =
                                    RoundedBitmapDrawableFactory.create(getResources(), imageBitmap);
                            imageDrawable.setCircular(true);
                            imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(),
                                    imageBitmap.getHeight()) / 2.0f);
                            mAvatar.setImageDrawable(imageDrawable);
                        }

                        @Override
                        public void onError(Exception e) {
                            mAvatar.setImageResource(R.mipmap.ic_launcher_round);
                        }
                    });

            // Show Hello message when user opens app
            if (sShowHelloMessage) {
                Snackbar.make(mFab, getResources().getString(R.string.hello_message) +
                        " " + mUserName + "!", Snackbar.LENGTH_LONG).show();
                sShowHelloMessage = false;
            }

            // Show Snackbars when trips are added or modified
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                if (bundle.getBoolean("tripAdded")) {
                    Snackbar.make(mFab, getResources().getString(R.string.trip_added),
                            Snackbar.LENGTH_SHORT).show();
                } else if (bundle.getBoolean("tripModified")) {
                    Snackbar.make(mFab, getResources().getString(R.string.trip_modified),
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            // Configure the Sign-In API
            GoogleSignInOptions googleSignInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build();

            // Obtain the Sign-In client
            mSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        }

        // Get Firebase instances
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Set FAB clickListener
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ManageTripActivity.class);
                intent.putExtra("userEmail", mUserEmail);
                intent.putExtra("isNewTrip", true);
                startActivity(intent);
            }
        });
    }

    /**
     * Set back button to close the NavDrawer
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * At logout, send user to LoginActivity
     */
    private void signOut() {
        sShowHelloMessage = true;
        mFirebaseAuth.signOut();
        mSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                });
    }

    /**
     * Set actions for each NavDrawer option
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_home: {
                // All trips list
                fragmentClass = RecyclerFragment.class;
                sIsFavListDisplayed = false;
                setTitle(R.string.all_trips);
                break;
            }
            case R.id.nav_favourite: {
                // Favourite trips list
                fragmentClass = RecyclerFragment.class;
                sIsFavListDisplayed = true;
                setTitle(R.string.fav_trips);
                break;
            }
            case R.id.nav_about: {
                // About us fragment
                fragmentClass = AboutFragment.class;
                sIsFavListDisplayed = false;
                setTitle(R.string.about_me);
                break;
            }
            case R.id.sing_out: {
                signOut();
                break;
            }
            case R.id.nav_contact: {
                // Create email message for technical support
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse(getString(R.string.mailto)));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources()
                        .getString(R.string.trips_support));
                emailIntent.putExtra(Intent.EXTRA_TEXT, getResources()
                        .getString(R.string.hello_message) + "\n\n" + mUserName);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.chooser_title)));
                break;
            }
            default:
                fragmentClass = RecyclerFragment.class;
                sIsFavListDisplayed = false;
                setTitle(R.string.all_trips);
        }

        try {
            mFragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.nav_host_fragment, mFragment).commit();

        // Close drawer after action
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}