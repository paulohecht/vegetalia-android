package br.com.vegetalia.app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DialogTitle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

import br.com.vegetalia.app.utils.DialogUtils;

public class NewPostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        setupToolbar();

        setupSaveButton();


    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupSaveButton() {
        findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
            }
        });
    }

    private void save() {

        showProgress();

        String title = ((EditText)findViewById(R.id.title)).getText().toString().trim();
        if (title.isEmpty()) {
            DialogUtils.alert(NewPostActivity.this, getString(R.string.new_post_title_empty_error));
            hideProgress();
            return;
        }

        String ingredients = ((EditText)findViewById(R.id.ingredients)).getText().toString().trim();
        if (ingredients.isEmpty()) {
            DialogUtils.alert(NewPostActivity.this, getString(R.string.new_post_ingredients_empty_error));
            hideProgress();
            return;
        }

        String steps = ((EditText)findViewById(R.id.steps)).getText().toString().trim();
        if (steps.isEmpty()) {
            DialogUtils.alert(NewPostActivity.this, getString(R.string.new_post_steps_empty_error));
            hideProgress();
            return;
        }

        String video_url = ((EditText)findViewById(R.id.video_url)).getText().toString().trim();

        final DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        final String key = db.child("posts").push().getKey();
        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map postValues = new HashMap();
        postValues.put("user_id", userId);
        postValues.put("title", title);
        postValues.put("ingredients", ingredients);
        postValues.put("steps", steps);
        postValues.put("video_url", (!video_url.isEmpty() ? video_url : null) );
        postValues.put("created_at", ServerValue.TIMESTAMP);

        Map updateValues = new HashMap();
        updateValues.put("posts/" + key, postValues);

        db.updateChildren(updateValues, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    DialogUtils.alert(NewPostActivity.this, getString(R.string.new_post_save_error));
                    return;
                }
                finish();
            }
        });
    }

    private void showProgress() {
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        findViewById(R.id.progress).setVisibility(View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

}
