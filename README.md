# react-native-mediastore

React Native Google One Tip Signin

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

// Save username and password
GoogleOneTapSignIn.savePassword('test@example.app', 'test')

// Prompt and login using saved credentials
GoogleOneTapSignIn.signIn()

// Logout from all saved credentials
GoogleOneTapSignIn.signOut()

// Delete specific credentials (iOS only)
GoogleOneTapSignIn.deletePassword('test@example.app', 'test')

```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
