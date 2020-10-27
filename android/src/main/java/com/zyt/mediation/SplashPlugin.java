package com.zyt.mediation;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.zyt.med.internal.splash.SplashAdListener;

import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import mobi.android.SplashAd;

/**
 * @author ling.zhang
 * date 2020/8/6 3:27 PM
 * description:
 */
public class SplashPlugin extends BasePlugin {
    private BinaryMessenger binaryMessenger;

    public SplashPlugin(BinaryMessenger binaryMessenger) {
        this.binaryMessenger = binaryMessenger;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            case M_SPLASH_LOAD_AD:
                loadAd(call, result);
                break;
            default:
                result.notImplemented();
        }
    }

    public void loadAd(MethodCall call, MethodChannel.Result result) {
        final String adUnitId = call.argument(A_AD_UNIT_ID);
        Integer channelId = call.argument(A_CHANNEL_ID);
        if (TextUtils.isEmpty(adUnitId)) {
            result.error("error", "adUnitId is empty", null);
            return;
        }

        MethodChannel adChannel = null;
        if (channelId != null && binaryMessenger != null) {
            adChannel = new MethodChannel(binaryMessenger, P_SPLASH + channelId);
        }
        final MethodChannel finalAdChannel = adChannel;
        SplashAd.loadAd(null, adUnitId, new SplashAdListener() {
            private ViewGroup parentViewGroup;

            @Override
            public void onAdLoaded(SplashAdResponse splashAdResponse) {
                Activity activity = ComponenttHodler.getActivity();
                if (activity != null) {
                    View contentView = activity.findViewById(android.R.id.content);
                    if (contentView instanceof ViewGroup) {
                        parentViewGroup = new FrameLayout(contentView.getContext());
                        ((ViewGroup) contentView).addView(parentViewGroup);
                        splashAdResponse.show(parentViewGroup);
                    }
                }
            }

            @Override
            public void onAdShow() {
                if (finalAdChannel != null) {
                    finalAdChannel.invokeMethod(C_SPLASH_ON_AD_SHOW, MapBuilder.of(A_AD_UNIT_ID, adUnitId));
                }
            }

            @Override
            public void onAdDismiss() {
                removeView(parentViewGroup);

                if (finalAdChannel != null) {
                    finalAdChannel.invokeMethod(C_SPLASH_ON_AD_CLOSE, MapBuilder.of(A_AD_UNIT_ID, adUnitId));
                }
            }

            @Override
            public void onAdClicked() {
                if (finalAdChannel != null) {
                    finalAdChannel.invokeMethod(C_SPLASH_ON_AD_CLICK, MapBuilder.of(A_AD_UNIT_ID, adUnitId));
                }
            }

            @Override
            public void onError(String s, String s1) {
                removeView(parentViewGroup);
                if (finalAdChannel != null) {
                    Map<String, String> argMap = new MapBuilder<String, String>()
                            .put(A_AD_UNIT_ID, s)
                            .put(A_ERR_MSG, s1)
                            .build();
                    finalAdChannel.invokeMethod(C_SPLASH_ON_ERROR, argMap);
                }
            }
        });
        result.success(null);
    }

    private void removeView(ViewGroup viewGroup) {
        if (viewGroup != null) {
            viewGroup.removeAllViews();
            ViewParent parent = viewGroup.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(viewGroup);
            }
        }
    }
}