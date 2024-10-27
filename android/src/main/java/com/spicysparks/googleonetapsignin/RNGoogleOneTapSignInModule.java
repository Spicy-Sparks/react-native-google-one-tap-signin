package com.spicysparks.googleonetapsignin;

import androidx.annotation.NonNull;

import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePasswordRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.PasswordCredential;
import androidx.credentials.CustomCredential;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RNGoogleOneTapSignInModule extends ReactContextBaseJavaModule {

  private String webClientId;

  CredentialManager credentialManager;
  GetCredentialRequest getCredRequest;
  GetGoogleIdOption googleIdOption;

  @Override
  public String getName() {
      return "RNGoogleOneTapSignIn";
  }

  public RNGoogleOneTapSignInModule(final ReactApplicationContext reactContext) {
    super(reactContext);

    credentialManager = CredentialManager.create(reactContext);
  }
  

  @ReactMethod
  public void configure(
    final ReadableMap config,
    final Promise promise
  ) {
    this.webClientId = config.hasKey("webClientId") ? config.getString("webClientId") : null;
    
    if (this.webClientId == null) {
      promise.reject("ERROR", "webClientId is required");
      return;
    }
    
    googleIdOption = new GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(this.webClientId)
      .build();
    
    getCredRequest = new GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build();

    promise.resolve(null);
  }

  @ReactMethod
  public void signIn(Promise promise) {
    var activity = getCurrentActivity();
    if (activity == null) {
      promise.reject("ERROR", "Activity is null");
      return;
    }
    Executor executor = Executors.newSingleThreadExecutor();
    credentialManager.getCredentialAsync(
      activity,
      getCredRequest,
      null,
      executor,
      new CredentialManagerCallback<>() {
        @Override
        public void onResult(GetCredentialResponse result) {
          Credential credential = result.getCredential();

          if (credential instanceof PasswordCredential) {
            // This will be the case when the user has saved a password credential
            // But we are not using it for now
            String username = ((PasswordCredential) credential).getId();
            String password = ((PasswordCredential) credential).getPassword();
            WritableMap args = Arguments.createMap();
            args.putString("id", username);
            args.putString("type", "password");
            args.putString("password", password);
            promise.resolve(args);
          } else if (credential instanceof CustomCredential) {

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
              GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(
                  (credential).getData());

              String id = googleIdTokenCredential.getId();
              String idToken = googleIdTokenCredential.getIdToken();
              String givenName = googleIdTokenCredential.getGivenName();
              String familyName = googleIdTokenCredential.getFamilyName();
              WritableMap args = Arguments.createMap();
              args.putString("id", id);
              args.putString("idToken", idToken);
              args.putString("givenName", givenName);
              args.putString("familyName", familyName);
              args.putString("type", "google");
              promise.resolve(args);
            } else {
              promise.reject("ERROR", "Unexpected type of credential");
            }
          } else {
            promise.reject("ERROR", "Unexpected type of credential - 2");
          }
        }
        @Override
        public void onError(@NonNull GetCredentialException e) {
          promise.reject("ERROR", e.getMessage());
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
}
