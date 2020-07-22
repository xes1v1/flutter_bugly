package com.crazecoder.flutterbugly;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.crazecoder.flutterbugly.bean.BuglyInitResultInfo;
import com.crazecoder.flutterbugly.callback.UpgradeCallback;
import com.crazecoder.flutterbugly.utils.JsonUtil;
import com.crazecoder.flutterbugly.utils.MapUtil;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.crashreport.CrashReport;

import java.util.Map;

import io.flutter.BuildConfig;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterBuglyPlugin
 */
public class FlutterBuglyPlugin implements FlutterPlugin, MethodCallHandler {
    public static BuglyConfig config;
    private Result result;
    private boolean isResultSubmitted = false;
    private UpgradeInfo upgradeInfo;
    private static UpgradeCallback callback;

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "crazecoder/flutter_bugly");
        FlutterBuglyPlugin plugin = new FlutterBuglyPlugin();
        channel.setMethodCallHandler(plugin);
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        final MethodChannel channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "crazecoder/flutter_bugly");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

    }

    @Override
    public void onMethodCall(final MethodCall call, final Result result) {
        isResultSubmitted = false;
        this.result = result;
        if (call.method.equals("initBugly")) {
            if (call.hasArgument("appId")) {
                if (call.hasArgument("autoInit")) {
                    Beta.autoInit = false;
                }
                if (call.hasArgument("enableHotfix")) {
                    Beta.enableHotfix = call.argument("enableHotfix");
                }
                if (call.hasArgument("autoCheckUpgrade")) {
                    Beta.autoCheckUpgrade = call.argument("autoCheckUpgrade");
                }
                if (call.hasArgument("autoDownloadOnWifi")) {
                    Beta.autoDownloadOnWifi = call.argument("autoDownloadOnWifi");
                }
                if (call.hasArgument("initDelay")) {
                    int delay = call.argument("initDelay");
                    Beta.initDelay = delay * 1000;
                }
                if (call.hasArgument("enableNotification")) {
                    Beta.enableNotification = call.argument("enableNotification");
                }
                if (call.hasArgument("upgradeCheckPeriod")) {
                    int period = call.argument("upgradeCheckPeriod");
                    Beta.upgradeCheckPeriod = period * 1000;
                }
                if (call.hasArgument("showInterruptedStrategy")) {
                    Beta.showInterruptedStrategy = call.argument("showInterruptedStrategy");
                }
                if (call.hasArgument("canShowApkInfo")) {
                    Beta.canShowApkInfo = call.argument("canShowApkInfo");
                }
//                Beta.canShowUpgradeActs.add(activity.getClass());
//                /*在application中初始化时设置监听，监听策略的收取*/
//                Beta.upgradeListener = new UpgradeListener() {
//                    @Override
//                    public void onUpgrade(int ret, UpgradeInfo strategy, boolean isManual, boolean isSilence) {
//                        if (callback != null) {
//                            callback.onUpgrade(strategy);
//                        }
//                    }
//                };
                String appId = call.argument("appId").toString();
                Bugly.init(config.context, appId, BuildConfig.DEBUG, config.strategy);
                if (call.hasArgument("channel")) {
                    String channel = call.argument("channel");
                    if (!TextUtils.isEmpty(channel))
                        Bugly.setAppChannel(config.context, channel);
                }
                result(getResultBean(true, appId, "Bugly 初始化成功"));
            } else {
                result(getResultBean(false, null, "Bugly appId不能为空"));
            }
        } else if (call.method.equals("setAppChannel")) {
            if (call.hasArgument("channel")) {
                String channel = call.argument("channel");
                Bugly.setAppChannel(config.context, channel);
            }
            result(null);
        } else if (call.method.equals("setUserId")) {
            if (call.hasArgument("userId")) {
                String userId = call.argument("userId");
                Bugly.setUserId(config.context, userId);
            }
            result(null);
        } else if (call.method.equals("setUserTag")) {
            if (call.hasArgument("userTag")) {
                Integer userTag = call.argument("userTag");
                if (userTag != null)
                    Bugly.setUserTag(config.context, userTag);
            }
            result(null);
        } else if (call.method.equals("putUserData")) {
            if (call.hasArgument("key") && call.hasArgument("value")) {
                String userDataKey = call.argument("key");
                String userDataValue = call.argument("value");
                Bugly.putUserData(config.context, userDataKey, userDataValue);
            }
            result(null);
        } else if (call.method.equals("checkUpgrade")) {
            boolean isManual = false;
            boolean isSilence = false;
            boolean useCache = true;
            if (call.hasArgument("isManual")) {
                isManual = call.argument("isManual");
            }
            if (call.hasArgument("isSilence")) {
                isSilence = call.argument("isSilence");
            }
            if (call.hasArgument("useCache")) {
                useCache = call.argument("useCache");
            }
            final boolean finalUseCache = useCache;
            callback = new UpgradeCallback() {
                @Override
                public void onUpgrade(UpgradeInfo strategy) {
                    if (finalUseCache) {
                        if (strategy != null) {
                            upgradeInfo = strategy;
                        }
                        result(upgradeInfo);
                    } else {
                        result(strategy);
                    }
                }
            };
            Beta.checkUpgrade(isManual, isSilence);
        } else if (call.method.equals("getUpgradeInfo")) {
            UpgradeInfo strategy = Beta.getUpgradeInfo();
            result(strategy);
        } else if (call.method.equals("postCatchedException")) {
            postException(call);
            result(null);
        } else {
            result.notImplemented();
            isResultSubmitted = true;
        }

    }

    private void postException(MethodCall call) {
        String message = "";
        String detail = null;
        Map<String, String> map = null;
        if (call.hasArgument("crash_message")) {
            message = call.argument("crash_message");
        }
        if (call.hasArgument("crash_detail")) {
            detail = call.argument("crash_detail");
        }
        if (TextUtils.isEmpty(detail)) return;
        if (call.hasArgument("crash_data")) {
            map = call.argument("crash_data");
        }
        CrashReport.postException(8, "Flutter Exception", message, detail, map);
    }

    private void result(Object object) {
        if (result != null && !isResultSubmitted) {
            if (object == null) {
                result.success(null);
            } else {
                result.success(JsonUtil.toJson(MapUtil.deepToMap(object)));
            }
            isResultSubmitted = true;
        }
    }

    private BuglyInitResultInfo getResultBean(boolean isSuccess, String appId, String msg) {
        BuglyInitResultInfo bean = new BuglyInitResultInfo();
        bean.setSuccess(isSuccess);
        bean.setAppId(appId);
        bean.setMessage(msg);
        return bean;
    }


}