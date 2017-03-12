package br.com.vegetalia.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            if (!FirebaseAuth.getInstance().getCurrentUser().isAnonymous()) {
                Log.d(App.LOG_TAG, "Name: " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                Log.d(App.LOG_TAG, "FB Name: " + FirebaseAuth.getInstance().getCurrentUser().getProviderData().get(0).getDisplayName());
                Log.d(App.LOG_TAG, "Image: " + FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl());
                Log.d(App.LOG_TAG, "FB Image: " + FirebaseAuth.getInstance().getCurrentUser().getProviderData().get(0).getPhotoUrl());
            }
            continueToMainActivity();
            return;
        }

        FirebaseAuth.getInstance()
            .signInAnonymously()
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful() || FirebaseAuth.getInstance().getCurrentUser() == null) {
                        Toast.makeText(SplashActivity.this, R.string.splash_authentication_error, Toast.LENGTH_SHORT).show();
                        return;
                        //TODO: REDIRECT TO FAIL SCREEN...
                    }
                    continueToMainActivity();
                }
            });

    }

    private void continueToMainActivity() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }
}
