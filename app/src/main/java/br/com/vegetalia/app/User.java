package br.com.vegetalia.app;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class User {

    public static DataSnapshotObserver fetchProfile (final String userId, final DataSnapshotObserver.OnDataSnapshotReceivedListener callback) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users").child(userId);
        return DataSnapshotObserver.create(reference, callback);
    }

    public static void updateFCMToken() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("tokens/" + userId);
        db.setValue(FirebaseInstanceId.getInstance().getToken());
    }

}


