package br.com.vegetalia.app;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.FirebaseDatabase;

public class App extends Application {

    public static final String LOG_TAG = "VEG-DEBUG";

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        MobileAds.initialize(getApplicationContext(), getString(R.string.admob_app_id));

        FacebookSdk.sdkInitialize(getApplicationContext());

    }
}
