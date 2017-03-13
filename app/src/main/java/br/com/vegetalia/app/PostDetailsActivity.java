package br.com.vegetalia.app;

import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.squareup.picasso.Picasso;

public class PostDetailsActivity extends AppCompatActivity {

    DataSnapshotObserver postObserver;
    DataSnapshotObserver authorObserver;
    DataSnapshotObserver likedObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final String postId = getIntent().getStringExtra("postId");

        likedObserver = Post.fetchLiked(postId, new DataSnapshotObserver.OnDataSnapshotReceivedListener() {
            @Override
            public void onDataSnapshotReceived(DataSnapshot dataSnapshot) {
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                if (dataSnapshot.exists() && dataSnapshot.getValue(Boolean.class)) {
                    fab.setImageResource(R.drawable.ic_like_on);
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Post.like(postId, false);
                        }
                    });
                }
                else {
                    fab.setImageResource(R.drawable.ic_like_off);
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Post.like(postId, true);
                        }
                    });
                }
            }
        });

        postObserver = Post.fetchPost(postId, new DataSnapshotObserver.OnDataSnapshotReceivedListener() {
            @Override
            public void onDataSnapshotReceived(DataSnapshot dataSnapshot) {
                CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
                collapsingToolbar.setTitle(dataSnapshot.child("title").getValue(String.class));
                Picasso.with(PostDetailsActivity.this)
                        .load(dataSnapshot.child("image").getValue(String.class))
                        .error(R.drawable.placeholder_post)
                        .into(((ImageView)findViewById(R.id.image)));

                ((TextView)findViewById(R.id.ingredients)).setText(dataSnapshot.child("ingredients").getValue(String.class));
                ((TextView)findViewById(R.id.steps)).setText(dataSnapshot.child("steps").getValue(String.class));

                if (authorObserver == null) {
                    authorObserver = User.fetchProfile(dataSnapshot.child("user_id").getValue(String.class), new DataSnapshotObserver.OnDataSnapshotReceivedListener() {
                        @Override
                        public void onDataSnapshotReceived(DataSnapshot userDataSnapshot) {
                            ((TextView) findViewById(R.id.user_name)).setText(userDataSnapshot.child("name").getValue(String.class));
                            Picasso.with(PostDetailsActivity.this)
                                    .load(userDataSnapshot.child("image").getValue(String.class))
                                    .error(R.drawable.placeholder_post)
                                    .into(((ImageView) findViewById(R.id.user_image)));
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}
