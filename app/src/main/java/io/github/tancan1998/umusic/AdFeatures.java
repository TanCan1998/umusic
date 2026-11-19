package io.github.tancan1998.umusic;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Filters QQ Music's advertising paths at their own request, eligibility, or binding boundaries. */
final class AdFeatures {
    private static final String QQ = "com.tencent.qqmusic.";
    private static final String SONG = "com.tencent.qqmusicplayerprocess.songinfo.SongInfo";
    private static final String STYLE = QQ + "business.playernew.interactor.playerstyle.PlayerStyle";
    private static final String LAUNCH = QQ + "business.ad.vipearningmode.manager.k";
    private static final String TIP_MANAGER = QQ + "minibarviptips.MinibarTipManager";
    private static final String TIP_VIEW = QQ + "minibarviptips.view.MinibarTopTipView";
    private static final String COMMON_TIP_VIEW = QQ + "minibarviptips.view.MinibarTopVipCommTipView";
    private static final String TIP_PACKET = "com.tencent.jce.superSound.MinibarTip";
    private static final String INSERT = QQ + "business.ad.freemode.manager.FreeModePlayerAdManager";
    private static final String AUDIO = QQ + "business.ad.media.i";
    private static final String RESPONSE = QQ + "dialog.universal.response.d";
    private static final String RESOURCE = "com.tme.operationpointplatform.m$a";
    private static final String SIGN = "sign_promo";
    private static final String MINI = "minibar_promo";
    private static final String PLAYER = "player_ads";
    private static final String HOME = "home_promo";
    private static final String AUDIO_AD = "audio_ads";
    private static final long COIN_SIGN_RESOURCE = 108102L;
    private static final ThreadLocal<Object> blockedResponse = new ThreadLocal<>();

    private AdFeatures() {}

    static void install(Entry module, ClassLoader loader) {
        installSignPopup(module, loader);
        skip("earning_promo", QQ + "business.ad.vipearningmode.q0", "u0", void.class, null);

        installMinibarPromotions(module, loader);

        skip(PLAYER, INSERT, "K", void.class, null, STYLE, "int");
        skip(PLAYER, INSERT, "v", boolean.class, false, SONG, "boolean", "boolean");
        skip(PLAYER, INSERT, "e0", boolean.class, false, "int", SONG, STYLE);
        skip(PLAYER, INSERT, "d0", void.class, null, ViewGroup.class.getName(), SONG, "int");
        skip(PLAYER, QQ + "business.ad.player.PlayerAdControl", "e", boolean.class, false,
                SONG, QQ + "business.ad.player.PlayerAdControl$AdType");
        String eggs = QQ + "business.ad.easterEgg.h";
        skip(PLAYER, eggs, "d", boolean.class, false, SONG);
        skip(PLAYER, eggs, "h", boolean.class, false, SONG);
        installRotateRequest(module, loader);
        skip(PLAYER, QQ + "modular.module.musichall.frames.radar.ad.k", "p", boolean.class, false);

        String banners = QQ + "business.ad.platformAd.BannerManager";
        String adItem = QQ + "business.ad.platformAd.g";
        skip(HOME, banners, "u", void.class, null);
        skip(HOME, banners, "p", adItem, null);
        skip(HOME, banners, "r", adItem, null, String.class.getName());
        installHomeBanners(module);

        skip("recognize_ads", QQ + "recognizekt.RecognizeActivity$j", "innerShow", void.class, null, "boolean");
        skip("car_ads", QQ + "business.ad.carAd.w", "p", boolean.class, false);
        skip("car_ads", QQ + "business.ad.carAd.w", "t", void.class, null, Activity.class.getName());
        skip("car_ads", QQ + "business.ad.carAd.w", "r", void.class, null,
                Activity.class.getName(), "java.util.List", String.class.getName());

        skip(AUDIO_AD, QQ + "abtest.abtester.MusicRadioAudioAdABTester", "getRequestAd", boolean.class, false);
        skip(AUDIO_AD, AUDIO, "h", boolean.class, false, SONG, QQ + "business.ad.media.request.b");
        skip(AUDIO_AD, AUDIO, "r", SONG, null, SONG);
        skip(AUDIO_AD, AUDIO, "G", boolean.class, false, SONG);

        installSearchHint(module);
        installHomeTabs(module);
        skip(HOME, QQ + "ui.MainDeskTitleTabView", "r0", void.class, null, "int");
    }

