import { NativeModules, Platform } from 'react-native';

const { RNGoogleOneTapSignIn } = NativeModules;

const IS_IOS = Platform.OS === 'ios';

class GoogleOneTapSignIn {
  configPromise;

  constructor() {
    if (__DEV__ && !RNGoogleOneTapSignIn) {
      console.error(
        `RN GoogleOneTapSignIn native module is not correctly linked. Please read the readme, setup and troubleshooting instructions carefully or try manual linking. If you're using Expo, please use expo-google-sign-in. This is because Expo does not support custom native modules.`
      );
    }
  }

  async signIn() {
    if (IS_IOS) {
      return true;
    } else {
      await this.configPromise;
      return await RNGoogleOneTapSignIn.signIn();
    }
  }

  async hasPlayServices(options = { showPlayServicesUpdateDialog: true }) {
    if (IS_IOS) {
      return true;
    } else {
      if (options && options.showPlayServicesUpdateDialog === undefined) {
        throw new Error(
          'RNGoogleOneTapSignIn: Missing property `showPlayServicesUpdateDialog` in options object for `hasPlayServices`'
        );
      }
      return RNGoogleOneTapSignIn.playServicesAvailable(options.showPlayServicesUpdateDialog);
    }
  }

  configure(options = {}) {
    if (options.offlineAccess && !options.webClientId) {
      throw new Error('RNGoogleOneTapSignIn: offline use requires server web ClientID');
    }

    this.configPromise = RNGoogleOneTapSignIn.configure(options);
  }

  async signInSilently() {
    if (IS_IOS) {
      return true;
    } else {
      await this.configPromise;
      return RNGoogleOneTapSignIn.signInSilently();
    }
  }

  async savePassword(userId, password) {
    if (IS_IOS) {
      return true;
    } else {
      await this.configPromise;
      return RNGoogleOneTapSignIn.savePassword(userId, password);
    }
  }

  async signOut() {
    if (IS_IOS) {
      return true;
    } else {
      await this.configPromise;
      return RNGoogleOneTapSignIn.signOut();
    }
  }
}

export const GoogleOneTapSignInSingleton = new GoogleOneTapSignIn();

export const statusCodes = {
  SIGN_IN_CANCELLED: RNGoogleOneTapSignIn.SIGN_IN_CANCELLED,
  IN_PROGRESS: RNGoogleOneTapSignIn.IN_PROGRESS,
  PLAY_SERVICES_NOT_AVAILABLE: RNGoogleOneTapSignIn.PLAY_SERVICES_NOT_AVAILABLE,
  SIGN_IN_REQUIRED: RNGoogleOneTapSignIn.SIGN_IN_REQUIRED,
};
