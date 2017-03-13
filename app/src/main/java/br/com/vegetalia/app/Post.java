package br.com.vegetalia.app;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

public class Post {

    public static DataSnapshotObserver fetchPost (String postId, DataSnapshotObserver.OnDataSnapshotReceivedListener callback) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("posts").child(postId);
        return DataSnapshotObserver.create(reference, callback);
    }

    public static DataSnapshotObserver fetchLiked (String postId, DataSnapshotObserver.OnDataSnapshotReceivedListener callback) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("post_likes").child(postId).child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        return DataSnapshotObserver.create(reference, callback);
    }



    public static void like (final String itemId, final boolean liked) {
        FirebaseDatabase.getInstance().getReference("posts").child(itemId).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                int likes_count = mutableData.hasChild("likes_count") ? mutableData.child("likes_count").getValue(Integer.class) : 0;
                final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference likeReference = FirebaseDatabase.getInstance().getReference("post_likes").child(itemId).child(userId);
                mutableData.child("likes_count").setValue(Math.max(likes_count + (liked ? 1 : -1), 0));
                likeReference.setValue(liked);
                return Transaction.success(mutableData);
            }
            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
            }
        });
    }

}