    private static void installSignPopup(Entry module, ClassLoader loader) {
        skip(SIGN, LAUNCH, "G", void.class, null, LAUNCH + "$a");
        skip(SIGN, LAUNCH, "D", void.class, null, LAUNCH + "$a", "int");
        module.expected(SIGN);
        try {
            Method getResourceId = method(loader, "com.tme.operationpointplatform.m", "h", long.class);
            Class<?> responseType = Class.forName(RESPONSE, false, loader);
            Method enabled = method(loader, RESPONSE, "b", int.class);
            Method consume = method(loader, QQ + "dialog.universal.s", "B", void.class,
                    responseType, boolean.class, String.class, int.class,
                    Class.forName("com.tencent.qqmusiccommon.xray.a$a", false, loader),
                    Class.forName("com.tme.operationpointplatform.trace.i", false, loader),
                    Class.forName("com.tencent.qqmusiccommon.xray.dialog.b", false, loader),
                    Class.forName(RESOURCE, false, loader));
            XposedInterface.HookHandle gate = bind(module, enabled, "startup-response-disabled", chain -> {
                return chain.getThisObject() == blockedResponse.get() ? 0 : chain.proceed();
            });
            try {
                bind(module, consume, "startup-response-resource", chain -> {
                    module.entered(SIGN);
                    if (!module.enabled(SIGN)) return chain.proceed();
                    Object resource = chain.getArg(7);
                    if (resource == null || ((Long) getResourceId.invoke(resource)) != COIN_SIGN_RESOURCE
                            || chain.getArg(0) == null) return chain.proceed();
                    Object previous = blockedResponse.get();
                    blockedResponse.set(chain.getArg(0));
                    try {
                        Object result = chain.proceed();
                        module.hit(SIGN);
                        return result;
                    } finally {
                        if (previous == null) blockedResponse.remove();
                        else blockedResponse.set(previous);
                    }
                });
            } catch (Throwable error) {
                gate.unhook();
                throw error;
            }
            module.installed(SIGN);
        } catch (Throwable error) {
            module.warning(SIGN, "startup popup resource 108102", error);
        }
    }

