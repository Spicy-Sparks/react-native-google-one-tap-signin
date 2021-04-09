![React Native Google One Tap Sign In](img/header.png)

<p align="center">
  <a href="https://www.npmjs.com/package/@react-native-google-signin/google-signin"><img src="https://badge.fury.io/js/%40react-native-community%2Fgoogle-signin.svg" alt="NPM Version"></a>
</p>

### 🚧🚧 Maintenance notice 🚧🚧

See this [issue](https://github.com/react-native-google-signin/google-signin/issues/942)

## Features

- Support all 3 types of authentication methods (standard, with server-side validation or with offline access (aka server side access))
- Promise-based API consistent between Android and iOS
- Typings for TypeScript and Flow
- Native signin buttons

## Project setup and initialization

For RN >= 0.60 please use version installed from `@react-native-google-signin/google-signin`

`yarn add @react-native-google-signin/google-signin`

For RN <= 0.59 use version 2 installed from `react-native-google-signin`

`yarn add react-native-google-signin`

Then follow the [Android guide](docs/android-guide.md) and [iOS guide](docs/ios-guide.md)

## Public API

### 1. GoogleOneTapSignIn

```js
import {
  GoogleOneTapSignIn,
  GoogleOneTapSignInButton,
  statusCodes,
} from 'react-native-google-one-tap-signin';
```

#### `configure(options)`

It is mandatory to call this method before attempting to call `signIn()` and `signInSilently()`. This method is sync meaning you can call `signIn` / `signInSilently` right after it. In typical scenarios, `configure` needs to be called only once, after your app starts. In the native layer, this is a synchronous call.

Example usage with default options: you get user email and basic profile info.

```js
import { GoogleOneTapSignIn } from 'react-native-google-one-tap-signin';

GoogleOneTapSignIn.configure();
```

Example to access Google Drive both from the mobile application and from your backend server:

```js
GoogleOneTapSignIn.configure({
  webClientId: '<FROM DEVELOPER CONSOLE>', // client ID of type WEB for your server (needed to verify user ID and offline access)
});
```

#### `signIn()`

Prompts a modal to let the user sign in into your application. Resolved promise returns an [`userInfo` object](#3-userinfo). Rejects with error otherwise.

```js
// import statusCodes along with GoogleSignin
import { GoogleOneTapSignIn, statusCodes } from 'react-native-google-one-tap-signin';

// Somewhere in your code
signIn = async () => {
  try {
    await GoogleOneTapSignIn.hasPlayServices();
    const userInfo = await GoogleOneTapSignIn.signIn();
    this.setState({ userInfo });
  } catch (error) {
    if (error.code === statusCodes.SIGN_IN_CANCELLED) {
      // user cancelled the login flow
    } else if (error.code === statusCodes.IN_PROGRESS) {
      // operation (e.g. sign in) is in progress already
    } else if (error.code === statusCodes.PLAY_SERVICES_NOT_AVAILABLE) {
      // play services not available or outdated
    } else {
      // some other error happened
    }
  }
};
```

#### `signInSilently()`

May be called eg. in the `componentDidMount` of your main component. This method returns the [current user](#3-userinfo) and rejects with an error otherwise.

To see how to handle errors read [`signIn()` method](#signin)

```js
getCurrentUserInfo = async () => {
  try {
    const userInfo = await GoogleOneTapSignIn.signInSilently();
    this.setState({ userInfo });
  } catch (error) {
    if (error.code === statusCodes.SIGN_IN_REQUIRED) {
      // user has not signed in yet
    } else {
      // some other error
    }
  }
};
```

#### `savePassword(userId, password)`

This method has to be called once a user has performed a normal sign in or sign up to save its userId and password for automatic login in next sessions

#### `signOut()`

Removes user session from the device.

```js
signOut = async () => {
  try {
    await GoogleOneTapSignIn.signOut();
    this.setState({ user: null }); // Remember to remove the user from your app's state as well
  } catch (error) {
    console.error(error);
  }
};
```

#### `hasPlayServices(options)`

Checks if device has Google Play Services installed. Always resolves to true on iOS.

Presence of up-to-date Google Play Services is required to show the sign in modal, but it is _not_ required to perform calls to `configure` and `signInSilently`. Therefore, we recommend to call `hasPlayServices` directly before `signIn`.

```js
try {
  await GoogleOneTapSignIn.hasPlayServices({ showPlayServicesUpdateDialog: true });
  // google services are available
} catch (err) {
  console.error('play services are not available');
}
```

`hasPlayServices` accepts one parameter, an object which contains a single key: `showPlayServicesUpdateDialog` (defaults to `true`). When `showPlayServicesUpdateDialog` is set to true the library will prompt the user to take action to solve the issue, as seen in the figure below.

You may also use this call at any time to find out if Google Play Services are available and react to the result as necessary.

[![prompt install](img/prompt-install.png)](#prompt-install)

#### `statusCodes`

These are useful when determining which kind of error has occured during sign in process. Import `statusCodes` along with `GoogleSignIn`. Under the hood these constants are derived from native GoogleSignIn error codes and are platform specific. Always prefer to compare `error.code` to `statusCodes.SIGN_IN_CANCELLED` or `statusCodes.IN_PROGRESS` and not relying on raw value of the `error.code`.

| Name                          | Description                                                                                                                                                                                                                                                                                                                                                               |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SIGN_IN_CANCELLED`           | When user cancels the sign in flow                                                                                                                                                                                                                                                                                                                                        |
| `IN_PROGRESS`                 | Trying to invoke another operation (eg. `signInSilently`) when previous one has not yet finished. If you call eg. `signInSilently` twice, 2 calls to `signInSilently` in the native module will be done. The promise from the first call to `signInSilently` will be rejected with this error, and the second will resolve / reject with the result of the native module. |
| `SIGN_IN_REQUIRED`            | Useful for use with `signInSilently()` - no user has signed in yet                                                                                                                                                                                                                                                                                                        |
| `PLAY_SERVICES_NOT_AVAILABLE` | Play services are not available or outdated, this can only happen on Android                                                                                                                                                                                                                                                                                              |

[Example how to use `statusCodes`](#signin).

### 2. GoogleOneTapSignInButton

![signin button](img/signin-button.png)

```js
import { GoogleOneTapSignIn, GoogleOneTapSignInButton } from 'react-native-google-one-tap-signin';

render() {
  <GoogleSigninButton
    style={{ width: 192, height: 48 }}
    size={GoogleOneTapSignInButton.Size.Wide}
    color={GoogleOneTapSignInButton.Color.Dark}
    onPress={this._signIn}
    disabled={this.state.isSigninInProgress} />
}
```

#### Props

##### `size`

Possible values:

- Size.Icon: display only Google icon. Recommended size of 48 x 48.
- Size.Standard: icon with 'Sign in'. Recommended size of 230 x 48.
- Size.Wide: icon with 'Sign in with Google'. Recommended size of 312 x 48.

Default: `Size.Standard`. Given the `size` prop you pass, we'll automatically apply the recommended size, but you can override it by passing the style prop as in `style={{ width, height }}`.

##### `color`

Possible values:

- Color.Dark: apply a blue background
- Color.Light: apply a light gray background

##### `disabled`

Boolean. If true, all interactions for the button are disabled.

##### `onPress`

Handler to be called when the user taps the button

##### [Inherited `View` props...](https://facebook.github.io/react-native/docs/view#props)

### 3. `userInfo`

Example `userInfo` which is returned after successful sign in.

```
{
  idToken: string,
  serverAuthCode: string,
  scopes: Array<string>, // on iOS this is empty array if no additional scopes are defined
  user: {
    id: string,
    name: string // full name
    givenName: string,
    familyName: string,
    password: string,
    photo: string, // url
  }
}
```

## Want to contribute?

Check out the [contributor guide](docs/CONTRIBUTING.md)!

## Notes

Calling the methods exposed by this package may involve remote network calls and you should thus take into account that such calls may take a long time to complete (eg. in case of poor network connection).

**idToken Note**: idToken is not null only if you specify a valid `webClientId`. `webClientId` corresponds to your server clientID on the developers console. It **HAS TO BE** of type **WEB**

## Additional scopes

The default requested scopes are `email` and `profile`.

If you want to manage other data from your application (for example access user agenda or upload a file to drive) you need to request additional permissions. This can be accomplished by adding the necessary scopes when configuring the GoogleOneTapSignIn instance.

Please visit https://developers.google.com/identity/protocols/googlescopes or https://developers.google.com/oauthplayground/ for a list of available scopes.

## Troubleshooting

Please see the troubleshooting section in the [Android guide](docs/android-guide.md) and [iOS guide](docs/ios-guide.md).

## Licence

(MIT)
