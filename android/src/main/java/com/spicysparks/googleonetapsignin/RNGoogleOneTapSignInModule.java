  package com.spicysparks.googleonetapsignin;

  import android.app.Activity;
  import android.content.Intent;
  import android.content.IntentSender;
  import android.util.Log;

  import androidx.annotation.NonNull;
  import androidx.annotation.Nullable;

  import com.facebook.react.bridge.Arguments;
  import com.facebook.react.bridge.BaseActivityEventListener;
  import com.facebook.react.bridge.Promise;
  import com.facebook.react.bridge.ReactApplicationContext;
  import com.facebook.react.bridge.ReactContextBaseJavaModule;
  import com.facebook.react.bridge.ReactMethod;
  import com.facebook.react.bridge.ReadableMap;
  import com.facebook.react.bridge.UiThreadUtil;
  import com.facebook.react.bridge.WritableMap;;
  import com.google.android.gms.auth.api.identity.BeginSignInRequest;
  import com.google.android.gms.auth.api.identity.BeginSignInResult;
  import com.google.android.gms.auth.api.identity.Identity;
  import com.google.android.gms.auth.api.identity.SavePasswordRequest;
  import com.google.android.gms.auth.api.identity.SavePasswordResult;
  import com.google.android.gms.auth.api.identity.SignInClient;
  import com.google.android.gms.auth.api.identity.SignInCredential;
  import com.google.android.gms.auth.api.identity.SignInPassword;
  import com.google.android.gms.auth.api.signin.GoogleSignInClient;
  import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
  import com.google.android.gms.common.ConnectionResult;
  import com.google.android.gms.common.GoogleApiAvailability;
  import com.google.android.gms.common.SignInButton;
  import com.google.android.gms.common.api.ApiException;
  import com.google.android.gms.common.api.CommonStatusCodes;
  import com.google.android.gms.tasks.OnCompleteListener;
  import com.google.android.gms.tasks.OnFailureListener;
  import com.google.android.gms.tasks.OnSuccessListener;
  import com.google.android.gms.tasks.Task;

  import java.util.HashMap;
  import java.util.Map;

  import com.spicysparks.googleonetapsignin.PendingAuthRecovery;
  import com.spicysparks.googleonetapsignin.PromiseWrapper;

  import static com.spicysparks.googleonetapsignin.PromiseWrapper.ASYNC_OP_IN_PROGRESS;
  import static com.spicysparks.googleonetapsignin.Utils.getExceptionCode;
  import static com.spicysparks.googleonetapsignin.Utils.getUserProperties;


  public class RNGoogleOneTapSignInModule extends ReactContextBaseJavaModule {

      private String webClientId;

      private SignInClient oneTapClient;
      private BeginSignInRequest signInRequest;

      private static final int REQ_ONE_TAP = 2;
      private static final int REQUEST_CODE_GIS_SAVE_PASSWORD = 3; /* unique request id */
      public static final int RC_SIGN_IN = 9001;
      public static final int REQUEST_CODE_RECOVER_AUTH = 53294;
      public static final String MODULE_NAME = "RNGoogleOneTapSignIn";
      public static final String PLAY_SERVICES_NOT_AVAILABLE = "PLAY_SERVICES_NOT_AVAILABLE";
      public static final String ERROR_USER_RECOVERABLE_AUTH = "ERROR_USER_RECOVERABLE_AUTH";
      private static final String SHOULD_RECOVER = "SHOULD_RECOVER";

      private PendingAuthRecovery pendingAuthRecovery;

      private PromiseWrapper promiseWrapper;

      public PromiseWrapper getPromiseWrapper() {
          return promiseWrapper;
      }

      @Override
      public String getName() {
          return MODULE_NAME;
      }

      public RNGoogleOneTapSignInModule(final ReactApplicationContext reactContext) {
          super(reactContext);

          oneTapClient = Identity.getSignInClient(reactContext);

          promiseWrapper = new PromiseWrapper();
          reactContext.addActivityEventListener(new RNGoogleOneTapSignInActivityEventListener());
      }

    @Override
    public Map<String, Object> getConstants() {
      final Map<String, Object> constants = new HashMap<>();
      constants.put("BUTTON_SIZE_ICON", SignInButton.SIZE_ICON_ONLY);
      constants.put("BUTTON_SIZE_STANDARD", SignInButton.SIZE_STANDARD);
      constants.put("BUTTON_SIZE_WIDE", SignInButton.SIZE_WIDE);
      constants.put("BUTTON_COLOR_AUTO", SignInButton.COLOR_AUTO);
      constants.put("BUTTON_COLOR_LIGHT", SignInButton.COLOR_LIGHT);
      constants.put("BUTTON_COLOR_DARK", SignInButton.COLOR_DARK);
      constants.put("SIGN_IN_CANCELLED", String.valueOf(GoogleSignInStatusCodes.SIGN_IN_CANCELLED));
      constants.put("SIGN_IN_REQUIRED", String.valueOf(CommonStatusCodes.SIGN_IN_REQUIRED));
      constants.put("IN_PROGRESS", ASYNC_OP_IN_PROGRESS);
      constants.put(PLAY_SERVICES_NOT_AVAILABLE, PLAY_SERVICES_NOT_AVAILABLE);
      return constants;
    }

      @ReactMethod
      public void playServicesAvailable(boolean showPlayServicesUpdateDialog, Promise promise) {
          Activity activity = getCurrentActivity();

          if (activity == null) {
              Log.w(MODULE_NAME, "could not determine playServicesAvailable, activity is null");
              promise.reject(MODULE_NAME, "activity is null");
              return;
          }

          GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
          int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);

          if (status != ConnectionResult.SUCCESS) {
              if (showPlayServicesUpdateDialog && googleApiAvailability.isUserResolvableError(status)) {
                  int requestCode = 2404;
                  googleApiAvailability.getErrorDialog(activity, status, requestCode).show();
              }
              promise.reject(PLAY_SERVICES_NOT_AVAILABLE, "Play services not available");
          } else {
              promise.resolve(true);
          }
      }

      @ReactMethod
      public void configure(
              final ReadableMap config,
              final Promise promise
      ) {
          this.webClientId = config.hasKey("webClientId") ? config.getString("webClientId") : null;

          promise.resolve(null);
      }

      private void handleSignInTaskResult(@NonNull Intent intent) {
        try {
          SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(intent);

          WritableMap userParams = getUserProperties(credential);
          promiseWrapper.resolve(userParams);

        } catch (ApiException e) {
          int code = e.getStatusCode();
          switch (code) {
            case CommonStatusCodes.CANCELED:
            default:
              String errorDescription = GoogleSignInStatusCodes.getStatusCodeString(code);
              promiseWrapper.reject(String.valueOf(code), errorDescription);
          }
        }
      }

    @ReactMethod
    public void signInSilently(final Promise promise) {
      if (oneTapClient == null) {
        rejectWithNullClientError(promise);
        return;
      }

      final Activity activity = getCurrentActivity();

      if (activity == null) {
        promise.reject(MODULE_NAME, "activity is null");
        return;
      }

      signInRequest = BeginSignInRequest.builder()
        .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
          .setSupported(true)
          .build())
        .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
          .setSupported(true)
          // Your server's client ID, not your Android client ID.
          .setServerClientId(webClientId)
          // Only show accounts previously used to sign in.
          .setFilterByAuthorizedAccounts(true)
          .build())
        // Automatically sign in when exactly one credential is retrieved.
        .setAutoSelectEnabled(true)
        .build();

      promiseWrapper.setPromiseWithInProgressCheck(promise, "signIn");
      UiThreadUtil.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(activity, new OnSuccessListener<BeginSignInResult>() {
              @Override
              public void onSuccess(BeginSignInResult result) {
                try {
                  activity.startIntentSenderForResult(
                    result.getPendingIntent().getIntentSender(), REQ_ONE_TAP,
                    null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                  promise.reject(MODULE_NAME, e.getLocalizedMessage());
                }
              }
            })
            .addOnFailureListener(activity, new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                promise.reject(MODULE_NAME, e.getLocalizedMessage());
              }
            });
        }
      });
    }

      @ReactMethod
      public void signIn(final Promise promise) {

          if (oneTapClient == null) {
            rejectWithNullClientError(promise);
            return;
          }

          final Activity activity = getCurrentActivity();

          if (activity == null) {
              promise.reject(MODULE_NAME, "activity is null");
              return;
          }

          signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
              .setSupported(true)
              // Your server's client ID, not your Android client ID.
              .setServerClientId(webClientId)
              // Show all accounts on the device.
              .setFilterByAuthorizedAccounts(false)
              .build())
            .build();

          promiseWrapper.setPromiseWithInProgressCheck(promise, "signIn");
            UiThreadUtil.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  oneTapClient.beginSignIn(signInRequest)
                    .addOnSuccessListener(activity, new OnSuccessListener<BeginSignInResult>() {
                      @Override
                      public void onSuccess(BeginSignInResult result) {
                        try {
                          activity.startIntentSenderForResult(
                            result.getPendingIntent().getIntentSender(), REQ_ONE_TAP,
                            null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                          promise.reject(MODULE_NAME, e.getLocalizedMessage());
                        }
                      }
                    })
                    .addOnFailureListener(activity, new OnFailureListener() {
                      @Override
                      public void onFailure(@NonNull Exception e) {
                        // No saved credentials found. Launch the One Tap sign-up flow, or
                        // do nothing and continue presenting the signed-out UI.
                        promise.reject(MODULE_NAME, e.getLocalizedMessage());
                      }
                    });
                }
            });
      }

      @ReactMethod
      public void savePassword(final String userId, final String password, final Promise promise) {

          if(userId.isEmpty() || password.isEmpty()) {
            promise.reject(MODULE_NAME, "activity is null");
            return;
          }

          final Activity activity = getCurrentActivity();

          if (activity == null) {
            promise.reject(MODULE_NAME, "activity is null");
            return;
          }

          SignInPassword signInPassword = new SignInPassword(userId, password);
          SavePasswordRequest savePasswordRequest = SavePasswordRequest.builder().setSignInPassword(signInPassword).build();

          Identity.getCredentialSavingClient(activity)
          .savePassword(savePasswordRequest)
          .addOnSuccessListener(
            new OnSuccessListener<SavePasswordResult>() {
              @Override
              public void onSuccess(SavePasswordResult result) {
                try {
                  activity.startIntentSenderForResult(
                    result.getPendingIntent().getIntentSender(),
                    REQUEST_CODE_GIS_SAVE_PASSWORD,
                    /* fillInIntent= */ null,
                    /* flagsMask= */ 0,
                    /* flagsValue= */ 0,
                    /* extraFlags= */ 0,
                    /* options= */ null);
                } catch (IntentSender.SendIntentException e) {
                  promise.reject(MODULE_NAME, e.getLocalizedMessage());
                }
              }
            });
      }

      private class RNGoogleOneTapSignInActivityEventListener extends BaseActivityEventListener {
          @Override
          public void onActivityResult(Activity activity, final int requestCode, final int resultCode, final Intent intent) {
              if(requestCode == REQ_ONE_TAP){
                handleSignInTaskResult(intent);
              }
              else if (requestCode == REQUEST_CODE_GIS_SAVE_PASSWORD) {
                handleSavePasswordTaskResult(resultCode);
            }
          }
      }

      @ReactMethod
      public void signOut(final Promise promise) {
          if (oneTapClient == null) {
              rejectWithNullClientError(promise);
              return;
          }

          oneTapClient.signOut()
                  .addOnCompleteListener(new OnCompleteListener<Void>() {
                      @Override
                      public void onComplete(@NonNull Task<Void> task) {
                          handleSignOutOrRevokeAccessTask(task, promise);
                      }
                  });
      }

      private void handleSavePasswordTaskResult(int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
          /* password was saved */
          promiseWrapper.resolve(true);
        } else if (resultCode == Activity.RESULT_CANCELED) {
          /* password saving was cancelled */
          promiseWrapper.resolve(false);
        }
    }

      private void handleSignOutOrRevokeAccessTask(@NonNull Task<Void> task, final Promise promise) {
        if (task.isSuccessful()) {
          promise.resolve(null);
        } else {
          int code = getExceptionCode(task);
          String errorDescription = GoogleSignInStatusCodes.getStatusCodeString(code);
          promise.reject(String.valueOf(code), errorDescription);
        }
      }

      private void rejectWithNullClientError(Promise promise) {
          promise.reject(MODULE_NAME, "oneTapClient is null - call configure first");
      }

  }