    private static void installMinibarPromotions(Entry module, ClassLoader loader) {
        String currentData = QQ + "minibarviptips.data.o";
        boolean current = UiFeatures.hasMethod(TIP_VIEW, "Y", boolean.class, currentData);
        String data = current ? currentData : QQ + "minibarviptips.data.n";
        String effect = QQ + "minibarviptips.data." + (current ? "i0" : "h0");
        skip(MINI, effect, "I", void.class, null, SONG);
        skip(MINI, effect, "N", void.class, null, SONG, TIP_PACKET);
        skip(MINI, effect, "O", void.class, null, SONG, TIP_PACKET);
        skip(MINI, effect, "s", data, null, SONG);
        skip(MINI, TIP_MANAGER, current ? "H" : "F", boolean.class, false, SONG);

        String dispatch = current ? "U0" : "M0";
        UiFeatures.register(MINI, TIP_MANAGER, dispatch, void.class, chain -> {
            Object tip = chain.getArg(1);
            if (module.enabled(MINI) && (isTopPromotion(tip, current) || isCommonPromotion(tip))) {
                module.entered(MINI);
                module.hit(MINI);
                return null;
            }
            return chain.proceed();
        }, SONG, data, "boolean", "int");
        UiFeatures.register(MINI, TIP_VIEW, "Y", boolean.class, chain -> {
            if (module.enabled(MINI) && isTopPromotion(chain.getArg(0), current)) {
                module.entered(MINI);
                module.hit(MINI);
                return false;
            }
            return chain.proceed();
        }, data);
        UiFeatures.register(MINI, COMMON_TIP_VIEW, "E", boolean.class, chain -> {
            if (module.enabled(MINI) && isCommonPromotion(chain.getArg(0))) {
                module.entered(MINI);
                module.hit(MINI);
                return false;
            }
            return chain.proceed();
        }, data, SONG);
        UiFeatures.register(MINI, TIP_VIEW, "r0", void.class, chain -> {
            Object view = chain.getThisObject();
            boolean promo = isTopPromotion(chain.getArg(0), current);
            if (!promo) UiFeatures.forget(view);
            Object result = chain.proceed();
            if (promo) UiFeatures.hide(view, MINI);
            return result;
        }, data, SONG);
        UiFeatures.register(MINI, COMMON_TIP_VIEW, "N", void.class, chain -> {
            Object view = chain.getThisObject();
            boolean promo = isCommonPromotion(chain.getArg(0));
            if (!promo) UiFeatures.forget(view);
            Object result = chain.proceed();
            if (promo) UiFeatures.hide(view, MINI);
            return result;
        }, data, SONG);

        deoptimizeCaller(module, loader, TIP_VIEW, "W", SONG, data, String.class.getName(), "long", "long");
        deoptimizeCaller(module, loader, TIP_VIEW, "d", data);
        deoptimizeCaller(module, loader, TIP_VIEW, "f", SONG, data);
        deoptimizeCaller(module, loader, COMMON_TIP_VIEW, "d", data);
        deoptimizeCaller(module, loader, COMMON_TIP_VIEW, "f", SONG, data);
        deoptimizeCaller(module, loader, TIP_MANAGER, current ? "V0" : "N0",
                TIP_MANAGER, SONG, data, "boolean", "int", "int", Object.class.getName());
    }

    private static boolean isTopPromotion(Object tip, boolean current) throws Throwable {
        if (tip == null) return false;
        int type = (Integer) UiFeatures.invoke(tip, current ? "z" : "y");
        return type == 37 || type == 38 || type == 48 || promotionalTip(UiFeatures.invoke(tip, "b"))
                || promotionalTip(UiFeatures.invoke(tip, "s"));
    }

    private static boolean isCommonPromotion(Object tip) throws Throwable {
        if (tip == null) return false;
        Object material = UiFeatures.field(UiFeatures.invoke(tip, "a"), "material");
        return promotionalTip(UiFeatures.field(material, "text"));
    }

    static boolean promotionalTip(Object text) {
        if (!(text instanceof CharSequence)) return false;
        String value = text.toString();
        return (value.contains("搭配") && value.contains("音效") && value.contains("立即开启"))
                || value.contains("专属人声细节") || value.contains("声音魔法")
                || value.contains("用你的声音唱这首歌") || value.contains("AI生成你的演唱版")
                || value.contains("AI帮你生成");
    }

