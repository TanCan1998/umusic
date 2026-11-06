#!/usr/bin/env python3
"""Execute real HostSettings.java against narrow contracts from MT Java review.
Fixtures model host constructor checks, provider lifecycle and hook dispatch;
this is not an Android/Vector integration test and never touches the phone.
"""
import pathlib,subprocess,tempfile
ROOT=pathlib.Path(__file__).resolve().parent.parent
BASE='com.tencent.qqmusic.fragment.morefeatures.settings.base'
MORE='com.tencent.qqmusic.fragment.morefeatures'
files={}
def source(name,text):files[name.replace('.','/')+'.java']=text
source('android.content.Context','''package android.content; public class Context { public ClassLoader getClassLoader(){return getClass().getClassLoader();} }''')
source('android.content.ContextWrapper','''package android.content; public class ContextWrapper extends Context { public Context getBaseContext(){return this;} }''')
source('android.app.Activity','''package android.app; public class Activity extends android.content.Context {}''')
source('android.content.SharedPreferences','''package android.content; public interface SharedPreferences { boolean getBoolean(String k,boolean v); Editor edit(); interface Editor { Editor putBoolean(String k,boolean v); void apply(); }}''')
source('android.os.Bundle','''package android.os; public class Bundle { private final java.util.Map<String,Boolean> m=new java.util.HashMap<>();public void putBoolean(String k,boolean v){m.put(k,v);}public boolean getBoolean(String k,boolean v){return m.getOrDefault(k,v);} }''')
source('android.util.Log','''package android.util; public class Log {public static int i(String t,String m){return 0;}public static int w(String t,String m){return 0;}public static int e(String t,String m,Throwable e){return 0;}}''')
source('android.view.View','''package android.view; public class View {public interface OnClickListener {void onClick(View v);}public android.content.Context getContext(){return null;}}''')
source('android.view.ViewGroup','''package android.view; public class ViewGroup extends View {public int getChildCount(){return 0;}public View getChildAt(int i){return null;}}''')
source('android.widget.TextView','''package android.widget; public class TextView extends android.view.View {private CharSequence t="设置";public CharSequence getText(){return t;}public void setText(String s){t=s;}}''')
source('org.json.JSONObject','''package org.json; public class JSONObject {public JSONObject(String s){}public JSONObject optJSONObject(String k){return new JSONObject("");}public int optInt(String k){return 1;}public long optLong(String k){return 0;}public String optString(String k,String d){return d;}}''')
source('io.github.libxposed.api.XposedInterface','''package io.github.libxposed.api; import java.lang.reflect.Method; public interface XposedInterface {enum ExceptionMode{PROTECTIVE}interface Hooker{Object intercept(Chain c)throws Throwable;}interface Chain{Object getThisObject();Object getArg(int i);Object proceed()throws Throwable;}interface HookHandle{void unhook();}interface HookBuilder{HookBuilder setId(String s);HookBuilder setExceptionMode(ExceptionMode m);HookHandle intercept(Hooker h);}}''')
source('io.github.tancan1998.umusic.Entry','''package io.github.tancan1998.umusic;
import java.lang.reflect.*;import java.util.*;import io.github.libxposed.api.XposedInterface.*;
public class Entry {
 public static final Map<Method,Hooker> hooks=new HashMap<>();public static String failId;
 public HookBuilder hook(Method m){return new HookBuilder(){String id;public HookBuilder setId(String s){id=s;return this;}public HookBuilder setExceptionMode(ExceptionMode mode){return this;}public HookHandle intercept(Hooker h){if(id.equals(failId))throw new IllegalStateException("registration failure");if(hooks.put(m,h)!=null)throw new AssertionError("duplicate hook");return ()->hooks.remove(m);}};}
 public interface Original{Object run()throws Throwable;}
 public static Object invoke(Object obj,Class<?> type,String name,Class<?>[] sig,Object[] args,Original original){try{Method method=type.getDeclaredMethod(name,sig);Hooker h=hooks.get(method);return h==null?original.run():h.intercept(new Chain(){public Object getThisObject(){return obj;}public Object getArg(int n){return args[n];}public Object proceed()throws Throwable{return original.run();}});}catch(Throwable e){if(e instanceof RuntimeException)throw (RuntimeException)e;throw new RuntimeException(e);}}
 public final Prefs prefs=new Prefs(); public android.content.SharedPreferences preferences(android.content.Context c){return prefs;}public String diagnostics(){return "{}";}public void settingChanged(){}
 public static class Prefs implements android.content.SharedPreferences {public final Map<String,Boolean> data=new HashMap<>();public boolean getBoolean(String k,boolean v){return data.getOrDefault(k,v);}public Editor edit(){return new Editor(){public Editor putBoolean(String k,boolean v){data.put(k,v);return this;}public void apply(){}};}}
}''')
source(BASE+'.SettingEvent',f'''package {BASE};public interface SettingEvent {{void onEvent(int n);}}''')
source(MORE+'.settings.view.SettingView',f'''package {MORE}.settings.view; public class SettingView {{public interface d{{boolean isSwitchOn();void onSwitchStatusChange(boolean b);}}}}''')
source(BASE+'.Setting',f'''package {BASE};
import android.content.Context;import android.view.View;import {MORE}.settings.view.SettingView;
public class Setting {{
 public int type;public String title;public Object tag;public boolean on;public View.OnClickListener click;public SettingView.d listener;
 public static Builder builder(Context c){{return new Builder();}}
 public Object getTag(){{return tag;}}public String getTitleText(){{return title;}}public void setTitleText(String s){{title=s;}}public void setSwitchOn(boolean b){{on=b;}}
 public static class Builder {{Setting s=new Setting();public Builder title(String v){{s.title=v;return this;}}public Builder type(int v){{s.type=v;return this;}}public Builder tag(Object v){{s.tag=v;return this;}}public Builder click(View.OnClickListener v){{s.click=v;return this;}}public Builder switcher(SettingView.d v){{s.listener=v;return this;}}public Builder bottomTitle(String v){{return this;}}public Builder rightTitle(String v){{return this;}}
 public Setting build(){{if(s.type==0&&s.listener==null)throw new IllegalArgumentException("switch listener required");if((s.type==1||s.type==2)&&s.click==null)throw new IllegalArgumentException("click listener required");return s;}}
 }}
}}''')
source(BASE+'.SettingProvider',f'''package {BASE}; public abstract class SettingProvider {{protected final android.content.Context context;protected final SettingEvent host;protected final Setting setting;public SettingProvider(android.content.Context c,SettingEvent h){{context=c;host=h;setting=create();if(setting==null)throw new NullPointerException("setting is null");}}public abstract Setting create();public Setting getSetting(){{return setting;}}public boolean shouldShown(){{return true;}}public void onCreate(){{}}public void onResume(){{}}public void onPause(){{}}public void onDestroy(){{}}}}''')
source(BASE+'.BaseSettingPackage',f'''package {BASE}; import java.util.*;
public abstract class BaseSettingPackage implements SettingEvent {{protected final android.os.Bundle bundle;protected final android.content.Context context;protected final SettingEvent host;protected final List<SettingProvider> settingProviders;protected final List<Setting> settings=new ArrayList<>();
 public BaseSettingPackage(android.content.Context c,SettingEvent h,android.os.Bundle b){{context=c;host=h;bundle=b;settingProviders=createSettingProvider();if(settingProviders==null||settingProviders.size()<1)throw new IllegalArgumentException("settingProviders == null");fill();if(settings.size()<1)throw new IllegalArgumentException("settings == null");}}
 public abstract java.util.concurrent.CopyOnWriteArrayList<SettingProvider> createSettingProvider();private void fill(){{for(SettingProvider p:settingProviders)if(p.shouldShown())settings.add(p.getSetting());}}
 public List<Setting> getSettings(){{return settings;}}public List<SettingProvider> getProviders(){{return settingProviders;}}
 public void rebuildSetting(){{List<SettingProvider> fresh=createSettingProvider();settingProviders.clear();settingProviders.addAll(fresh);settings.clear();fill();}}
 public void onCreate(){{for(SettingProvider p:settingProviders)p.onCreate();}}public void onResume(){{for(SettingProvider p:settingProviders)p.onResume();}}public void onPause(){{for(SettingProvider p:settingProviders)p.onPause();}}public void onDestroy(){{for(SettingProvider p:settingProviders)p.onDestroy();}}public void onEvent(int n){{host.onEvent(n);}}
}}''')
source(MORE+'.settings.providers.z',f'''package {MORE}.settings.providers;import {BASE}.*;
public class z extends SettingProvider {{public z(android.content.Context c,SettingEvent h,String s){{super(c,h);setting.setTitleText(s);}}public Setting create(){{return (Setting)io.github.tancan1998.umusic.Entry.invoke(this,z.class,"create",new Class[0],new Object[0],()->Setting.builder(context).type(3).build());}}}}''')
source(MORE+'.settings.providers.p1',f'''package {MORE}.settings.providers;import {BASE}.*;
public class p1 extends SettingProvider {{public p1(android.content.Context c,SettingEvent h){{super(c,h);}}public Setting create(){{return Setting.builder(context).title("设置").type(2).tag(p1.class).click(v->{{}}).build();}}}}''')
source(MORE+'.settings.packages.d',f'''package {MORE}.settings.packages;import {BASE}.*;import java.util.concurrent.CopyOnWriteArrayList;
public class d extends BaseSettingPackage {{public static int originals=0,events=0;public d(android.content.Context c,SettingEvent h,android.os.Bundle b){{super(c,h,b);}}
 @SuppressWarnings("unchecked") public CopyOnWriteArrayList<SettingProvider> createSettingProvider(){{return (CopyOnWriteArrayList<SettingProvider>)io.github.tancan1998.umusic.Entry.invoke(this,d.class,"createSettingProvider",new Class[0],new Object[0],()->{{originals++;CopyOnWriteArrayList<SettingProvider> l=new CopyOnWriteArrayList<>();l.add(new {MORE}.settings.providers.z(context,this,"原设置"));return l;}});}}
 public void onEvent(int n){{io.github.tancan1998.umusic.Entry.invoke(this,d.class,"onEvent",new Class[]{{int.class}},new Object[]{{n}},()->{{events++;host.onEvent(n);return null;}});}}
}}''')
source(MORE+'.settings.packages.b',f'''package {MORE}.settings.packages;import {BASE}.*;import java.util.concurrent.CopyOnWriteArrayList;
public class b extends BaseSettingPackage {{public b(android.content.Context c,SettingEvent h,android.os.Bundle b){{super(c,h,b);}}
 @SuppressWarnings("unchecked") public CopyOnWriteArrayList<SettingProvider> createSettingProvider(){{return (CopyOnWriteArrayList<SettingProvider>)io.github.tancan1998.umusic.Entry.invoke(this,b.class,"createSettingProvider",new Class[0],new Object[0],()->{{CopyOnWriteArrayList<SettingProvider> l=new CopyOnWriteArrayList<>();l.add(new {MORE}.settings.providers.p1(context,this));l.add(new {MORE}.settings.providers.z(context,this,"定时关闭"));return l;}});}}
}}''')
source('com.tencent.qqmusic.activity.base.BaseFragmentActivity',f'''package com.tencent.qqmusic.activity.base;public class BaseFragmentActivity extends android.app.Activity{{public void addSecondFragment(Class<?> c,android.os.Bundle b){{}}}}''')
source(MORE+'.SettingFeaturesFragment',f'''package {MORE}; public class SettingFeaturesFragment {{public android.os.Bundle getArguments(){{return new android.os.Bundle();}}private void initTitleBar(android.view.View v){{}}private void initSuperVipBtnConfig(){{}}private void showSimpleModeTip(){{}}}}''')
source('io.github.tancan1998.umusic.NativeSettingsContractTest',f'''package io.github.tancan1998.umusic;
import {BASE}.*;import {MORE}.settings.packages.*;import java.util.*;
public class NativeSettingsContractTest {{
 static void check(boolean ok,String m){{if(!ok)throw new AssertionError(m);}}
 public static void main(String[] args)throws Exception {{
  Entry module=new Entry();HostSettings.install(module,NativeSettingsContractTest.class.getClassLoader());check(Entry.hooks.size()==7,"all seven hooks registered");
  android.content.Context ctx=new android.content.Context();final List<Integer> events=new ArrayList<>();SettingEvent event=n->events.add(n);
  d ordinary=new d(ctx,event,new android.os.Bundle());check(ordinary.getSettings().size()==1,"ordinary settings preserved");check(d.originals==1,"ordinary factory runs");ordinary.onEvent(5);check(d.events==1&&events.equals(Arrays.asList(5)),"ordinary event 5 unchanged");events.clear();
  android.os.Bundle marker=new android.os.Bundle();marker.putBoolean("umusic-settings",true);d nativePage=new d(ctx,event,marker);
  List<Setting> identity=nativePage.getSettings();check(identity.size()==23,"nonempty constructor with all nine rows");check(nativePage.getProviders().size()==23,"rows and providers match");check(d.originals==1,"business providers never created for module page");
  for(SettingProvider p:nativePage.getProviders())check(p.getClass().getName().endsWith(".z"),"no business lifecycle provider");
  nativePage.onCreate();nativePage.onResume();nativePage.onPause();nativePage.onDestroy();
  for(int i=0;i<12;i++){{Setting s=identity.get(i);check(s.type==0&&s.listener!=null,"native switch callback");s.listener.onSwitchStatusChange(false);check(!s.listener.isSwitchOn()&&!s.on,"off state persisted and model synchronized");}}
  nativePage.onEvent(5);nativePage.onEvent(6);check(d.events==1,"module events bypass ordinary business fields");check(events.equals(Arrays.asList(0,0)),"module events full refresh");
  for(int i=0;i<4;i++){{nativePage.rebuildSetting();check(nativePage.getSettings()==identity,"adapter list identity retained");check(identity.size()==23,"rebuild neither duplicates nor drops rows");for(int k=0;k<12;k++)check(!identity.get(k).listener.isSwitchOn(),"off preference survives rebuild");}}
  Setting toggle=identity.get(0);toggle.listener.onSwitchStatusChange(true);nativePage.rebuildSetting();check(identity.get(0).listener.isSwitchOn(),"on preference survives rebuild");
  for(int i=12;i<23;i++)check(identity.get(i).type==3,"diagnostics use nonbutton section contract");
  b drawer=new b(ctx,event,new android.os.Bundle());List<Setting> drawerIdentity=drawer.getSettings();
  for(int repeat=0;repeat<4;repeat++){{check(drawerIdentity.size()==3,"one added drawer provider");check(drawerIdentity.get(0).title.equals("设置")&&drawerIdentity.get(1).title.equals("U 音设置")&&drawerIdentity.get(2).title.equals("定时关闭"),"entry directly after original settings, timer aligned");check(drawerIdentity.get(1).type==2&&drawerIdentity.get(1).click!=null,"entry satisfies click contract");drawer.rebuildSetting();check(drawer.getSettings()==drawerIdentity,"drawer list identity retained");}}
  SettingProvider normal=new {MORE}.settings.providers.z(ctx,event,"ordinary group");check(normal.getSetting().type==3,"thread local row restored after carrier construction");
  Entry.hooks.clear();Entry.failId="umusic-native-title";HostSettings.install(module,NativeSettingsContractTest.class.getClassLoader());check(Entry.hooks.isEmpty(),"partial registration rolled back");
  System.out.println("PASS: constructor, lifecycle, ordinary page isolation, events 5/6, switch persistence, repeated rebuild, drawer order, thread-local cleanup, registration rollback");
 }}
}}''')
with tempfile.TemporaryDirectory(prefix='umusic-native-contract-') as directory:
    work=pathlib.Path(directory)
    for name,text in files.items():
        path=work/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text)
    real=ROOT/'app/src/main/java/io/github/tancan1998/umusic/HostSettings.java'
    path=work/'io/github/tancan1998/umusic/HostSettings.java';path.write_text(real.read_text())
    output=work/'classes';output.mkdir()
    status=work/'io/github/tancan1998/umusic/StatusText.java';status.write_text((ROOT/'app/src/main/java/io/github/tancan1998/umusic/StatusText.java').read_text())
    java=[str(work/name) for name in files]+[str(path),str(status)]
    subprocess.run(['javac','-encoding','UTF-8','-d',str(output)]+java,check=True,capture_output=True,text=True)
    subprocess.run(['java','-cp',str(output),'io.github.tancan1998.umusic.NativeSettingsContractTest'],check=True)
