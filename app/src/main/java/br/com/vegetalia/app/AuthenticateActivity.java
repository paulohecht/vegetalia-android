package br.com.vegetalia.app;

import android.content.Intent;
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
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import java.util.Arrays;

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
                        DialogUtils.confirm(AuthenticateActivity.this, "Essa conta já existe, deseja autenticar com ela? O histórico de curtidas anônimas será perdido.", new DialogUtils.OnConfirm() {
                            @Override
                            public void onConfirm() {
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

    private void completeLogin() {
        finish();
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
