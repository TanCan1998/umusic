package com.xiandehuang.umusic;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.ValueCallback;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONObject;

/** Exact signatures verified with MT against QQ Music 20.9.8.8/7608. */
public final class Entry extends XposedModule {
    private static final String TAG="UMusic";
    private ClassLoader loader;
    private Context context;
    private volatile SharedPreferences prefs;
    private final Map<String,Stat> stats=new LinkedHashMap<>();
    private long lastWrite;
    private final java.util.concurrent.atomic.AtomicBoolean toasted=new java.util.concurrent.atomic.AtomicBoolean();
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private android.os.Handler writer;
    private synchronized boolean hasHooks(){for(Stat s:stats.values())if(s.installed>0)return true;return false;}
    private static final class Stat {
        int installed,expected; long entered,hits; String error="";
    }
    @Override public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam p) {
        if (!"com.tencent.qqmusic".equals(p.getPackageName())) return;
        loader=p.getDefaultClassLoader();
        if(!"com.tencent.qqmusic".equals(Application.getProcessName()))return;
        android.os.HandlerThread diagnostics=new android.os.HandlerThread("umusic-diagnostics");diagnostics.start();writer=new android.os.Handler(diagnostics.getLooper());
        try {
            try{
                Method resume=android.app.Instrumentation.class.getDeclaredMethod("callActivityOnResume",android.app.Activity.class);
                hook(resume).setId("umusic-toast-resume").intercept(new XposedInterface.Hooker(){
                    public Object intercept(XposedInterface.Chain c)throws Throwable{Object result=c.proceed();showToast((android.app.Activity)c.getArg(0));return result;}
                });deoptimize(resume);
            }catch(Throwable t){Log.w(TAG,"toast fallback unavailable",t);}
            Method attach=Application.class.getDeclaredMethod("attach",Context.class);
            hook(attach).setId("umusic-context").intercept(new XposedInterface.Hooker(){
                @Override public Object intercept(XposedInterface.Chain c) throws Throwable {
                    Object result=c.proceed(); context=(Context)c.getArg(0);prefs=context.getSharedPreferences("umusic-purify",Context.MODE_PRIVATE); registerToast((Application)c.getThisObject()); flush(true); return result;
                }
            });
            try {
                Object app=Class.forName("android.app.ActivityThread").getDeclaredMethod("currentApplication").invoke(null);
                if(app instanceof Application){context=(Context)app;registerToast((Application)app);}
            } catch(Throwable ignored) {}
            if(context!=null)prefs=context.getSharedPreferences("umusic-purify",Context.MODE_PRIVATE);
            install(loader);
            MembershipFeatures.install(this,loader);
            UiFeatures.install(this,loader);
            SearchFeatures.install(this,loader);
            DailySign.install(this,loader);
            installDiagnostics();
            HostSettings.install(this,loader);
        }catch(Throwable t){Log.e(TAG,"module initialization failed",t);}
    }
    private final java.util.concurrent.atomic.AtomicBoolean lifecycleRegistered=new java.util.concurrent.atomic.AtomicBoolean();
    private void registerToast(Application app){
        if(!lifecycleRegistered.compareAndSet(false,true))return;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityResumed(android.app.Activity a){
                showToast(a);
            }
            public void onActivityCreated(android.app.Activity a,android.os.Bundle b){}
            public void onActivityStarted(android.app.Activity a){}
            public void onActivityPaused(android.app.Activity a){}
            public void onActivityStopped(android.app.Activity a){}
            public void onActivitySaveInstanceState(android.app.Activity a,android.os.Bundle b){}
            public void onActivityDestroyed(android.app.Activity a){}
        });
    }
    private void showToast(android.app.Activity a){
        DailySign.maybe(a.getApplicationContext());
        if(hasHooks() && toasted.compareAndSet(false,true)){
            try{android.widget.Toast.makeText(a.getApplicationContext(),"U 音已加载\n入口：我的 → 右上角更多 → U 音设置",android.widget.Toast.LENGTH_LONG).show();Log.i(TAG,"startup toast shown");}
            catch(Throwable t){toasted.set(false);Log.w(TAG,"startup toast failed",t);}flush(true);
        }
    }
    boolean enabled(String feature){
        try {SharedPreferences p=prefs;return p==null || (p.getBoolean("enabled",true)&&p.getBoolean(feature,true));}
        catch(Throwable t){return false;}
    }
    private synchronized Stat state(String f){Stat s=stats.get(f);if(s==null){s=new Stat();stats.put(f,s);}return s;}
    synchronized void expected(String feature){state(feature).expected++;}
    synchronized void installed(String feature){state(feature).installed++;}
    synchronized void entered(String feature){state(feature).entered++;flush(false);}
    synchronized void hit(String feature){state(feature).hits++;flush(false);}
    synchronized void warning(String feature,String target,Throwable error){state(feature).error+=target+": "+error.getClass().getSimpleName()+"; ";Log.w(TAG,target,error);}
    void replace(final String feature,String owner,String name,final Class<?> ret,final Object value,String...types){
        state(feature).expected++;
        try{
            Class<?> cls=Class.forName(owner,false,loader);Class<?>[] args=new Class<?>[types.length];
            for(int i=0;i<types.length;i++)args[i]=type(types[i]);
            Method m=cls.getDeclaredMethod(name,args);
            if(m.getReturnType()!=ret)throw new NoSuchMethodException("return type mismatch");
            hook(m).setId("umusic-"+owner+"-"+name).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(new XposedInterface.Hooker(){
                @Override public Object intercept(XposedInterface.Chain c) throws Throwable {
                    Stat s=state(feature);s.entered++;
                    if(!enabled(feature)){flush(false);return c.proceed();}
                    s.hits++; if(s.hits<=3)Log.i(TAG,"blocked "+feature+" "+c.getExecutable());
                    flush(false);return value;
                }
            });state(feature).installed++;
        }catch(Throwable t){state(feature).error+=owner+"#"+name+": "+t.getClass().getSimpleName()+"; ";Log.w(TAG,"hook unavailable "+owner+"#"+name,t);}
    }
    private Class<?> type(String n)throws Exception{
        if(n.equals("boolean"))return boolean.class;if(n.equals("int"))return int.class;return Class.forName(n,false,loader);
    }
    private void install(ClassLoader cl){
        replace("splash","com.tencent.qqmusic.business.ad.splash.f","a",int.class,7);
        replace("splash","com.tencent.qqmusic.business.ad.splash.f","b",boolean.class,false);
        replace("splash","com.tencent.qqmusic.business.ad.splash.f","c",boolean.class,false);
        replace("splash","com.tencent.qqmusic.business.ad.splash.hotlaunch.g","F",boolean.class,false,"android.app.Activity","boolean");
        replace("splash","com.tencent.qqmusic.business.ad.splash.hotlaunch.g","R",boolean.class,false,"android.app.Activity");
        replace("shake","com.tencentmusic.ad.tmead.core.widget.interactive.ShakeSensor","register",boolean.class,false);
        String mask="com.tencent.qqmusic.business.ad.recommend.TMEMaskAdManager";
        replace("mask",mask,"L",boolean.class,false);
        replace("mask",mask,"y",boolean.class,false);
        replace("mask",mask,"v",boolean.class,false,"android.content.Context","int");
        replace("mask",mask,"l0",void.class,null,"int","android.content.Context");
        replace("mask",mask,"b0",void.class,null);
        String mine="com.tencent.qqmusic.fragment.mymusic.recommend.MergeMyMusicAdManager";
        replace("mine",mine,"B",boolean.class,false);
        replace("mine",mine,"h",boolean.class,false,"boolean","java.lang.String");
        replace("mine",mine,"M",void.class,null,"java.util.ArrayList","int");
        state("mine").expected++;
        try{
            Method m=Class.forName(mine,false,cl).getDeclaredMethod("Q",boolean.class,ValueCallback.class);
            if(m.getReturnType()!=void.class)throw new NoSuchMethodException("return type mismatch");
            hook(m).setId("umusic-my-request").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(new XposedInterface.Hooker(){
                @Override public Object intercept(XposedInterface.Chain c)throws Throwable{
                    Stat s=state("mine");s.entered++;
                    if(!enabled("mine"))return c.proceed();
                    // The original disabled-position branch completes with Boolean.FALSE.
                    @SuppressWarnings("unchecked") ValueCallback<Boolean> callback=(ValueCallback<Boolean>)c.getArg(1);
                    if(callback!=null)callback.onReceiveValue(Boolean.FALSE);
                    s.hits++;flush(false);return null;
                }
            });state("mine").installed++;
        }catch(Throwable t){state("mine").error+="Q: "+t.getClass().getSimpleName();}
        Log.i(TAG,"installed "+report());flush(true);
    }
    private void installDiagnostics(){
        try{
            Class<?> provider=Class.forName("com.tme.push.matrix.interprocess.contentprovider.MatrixContentProvider",false,loader);
            Method query=provider.getDeclaredMethod("query",android.net.Uri.class,String[].class,String.class,String[].class,String.class);
            hook(query).setId("umusic-status-query").intercept(new XposedInterface.Hooker(){
                @Override public Object intercept(XposedInterface.Chain c)throws Throwable{
                    android.net.Uri uri=(android.net.Uri)c.getArg(0);
                    if(uri==null || !"/umusic-status".equals(uri.getPath()))return c.proceed();
                    int uid=android.os.Binder.getCallingUid();
                    boolean allowed=uid==0 || uid==2000 || uid==android.os.Process.myUid() || uid==getModuleApplicationInfo().uid;
                    if(!allowed && context!=null){String[] pkgs=context.getPackageManager().getPackagesForUid(uid);if(pkgs!=null)for(String pkg:pkgs)if(pkg.equals("com.xiandehuang.umusic"))allowed=true;}
                    if(!allowed)throw new SecurityException("private diagnostics");
                    android.database.MatrixCursor result=new android.database.MatrixCursor(new String[]{"key","value"});
                    result.addRow(new Object[]{"status",report()});return result;
                }
            });
        }catch(Throwable t){Log.w(TAG,"diagnostics endpoint unavailable",t);}
    }
    SharedPreferences preferences(Context c){if(prefs==null)prefs=c.getApplicationContext().getSharedPreferences("umusic-purify",Context.MODE_PRIVATE);return prefs;}
    String diagnostics(){return report();}
    void diagnosticChanged(){flush(true);}
    void settingChanged(){main.post(()->{UiFeatures.refresh();DailySign.maybe(context);});flush(true);}
    private synchronized String report(){
        JSONObject root=new JSONObject();try{
            root.put("module","U音 0.8");root.put("pid",android.os.Process.myPid());root.put("dailySignResult",DailySign.status());root.put("dailySignStreak",DailySign.streak());root.put("accountEventsReady",DailySign.accountEventsReady());
            root.put("adaptation","actual_hook_registration");root.put("time",System.currentTimeMillis());root.put("toastShown",toasted.get());root.put("lifecycleRegistered",lifecycleRegistered.get());
            for(Map.Entry<String,Stat> e:stats.entrySet()){
                Stat s=e.getValue();JSONObject j=new JSONObject();j.put("expected",s.expected);j.put("installed",s.installed);j.put("status",s.installed==0?"unavailable":s.installed==s.expected?"installed":"partial");j.put("entered",s.entered);j.put("hits",s.hits);j.put("enabled",enabled(e.getKey()));j.put("error",s.error);root.put(e.getKey(),j);
            }
        }catch(Throwable ignored){}return root.toString();
    }
    private synchronized void flush(boolean force){
        if(context==null || writer==null)return;long now=android.os.SystemClock.elapsedRealtime();if(!force && now-lastWrite<1500)return;lastWrite=now;
        final String json=report();final Context app=context;
        writer.post(new Runnable(){public void run(){
            try{
                File dir=new File(app.getFilesDir(),"umusic");dir.mkdirs();
                File tmp=new File(dir,"status-"+android.os.Process.myPid()+".tmp");
                try(FileOutputStream stream=new FileOutputStream(tmp)){stream.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
                if(!tmp.renameTo(new File(dir,"status-"+android.os.Process.myPid()+".json")))tmp.delete();
            }catch(Throwable t){Log.w(TAG,"status write failed",t);}
        }});
    }
}
