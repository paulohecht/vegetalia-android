package br.com.vegetalia.app;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class User {

    public static DataSnapshotObserver fetchProfile (final String userId, final DataSnapshotObserver.OnDataSnapshotReceivedListener callback) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users").child(userId);
        return DataSnapshotObserver.create(reference, callback);
    }

}


