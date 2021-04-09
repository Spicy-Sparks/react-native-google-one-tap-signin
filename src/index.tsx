import { NativeModules } from 'react-native';

type GoogleOneTapSigninType = {
  multiply(a: number, b: number): Promise<number>;
};

const { GoogleOneTapSignin } = NativeModules;

export default GoogleOneTapSignin as GoogleOneTapSigninType;
