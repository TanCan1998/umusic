#!/usr/bin/env python3
"""Run the real MembershipFeatures against a minimal host/Hook contract on JVM."""
import pathlib,subprocess,tempfile
ROOT=pathlib.Path(__file__).resolve().parent.parent
files={}
def source(name,text):files[name.replace('.','/')+'.java']=text
source('android.app.Activity','package android.app;public class Activity {}')
source('io.github.libxposed.api.XposedInterface','''package io.github.libxposed.api;public interface XposedInterface {enum ExceptionMode{PROTECTIVE}interface Hooker{Object intercept(Chain c)throws Throwable;}interface Chain{Object proceed()throws Throwable;}interface HookHandle{void unhook();}interface HookBuilder{HookBuilder setId(String s);HookBuilder setExceptionMode(ExceptionMode m);HookHandle intercept(Hooker h);}}''')
source('io.github.tancan1998.umusic.Entry','''package io.github.tancan1998.umusic;
import java.lang.reflect.*;import java.util.*;import io.github.libxposed.api.XposedInterface.*;
public class Entry {
 public boolean master=true;public String failId;public final Map<String,Boolean> flags=new HashMap<>();
 public final Map<String,Integer> expected=new HashMap<>(),installed=new HashMap<>(),entered=new HashMap<>(),hits=new HashMap<>();public final Map<Method,Hooker> hooks=new HashMap<>();public final List<String> errors=new ArrayList<>();
 public boolean enabled(String k){return master&&flags.getOrDefault(k,true);}void expected(String k){expected.merge(k,1,Integer::sum);}void installed(String k){installed.merge(k,1,Integer::sum);}void entered(String k){entered.merge(k,1,Integer::sum);}void hit(String k){hits.merge(k,1,Integer::sum);}void warning(String k,String msg,Throwable t){errors.add(k+":"+msg);}void deoptimize(Method m){}
 public HookBuilder hook(Method m){return new HookBuilder(){String id;public HookBuilder setId(String s){id=s;return this;}public HookBuilder setExceptionMode(ExceptionMode mode){return this;}public HookHandle intercept(Hooker h){if(id.equals(failId))throw new IllegalStateException("registration failed");if(hooks.put(m,h)!=null)throw new AssertionError("duplicate method");return ()->hooks.remove(m);}};}
 void replace(String feature,String owner,String name,Class<?> ret,Object value,String...types){expected(feature);try{Class<?>[] sig=new Class[types.length];for(int i=0;i<sig.length;i++)sig[i]=types[i].equals("boolean")?boolean.class:Class.forName(types[i]);Method m=Class.forName(owner).getDeclaredMethod(name,sig);if(m.getReturnType()!=ret)throw new NoSuchMethodException();hook(m).setId(owner+name).intercept(c->{entered(feature);if(!enabled(feature))return c.proceed();hit(feature);return value;});installed(feature);}catch(Throwable t){warning(feature,owner+name,t);}}
 Object invoke(Object obj,Class<?> owner,String name,Class<?>[] sig,Object...args)throws Throwable{Method m=owner.getDeclaredMethod(name,sig);m.setAccessible(true);Hooker h=hooks.get(m);return h==null?m.invoke(obj,args):h.intercept(()->m.invoke(obj,args));}
}''')
source('com.tencent.qqmusic.business.user.UserHelper','''package com.tencent.qqmusic.business.user;public class UserHelper {public static boolean isVip(){return false;}public static boolean isGreenVip(){return false;}public static boolean isSuperGreen(){return false;}public static boolean isSuperVip(){return false;}}''')
source('com.tencent.qqmusic.business.user.a','''package com.tencent.qqmusic.business.user;public class a {public boolean n2(){return false;}public boolean L1(){return false;}public boolean i2(){return false;}public boolean j2(){return false;}}''')
source('com.tencent.qqmusic.dialog.universal.response.c','package com.tencent.qqmusic.dialog.universal.response;public class c {}')
source('com.tencent.qqmusiccommon.xray.dialog.b','package com.tencent.qqmusiccommon.xray.dialog;public class b {}')
source('com.tencent.qqmusic.dialog.vipremind.f','''package com.tencent.qqmusic.dialog.vipremind;public class f {public int shown;public void q(String s,com.tencent.qqmusic.dialog.universal.response.c c,String uuid,com.tencent.qqmusiccommon.xray.dialog.b b){shown++;}}''')
source('com.tencent.qqmusic.business.playernew.vipguide.s','''package com.tencent.qqmusic.business.playernew.vipguide;public class s {private boolean x(boolean b){return true;}}''')
source('com.tencent.qqmusicplayerprocess.audio.supersound.AepEffect','package com.tencent.qqmusicplayerprocess.audio.supersound;public class AepEffect {}')
source('com.tencent.qqmusic.activity.soundfx.supersound.kuikly.u6','''package com.tencent.qqmusic.activity.soundfx.supersound.kuikly;public class u6 {public boolean V(com.tencent.qqmusicplayerprocess.audio.supersound.AepEffect e,android.app.Activity a,String s){return false;}}''')
source('com.tencent.qqmusic.activity.soundfx.supersound.rights.g','''package com.tencent.qqmusic.activity.soundfx.supersound.rights;public abstract class g {public static final class d extends g {public d(){}}}''')
source('rx.d','''package rx;public class d {public final Object value;public d(Object v){value=v;}public static d a0(Object v){return new d(v);}}''')
source('com.tencent.qqmusic.activity.soundfx.supersound.rights.SoundEffectRightsManager','''package com.tencent.qqmusic.activity.soundfx.supersound.rights;public class SoundEffectRightsManager {public int originals;public rx.d R(int t,int id){originals++;return rx.d.a0(null);}public g e0(int t,int id){originals++;return null;}}''')
source('com.tencent.qqmusic.activity.soundfx.TestCaller','''package com.tencent.qqmusic.activity.soundfx;public class TestCaller {public static Object call(java.util.concurrent.Callable<Object> c)throws Exception{return c.call();}}''')
source('io.github.tancan1998.umusic.MembershipContractTest','''package io.github.tancan1998.umusic;
import com.tencent.qqmusic.business.user.*;import com.tencent.qqmusic.activity.soundfx.supersound.rights.*;import com.tencent.qqmusic.activity.soundfx.TestCaller;
public class MembershipContractTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Object query(Entry m)throws Throwable{return m.invoke(null,UserHelper.class,"isVip",new Class[0]);}
 static Object soundQuery(Entry m)throws Exception{return TestCaller.call(()->{try{return query(m);}catch(Throwable t){throw new RuntimeException(t);}});}
 public static void main(String[] args)throws Throwable {
  Entry m=new Entry();MembershipFeatures.install(m,MembershipContractTest.class.getClassLoader());check(m.errors.isEmpty(),"all host contracts resolved");
  check(m.expected.get("vip_popup")==2&&m.installed.get("vip_popup")==2,"popup exact signatures");check(m.expected.get("local_vip")==8&&m.installed.get("local_vip")==8,"all eight user paths");check(m.expected.get("sound_effects")==11&&m.installed.get("sound_effects")==11,"all eleven sound hooks");
  check((Boolean)query(m),"local override on");m.flags.put("local_vip",false);check(!(Boolean)query(m),"sound-only does not leak to ordinary membership");check((Boolean)soundQuery(m),"sound-only override works");
  m.flags.put("local_vip",true);m.flags.put("sound_effects",false);check(!(Boolean)soundQuery(m),"sound off remains original with local VIP on");check((Boolean)query(m),"local VIP remains on outside sound");
  m.master=false;check(!(Boolean)query(m)&&!(Boolean)soundQuery(m),"master off restores all user results");m.master=true;m.flags.put("sound_effects",true);
  a user=new a();for(String name:new String[]{"n2","L1","i2","j2"})check((Boolean)m.invoke(user,a.class,name,new Class[0]),"direct user model "+name);
  SoundEffectRightsManager mgr=new SoundEffectRightsManager();Class<?>[] ids={int.class,int.class};
  Object sync=m.invoke(mgr,SoundEffectRightsManager.class,"e0",ids,1,2);Object async=m.invoke(mgr,SoundEffectRightsManager.class,"R",ids,1,2);check(sync instanceof g.d&&((rx.d)async).value instanceof g.d,"proper host success and Rx payload");check(mgr.originals==0,"enabled bypasses original network/trial entry");
  m.flags.put("sound_effects",false);check(m.invoke(mgr,SoundEffectRightsManager.class,"e0",ids,1,2)==null,"sync off fallback");check(((rx.d)m.invoke(mgr,SoundEffectRightsManager.class,"R",ids,1,2)).value==null&&mgr.originals==2,"Rx off fallback");
  com.tencent.qqmusic.dialog.vipremind.f dialog=new com.tencent.qqmusic.dialog.vipremind.f();Class<?>[] params={String.class,com.tencent.qqmusic.dialog.universal.response.c.class,String.class,com.tencent.qqmusiccommon.xray.dialog.b.class};
  m.invoke(dialog,dialog.getClass(),"q",params,"{}",new com.tencent.qqmusic.dialog.universal.response.c(),"id",null);check(dialog.shown==0,"popup suppressed");m.flags.put("vip_popup",false);m.invoke(dialog,dialog.getClass(),"q",params,"{}",new com.tencent.qqmusic.dialog.universal.response.c(),"id",null);check(dialog.shown==1,"popup off fallback");
  check((Boolean)m.invoke(new com.tencent.qqmusic.business.playernew.vipguide.s(),com.tencent.qqmusic.business.playernew.vipguide.s.class,"x",new Class[]{boolean.class},true),"trial dialog off fallback");
  Entry fail=new Entry();fail.failId="umusic-sound-rights-e0";MembershipFeatures.install(fail,MembershipContractTest.class.getClassLoader());check(fail.installed.get("sound_effects")==9,"failed rights unit not counted");check(!fail.hooks.containsKey(SoundEffectRightsManager.class.getDeclaredMethod("R",int.class,int.class)),"partially installed rights hook rolled back");check(fail.installed.get("local_vip")==8&&fail.installed.get("vip_popup")==2,"unrelated hooks survive");
  System.out.println("PASS: exact registration, local/sound isolation in both directions, master off, direct user model, native sync/Rx success, original fallback, popup on/off, partial registration rollback");
 }
}''')
with tempfile.TemporaryDirectory(prefix='umusic-membership-') as directory:
 work=pathlib.Path(directory)
 for name,text in files.items():
  p=work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
 real=work/'io/github/tancan1998/umusic/MembershipFeatures.java';real.write_text((ROOT/'app/src/main/java/io/github/tancan1998/umusic/MembershipFeatures.java').read_text())
 classes=work/'classes';classes.mkdir()
 subprocess.run(['javac','-encoding','UTF-8','-d',str(classes)]+[str(work/n) for n in files]+[str(real)],check=True,capture_output=True,text=True)
 subprocess.run(['java','-cp',str(classes),'io.github.tancan1998.umusic.MembershipContractTest'],check=True)
