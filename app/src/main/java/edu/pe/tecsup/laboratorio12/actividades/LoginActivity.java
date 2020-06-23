package edu.pe.tecsup.laboratorio12.actividades;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import edu.pe.tecsup.laboratorio12.MainActivity;
import edu.pe.tecsup.laboratorio12.R;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private ProgressBar progressBar;
    private View loginPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressBar = (ProgressBar)findViewById(R.id.progressbar);
        loginPanel = findViewById(R.id.login_panel);

        // Init FirebaseAuth
        initFirebaseAuth();
//        Init GoogleSignIn;
        initGoogleSignIn();
        // Init FacebookSignIn
        // Init FirebaseAuthStateListener
        initFirebaseAuthStateListener();

        initFacebookSignIn();
    }

    /**
     * Firebase Auth
     */
    private FirebaseAuth mAuth;

    private EditText emailInput;
    private EditText passwordInput;

    private void initFirebaseAuth(){
        // initialize the FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();

        emailInput = (EditText)findViewById(R.id.email_input);
        passwordInput = (EditText)findViewById(R.id.password_input);
    }

    public void callLogin(View view){

        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "You must complete these fields", Toast.LENGTH_SHORT).show();
            return;
        }

        loginPanel.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        // Sign In user
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmailAndPassword:onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            loginPanel.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "signInWithEmailAndPassword:failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Username and/or password invalid", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void callRegister(View view){

        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "You must complete these fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if(password.length() < 6){
            Toast.makeText(this, "Using a weak password", Toast.LENGTH_SHORT).show();
            return;
        }

        loginPanel.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmailAndPassword:onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            loginPanel.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "createUserWithEmailAndPassword:failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Register failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Firebase AuthStateListener
     */
    private FirebaseAuth.AuthStateListener mAuthListener;

    private void initFirebaseAuthStateListener(){
        // and the AuthStateListener method so you can track whenever the user signs in or out
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());

                    Toast.makeText(LoginActivity.this, "Welcome " + user.getEmail(), Toast.LENGTH_SHORT).show();

                    // Go MainActivity
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
    /**
     * Google SignIn
     */

    /* Request code used to invoke sign in user interactions for Google+ */
    private static final int GOOGLE_SIGNIN_REQUEST = 1000;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    private void initGoogleSignIn(){

        // Configure SingIn Button
        SignInButton mGoogleLoginButton = (SignInButton) findViewById(R.id.sign_in_button);
        mGoogleLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginPanel.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);

                // OnClick Google SingIn Button
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, GOOGLE_SIGNIN_REQUEST);
            }
        });

        // Configure sign-in to request the user's ID, email address, and basic profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("977238766579-bns401tmsgg2jpqtjs9eb40j3qa1hdek.apps.googleusercontent.com")
                .requestEmail()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        // An unresolvable error has occurred and Google APIs (including Sign-In) will not be available.
                        Log.e(TAG, "onConnectionFailed:" + connectionResult);
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

    }
    /**
     * Facebook SignIn
     */

    private static final int FACEBOOK_SIGNIN_REQUEST = 64206;

    private CallbackManager mCallbackManager;

    private void initFacebookSignIn(){
        Log.d(TAG, "initFacebookSignIn");

        // Si no proceder a inicializar el CallbackManager
        mCallbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = findViewById(R.id.facebook_signin_button);
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult.getAccessToken());
                try{

                    if (AccessToken.getCurrentAccessToken() != null) {

                        // Facebook Sign In was successful
                        Profile profile = Profile.getCurrentProfile();

                        if(profile != null){
                            onFacebookSuccess(profile);
                        }else{
                            new ProfileTracker() {
                                @Override
                                protected void onCurrentProfileChanged(Profile oldProfile, final Profile profile) {
                                    stopTracking();
                                    onFacebookSuccess(profile);
                                }
                            }.startTracking();

                        }

                    }else {
                        Log.e(TAG, "Facebook Sign In failed!");
                        throw new Exception("Facebook Sign In failed!");
                    }

                }catch (Throwable t){
                    Log.e(TAG, "onThrowable: " + t.getMessage(), t);
                    Toast.makeText(LoginActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
                // ...
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                Toast.makeText(LoginActivity.this, String.valueOf(error), Toast.LENGTH_LONG).show();
                // ...
            }
        });

    }

    private void onFacebookSuccess(final Profile profile){
        Log.d(TAG, "onCurrentProfileChanged: " + profile);
        Log.d(TAG, "ID: " + profile.getId());
        Log.d(TAG, "NAME: " + profile.getName());
        Log.d(TAG, "PICTURE: " + profile.getProfilePictureUri(100, 100).toString());
        Log.d(TAG, "TOKEN: " + AccessToken.getCurrentAccessToken().getToken());

        // SignIn in firebaseAuthWithFacebook
        AuthCredential credential = FacebookAuthProvider.getCredential(AccessToken.getCurrentAccessToken().getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            loginPanel.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "signInWithCredential:failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            Log.d(TAG, "onActivityResult: " + requestCode);
            // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
            if (requestCode == GOOGLE_SIGNIN_REQUEST) {
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (result.isSuccess()) {

                    // Google Sign In was successful
                    GoogleSignInAccount account = result.getSignInAccount();
                    Log.d(TAG, "IC: " + account.getId());
                    Log.d(TAG, "DISPLAYNAME: " + account.getDisplayName());
                    Log.d(TAG, "EMAIL: " + account.getEmail());
                    Log.d(TAG, "PHOTO: " + account.getPhotoUrl());
                    Log.d(TAG, "TOKEN: " + account.getIdToken());

                    // SignIn in firebaseAuthWithGoogle
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    mAuth.signInWithCredential(credential)
                            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                                    if (!task.isSuccessful()) {
                                        loginPanel.setVisibility(View.VISIBLE);
                                        progressBar.setVisibility(View.GONE);
                                        Log.e(TAG, "signInWithCredential:failed", task.getException());
                                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });


                } else {
                    // Google Sign In failed, hide Progress Bar & Show Login Panel again
                    loginPanel.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Google Sign In failed!");
                    Toast.makeText(LoginActivity.this,"No se pudo",Toast.LENGTH_SHORT).show();
                }
            }else if(FACEBOOK_SIGNIN_REQUEST == requestCode){

            // Pass the activity result back to the Facebook SDK
            mCallbackManager.onActivityResult(requestCode, resultCode, data);

        }


    }catch (Throwable t){
            try {
                // Google Sign In failed, hide Progress Bar & Show Login Panel again
                loginPanel.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "onThrowable: " + t.getMessage(), t);
                if(getApplication()!=null) Toast.makeText(getApplication(), t.getMessage(), Toast.LENGTH_LONG).show();
            } catch (Throwable x) {}
        }

    }


}