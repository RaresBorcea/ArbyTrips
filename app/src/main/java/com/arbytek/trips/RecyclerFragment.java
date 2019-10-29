package com.arbytek.trips;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arbytek.trips.models.Trip;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

/**
 * Fragment for trips RecyclerView
 */

public class RecyclerFragment extends Fragment {
    // User information
    private String mUserEmail, mUserName;

    // Firebase instance variables
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseStorage mStorage;

    // User all and favourite trips lists
    private CollectionReference mFavTripListRef;
    private CollectionReference mAllTripListRef;

    FirestoreRecyclerAdapter mAdapter;
    Query mQuery;
    private LinearLayoutManager mLayoutManager;

    // Empty-list layouts to display hints
    private LinearLayout mEmptyList;
    private LinearLayout mEmptyFav;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recycler, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Set RecyclerView and its layout manager
        final ContextMenuRecyclerView recyclerView = view.findViewById(R.id.recyclerview_trips);
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        // Register to display contextMenu onLongClick
        registerForContextMenu(recyclerView);

        // Enable caching and set size for efficiency
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);

        // Disable itemChanged animation to avoid blinking
        ((DefaultItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        // Get the signed-in account to display user data
        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(getActivity());
        if (googleSignInAccount != null) {
            mUserEmail = googleSignInAccount.getEmail();
            mUserName = googleSignInAccount.getDisplayName();
        }

        // Get Firebase instances
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();

        // Get empty list layouts to display hints
        mEmptyList = view.findViewById(R.id.layout_allempty);
        mEmptyFav = view.findViewById(R.id.layout_favempty);

        // Existing Firestore path required by collections
        // Otherwise non-existent path exception at sign-out
        if (mUserEmail != null) {
            // Get allTrips and favTrips references
            mFavTripListRef = mFirestore.collection("users").document(mUserEmail)
                    .collection("favTripLists");
            mAllTripListRef = mFirestore.collection("users").document(mUserEmail)
                    .collection("allTripLists");

            if (MainActivity.sIsFavListDisplayed) {
                // Display the Favourite Trips list
                mQuery = mFirestore.collection("users").document(mUserEmail)
                        .collection("favTripLists")
                        .orderBy("name", Query.Direction.ASCENDING);
            } else {
                // Display the All Trips list
                mQuery = mFirestore.collection("users").document(mUserEmail)
                        .collection("allTripLists")
                        .orderBy("name", Query.Direction.ASCENDING);
            }

            // Set the RecyclerView options
            FirestoreRecyclerOptions<Trip> options = new FirestoreRecyclerOptions.Builder<Trip>()
                    .setQuery(mQuery, Trip.class)
                    .build();

            // Create the adapter for the RecyclerView
            mAdapter = new FirestoreRecyclerAdapter<Trip, TripsViewHolder>(options) {
                @Override
                public TripsViewHolder onCreateViewHolder(ViewGroup group, int i) {
                    View view = LayoutInflater.from(group.getContext())
                            .inflate(R.layout.trip_item, group, false);

                    return new TripsViewHolder(view);
                }

                @Override
                public void onBindViewHolder(final TripsViewHolder holder, final int position, Trip model) {
                    holder.setTrip(model.getName(), model.getDestination(), model.getPrice(),
                            model.getRating(), model.getImage(), model.getFavStatus());

                    // Set Favourite button listener
                    holder.setButtonFavourite(model);

                    // Create and set trip onClickListener
                    View.OnClickListener tripClickListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // On trip click, start the TripDetailsActivity
                            Trip trip = (Trip) mAdapter.getItem(position);
                            Intent intent = new Intent(getActivity(), TripDetailsActivity.class);
                            intent.putExtra("trip", trip);
                            startActivity(intent);
                        }
                    };

                    holder.itemView.setOnClickListener(tripClickListener);
                    holder.mTextViewName.setOnClickListener(tripClickListener);
                    holder.mTextViewDestination.setOnClickListener(tripClickListener);
                    holder.mTextViewPrice.setOnClickListener(tripClickListener);
                    holder.mTextViewRating.setOnClickListener(tripClickListener);
                    holder.mImageViewLocation.setOnClickListener(tripClickListener);
                }

                // If no trip added in lists, suggest user how to add trips using empty layouts
                @Override
                public void onDataChanged() {
                    if (getItemCount() == 0) {
                        recyclerView.setVisibility(View.GONE);
                        // Display correct hint based on list type: all or favourites
                        if (MainActivity.sIsFavListDisplayed) {
                            mEmptyFav.setVisibility(View.VISIBLE);
                        } else {
                            mEmptyList.setVisibility(View.VISIBLE);
                        }
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        if (MainActivity.sIsFavListDisplayed) {
                            mEmptyFav.setVisibility(View.GONE);
                        } else {
                            mEmptyList.setVisibility(View.GONE);
                        }
                    }
                }


            };

            recyclerView.setAdapter(mAdapter);
        }
    }

    public class TripsViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        private TextView mTextViewName;
        private TextView mTextViewDestination;
        private TextView mTextViewPrice;
        private TextView mTextViewRating;
        private ImageView mImageViewLocation;
        private ToggleButton mButtonFavourite;

        public TripsViewHolder(@NonNull View itemView) {
            super(itemView);

            // Obtain fields for binding
            mTextViewName = itemView.findViewById(R.id.textview_name);
            mTextViewDestination = itemView.findViewById(R.id.textview_destination);
            mTextViewPrice = itemView.findViewById(R.id.textview_price);
            mTextViewRating = itemView.findViewById(R.id.textview_rating);
            mImageViewLocation = itemView.findViewById(R.id.imageview_location);
            mButtonFavourite = itemView.findViewById(R.id.button_favorite);

            // Set onLongClickListeners to display OnContextMenu
            itemView.setOnLongClickListener(this);
            mTextViewName.setOnLongClickListener(this);
            mTextViewDestination.setOnLongClickListener(this);
            mTextViewPrice.setOnLongClickListener(this);
            mTextViewRating.setOnLongClickListener(this);
            mImageViewLocation.setOnLongClickListener(this);
            mButtonFavourite.setOnLongClickListener(this);
        }

        /**
         * Set trip items properties in RecyclerView list
         */
        public void setTrip(String tripName, String tripDestination, double tripPrice,
                            double tripRating, String tripImageUri, Boolean tripIsFavourite) {
            mTextViewName.setText(tripName);
            mTextViewDestination.setText(tripDestination);
            int price = (int) tripPrice;
            mTextViewPrice.setText(Integer.toString(price));
            mTextViewRating.setText(Double.toString(tripRating));

            if (tripImageUri != null) {
                Uri uri = Uri.parse(tripImageUri);
                Picasso.get().load(uri).fit().centerCrop()
                        .placeholder(R.drawable.placeholder).into(mImageViewLocation);
            } else {
                Picasso.get().load(R.drawable.placeholder).into(mImageViewLocation);
            }

            if (tripIsFavourite) {
                mButtonFavourite.setChecked(true);
            } else {
                mButtonFavourite.setChecked(false);
            }
        }

        /**
         * Set favourite button actions
         * Used to add or remove trip from the Favourite trips list
         */
        public void setButtonFavourite(final Trip currentTrip) {
            this.mButtonFavourite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DocumentSnapshot snapshot = (DocumentSnapshot) mAdapter.getSnapshots()
                            .getSnapshot(getAdapterPosition());
                    String tripId = snapshot.getId();

                    if (currentTrip.getFavStatus() == false) {
                        // Add current trip to Favourite list
                        // Create modified trip
                        Trip newTrip = new Trip(currentTrip.getName(), tripId,
                                currentTrip.getDestination(), currentTrip.getType(),
                                currentTrip.getPrice(), currentTrip.getStartDate(),
                                currentTrip.getEndDate(), currentTrip.getRating(),
                                currentTrip.getImage(), true);

                        // Add it to Favourite list
                        mFavTripListRef.document(tripId).set(newTrip);

                        // Modify its favourite status in the main list
                        mAllTripListRef.document(tripId).set(newTrip);
                        mButtonFavourite.setChecked(true);

                    } else {
                        // Remove current trip from Favourite list
                        // Create modified trip
                        Trip newTrip = new Trip(currentTrip.getName(), tripId,
                                currentTrip.getDestination(), currentTrip.getType(),
                                currentTrip.getPrice(), currentTrip.getStartDate(),
                                currentTrip.getEndDate(), currentTrip.getRating(),
                                currentTrip.getImage(), false);

                        // Remove trip from Favourite list
                        mFavTripListRef.document(tripId).delete();

                        // Modify its favourite status in the main list
                        mAllTripListRef.document(tripId).set(newTrip);
                        mButtonFavourite.setChecked(false);
                    }
                }
            });
        }

        /**
         * OnLongClickListener used to display ContextMenu at trip long press in RecyclerView
         */
        @Override
        public boolean onLongClick(View v) {
            itemView.showContextMenu();
            return true;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set adapter to start listening for database changes
        if (mQuery != null) {
            mAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // Set adapter to stop listening for database changes
        if (mAdapter != null) {
            mAdapter.stopListening();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Inflate Menu from xml resource
        MenuInflater menuInflater = getActivity().getMenuInflater();
        menuInflater.inflate(R.menu.context_menu_recycler, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        // Obtain the RecyclerView position of the longClicked item
        ContextMenuRecyclerView.RecyclerViewContextMenuInfo menuInfo =
                (ContextMenuRecyclerView.RecyclerViewContextMenuInfo) item.getMenuInfo();
        int position = menuInfo.position;

        DocumentSnapshot snapshot = (DocumentSnapshot) mAdapter.getSnapshots().getSnapshot(position);
        final String tripId = snapshot.getId();

        // Obtain the trip corresponding to this position
        final Trip trip = (Trip) mAdapter.getItem(position);

        switch (item.getItemId()) {
            case R.id.edit_option:
                // If edit option selected, start ManageTripActivity
                Intent intent = new Intent(getActivity(), ManageTripActivity.class);
                intent.putExtra("trip", trip);
                intent.putExtra("tripId", tripId);
                intent.putExtra("userEmail", mUserEmail);
                startActivity(intent);
                break;

            case R.id.delete_option:
                // If delete option selected, show alertDialog to confirm selection
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                builder.setTitle(getResources().getString(R.string.delete_trip_confirmation))
                        .setPositiveButton(getResources().getString(R.string.delete_confirmation),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User clicked OK button

                                        // Delete trip image from Storage, if exists
                                        String imageUri = trip.getImage();
                                        if (imageUri != null) {
                                            StorageReference imageRef = mStorage.getReferenceFromUrl(trip.getImage());
                                            imageRef.delete();
                                        }

                                        // Used to confirm trip deletion from database collections
                                        final boolean[] deletedTrip = {true, true};

                                        // Delete from allTripsList
                                        mAllTripListRef.document(tripId).delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        deletedTrip[0] = true;
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        deletedTrip[0] = false;
                                                    }
                                                });

                                        // If favourite trip, also delete from favTripsList
                                        if (trip.getFavStatus()) {
                                            mFavTripListRef.document(tripId).delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            deletedTrip[1] = true;
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            deletedTrip[1] = false;
                                                        }
                                                    });
                                        }

                                        // Show Snackbar with deletion result
                                        if (deletedTrip[0] && deletedTrip[1]) {
                                            Snackbar.make(((MainActivity) getActivity()).getFab(),
                                                    getResources().getString(R.string.trip_deleted),
                                                    Snackbar.LENGTH_SHORT).show();
                                        } else {
                                            Snackbar.make(((MainActivity) getActivity()).getFab(),
                                                    getResources().getString(R.string.error_deleting_trip),
                                                    Snackbar.LENGTH_SHORT).show();
                                        }
                                    }
                                })
                        .setNegativeButton(getResources().getString(R.string.abort_delete),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User cancelled the dialog
                                    }
                                });
                builder.create().show();
                break;
        }

        return super.onContextItemSelected(item);
    }
}