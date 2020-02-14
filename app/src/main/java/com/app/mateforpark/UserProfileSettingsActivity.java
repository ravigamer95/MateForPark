package com.app.mateforpark;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.storage.UploadTask.TaskSnapshot;
import com.hbb20.CountryCodePicker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UserProfileSettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;

    private EditText mUserName, mUserAge, mProfileImageUrl, mUserGender, mUserCountry, mUserEmail;
    private Button mSaveChanges;
    private TextView mBack;

    private Uri resultUri;
    private ImageView mProfileImage;

    private String usersId, usersName, usersAge, usersImageUrl, usersGender, usersCountry, usersEmail;
    CountryCodePicker mCountryCodePicker;

    private Spinner mySpinner;
    private String emptyField = "";

    private final int flag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_settings);

        mUserName = findViewById(R.id.username);
        mUserCountry = findViewById(R.id.country);
        mUserEmail = findViewById(R.id.useremail);
        mUserAge = findViewById(R.id.userage);
        mUserGender = findViewById(R.id.gender);
        mProfileImage = findViewById(R.id.profileimage);
        mCountryCodePicker = findViewById(R.id.countrypicker);
        mSaveChanges = findViewById(R.id.savechanges);

        mBack = findViewById(R.id.back);

        mySpinner = findViewById(R.id.spinner);

        //create data to show inside spinner
        final ArrayAdapter<String> myAdapter = new ArrayAdapter<>(UserProfileSettingsActivity.this,
                R.layout.spinner_item, getResources().getStringArray(R.array.gender));

        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //allow the adapter to show the data inside the spinner.
        mySpinner.setAdapter(myAdapter);

        mAuth = FirebaseAuth.getInstance();

        //gets current userid
        usersId = mAuth.getCurrentUser().getUid();

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(usersId);

        getUserInfo();

        //to select image from galary. Only enable this when Editprofile button is clicked
        mProfileImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                //tells the app to go outside the app and not from the app
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);

            }
        });

        mSaveChanges.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {


                saveUserInformation();

            }
        });

        mBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                return;
            }
        });


    }

    private void saveUserInformation() {

        usersName = mUserName.getText().toString();
        usersEmail = mUserEmail.getText().toString();
        usersCountry = mUserCountry.getText().toString();
        usersAge = mUserAge.getText().toString();
        usersGender = mUserGender.getText().toString();

        final String country = mCountryCodePicker.getSelectedCountryEnglishName();
        final String userGender = mySpinner.getSelectedItem().toString().trim();

        Map userInfo = new HashMap();
        userInfo.put("name", usersName);
        if (TextUtils.isEmpty(usersAge) || Integer.parseInt(usersAge) < 18 || (Integer.parseInt(usersAge) == 0)) {
            mUserAge.setError("Please enter valid age");
            Toast.makeText(this, "Please fill empty fields to complete your profile.", Toast.LENGTH_SHORT).show();
            return;
        } else {
            userInfo.put("age", usersAge);
        }

        userInfo.put("gender", userGender);
        userInfo.put("country", country);


        mUserDatabase.updateChildren(userInfo);

        if (resultUri != null) {
            StorageReference filepath = FirebaseStorage.getInstance().getReference().child("photo").child(usersId);
            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //tomake the image small size
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);

            byte[] data = baos.toByteArray();

            //uploading the image
            UploadTask uploadTask = filepath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                }
            });

            uploadTask.addOnSuccessListener(new OnSuccessListener<TaskSnapshot>() {
                @Override
                public void onSuccess(TaskSnapshot taskSnapshot) {
                    //Here we are ging to grab the url frm the profile image that was just saved. that image is allows us to get the image from internet
                    //taskSnapshot.getMetadata().getReference().getDownloadUrl().toString()
                    if (taskSnapshot.getMetadata() != null) {
                        if (taskSnapshot.getMetadata().getReference() != null) {
                            Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                            result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String imageUrl = uri.toString();

                                    Map userInfo = new HashMap();
                                    userInfo.put("photo", imageUrl);

                                    mUserDatabase.updateChildren(userInfo);
                                }
                            });

                        }
                    }

                    finish();
                    return;

                }
            });

            Toast.makeText(this, "Profile information updated successfully.", Toast.LENGTH_SHORT).show();
        } else {
            finish();
        }


    }

    private void getUserInfo() {

        final String userGender = mySpinner.getSelectedItem().toString().trim();

        //add a listener to check for current user infromation
        mUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {

                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    //get the child you want
                    if (map.get("name") != null) {
                        usersName = map.get("name").toString();
                        mUserName.setText(usersName);
                    }

                    if (map.get("age") != null) {
                        usersAge = map.get("age").toString();
                        mUserAge.setText(usersAge);
                    }


                    if (map.get("gender") != null) {
                        usersGender = map.get("gender").toString();


                        if (!usersGender.equals("Do not specify")) {
                            mUserGender.setEnabled(false);
                            mUserGender.setFocusable(false);
                            mySpinner.setVisibility(View.GONE);
                            mUserGender.setText(usersGender);

                        } else {
                            mUserGender.setVisibility(View.GONE);
                            mUserGender.setText(userGender);
                        }

                    }
                    if (map.get("email") != null) {
                        usersEmail = map.get("email").toString();
                        mUserEmail.setText(usersEmail);
                        mUserEmail.setEnabled(false);
                        mUserEmail.setFocusable(false);
                    }

                    if (map.get("country") != null) {
                        usersCountry = map.get("country").toString();
                        mUserCountry.setVisibility(View.GONE);
                    }

                    Glide.clear(mProfileImage);

                    if (map.get("photo") != null) {
                        usersImageUrl = map.get("photo").toString();

                        switch (usersImageUrl) {

                            case "default":
                                Glide.with(getApplication()).load(R.drawable.default_image).into(mProfileImage);
                                break;
                            default:
                                Glide.with(getApplication()).load(usersImageUrl).into(mProfileImage);
                                break;

                        }
                        //To load image into profile

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //if the intent from activity code is 1
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {

            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImage.setImageURI(resultUri);

        }
    }

}