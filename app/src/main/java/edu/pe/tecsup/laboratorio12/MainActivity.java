package edu.pe.tecsup.laboratorio12;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.List;

import edu.pe.tecsup.laboratorio12.actividades.RegisterActivity;
import edu.pe.tecsup.laboratorio12.adapter.PostRVAdapter;
import edu.pe.tecsup.laboratorio12.modelo.Post;
import edu.pe.tecsup.laboratorio12.modelo.User;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private FirebaseAnalytics mFirebaseAnalytics;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Forzar un error
//        getIntent().getExtras().getString("NO_EXISTE").getBytes();


        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Bundle bundle = new Bundle();
        bundle.putString("fullname", "Marco Sullca");
        // Get currentuser from FirebaseAuth
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "currentUser: " + currentUser);

        // Save/Update current user to Firebase Database
        User user = new User();
        user.setUid(currentUser.getUid());
        user.setDisplayName(currentUser.getDisplayName());
        user.setEmail(currentUser.getEmail());
        user.setPhotoUrl((currentUser.getPhotoUrl()!=null?currentUser.getPhotoUrl().toString():null));
        // user.setEtc...

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.child(user.getUid()).setValue(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onSuccess");
                        }else{
                            Log.e(TAG, "onFailure", task.getException());
                        }
                    }
                });

        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);


        mFirebaseAnalytics.setUserProperty("usuario", "shilary");
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "user: " + usuario);
        // Obtenemos el refreshedToken (instanceid)
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String newToken = instanceIdResult.getToken();
                Log.e("newToken",newToken);
            }
        });

        // Nos suscribimos al tópico 'ALL'
        FirebaseMessaging.getInstance().subscribeToTopic("ALL");

//        Ingreso de otro grupo de lineas

        // Obteniendo datos del usuario de Firebase en tiempo real
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange " + dataSnapshot.getKey());

                // Obteniendo datos del usuario
                User user = dataSnapshot.getValue(User.class);
                setTitle(user.getDisplayName());
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "onCancelled " + databaseError.getMessage(), databaseError.toException());
            }
        });

        // Lista de post con RecyclerView
        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final PostRVAdapter postRVAdapter = new PostRVAdapter();
        recyclerView.setAdapter(postRVAdapter);

        // Obteniendo lista de posts de Firebase (con realtime)
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded " + dataSnapshot.getKey());

                // Obteniendo nuevo post de Firebase
                String postKey = dataSnapshot.getKey();
                final Post addedPost = dataSnapshot.getValue(Post.class);
                Log.d(TAG, "addedPost " + addedPost);

                // Actualizando adapter datasource
                List<Post> posts = postRVAdapter.getPosts();
                posts.add(0, addedPost);
                postRVAdapter.notifyDataSetChanged();

                // ...
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged " + dataSnapshot.getKey());

                // Obteniendo post modificado de Firebase
                String postKey = dataSnapshot.getKey();
                Post changedPost = dataSnapshot.getValue(Post.class);
                Log.d(TAG, "changedPost " + changedPost);

                // Actualizando adapter datasource
                List<Post> posts = postRVAdapter.getPosts();
                int index = posts.indexOf(changedPost); // Necesario implementar Post.equals()
                if(index != -1){
                    posts.set(index, changedPost);
                }
                postRVAdapter.notifyDataSetChanged();

                // ...
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved " + dataSnapshot.getKey());

                // Obteniendo post eliminado de Firebase
                String postKey = dataSnapshot.getKey();
                Post removedPost = dataSnapshot.getValue(Post.class);
                Log.d(TAG, "removedPost " + removedPost);

                // Actualizando adapter datasource
                List<Post> posts = postRVAdapter.getPosts();
                posts.remove(removedPost); // Necesario implementar Post.equals()
                postRVAdapter.notifyDataSetChanged();

                // ...
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved " + dataSnapshot.getKey());

                // A post has changed position, use the key to determine if we are
                // displaying this post and if so move it.
                Post movedPost = dataSnapshot.getValue(Post.class);
                String postKey = dataSnapshot.getKey();

                // ...
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "onCancelled " + databaseError.getMessage(), databaseError.toException());
            }
        };
        postsRef.addChildEventListener(childEventListener);


        // Cargar parámetros de Remote Config
        final FirebaseRemoteConfig mRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mRemoteConfig.setConfigSettings(configSettings);

        // Definimos los valores predeterminados en caso no se obtenga de Remote Config
        HashMap<String, Object> defaults = new HashMap<>();
        defaults.put("tecsup_enabled", false);
        defaults.put("tecsup_mensaje", "");
        defaults.put("tecsup_color", "#3F51B5");
        mRemoteConfig.setDefaults(defaults);

        // Tiempo de vida de la caché en segundos
        long cacheExpiration = 3600;

        // En modo desarrollo deshabilitamos la caché
        if (mRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        // Obtenemos los parámetros del Remote Config
        mRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onSuccess");
                            // Activamos los parámetros de config
                            mRemoteConfig.activateFetched();

                            // Aplicamos cambios en las vistas
                            boolean tecsup_enabled = mRemoteConfig.getBoolean("tecsup_enabled");
                            if(tecsup_enabled){
                                String tecsup_mensaje = mRemoteConfig.getString("tecsup_mensaje");
                                new AlertDialog.Builder(MainActivity.this).setMessage(tecsup_mensaje).create().show();

                                String tecsup_color = mRemoteConfig.getString("tecsup_color");
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(tecsup_color)));
                            }

                        } else {
                            Log.e(TAG, "onFailure", task.getException());
                        }
                    }
                });




    }
    private static final int REGISTER_FORM_REQUEST = 100;

    public void showRegister(View view){
        startActivityForResult(new Intent(this, RegisterActivity.class), REGISTER_FORM_REQUEST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                callLogout(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void callLogout(View view){
        Log.d(TAG, "Sign out user");
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        finish();
    }




}