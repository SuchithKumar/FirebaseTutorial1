package com.ftpdevelopers.firebasetutorial1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.measurement.module.Analytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
        private static final String TAG = "MainActivity";

        public static final String ANONYMOUS = "anonymous";
        public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

        private ListView mMessageListView;
        private MessageAdapter mMessageAdapter;
        private ProgressBar mProgressBar;
        private ImageButton mPhotoPickerButton;
        private EditText mMessageEditText;
        private Button mSendButton;

        private String mUsername;

        private FirebaseDatabase firebaseDatabase;
        private DatabaseReference mdatabaseReference;
        private ChildEventListener childEventListener;

        private FirebaseAuth firebaseAuth;
        private FirebaseAuth.AuthStateListener firebaseAuthStateListener;

        private final Integer RC_SIGN_IN = 1;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            firebaseDatabase = FirebaseDatabase.getInstance();
            mdatabaseReference = firebaseDatabase.getReference().child("messages");

            firebaseAuth = FirebaseAuth.getInstance();

            mUsername = ANONYMOUS;

            // Initialize references to views
            mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
            mMessageListView = (ListView) findViewById(R.id.messageListView);
            mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
            mMessageEditText = (EditText) findViewById(R.id.messageEditText);
            mSendButton = (Button) findViewById(R.id.sendButton);

            // Initialize message ListView and its adapter
            List<FriendlyMessage> friendlyMessages = new ArrayList<>();
            mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
            mMessageListView.setAdapter(mMessageAdapter);

            // Initialize progress bar
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);

            // ImagePickerButton shows an image picker to upload a image for a message
            mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: Fire an intent to show an image picker
                }
            });



            // Enable Send button when there's text to send
            mMessageEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (charSequence.toString().trim().length() > 0) {
                        mSendButton.setEnabled(true);
                    } else {
                        mSendButton.setEnabled(false);
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });
            mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

            // Send button sends a message and clears the EditText
            mSendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: Send messages on click
                    FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(),mUsername,null);
                    mdatabaseReference.push().setValue(friendlyMessage);
                    // Clear input box
                    mMessageEditText.setText("");
                }
            });

            firebaseAuthStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if(firebaseUser != null){
//                        Toast.makeText(MainActivity.this,"Welcome , You have been authenticated :)",Toast.LENGTH_SHORT).show();
                          onSignedInInitialize(firebaseUser.getDisplayName());

                    }else{
                        onSignedOutCleanup();
                        startActivityForResult(
                                AuthUI.getInstance()
                                        .createSignInIntentBuilder()
                                        .setIsSmartLockEnabled(false)
                                        .setAvailableProviders(Arrays.asList(
                                                new AuthUI.IdpConfig.GoogleBuilder().build(),
                                                new AuthUI.IdpConfig.EmailBuilder().build()
                                                ))
                                        .build(),
                                RC_SIGN_IN);


                    }

                }

            };

        };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(requestCode == RC_SIGN_IN){
            if(resultCode == RESULT_OK){
                Toast.makeText(MainActivity.this,"User Successfully Authenticated!",Toast.LENGTH_SHORT).show();
            }else if(resultCode == RESULT_CANCELED){
                finish();
            }
        }
    }

    private void onSignedInInitialize(String displayName) {
        mUsername = displayName;
        attachDatabaseReadListener();


    }

    private void onSignedOutCleanup() {
            mUsername = ANONYMOUS;
            mMessageAdapter.clear();
            detachDatabaseReadListener();


    }

    private void attachDatabaseReadListener() {
        if (childEventListener == null) {
            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
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
            };
            mdatabaseReference.addChildEventListener(childEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (childEventListener != null) {
            mdatabaseReference.removeEventListener(childEventListener);
            childEventListener = null;
        }
    }


    @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_menu, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()){
                case R.id.sign_out_menu : {
                    AuthUI.getInstance().signOut(MainActivity.this);
                    return true;
                }

                default: {
                    return super.onOptionsItemSelected(item);
                }
            }

        }

    @Override
    protected void onPause() {
        super.onPause();
        firebaseAuth.removeAuthStateListener(firebaseAuthStateListener);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(firebaseAuthStateListener != null) {
            firebaseAuth.addAuthStateListener(firebaseAuthStateListener);
        }
        detachDatabaseReadListener();
        mMessageAdapter.clear();
    }
}
