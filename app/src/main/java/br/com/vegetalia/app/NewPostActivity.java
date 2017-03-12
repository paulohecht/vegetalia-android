package br.com.vegetalia.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DialogTitle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import br.com.vegetalia.app.utils.DialogUtils;

public class NewPostActivity extends AppCompatActivity {

    static final int REQUEST_STORAGE_READ_ACCESS_PERMISSION = 101;
    static final int REQUEST_STORAGE_WRITE_ACCESS_PERMISSION = 102;

    static final int REQUEST_IMAGE_FROM_CAMERA = 1;
    static final int REQUEST_IMAGE_FROM_GALLERY = 2;

    private File tempFile = null;
    private File cropTempFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser().isAnonymous()) {
            startActivity(new Intent(NewPostActivity.this, AuthenticateActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_new_post);

        setupToolbar();

        setupSaveButton();

        setupImageButtons();

    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupImageButtons() {
        findViewById(R.id.image_from_camera_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getImageFromCamera();
            }
        });

        findViewById(R.id.image_from_gallery_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getImageFromGallery();
            }
        });

        if (cropTempFile != null) {
            findViewById(R.id.image_clear_button).setVisibility(View.VISIBLE);
            findViewById(R.id.image_clear_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cropTempFile = null;
                    ((ImageView)findViewById(R.id.image)).setImageResource(R.drawable.placeholder_post);
                    setupImageButtons();
                }
            });
        }
        else {
            findViewById(R.id.image_clear_button).setVisibility(View.GONE);
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
                if (cropTempFile == null) {
                    finish();
                }
                else {
                    uploadFile(db, key, new OnSaveCompletedListener() {
                        @Override
                        public void onSaveCompleted() {
                            finish();
                        }
                    });
                }
            }
        });
    }

    private void uploadFile(final DatabaseReference db, final String key, final OnSaveCompletedListener callback) {
        final StorageReference photoRef = FirebaseStorage.getInstance().getReference().child("posts").child(key + ".jpg");
        photoRef.putFile(Uri.fromFile(cropTempFile))
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        StorageMetadata metadata = taskSnapshot.getMetadata();
                        if (metadata == null) return;
                        Uri downloadUrl = metadata.getDownloadUrl();
                        if (downloadUrl == null) return;
                        String downloadPath = downloadUrl.toString();
                        HashMap postValues = new HashMap();
                        postValues.put("image", downloadPath);
                        postValues.put("updated_at", ServerValue.TIMESTAMP);
                        db.child("posts").child(key).updateChildren(postValues);
                        callback.onSaveCompleted();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        DialogUtils.alert(NewPostActivity.this, "Houve um erro ao salvar o post...");
                        //ROLLBACK?
                    }
                });
    }


    private void getImageFromCamera() {
        Intent getImageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (getImageIntent.resolveActivity(NewPostActivity.this.getPackageManager()) != null) {
            try {
                tempFile = File.createTempFile("temp", ".jpg", NewPostActivity.this.getCacheDir());
            } catch (IOException ex) {
                DialogUtils.alert(NewPostActivity.this, "Houve um erro ao criar o arquivo temporário...");
            }
            if (tempFile != null) {
                Uri tempFileUri = FileProvider.getUriForFile(NewPostActivity.this, "br.com.vegetalia.app.fileprovider", tempFile);
                getImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempFileUri);
                startActivityForResult(getImageIntent, REQUEST_IMAGE_FROM_CAMERA);
            }
        }
    }

    protected void requestPermission(final String permission, String rationale, final int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            DialogUtils.confirm(NewPostActivity.this, rationale, new DialogUtils.OnConfirm() {
                @Override
                public void onConfirm() {
                    ActivityCompat.requestPermissions(NewPostActivity.this, new String[]{permission}, requestCode);
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    private void getImageFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, "Precisamos da sua permissão para selecionar uma imagem da galeria.", REQUEST_STORAGE_READ_ACCESS_PERMISSION);
        } else {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Selecionar"), REQUEST_IMAGE_FROM_GALLERY);
        }
    }

    private void openCrop(Uri origin) {
        UCrop.Options options = new UCrop.Options();
        options.setHideBottomControls(true);
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(100);
        options.setToolbarColor(getResources().getColor(R.color.primary));
        options.setStatusBarColor(getResources().getColor(R.color.primary_dark));
        options.setToolbarTitle("Recortar a imagem...");
        try {
            cropTempFile = File.createTempFile("temp_crop", ".jpg", NewPostActivity.this.getCacheDir());
        } catch (IOException e) {
            DialogUtils.alert(NewPostActivity.this, "Houve um erro ao criar o arquivo temporário...");
        }
        UCrop.of(origin, Uri.fromFile(cropTempFile))
                .withAspectRatio(800, 450)
                .withMaxResultSize(800, 450)
                .withOptions(options)
                .start(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_FROM_CAMERA && resultCode == Activity.RESULT_OK && null != tempFile) {
            openCrop(Uri.fromFile(tempFile));
        }
        if (requestCode == REQUEST_IMAGE_FROM_GALLERY && resultCode == RESULT_OK && null != data) {
            openCrop(data.getData());
        }

        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            ((ImageView)findViewById(R.id.image)).setImageURI(UCrop.getOutput(data));
            setupImageButtons();
        } else if (resultCode == UCrop.RESULT_ERROR) {
            DialogUtils.alert(NewPostActivity.this, "Houve um erro ao recortar a foto...");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_READ_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImageFromGallery();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }


    private void showProgress() {
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        findViewById(R.id.progress).setVisibility(View.GONE);
    }

    public interface OnSaveCompletedListener {
        public void onSaveCompleted();
    }

}
