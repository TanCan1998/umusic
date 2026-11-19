#!/usr/bin/env python3
"""Exercise new ad hooks against narrow host contracts without a phone or network."""
import pathlib
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
baseline = ROOT / 'tools/test_ui_daily.py'
scope = {'__file__': str(baseline)}
exec(compile(baseline.read_text().split('\nwith tempfile.TemporaryDirectory(', 1)[0], str(baseline), 'exec'), scope)
files, src = scope['files'], scope['src']
files.pop('io/github/tancan1998/umusic/UiDailyTest.java')
files['android/view/View.java'] = files['android/view/View.java'].replace(
    'public int getId()', 'public <T extends View>T findViewById(int id){return null;}public int getId()')
src('android.app.Activity', 'package android.app; public class Activity extends android.content.Context {}')
src('com.tme.operationpointplatform.m', '''package com.tme.operationpointplatform;
public class m {
    private final long id;
    public m(long id) { this.id = id; }
    public long h() { return id; }
    public static class a extends m { public a(long id) { super(id); } }
}''')
src('com.tme.operationpointplatform.trace.i', 'package com.tme.operationpointplatform.trace; public class i {}')
# Model the DEX binary name directly because Java source disallows a nested type with the same simple name.
src('com.tencent.qqmusiccommon.xray.a$a', 'package com.tencent.qqmusiccommon.xray; public class a$a {}')
src('com.tencent.qqmusiccommon.xray.dialog.b', 'package com.tencent.qqmusiccommon.xray.dialog; public class b {}')
src('com.tencent.qqmusic.dialog.universal.response.d', '''package com.tencent.qqmusic.dialog.universal.response;
public class d { public int b() { return 1; } }''')
src('com.tencent.qqmusic.dialog.universal.s', '''package com.tencent.qqmusic.dialog.universal;
import com.tencent.qqmusic.dialog.universal.response.d;
import com.tme.operationpointplatform.m;
import com.tme.operationpointplatform.trace.i;
import com.tencent.qqmusiccommon.xray.a$a;
import com.tencent.qqmusiccommon.xray.dialog.b;
import io.github.tancan1998.umusic.Entry;
public class s {
    public int visible, released;
    public boolean fail;
    public void B(d response, boolean cold, String page, int scene, a$a parser, i trace, b caller, m.a resource) {
        int enabled = (Integer) Entry.call(response, d.class, "b", new Class<?>[0], new Object[0], ignored -> response.b());
        if (enabled == 1) visible++;
        if (fail) throw new IllegalStateException("simulated downstream failure");
        released++;
    }
}''')
src('rx.d', '''package rx;
public class d {
    public final Object value;
    private d(Object value) { this.value = value; }
    public static d a0(Object value) { return new d(value); }
}''')
src('com.tencent.qqmusic.business.playercommon.normalplayer.optimized.ad.a', '''
package com.tencent.qqmusic.business.playercommon.normalplayer.optimized.ad;
public class a {
    public rx.d c(com.tencent.qqmusicplayerprocess.network.w request) { return rx.d.a0(request); }
}''')
src('com.tencent.qqmusic.homepage.header.m2', 'package com.tencent.qqmusic.homepage.header; public class m2 {}')
src('com.tencent.qqmusic.homepage.header.v', '''package com.tencent.qqmusic.homepage.header;
import java.util.List;
public class v {
    public List<?> bound;
    public void v(m2 header, List<?> items, boolean resumed) { bound = items; }
}''')
src('com.tencent.qqmusicplayerprocess.songinfo.SongInfo', 'package com.tencent.qqmusicplayerprocess.songinfo; public class SongInfo {}')
src('com.tencent.jce.superSound.MinibarTip', 'package com.tencent.jce.superSound; public class MinibarTip {}')
src('com.tencent.qqmusic.minibarviptips.data.o', '''package com.tencent.qqmusic.minibarviptips.data;
public record o(int type, String title, Banner banner) {
    public int z() { return type; }
    public String b() { return title; }
    public String s() { return null; }
    public Banner a() { return banner; }
    public record Banner(Material material) {}
    public record Material(String text) {}
}''')
src('com.tencent.qqmusic.minibarviptips.data.i0', '''package com.tencent.qqmusic.minibarviptips.data;
import com.tencent.jce.superSound.MinibarTip;
import com.tencent.qqmusicplayerprocess.songinfo.SongInfo;
public class i0 {
    public int requests;
    public void I(SongInfo song) { requests++; }
    public void N(SongInfo song, MinibarTip tip) {}
    public void O(SongInfo song, MinibarTip tip) {}
    public o s(SongInfo song) { return new o(37, "音效推广", null); }
}''')
src('com.tencent.qqmusic.minibarviptips.MinibarTipManager', '''package com.tencent.qqmusic.minibarviptips;
import com.tencent.qqmusic.minibarviptips.data.o;
import com.tencent.qqmusicplayerprocess.songinfo.SongInfo;
public class MinibarTipManager {
    public int dispatched;
    public void U0(SongInfo song, o tip, boolean immediate, int source) { dispatched++; }
    public boolean H(SongInfo song) { return true; }
}''')
for short,gate,render in [('MinibarTopTipView','Y(o tip)','r0'),('MinibarTopVipCommTipView','E(o tip, SongInfo song)','N')]:
    src('com.tencent.qqmusic.minibarviptips.view.'+short, f'''package com.tencent.qqmusic.minibarviptips.view;
import com.tencent.qqmusic.minibarviptips.data.o;
import com.tencent.qqmusicplayerprocess.songinfo.SongInfo;
public class {short} extends android.view.View {{
    public boolean {gate} {{ return true; }}
    public void {render}(o tip, SongInfo song) {{ setVisibility(VISIBLE); }}
}}''')
src('io.github.tancan1998.umusic.AdFeaturesTest', '''package io.github.tancan1998.umusic;
import java.lang.reflect.Method;
import java.util.List;
import com.tencent.qqmusic.dialog.universal.response.d;
import com.tencent.qqmusic.dialog.universal.s;
import com.tencent.qqmusic.homepage.header.m2;
import com.tencent.qqmusic.homepage.header.v;
import com.tencent.qqmusicplayerprocess.network.w;
import com.tencent.qqmusicplayerprocess.songinfo.SongInfo;
import com.tencent.qqmusic.minibarviptips.MinibarTipManager;
import com.tencent.qqmusic.minibarviptips.data.i0;
import com.tencent.qqmusic.minibarviptips.data.o;
import com.tencent.qqmusic.minibarviptips.view.MinibarTopTipView;
import com.tencent.qqmusic.minibarviptips.view.MinibarTopVipCommTipView;
import com.tme.operationpointplatform.m;
public class AdFeaturesTest {
    public record Card(int type) { public int d() { return type; } }

    static void check(boolean value, String description) {
        if (!value) throw new AssertionError(description);
    }

    static Object call(Object owner, String name, Class<?>[] types, Object... arguments) throws Exception {
        Method method = owner.getClass().getDeclaredMethod(name, types);
        return Entry.call(owner, owner.getClass(), name, types, arguments, values -> method.invoke(owner, values));
    }

    static void popup(s owner, d response, long id) throws Exception {
        Class<?> parser = Class.forName("com.tencent.qqmusiccommon.xray.a$a");
        call(owner, "B", new Class<?>[]{d.class, boolean.class, String.class, int.class, parser,
             com.tme.operationpointplatform.trace.i.class, com.tencent.qqmusiccommon.xray.dialog.b.class, m.a.class},
             response, true, "", 1, null, null, null, new m.a(id));
    }

    public static void main(String[] args) throws Exception {
        Entry module = new Entry();
        ClassLoader loader = AdFeaturesTest.class.getClassLoader();
        UiFeatures.install(module, loader);
        AdFeatures.install(module, loader);
        s popupManager = new s();
        d response = new d();
        popup(popupManager, response, 108102);
        check(popupManager.visible == 0 && popupManager.released == 1, "only the signup popup follows the original disabled branch");
        popup(popupManager, response, 1);
        check(popupManager.visible == 1 && popupManager.released == 2, "unrelated popup passes through");
        popupManager.fail = true;
        try {
            popup(popupManager, response, 108102);
            throw new AssertionError("the original handler failure must propagate");
        } catch (RuntimeException expected) {
            check((Integer) call(response, "b", new Class<?>[0]) == 1, "response scope clears after failure");
        }
        popupManager.fail = false;
        module.toggles.put("sign_promo", false);
        popup(popupManager, response, 108102);
        check(popupManager.visible == 2 && popupManager.released == 3, "feature off restores signup popup");

        var ads = new com.tencent.qqmusic.business.playercommon.normalplayer.optimized.ad.a();
        w request = new w();
        Class<?>[] adSignature = {w.class};
        check(((rx.d) call(ads, "c", adSignature, request)).value == null, "rotation emits the native no-ad value");
        module.toggles.put("enabled", false);
        check(((rx.d) call(ads, "c", adSignature, request)).value == request, "master off restores rotary request");
        module.toggles.put("enabled", true);

        v home = new v();
        var album = new Card(1);
        var unknown = new Card(100);
        var input = List.of(album, new Card(7), new Card(16), unknown);
        call(home, "v", new Class<?>[]{m2.class, List.class, boolean.class}, new m2(), input, true);
        check(home.bound.equals(List.of(album, unknown)) && input.size() == 4, "home keeps album and unknown items without mutating input");
        check(AdFeatures.promotionalTip("搭配许嵩音效还原专属人声细节") &&
              AdFeatures.promotionalTip("声音魔法！AI生成你的演唱版") &&
              !AdFeatures.promotionalTip("播放队列") && !AdFeatures.promotionalTip(null), "scoped promotion wording");
        check(module.installed.get("minibar_promo") == 10, "all current minibar hooks register");
        var manager = new MinibarTipManager();
        var song = new SongInfo();
        var tipSignature = new Class<?>[]{o.class, SongInfo.class};
        var dispatchSignature = new Class<?>[]{SongInfo.class, o.class, boolean.class, int.class};
        var ordinary = new o(54, "播放队列", null);
        var effect = new o(37, "使用推荐音效", null);
        var singing = new o(48, "生成你的演唱版", null);
        var banner = new o(1000, null, new o.Banner(new o.Material("声音魔法！")));
        var fx = new i0();
        call(fx, "I", new Class<?>[]{SongInfo.class}, song);
        check(fx.requests == 0, "upstream sound promotion request blocked");
        for (var promo : List.of(effect, singing, banner))
            call(manager, "U0", dispatchSignature, song, promo, false, 0);
        call(manager, "U0", dispatchSignature, song, ordinary, false, 0);
        check(manager.dispatched == 1, "only ordinary tip reaches host dispatcher");
        var tipView = new MinibarTopTipView();
        check(!(Boolean) call(tipView, "Y", new Class<?>[]{o.class}, singing), "AI tip eligibility blocked");
        call(tipView, "r0", tipSignature, effect, song);
        check(tipView.getVisibility() == 8, "promotion binding hides view");
        call(tipView, "r0", tipSignature, ordinary, song);
        check(tipView.getVisibility() == 0, "recycled view restores ordinary tip");
        var common = new MinibarTopVipCommTipView();
        check(!(Boolean) call(common, "E", tipSignature, banner, song), "common promo eligibility blocked");
        module.toggles.put("minibar_promo", false);
        call(manager, "U0", dispatchSignature, song, effect, false, 0);
        check(manager.dispatched == 2, "minibar switch off restores host behavior");
        check((Boolean) call(common, "E", tipSignature, banner, song), "original common eligibility restored");
        System.out.println("PASS: signup popup, player ads, home banners, current minibar promotions and ordinary tip restoration");
    }
}''')

with tempfile.TemporaryDirectory(prefix='umusic-ad-contract-') as directory:
    work = pathlib.Path(directory)
    for name in ('UiFeatures', 'AdFeatures'):
        relative = f'io/github/tancan1998/umusic/{name}.java'
        files[relative] = (ROOT / 'app/src/main/java' / relative).read_text()
    for name, text in files.items():
        path = work / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
    classes = work / 'classes'
    result = subprocess.run(['javac', '-encoding', 'UTF-8', '-d', str(classes)] + [str(p) for p in work.rglob('*.java')],
                            capture_output=True, text=True)
    if result.returncode:
        raise SystemExit(result.stderr)
    subprocess.run(['java', '-cp', str(classes), 'io.github.tancan1998.umusic.AdFeaturesTest'], check=True)
