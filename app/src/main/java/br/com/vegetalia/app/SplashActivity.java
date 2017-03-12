package br.com.vegetalia.app;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

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
                        final DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        final String userUri = "users/" + userId;
                        db.child(userUri).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (!dataSnapshot.exists()) {

                                    HashMap users = new HashMap();
                                    users.put("name", null);
                                    users.put("image", null);
                                    users.put("favorites_count", 0);
                                    users.put("likes_count", 0);
                                    users.put("posts_count", 0);
                                    users.put("followers_count", 0);
                                    users.put("followings_count", 0);

                                    HashMap updates = new HashMap();
                                    updates.put(userUri, users);
                                    updates.put("tokens/" + userId, FirebaseInstanceId.getInstance().getToken());

                                    db.updateChildren(updates);
                                }
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                finish();

                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                            }
                        });
                    }
                });

    }
}
