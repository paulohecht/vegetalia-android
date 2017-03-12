package br.com.vegetalia.app;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class DataSnapshotObserver {

    DatabaseReference reference;
    ValueEventListener listener;

    public static br.com.vegetalia.app.DataSnapshotObserver create(DatabaseReference reference, OnDataSnapshotReceivedListener callback) {
        return new br.com.vegetalia.app.DataSnapshotObserver(reference, callback);
    }

    private DataSnapshotObserver(DatabaseReference reference, final OnDataSnapshotReceivedListener callback) {
        this.reference = reference;
        this.listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (callback != null) callback.onDataSnapshotReceived(dataSnapshot);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };;
        reference.addValueEventListener(listener);
    }

    public void cancel() {
        reference.removeEventListener(listener);
        reference = null;
        listener = null;
    }

    public interface OnDataSnapshotReceivedListener {
        public void onDataSnapshotReceived(DataSnapshot dataSnapshot);
    }
}