    private static void deoptimizeCaller(Entry module, ClassLoader loader, String owner, String name, String... args) {
        try {
            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                types[i] = switch (args[i]) {
                    case "boolean" -> boolean.class;
                    case "int" -> int.class;
                    case "long" -> long.class;
                    default -> Class.forName(args[i], false, loader);
                };
            }
            module.deoptimize(Class.forName(owner, false, loader).getDeclaredMethod(name, types));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Optional caller optimization on compatible frameworks.
        }
    }

    private static void installRotateRequest(Entry module, ClassLoader loader) {
        module.expected(PLAYER);
        try {
            Class<?> observable = Class.forName("rx.d", false, loader);
            Method just = method(loader, "rx.d", "a0", observable, Object.class);
            Method request = method(loader, QQ + "business.playercommon.normalplayer.optimized.ad.a",
                    "c", observable, Class.forName("com.tencent.qqmusicplayerprocess.network.w", false, loader));
            bind(module, request, "player-rotate-no-ad", chain -> {
                module.entered(PLAYER);
                if (!module.enabled(PLAYER)) return chain.proceed();
                Object noAd = just.invoke(null, new Object[]{null});
                module.hit(PLAYER);
                return noAd;
            });
            module.installed(PLAYER);
        } catch (Throwable error) {
            module.warning(PLAYER, "player rotate no-ad response", error);
        }
    }

    private static void installHomeBanners(Entry module) {
        UiFeatures.register(HOME, QQ + "homepage.header.v", "v", void.class, chain -> {
            module.entered(HOME);
            if (!module.enabled(HOME) || !(chain.getArg(1) instanceof List<?> items)) return chain.proceed();
            List<Object> retained = new ArrayList<>(items.size());
            for (Object item : items) {
                if (item == null || !promotionalBanner((Integer) UiFeatures.invoke(item, "d"))) retained.add(item);
            }
            if (retained.size() == items.size()) return chain.proceed();
            module.hit(HOME);
            return chain.proceed(new Object[]{chain.getArg(0), retained, chain.getArg(2)});
        }, QQ + "homepage.header.m2", "java.util.List", "boolean");
    }

    static boolean promotionalBanner(int businessType) {
        return switch (businessType) {
            case 2, 3, 7, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 24 -> true;
            default -> false;
        };
    }

    private static void installSearchHint(Entry module) {
        String key = "home_search_hint";
        UiFeatures.register(key, QQ + "ui.SearchInputAnimationView", "setHint", void.class, chain -> {
            module.entered(key);
            if (!module.enabled(key)) return chain.proceed();
            module.hit(key);
            return chain.proceed(new Object[]{"搜索", null, false});
        }, String.class.getName(), String.class.getName(), "boolean");
    }

    private static void installHomeTabs(Entry module) {
        String tabs = QQ + "ui.MainDeskTitleTabView";
        String badgeKey = "home_coin_badge";
        String tabKey = "home_extra_tabs";
        UiFeatures.register(badgeKey, tabs, "a2", void.class, chain -> {
            Object result = chain.proceed();
            View item = (View) chain.getArg(0);
            Object data = chain.getArg(1);
            if (item == null || data == null) return result;
            String title = (String) UiFeatures.invoke(data, "q");
            for (int id : new int[]{0x7f094e62, 0x7f094298}) {
                View badge = item.findViewById(id);
                if ("金币".equals(title)) UiFeatures.hide(badge, badgeKey);
                else UiFeatures.forget(badge);
            }
            return result;
        }, View.class.getName(), tabs + "$TabItem", "boolean", "boolean");
        // Mark only the actual tab view. Keep the host's page indices and navigation data intact.
        UiFeatures.register(tabKey, tabs, "h0", void.class, chain -> {
            Object result = chain.proceed();
            View item = (View) chain.getArg(0);
            Object data = chain.getArg(1);
            if (item == null || data == null) return result;
            String title = (String) UiFeatures.invoke(data, "q");
            if ("金币".equals(title) || "AI作歌".equals(title) || "刷歌".equals(title)) {
                UiFeatures.hide(item, tabKey);
            } else {
                UiFeatures.forget(item);
            }
            return result;
        }, View.class.getName(), tabs + "$TabItem", "boolean");
    }

    private static Entry module() {
        return UiFeatures.module();
    }

    private static void skip(String feature, String owner, String name, Object returnType, Object value, String... args) {
        UiFeatures.register(feature, owner, name, returnType, chain -> {
            Entry module = module();
            module.entered(feature);
            if (!module.enabled(feature)) return chain.proceed();
            module.hit(feature);
            return value;
        }, args);
    }

    private static Method method(ClassLoader loader, String owner, String name, Class<?> type, Class<?>... args)
            throws ReflectiveOperationException {
        Method method = Class.forName(owner, false, loader).getDeclaredMethod(name, args);
        if (method.getReturnType() != type) throw new NoSuchMethodException(owner + "#" + name + " return type");
        method.setAccessible(true);
        return method;
    }

    private static XposedInterface.HookHandle bind(Entry module, Method method, String id,
                                                  XposedInterface.Hooker hooker) {
        XposedInterface.HookHandle handle = module.hook(method).setId("umusic-" + id)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(hooker);
        try {
            module.deoptimize(method);
        } catch (RuntimeException ignored) {
            // Registration still works on frameworks where deoptimization is unavailable.
        }
        return handle;
    }
}
