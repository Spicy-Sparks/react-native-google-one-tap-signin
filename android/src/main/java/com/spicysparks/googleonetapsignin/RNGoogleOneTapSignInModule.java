package com.spicysparks.googleonetapsignin;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePasswordRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPasswordOption;
import androidx.credentials.PasswordCredential;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RNGoogleOneTapSignInModule extends ReactContextBaseJavaModule {

  CredentialManager credentialManager;
  GetCredentialRequest getCredRequest;

  @Override
  public String getName() {
      return "RNGoogleOneTapSignIn";
  }

  public RNGoogleOneTapSignInModule(final ReactApplicationContext reactContext) {
    super(reactContext);
    credentialManager = CredentialManager.create(reactContext);
    GetPasswordOption getPasswordOption = new GetPasswordOption();
    getCredRequest = new GetCredentialRequest.Builder()
      .addCredentialOption(getPasswordOption)
      .setPreferImmediatelyAvailableCredentials(true)
      .build();
  }

  private static final String TAG = "OneTapSignIn";

  @ReactMethod
  public void signIn(Promise promise) {
    Log.d(TAG, "signIn() called");
    var activity = getCurrentActivity();
    if (activity == null) {
      Log.e(TAG, "Activity is null");
      promise.reject("ERROR", "Activity is null");
      return;
    }
    Log.d(TAG, "Activity: " + activity.getClass().getName());
    Log.d(TAG, "Requesting credentials...");
    Executor executor = Executors.newSingleThreadExecutor();
    credentialManager.getCredentialAsync(
      activity,
      getCredRequest,
      null,
      executor,
      new CredentialManagerCallback<>() {
        @Override
        public void onResult(GetCredentialResponse result) {
          Log.d(TAG, "Got credential response");
          Credential credential = result.getCredential();
          Log.d(TAG, "Credential type: " + credential.getClass().getName());
          if (credential instanceof PasswordCredential) {
            String username = ((PasswordCredential) credential).getId();
            String password = ((PasswordCredential) credential).getPassword();
            Log.d(TAG, "PasswordCredential - username: " + username + ", password length: " + password.length());
            WritableMap args = Arguments.createMap();
            args.putString("id", username);
            args.putString("password", password);
            promise.resolve(args);
          } else {
            Log.e(TAG, "Unexpected credential type: " + credential.getClass().getName());
            promise.reject("ERROR", "Unexpected type of credential: " + credential.getClass().getName());
          }
        }
        @Override
        public void onError(@NonNull GetCredentialException e) {
          Log.e(TAG, "GetCredentialException: " + e.getClass().getName() + " - " + e.getMessage());
          Log.e(TAG, "Error type: " + e.getType());
          promise.reject(e.getType(), e.getMessage());
        }
      }
    );
  }

  @ReactMethod
  public void signOut(Promise promise) {
    Executor executor = Executors.newSingleThreadExecutor();
    ClearCredentialStateRequest clearCredentialRequest = new ClearCredentialStateRequest();
    credentialManager.clearCredentialStateAsync(
      clearCredentialRequest,
      null,
      executor,
      new CredentialManagerCallback<>() {
        @Override
        public void onResult(Void unused) {
          promise.resolve(true);
        }
        @Override
        public void onError(@NonNull ClearCredentialException e) {
          promise.reject("ERROR", e.getMessage());
        }
      }
    );
  }

  @ReactMethod
  public void savePassword(String username, String password, Promise promise) {
    Log.d(TAG, "savePassword() called for: " + username);
    var activity = getCurrentActivity();
    if (activity == null) {
      Log.e(TAG, "savePassword: Activity is null");
      promise.reject("ERROR", "Activity is null");
      return;
    }
    Log.d(TAG, "Creating password request...");
    Executor executor = Executors.newSingleThreadExecutor();
    CreatePasswordRequest createPasswordRequest = new CreatePasswordRequest(username, password);
    credentialManager.createCredentialAsync(
      activity,
      createPasswordRequest,
      null,
      executor,
      new CredentialManagerCallback<>() {
        @Override
        public void onResult(CreateCredentialResponse result) {
          Log.d(TAG, "Password saved successfully");
          promise.resolve(true);
        }
        @Override
        public void onError(CreateCredentialException e) {
          Log.e(TAG, "CreateCredentialException: " + e.getClass().getName() + " - " + e.getMessage());
          Log.e(TAG, "Error type: " + e.getType());
          promise.reject(e.getType(), e.getMessage());
        }
      }
    );
  }

  @ReactMethod
  public void deletePassword(String username, String password, Promise promise) {
    promise.resolve(true);
  }

  @ReactMethod
  public void signInWithGoogle(String webClientId, Promise promise) {
    Log.d(TAG, "signInWithGoogle() called with webClientId: " + webClientId);
    var activity = getCurrentActivity();
    if (activity == null) {
      Log.e(TAG, "signInWithGoogle: Activity is null");
      promise.reject("ERROR", "Activity is null");
      return;
    }

    // Build Google ID option with auto-select enabled
    GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true) // Only show accounts that have already authorized the app
      .setAutoSelectEnabled(true) // Enable auto-select for seamless sign-in
      .setServerClientId(webClientId)
      .build();

    GetCredentialRequest request = new GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build();

    Executor executor = Executors.newSingleThreadExecutor();
    Log.d(TAG, "signInWithGoogle: Requesting Google ID credential...");

    credentialManager.getCredentialAsync(
      activity,
      request,
      null,
      executor,
      new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
        @Override
        public void onResult(GetCredentialResponse result) {
          Log.d(TAG, "signInWithGoogle: Got credential response");
          Credential credential = result.getCredential();
          Log.d(TAG, "signInWithGoogle: Credential type: " + credential.getClass().getName());

          if (credential instanceof CustomCredential) {
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
              try {
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                String idToken = googleIdTokenCredential.getIdToken();
                String id = googleIdTokenCredential.getId();
                String displayName = googleIdTokenCredential.getDisplayName();
                String givenName = googleIdTokenCredential.getGivenName();
                String familyName = googleIdTokenCredential.getFamilyName();
                String profilePictureUri = googleIdTokenCredential.getProfilePictureUri() != null
                  ? googleIdTokenCredential.getProfilePictureUri().toString()
                  : "";

                Log.d(TAG, "signInWithGoogle: Got Google ID token for: " + id);

                WritableMap args = Arguments.createMap();
                args.putString("idToken", idToken);
                args.putString("id", id);
                args.putString("email", id);
                args.putString("displayName", displayName != null ? displayName : "");
                args.putString("givenName", givenName != null ? givenName : "");
                args.putString("familyName", familyName != null ? familyName : "");
                args.putString("photo", profilePictureUri);
                promise.resolve(args);
              } catch (Exception e) {
                Log.e(TAG, "signInWithGoogle: Failed to parse Google ID token", e);
                promise.reject("ERROR", "Failed to parse Google ID token: " + e.getMessage());
              }
            } else {
              Log.e(TAG, "signInWithGoogle: Unexpected custom credential type: " + credential.getType());
              promise.reject("ERROR", "Unexpected custom credential type");
            }
          } else {
            Log.e(TAG, "signInWithGoogle: Unexpected credential class: " + credential.getClass().getName());
            promise.reject("ERROR", "Unexpected credential type");
          }
        }

        @Override
        public void onError(@NonNull GetCredentialException e) {
          Log.e(TAG, "signInWithGoogle: GetCredentialException: " + e.getClass().getName() + " - " + e.getMessage());
          Log.e(TAG, "signInWithGoogle: Error type: " + e.getType());
          promise.reject(e.getType(), e.getMessage());
        }
      }
    );
  }
}
