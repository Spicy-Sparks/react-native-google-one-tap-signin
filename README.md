# react-native-mediastore

React Native Google One Tip Signin (only Android)

## Installation

```sh
npm install react-native-google-one-tap-signin
```

```sh
yarn add react-native-google-one-tap-signin
```

## Usage

```js

import GoogleOneTapSignIn from "react-native-google-one-tap-signin";

const handleSignIn = async () => {
  try {
    await GoogleOneTapSignIn.configure({
      webClientId: GOOGLE_ONE_TAP_SIGN_IN,
    })
    
    const userInfo = await GoogleOneTapSignIn.signIn()

    if (!userInfo || !userInfo.idToken) {
      console.error('GoogleOneTapSignIn error', 'There was some issue with getting id token', userInfo)
      return
    }

    // YOUR USER HERE
    // You can store this value in store or sign in to your backend
    console.log(userInfo)

  } catch (error) {
    // We might want to provide this error information to an error reporting service
    console.error('GoogleOneTapSignIn error', error)
  }
}

```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
