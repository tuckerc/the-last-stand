package com.rachnicrice.the_last_stand;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public final String TAG = "rnr";
    private static final int PERMISSIONS_REQUEST = 100;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build());

        // Create and launch sign-in intent
        if(FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("message");
            myRef.setValue("Hello, World!");
        }else{
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    202);
        };


        //Practice setting up a firebase database and adding info to it!
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user.getUid();

        // grab all teams from db
        DatabaseReference teamRef = database.getReference("teams");

        teamRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // boolean for tracking if user on a team
                boolean isOnTeam = false;
                // knights team size counter
                int knightsSize = 0;
                // dragons team size counter
                int dragonsSize = 0;

                // get the teams
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                // loop through the teams
                for(DataSnapshot team : children) {
                    for(DataSnapshot user : team.getChildren()) {
                        // increment team size count
                        if(team.getKey().equals("knights")) {
                            knightsSize++;
                        } else dragonsSize++;

                        // check to see if user on team
                        if(uid.equals(user.getKey())) {
                            isOnTeam = true;
                        }
                        Log.d(TAG, user.getKey());
                    }
                }

                // if isOnTeam is false, add user to smallest team
                if(!isOnTeam) {
                    if(knightsSize > dragonsSize) {
                        // add user to team dragons
                        final String dragonsPath = "teams/dragons/" + uid;
                        DatabaseReference dragonsRef = FirebaseDatabase.getInstance().getReference(dragonsPath);
                        dragonsRef.setValue(true);
                    } else {
                        // add user to team knights
                        final String knightsPath = "teams/knights/" + uid;
                        DatabaseReference knightsRef = FirebaseDatabase.getInstance().getReference(knightsPath);
                        knightsRef.setValue(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // identify team with least users

        // if user not assigned to a team, assign to team with least users

//        myRef.setValue("Hello, World!");

//        myRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                // This method is called once with the initial value and again
//                // whenever data at this location is updated.
//                String value = dataSnapshot.getValue(String.class);
//                Log.d(TAG, "Value is: " + value);
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                // Failed to read value
//                Log.w(TAG, "Failed to read value.", error.toException());
//            }
//        });

        //Grab the log out button
        Button logout = findViewById(R.id.logout);

        //When the log out button is pressed, log the user out
        logout.setOnClickListener((v) -> {
            AuthUI.getInstance()
                    .signOut(this);
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    202);
        });

//Referenced from https://www.androidauthority.com/create-a-gps-tracking-application-with-firebase-realtime-databse-844343/
        //Check whether GPS tracking is enabled//
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            finish();
        }

        //Check whether this app has access to the location permission//
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        //If the location permission has been granted, then start the TrackerService//
        if (permission == PackageManager.PERMISSION_GRANTED) {
            startTrackerService();
        } else {

        //If the app doesn’t currently have access to the user’s location, then request access//
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {

        //If the permission has been granted...//
        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        //...then start the GPS tracking service//
            startTrackerService();
        } else {

        //If the user denies the permission request, then display a toast with some more information//
            Toast.makeText(this, "Please enable location services to allow GPS tracking", Toast.LENGTH_SHORT).show();
        }
    }

    //Start the TrackerService//
    private void startTrackerService() {
        startService(new Intent(this, TrackingService.class));

        //Notify the user that tracking has been enabled//
        Toast.makeText(this, "GPS tracking enabled", Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "request code on auth: " + requestCode);

        if (requestCode == 202) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in

            } else {
                Log.i(TAG, "Sign in failed!");
            }
        }
    }


}
