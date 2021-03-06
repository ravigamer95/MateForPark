package com.app.mateforpark.UserMainActivities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.app.mateforpark.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class Forget_Password_Activity extends AppCompatActivity {

    FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthStateListener;
    EditText mUserEmail;
    Button mSend;
    TextView  mBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        mUserEmail = findViewById(R.id.useremail);
        mSend = findViewById(R.id.send);
        mBack = findViewById(R.id.back);

        // FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        mAuth = FirebaseAuth.getInstance();

        mBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Forget_Password_Activity.this, Login_Activity.class);
                startActivity(intent);
                return;
            }
        });

        mSend.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String email = mUserEmail.getText().toString();

                        if (TextUtils.isEmpty(email)) {
                            Toast.makeText(getApplication(), "Enter your registered email id", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(Forget_Password_Activity.this, "We have sent instructions to your email!", Toast.LENGTH_SHORT).show();
                                    mUserEmail.getText().clear();
                                } else {
                                    Toast.makeText(Forget_Password_Activity.this, "Cannot find Email. Please register or try again later.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });


    }
}
