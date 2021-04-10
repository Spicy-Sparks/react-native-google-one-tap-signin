import React, { PureComponent } from 'react';
import PropTypes from 'prop-types';

import {
  NativeModules,
  requireNativeComponent,
  ViewPropTypes,
  Platform,
  DeviceEventEmitter,
  StyleSheet,
} from 'react-native';

const IS_IOS = Platform.OS === 'ios';

const { RNGoogleOneTapSignIn } = NativeModules;
const RNGoogleOneTapSignInButton = requireNativeComponent('RNGoogleOneTapSignInButton', null);

export class GoogleOneTapSignInButton extends PureComponent {
  static propTypes = {
    ...ViewPropTypes,
    size: PropTypes.number,
    color: PropTypes.number,
    disabled: PropTypes.bool,
    onPress: PropTypes.func.isRequired,
  };

  static defaultProps = {
    size: RNGoogleOneTapSignIn.BUTTON_SIZE_STANDARD,
  };

  componentDidMount() {
    if (Platform.OS === 'android') {
      this._clickListener = DeviceEventEmitter.addListener('RNGoogleOneTapSignInButtonClicked', () => {
        this.props.onPress && this.props.onPress();
      });
    }
  }

  componentWillUnmount() {
    this._clickListener && this._clickListener.remove();
  }

  getRecommendedSize() {
    switch (this.props.size) {
      case RNGoogleOneTapSignIn.BUTTON_SIZE_ICON:
        return styles.iconSize;
      case RNGoogleOneTapSignIn.BUTTON_SIZE_WIDE:
        return styles.wideSize;
      default:
        return styles.standardSize;
    }
  }

  render() {
    const { style, ...props } = this.props;

    return <RNGoogleOneTapSignInButton style={[this.getRecommendedSize(), style]} {...props} />;
  }
}

const styles = StyleSheet.create({
  iconSize: {
    width: 48,
    height: 48,
  },
  standardSize: { width: 212, height: 48 },
  wideSize: { width: 312, height: 48 },
});

GoogleOneTapSignInButton.Size = IS_IOS ? {} : {
  Icon: RNGoogleOneTapSignIn.BUTTON_SIZE_ICON,
  Standard: RNGoogleOneTapSignIn.BUTTON_SIZE_STANDARD,
  Wide: RNGoogleOneTapSignIn.BUTTON_SIZE_WIDE,
};

GoogleOneTapSignInButton.Color = IS_IOS ? {} : {
  Auto: RNGoogleOneTapSignIn.BUTTON_COLOR_AUTO,
  Light: RNGoogleOneTapSignIn.BUTTON_COLOR_LIGHT,
  Dark: RNGoogleOneTapSignIn.BUTTON_COLOR_DARK,
};
