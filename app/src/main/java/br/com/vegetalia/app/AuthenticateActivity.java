package br.com.vegetalia.app;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.internal.ImageRequest;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import br.com.vegetalia.app.utils.DialogUtils;

public class AuthenticateActivity extends AppCompatActivity {

    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticate);

        setupLoginFacebook();
    }

    private void setupLoginFacebook() {
        callbackManager = CallbackManager.Factory.create();
        findViewById(R.id.login_facebook).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginManager.getInstance().logInWithReadPermissions(AuthenticateActivity.this, Arrays.asList("email", "public_profile"));
            }
        });
        LoginManager.getInstance().registerCallback(callbackManager,
            new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    Log.d(App.LOG_TAG, "onSuccess");
                    handleFacebookAccessToken(loginResult.getAccessToken());
                }

                @Override
                public void onCancel() {
                    Log.d(App.LOG_TAG, "cancel");
                }

                @Override
                public void onError(FacebookException exception) {
                    Log.d(App.LOG_TAG, "error");
                }
            }
        );
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(App.LOG_TAG, "handleFacebookAccessToken:" + token);
        final AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        FirebaseAuth.getInstance().getCurrentUser().linkWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                Log.d(App.LOG_TAG, "linkWithCredential:onComplete:" + task.isSuccessful());
                if (!task.isSuccessful()) {
                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        suggestLoginWhenUserCollides(credential);
                    }
                    else {
                        Log.w(App.LOG_TAG, "linkWithCredential", task.getException());
                        DialogUtils.alert(AuthenticateActivity.this, "Ocorreu um erro com a autenticação.");
                    }
                }
                else {
                    completeLogin();
                }
            }
        });
    }

    private void suggestLoginWhenUserCollides(final AuthCredential credential) {
        DialogUtils.confirm(AuthenticateActivity.this, "Essa conta já existe, deseja autenticar com ela? O histórico de curtidas anônimas será perdido.", new DialogUtils.OnConfirm() {
            @Override
            public void onConfirm() {
                FirebaseAuth.getInstance().signOut();
                FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener(AuthenticateActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        Log.d(App.LOG_TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                                        if (!task.isSuccessful()) {
                                            Log.w(App.LOG_TAG, "signInWithCredential", task.getException());
                                            DialogUtils.alert(AuthenticateActivity.this, "Ocorreu um erro com a autenticação.");
                                        }
                                        else {
                                            completeLogin();
                                        }
                                    }
                                }
                        );
            }
        });
    }

    private void completeLogin() {

        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject me, GraphResponse response) {
                        if (AccessToken.getCurrentAccessToken() != null) {
                            if (me != null) {

                                final String name = me.optString("name");
                                final String image = ImageRequest.getProfilePictureUri(me.optString("id"), 500, 500).toString();

                                final DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                                UserProfileChangeRequest profileChange = new UserProfileChangeRequest
                                        .Builder()
                                        .setDisplayName(name)
                                        .setPhotoUri(Uri.parse(image))
                                        .build();
                                user.updateProfile(profileChange);

                                User.updateFCMToken();

                                Map userData = new HashMap();
                                userData.put("name", name);
                                userData.put("image", image);
                                Log.d(App.LOG_TAG, userData.toString());
                                Map updateValues = new HashMap();
                                updateValues.put("users/" + user.getUid(), userData);
                                Log.d(App.LOG_TAG, updateValues.toString());

                                db.updateChildren(updateValues, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            DialogUtils.alert(AuthenticateActivity.this, "Ocorreu um erro ao salvar o perfil.");
                                            return;
                                        }
                                        Intent intent = new Intent(AuthenticateActivity.this, SplashActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    }
                                });
                            }
                        }
                    }
                });
        GraphRequest.executeBatchAsync(request);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void showProgress() {
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        findViewById(R.id.progress).setVisibility(View.GONE);
    }

}
