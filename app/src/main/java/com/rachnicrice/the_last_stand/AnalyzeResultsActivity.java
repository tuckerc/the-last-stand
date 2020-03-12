package com.rachnicrice.the_last_stand;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.Objects;

public class AnalyzeResultsActivity extends AppCompatActivity {

    final String TAG = "rnr.Analyze";
    FirebaseDatabase database;
    Intent intent;
    SharedPreferences p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze_results);

        // set intent to go to ResultActivity and default to me winning
        intent = new Intent(getApplicationContext(),
                ResultActivity.class);
        intent.putExtra("eliminated", false);

        database = FirebaseDatabase.getInstance();

        p = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String myTeam = p.getString("my_team", "");
        String enemyTeam = p.getString("enemy_team", "");

        Intent i = getIntent();
        long myTime = i.getLongExtra("time", -1);
        Log.i(TAG, "my time: " + myTime);
        String myID = i.getStringExtra("my_id");
        String enemyID = i.getStringExtra("enemy_id");

        // get the times from bother users
        DatabaseReference users = database.getReference("users");
        users.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                handleUserEvent(dataSnapshot, TAG, myTeam, enemyTeam, myTime, myID, enemyID);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                handleUserEvent(dataSnapshot, TAG, myTeam, enemyTeam, myTime, myID, enemyID);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Handler handler = new Handler();
        Runnable r = () -> startActivity(intent);
        handler.postDelayed(r, 10*1000);
    }

    private void handleUserEvent (DataSnapshot dataSnapshot,
                                  String TAG,
                                  String myTeam,
                                  String enemyTeam,
                                  long myTime,
                                  String myID,
                                  String enemyID) {

        if(Objects.equals(dataSnapshot.getKey(), enemyID)) {
            if(dataSnapshot.getValue(Long.class) != null) {
                long enemyTime = dataSnapshot.getValue(Long.class);
                Log.i(TAG, "enemy time: " + enemyTime);
                Date myDate = new Date(myTime);
                Date enemyDate = new Date(enemyTime);
                // Intent to trigger on result
                // make sure enemy time is within the same minute
                if(Math.abs(myTime - enemyTime) < 60*1000) {

                    Log.i(TAG, "both players clicked in the last 60 seconds");

                    // determine who won
                    int timeDiff = myDate.compareTo(enemyDate);
                    if(timeDiff < 0) {
                        // I won!
                        Log.i(TAG, "I won!");
                        Log.i(TAG, myID + " defeats " + enemyID);
                        // move to results page with me as the winner
                        intent.putExtra("eliminated", false);
                        // change my team value back to true
                        DatabaseReference myTeamRef = database.getReference("teams/" + myTeam + "/" + myID);
                        myTeamRef.setValue(true);
                    }
                    else if(timeDiff > 0) {
                        // the enemy won :-(
                        Log.i(TAG, "The enemy won");
                        Log.i(TAG, enemyID + " defeats " + myID);
                        // move to results page with me as the winner
                        intent.putExtra("eliminated", true);
                        // change the enemy team value to true
                        DatabaseReference myTeamRef = database.getReference("teams/" + enemyTeam + "/" + enemyID);
                        myTeamRef.setValue(true);
                        Intent trackingService = new Intent(getApplicationContext(), TrackingService.class);
                        stopService(trackingService);
                    } else {
                        // there was a tie
                        Log.i(TAG, "There was a tie between " + myID +
                                " and " + enemyID);
                        // flip a coin
                        Double result = Math.random();
                        // if result less than 0.5, I am the winner
                        if(result < 0.5) {
                            Log.i(TAG, "I won by a coin toss");
                            Log.i(TAG, myID + " defeats " + enemyID);
                            // move to results page with me as the winner
                            intent.putExtra("eliminated", false);
                            // change my team value back to true
                            DatabaseReference myTeamRef = database.getReference("teams/" + myTeam + "/" + myID);
                            myTeamRef.setValue(true);
                        }
                        // if result greater than or equal to 0.5, I lose
                        else {
                            Log.i(TAG, "The enemy won by a coin toss");
                            Log.i(TAG, enemyID + " defeats " + myID);
                            // move to results page with me as the winner
                            intent.putExtra("eliminated", true);
                            // change the enemy team value to true
                            DatabaseReference myTeamRef = database.getReference("teams/" + enemyTeam + "/" + enemyID);
                            myTeamRef.setValue(true);
                        }
                    }
                } else {
                    // I must be the winner
                    Log.i(TAG, "I won by default!");
                    Log.i(TAG, myID + " defeats " + enemyID);
                    // move to results page with me as the winner
                    intent.putExtra("eliminated", false);
                    // update my team value to true
                    DatabaseReference myTeamRef = database.getReference("teams/" + myTeam + "/" + myID);
                    myTeamRef.setValue(true);
                }
            }
        }
    }
}
