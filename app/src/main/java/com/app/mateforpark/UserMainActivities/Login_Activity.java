package com.app.mateforpark.UserMainActivities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.app.mateforpark.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuth.AuthStateListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class Login_Activity extends AppCompatActivity {
    private int RC_SIGN_IN = 1;

    //To use google signin button
    private SignInButton mGoogleLogin;

    final String gender = "Do not specify";
    final String country = "";
    final String status = "";
    final String bio = "";
    final String countrycode = "";

    private Button mLogin;
    private EditText mEmail,mPassword;
    TextView mRegister, mForgetPassword, mTitle, mSubtitle;

    private DatabaseReference usersDb;
    //A client for interacting with the Google Sign In API.
    private GoogleSignInClient mGoogleSignInClient;

    //Firebase Variables
    private FirebaseAuth mAuth;
    private String currentUserID;

    //AuthStateListener is called when there is a change in the authentication state
    private FirebaseAuth.AuthStateListener firebaseAuthStateListener;

    //Database for Images
    private FirebaseStorage storage;
    private StorageReference storageReference;

    RelativeLayout rellay1, rellay2;
    Handler handler = new Handler();

    Runnable runnable= new Runnable() {
        @Override
        public void run() {
            rellay1.setVisibility(View.VISIBLE);
            rellay2.setVisibility(View.VISIBLE);
            mTitle.setVisibility(View.GONE);
            mSubtitle.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Assigning the xml ids to the variables
        mLogin = findViewById(R.id.login);
        mGoogleLogin = findViewById(R.id.googlelogin);

        setGoogleButtonText(mGoogleLogin,"Sign in");
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mRegister = findViewById(R.id.register);
        mForgetPassword = findViewById(R.id.forgetpassword);

        //Connect to Firebase. Get current session
        mAuth = FirebaseAuth.getInstance();

        // get the Firebase  storage reference
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        //for Splash screen
        rellay1 = findViewById(R.id.rellay1);
        rellay2 = findViewById(R.id.rellay2);

        //Title and subtitle
        mTitle = findViewById(R.id.maintitle);
        mSubtitle = findViewById(R.id.subtitle);

        handler.postDelayed(runnable, 2500);


        //OnAuthStateChanged gets invoked in the UI thread on changes in the authentication state
        firebaseAuthStateListener = new AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                /*
                //Checks for flag from registration
                final Bundle bundle = getIntent().getExtras();
                String flag;

                if(bundle != null){
                    flag = bundle.getString("flag");
                }
                else{
                    flag = "true";
                }

                if(user != null && flag == "true"){
                   userDashboard();
                }

                */

                if(user != null) {
                    Intent intent = new Intent(Login_Activity.this, Main_Activity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        mRegister.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Login_Activity.this, Register_Activity.class);
                startActivity(intent);

                finish();
            }
        });

        mForgetPassword.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Login_Activity.this, Forget_Password_Activity.class);
                startActivity(intent);
            }
        });

        mLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.addAuthStateListener(firebaseAuthStateListener);
                final FirebaseUser currentUser = mAuth.getCurrentUser();

                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();

                //Log.d("Login Session:","Successful");

                if (TextUtils.isEmpty(email)){
                    mEmail.setError("Please enter your Email ID");
                }
                else if(TextUtils.isEmpty(password)){
                    mPassword.setError("Please enter your Password");
                }
                else
                {
                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(Login_Activity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()){
                                Toast.makeText(Login_Activity.this, "Sign in Failed. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                Toast.makeText(Login_Activity.this, "Successfully signed in...", Toast.LENGTH_SHORT).show();
                                userDashboard();
                            }
                        }
                    });

                }

            }
        });

        mGoogleLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setupGoogleLogin();
                googleSignIn();
            }
        });
    }

    private void userDashboard() {
        Intent intent = new Intent(Login_Activity.this, Main_Activity.class);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthStateListener);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);

    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthStateListener);
        finish();
    }

    public void setupGoogleLogin(){

        //GoogleSignInOptions is options used to configure the GOOGLE_SIGN_IN_API.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

    }

    private void googleSignIn() {

        Intent googleIntent = mGoogleSignInClient.getSignInIntent();

        //we will handle the result code obtained from launching that intent on Override onActivity result.
        startActivityForResult(googleIntent,RC_SIGN_IN);

    }

    //set text inside google button
    protected void setGoogleButtonText(SignInButton signInButton, String buttonText){
        for (int i=0; i < signInButton.getChildCount(); i++){
            View v = signInButton.getChildAt(i);

            if(v instanceof TextView){
                TextView tv = (TextView) v;
                tv.setText(buttonText);
                return;
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN)
        {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }

    }

    private void handleSignInResult(Task<GoogleSignInAccount> completeTask) {

        try{
            final GoogleSignInAccount account = completeTask.getResult(ApiException.class);

            FirebaseGoogleAuth(account);

            Toast.makeText(Login_Activity.this, "Signed in Successfully...", Toast.LENGTH_SHORT).show();


            /*mAuth.fetchSignInMethodsForEmail(account.getEmail()).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                @Override
                public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {

                    boolean check = !task.getResult().getSignInMethods().isEmpty();

                    //Check for first time gmail login
                    if(!check)
                    {
                        Toast.makeText(LoginActivity.this, "Signed in Successfully...", Toast.LENGTH_SHORT).show();
                        FirebaseGoogleAuth(account);
                    }
                    //Check for multiple times gmail login

                    else
                    {
                        Toast.makeText(LoginActivity.this, "Email already exists! Please login or click forget password to reset.", Toast.LENGTH_SHORT).show();
                        mGoogleSignInClient.signOut();
                    }
                }
            });*/
        }
        catch (ApiException e)
        {
            Toast.makeText(Login_Activity.this, "Sign in Failed. Please try again.", Toast.LENGTH_SHORT).show();
            FirebaseGoogleAuth(null);
        }

    }

    private void FirebaseGoogleAuth(GoogleSignInAccount acct) {

        if(acct == null)
        {
            return;
        }
        else
        {
            AuthCredential authCredential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

            mAuth.signInWithCredential(authCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>(){

                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        //Toast.makeText(LoginActivity.this, "Successful", Toast.LENGTH_SHORT).show();
                        Log.d("Firebase:","Connection successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    }
                    else
                    {
                        // Toast.makeText(LoginActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                        Log.d("Firebase:","Connection Failed");
                        updateUI(null);
                    }
                }
            });
        }


    }


    private void updateUI(FirebaseUser fUser) {

        //Fix session handlings
        if (fUser == null) {
            return;
        } else {
            final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
            if (account != null) {

                final String personEmail = account.getEmail();
                final String personName = account.getDisplayName();
                final Uri personPhoto = account.getPhotoUrl();
                final String userId = mAuth.getCurrentUser().getUid();

                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

                rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild("Users/" + userId)) {
                            //Toast.makeText(LoginActivity.this, "Email already exists! Please login or click forget password to reset.", Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            //create Database
                            final DatabaseReference currentUserDb = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);

                            //To store the user information easily we use hashmaps
                            final Map userInfo = new HashMap<>();

                            userInfo.put("name", personName);
                            userInfo.put("email", personEmail);
                            userInfo.put("photo", personPhoto.toString());
                            userInfo.put("age", "");
                            userInfo.put("gender", gender);
                            userInfo.put("bio", bio);
                            userInfo.put("status", status);
                            userInfo.put("country", country);
                            userInfo.put("countrycode", countrycode);

                            currentUserDb.updateChildren(userInfo);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


            }
        }
    }

}
