package edu.pe.tecsup.laboratorio12.actividades;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import edu.pe.tecsup.laboratorio12.R;
import edu.pe.tecsup.laboratorio12.modelo.Post;
import pl.aprilapps.easyphotopicker.EasyImage;
import pl.tajchert.nammu.Nammu;
import pl.tajchert.nammu.PermissionCallback;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = RegisterActivity.class.getSimpleName();

    private EditText titleInput;
    private EditText bodyInput;
    private ImageView imagenPreview;
    private File lastPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        titleInput = findViewById(R.id.title_input);
        bodyInput = findViewById(R.id.body_input);
        imagenPreview = findViewById(R.id.imagen_preview);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.register, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_send:
                sendPost();
                return true;

            default:
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendPost() {
        Log.d(TAG, " sendPost()");

        final String title = titleInput.getText().toString();
        final String body = bodyInput.getText().toString();

        if(title.isEmpty() || body.isEmpty()){
            Toast.makeText(this, "Debes completar todos los campos", Toast.LENGTH_LONG).show();
            return;
        }
        if(lastPhoto == null || !lastPhoto.exists()){
            Toast.makeText(this, "Debes incluir una foto", Toast.LENGTH_LONG).show();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Cargando foto…");
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();


        // Get currentuser from FirebaseAuth
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "currentUser: " + currentUser);

        Uri localUri = Uri.fromFile(lastPhoto);
        // Registrar a Firebase Storage /posts/{userid}/{nombre del archivo}
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("posts").child(currentUser.getUid());
        final StorageReference photoRef = storageRef.child(localUri.getLastPathSegment());
        photoRef.putFile(localUri)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        Log.d(TAG, "Upload is " + progress + "% done");
                        progressDialog.setProgress((int)Math.round(progress));
                    }
                })
                .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        // Continue with the task to get the download URL
                        return photoRef.getDownloadUrl();
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        progressDialog.dismiss();
                        if(task.isSuccessful()){
                            Log.d(TAG, "onSuccess " + task.getResult());


                            // Registrar a Firebase Database
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");
        DatabaseReference postRef = postsRef.push();

        Post post = new Post();
        post.setId(postRef.getKey());
        post.setTitle(title);
        post.setBody(body);
        post.setUserid(currentUser.getUid());
        post.setPhotoUrl(task.getResult().toString());

        postRef.setValue(post)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onSuccess");
                            Toast.makeText(RegisterActivity.this, "Registro guardado", Toast.LENGTH_SHORT).show();
                            finish();
                        }else{
                            Log.e(TAG, "onFailure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Error al guardar", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                        }else{
                            Log.e(TAG, "onError " + task.getException());
                            Toast.makeText(RegisterActivity.this, "Error al cargar", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    public void takePicture(View view){
        Log.d(TAG, "takePicture");

        final String[] permissions = new String[]{
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };

        Nammu.init(this);

        // ValidatePermissions: https://github.com/tajchert/Nammu
        if(!Nammu.hasPermission(this, permissions)){
            Nammu.askForPermission(this, permissions, new PermissionCallback() {
                @Override
                public void permissionGranted() {
                    Log.d(TAG, "permissionGranted");
                    EasyImage.openChooserWithGallery(RegisterActivity.this, "Seleccione una imagen…", 0);
                }

                @Override
                public void permissionRefused() {
                    Log.d(TAG, "permissionRefused");
                    Toast.makeText(RegisterActivity.this, "No ha concedido el permiso", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        // Start camera/gallery: https://github.com/jkwiecien/EasyImage
        EasyImage.openChooserWithGallery(this, "Seleccione una imagen…", 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Nammu.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");

        EasyImage.handleActivityResult(requestCode, resultCode, data, this, new EasyImage.Callbacks() {
            @Override
            public void onImagesPicked(@NonNull List<File> imageFiles, EasyImage.ImageSource imageSource, int i) {
                if(imageFiles.isEmpty()) return;
                Log.d(TAG, "onImagePicked: file: " + imageFiles.get(0));
                Log.d(TAG, "onImagePicked: imageSource: " + imageSource);

                // Reducir la imagen a 800px solo si lo supera
                File resizedFile = scaleBitmapDown(imageFiles.get(0), 800);
                Log.d(TAG, "resizedFile: " + resizedFile);

                Picasso.with(RegisterActivity.this).load(resizedFile).into(imagenPreview);

                lastPhoto = resizedFile;
            }
            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                //Cancel handling, you might wanna remove taken photo if it was canceled
                if (source == EasyImage.ImageSource.CAMERA) {
                    File photoFile = EasyImage.lastlyTakenButCanceledPhoto(RegisterActivity.this);
                    if (photoFile != null) photoFile.delete();
                }
            }
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                Log.d(TAG, "onImagePickerError " + e.getMessage(), e);
            }
        });
    }

    // Redimensionar una imagen
    private File scaleBitmapDown(File file, int maxDimension) {
        try{
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());

            int originalWidth = bitmap.getWidth();
            int originalHeight = bitmap.getHeight();
            int resizedWidth = maxDimension;
            int resizedHeight = maxDimension;

            if (originalHeight > originalWidth) {
                resizedHeight = maxDimension;
                resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
            } else if (originalWidth > originalHeight) {
                resizedWidth = maxDimension;
                resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
            } else if (originalHeight == originalWidth) {
                resizedHeight = maxDimension;
                resizedWidth = maxDimension;
            }

            bitmap = Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);

            File resizedFile = new File(file.getPath() + "_resized.jpg");
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(resizedFile));

            return resizedFile;
        }catch (Throwable t){
            Log.e(TAG, t.toString(), t);
            return file;
        }
    }


}