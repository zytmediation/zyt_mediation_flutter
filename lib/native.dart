import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:zyt_mediation/call_back.dart';
import 'package:zyt_mediation/screen_util.dart';

import 'constants.dart';

class NativeAd extends StatefulWidget {
  final String _adUnitId;
  NativeCallBack nativeCallBack;

  NativeAd(this._adUnitId, {this.nativeCallBack});

  @override
  State<StatefulWidget> createState() {
    return NativeAdState(_adUnitId, nativeCallBack: nativeCallBack);
  }
}

class NativeAdState extends State<NativeAd> {
  MethodChannel _adChannel;
  String _adUnitId;
  double _width = ScreenUtil.SCREEN_WIDTH;
  double _height = ScreenUtil.SCREEN_HEIGHT;
  bool _show = false;
  NativeCallBack nativeCallBack;

  NativeAdState(this._adUnitId, {this.nativeCallBack});

  @override
  Widget build(BuildContext context) {
    return buildPlatformWidget();
  }

  Widget buildPlatformWidget() {
    return Offstage(
        offstage: !_show,
        child: Container(
            alignment: Alignment.center,
            width: _width,
            height: _height,
            child: Center(
                child: defaultTargetPlatform == TargetPlatform.android
                    ? buildAndroidView(_adUnitId)
                    : buildUIKitView(_adUnitId))));
  }

  buildAndroidView(String adUnitId) {
    return AndroidView(
      viewType: Constants.V_NATIVE,
      creationParams: {
        Constants.A_AD_UNIT_ID: _adUnitId,
      },
      onPlatformViewCreated: _onPlatformViewCreated,
      creationParamsCodec: const StandardMessageCodec(),
    );
  }

  buildUIKitView(_adUnitId) {
    return UiKitView();
  }

  _onPlatformViewCreated(int id) {
    _adChannel = MethodChannel(Constants.P_NATIVE + "/$id");
    _adChannel.setMethodCallHandler((call) {
      switch (call.method) {
        case Constants.C_ON_LAYOUT_CHANGE:
          var width = call.arguments[Constants.WIDTH];
          var height = call.arguments[Constants.HEIGHT];
          if (width > 0 && height > 0) {
            _width = ScreenUtil.px2dp(width);
            _height = ScreenUtil.px2dp(height);
            _show = true;
            setState(() {});
          }
          break;
        case Constants.C_NATIVE_ON_AD_LOADED:
          if (nativeCallBack != null && nativeCallBack.onLoaded != null) {
            nativeCallBack.onLoaded(call.arguments[Constants.A_AD_UNIT_ID]);
          }
          break;
        case Constants.C_NATIVE_ON_AD_CLICK:
          if (nativeCallBack != null && nativeCallBack.onAdClick != null) {
            nativeCallBack.onAdClick(call.arguments[Constants.A_AD_UNIT_ID]);
          }
          break;
        case Constants.C_NATIVE_ON_AD_CLOSE:
          if (nativeCallBack != null && nativeCallBack.onClose != null) {
            nativeCallBack.onClose(call.arguments[Constants.A_AD_UNIT_ID]);
          }
          break;
        case Constants.C_NATIVE_ON_ERROR:
          if (nativeCallBack != null && nativeCallBack.onError != null) {
            nativeCallBack.onError(call.arguments[Constants.A_AD_UNIT_ID],
                call.arguments[Constants.A_ERR_MSG]);
          }
          break;
      }
      return null;
    });
  }

  @override
  void dispose() {
    if (_adChannel != null) {
      _adChannel.setMethodCallHandler(null);
      _adChannel = null;
    }
    super.dispose();
  }
}