import { NativeModules, Platform } from 'react-native';

const { RNGoogleOneTapSignIn } = NativeModules;

const IS_IOS = Platform.OS === 'ios';

class GoogleOneTapSignIn {
  configPromise;

  constructor() {
    if (__DEV__ && !IS_IOS && !RNGoogleOneTapSignIn) {
      console.error(
        `RN GoogleOneTapSignIn native module is not correctly linked. Please read the readme, setup and troubleshooting instructions carefully or try manual linking. If you're using Expo, please use expo-google-sign-in. This is because Expo does not support custom native modules.`
      );
    }
  }

  signIn() {
    if (IS_IOS)
      return Promise.reject(new Error("Unsupported"));
    return RNGoogleOneTapSignIn.signIn();
  }

  savePassword(userId, password) {
    if (IS_IOS)
      return Promise.reject(new Error("Unsupported"));
    return RNGoogleOneTapSignIn.savePassword(userId, password);
  }

  deletePassword(userId, password) {
    if (IS_IOS)
      return Promise.reject(new Error("Unsupported"));
    return RNGoogleOneTapSignIn.deletePassword(userId, password);
  }

  signOut() {
    if (IS_IOS)
      return Promise.reject(new Error("Unsupported"));
    return RNGoogleOneTapSignIn.signOut();
  }
}

export const GoogleOneTapSignInSingleton = new GoogleOneTapSignIn();