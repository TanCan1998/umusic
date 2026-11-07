package io.github.tancan1998.umusic;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONObject;

/** Native providers and SettingView only. No module Activity or dialog. */
final class HostSettings {
    private static final String BASE="com.tencent.qqmusic.fragment.morefeatures.settings.base.";
    private static final String ROOT="com.tencent.qqmusic.fragment.morefeatures.";
    private static final String TAG="umusic-settings";
    private static final String[] KEYS={"enabled","splash","shake","mask","mine","vip_popup","local_vip","sound_effects","coins","auto_sign","yuanbao","search_ads"};
    private static final String[] LABELS={"U 音总开关","跳过开屏广告","禁止广告摇一摇","隐藏广告蒙层","净化我的页广告","去除开通会员弹窗","解锁本地会员","解锁本地音效","去除金币与领现金入口","自动签到与领奖","关闭元宝入口","去除搜索页广告"};
    // z extends SettingProvider and inherits every no-op lifecycle callback.
    // Its create() runs inside super(); provide an already validated Setting there.
    private final ThreadLocal<Object> pendingRow=new ThreadLocal<>();
    private final Entry module;
    private final Class<?> setting,builder,provider,drawer,pack,page,switcher,entryProvider;
    private final Field bundle,context,host;
    private final Constructor<?> carrier;
    private final Method newBuilder,build,getTag,getTitle;
    private final List<XposedInterface.HookHandle> handles=new ArrayList<>();

    private HostSettings(Entry module,ClassLoader cl)throws Exception {
        this.module=module;
        Class<?> base=Class.forName(BASE+"BaseSettingPackage",false,cl);
        setting=Class.forName(BASE+"Setting",false,cl);
        builder=Class.forName(BASE+"Setting$Builder",false,cl);
        provider=Class.forName(BASE+"SettingProvider",false,cl);
        Class<?> event=Class.forName(BASE+"SettingEvent",false,cl);
        drawer=Class.forName(ROOT+"settings.packages.b",false,cl);
        pack=Class.forName(ROOT+"settings.packages.d",false,cl);
        page=Class.forName(ROOT+"SettingFeaturesFragment",false,cl);
        switcher=Class.forName(ROOT+"settings.view.SettingView$d",false,cl);
        entryProvider=Class.forName(ROOT+"settings.providers.p1",false,cl);
        Class<?> z=Class.forName(ROOT+"settings.providers.z",false,cl);
        if(!provider.isAssignableFrom(z)||!switcher.isInterface())throw new IllegalStateException("provider contract changed");
        bundle=field(base,"bundle");context=field(base,"context");host=field(base,"host");
        carrier=z.getConstructor(Context.class,event,String.class);
        newBuilder=setting.getMethod("builder",Context.class);build=builder.getMethod("build");
        getTag=setting.getMethod("getTag");getTitle=setting.getMethod("getTitleText");
        require(build,setting);require(newBuilder,builder);
        // Resolve every contract before registering any hook.
        for(String name:new String[]{"title","bottomTitle","rightTitle"})builder.getMethod(name,String.class);
        builder.getMethod("type",int.class);builder.getMethod("tag",Object.class);
        builder.getMethod("click",View.OnClickListener.class);builder.getMethod("switcher",switcher);
        setting.getMethod("setSwitchOn",boolean.class);provider.getMethod("getSetting");
        switcher.getMethod("isSwitchOn");switcher.getMethod("onSwitchStatusChange",boolean.class);
        event.getMethod("onEvent",int.class);
        Class.forName("com.tencent.qqmusic.activity.base.BaseFragmentActivity",false,cl)
            .getMethod("addSecondFragment",Class.class,Bundle.class);
    }
    static void install(Entry module,ClassLoader cl){
        HostSettings integration=null;
        try{integration=new HostSettings(module,cl);integration.register();Log.i("UMusic","native settings integration installed");}
        catch(Throwable t){if(integration!=null)integration.rollback();Log.e("UMusic","native settings integration unavailable",t);}
    }
    private static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    private static void require(Method m,Class<?> result)throws Exception{if(m.getReturnType()!=result)throw new NoSuchMethodException(m+" return type");}
    private void hook(Method method,Class<?> result,String id,XposedInterface.Hooker interceptor)throws Exception{
        require(method,result);
        handles.add(module.hook(method).setId("umusic-"+id).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(interceptor));
    }
    private boolean marked(Object p)throws Exception {if(!pack.isInstance(p))return false;Bundle b=(Bundle)bundle.get(p);return b!=null&&b.getBoolean(TAG,false);}
    private boolean markedPage(Object p)throws Exception {Bundle b=(Bundle)p.getClass().getMethod("getArguments").invoke(p);return b!=null&&b.getBoolean(TAG,false);}
    private void register()throws Exception{
        hook(carrier.getDeclaringClass().getDeclaredMethod("create"),setting,"carrier-create",c->{Object row=pendingRow.get();return row==null?c.proceed():row;});
        // Constructor and rebuildSetting both consume this nonempty provider list.
        hook(pack.getDeclaredMethod("createSettingProvider"),CopyOnWriteArrayList.class,"native-providers",c->{
            if(!marked(c.getThisObject()))return c.proceed();
            return providers(c.getThisObject());
        });
        // d's event 5/6 branches assume its ordinary business fields were populated.
        // Module providers have no business events; use a full refresh for the marked page.
        hook(pack.getDeclaredMethod("onEvent",int.class),void.class,"native-event",c->{
            if(!marked(c.getThisObject()))return c.proceed();
            Object target=host.get(c.getThisObject());
            Class<?> event=host.getType();event.getMethod("onEvent",int.class).invoke(target,0);return null;
        });
        hook(page.getDeclaredMethod("initTitleBar",View.class),void.class,"native-title",c->{
            Object result=c.proceed();if(markedPage(c.getThisObject()))renameTitle((View)c.getArg(0));return result;
        });
        for(String name:new String[]{"initSuperVipBtnConfig","showSimpleModeTip"})
            hook(page.getDeclaredMethod(name),void.class,"native-"+name,c->markedPage(c.getThisObject())?null:c.proceed());
        // Register the visible entry last, once the destination is fully wired.
        hook(drawer.getDeclaredMethod("createSettingProvider"),CopyOnWriteArrayList.class,"more-providers",c->{
            Object original=c.proceed();
            try{
                @SuppressWarnings("unchecked") List<Object> source=(List<Object>)original;
                for(Object p:source)if(TAG.equals(getTag.invoke(provider.getMethod("getSetting").invoke(p))))return original;
                int at=-1;for(int i=0;i<source.size();i++)if(entryProvider.isInstance(source.get(i))){at=i+1;break;}
                if(at<0){Log.w("UMusic","original settings provider absent; entry omitted");return original;}
                Context ctx=(Context)context.get(c.getThisObject());
                Object b=row(ctx,"U 音设置",2,TAG);
                builder.getMethod("click",View.OnClickListener.class).invoke(b,(View.OnClickListener)v->navigate(v.getContext()));
                Object entry=wrap(ctx,c.getThisObject(),build.invoke(b));
                CopyOnWriteArrayList<Object> updated=new CopyOnWriteArrayList<>(source);updated.add(at,entry);return updated;
            }catch(Throwable t){Log.e("UMusic","more entry omitted",t);return original;}
        });
    }
    private Object row(Context ctx,String title,int type,Object tag)throws Exception{
        Object b=newBuilder.invoke(null,ctx);builder.getMethod("title",String.class).invoke(b,title);
        builder.getMethod("type",int.class).invoke(b,type);builder.getMethod("tag",Object.class).invoke(b,tag);return b;
    }
    private Object wrap(Context ctx,Object owner,Object row)throws Exception{
        Object previous=pendingRow.get();pendingRow.set(row);
        try{return carrier.newInstance(ctx,owner,getTitle.invoke(row));}
        finally{if(previous==null)pendingRow.remove();else pendingRow.set(previous);}
    }
    private CopyOnWriteArrayList<Object> providers(Object owner)throws Exception{
        Context ctx=(Context)context.get(owner);SharedPreferences prefs=module.preferences(ctx);
        CopyOnWriteArrayList<Object> result=new CopyOnWriteArrayList<>();
        for(int i=0;i<KEYS.length;i++){
            final String key=KEYS[i];Object b=row(ctx,LABELS[i],0,TAG+"-"+key);
            final Object[] model=new Object[1];
            Object listener=Proxy.newProxyInstance(switcher.getClassLoader(),new Class<?>[]{switcher},(proxy,m,args)->{
                switch(m.getName()){
                    case "isSwitchOn":return prefs.getBoolean(key,true);
                    case "onSwitchStatusChange":
                        boolean on=(Boolean)args[0];
                        prefs.edit().putBoolean(key,on).apply();
                        if(model[0]!=null)setting.getMethod("setSwitchOn",boolean.class).invoke(model[0],on);
                        module.settingChanged();return null;
                    case "hashCode":return System.identityHashCode(proxy);
                    case "equals":return proxy==args[0];
                    case "toString":return "U音 "+key;
                    default:throw new UnsupportedOperationException(m.toString());
                }
            });
            builder.getMethod("switcher",switcher).invoke(b,listener);Object s=build.invoke(b);model[0]=s;
            setting.getMethod("setSwitchOn",boolean.class).invoke(s,prefs.getBoolean(key,true));result.add(wrap(ctx,owner,s));
        }
        // Native section rows (type 3) have no click-listener requirement.
        JSONObject report=new JSONObject(module.diagnostics());
        for(int i=1;i<KEYS.length;i++){
            JSONObject s=report.optJSONObject(KEYS[i]);if(s==null)continue;
            int n=s.optInt("installed"),e=s.optInt("expected");
            String status=StatusText.format(KEYS[i],n,e,prefs.getBoolean("enabled",true)&&prefs.getBoolean(KEYS[i],true),s.optLong("hits"),report.optString("dailySignResult","未执行"),report.optInt("dailySignStreak"));
            String detail=LABELS[i]+" · "+status;
            Object b=row(ctx,detail,3,TAG+"-diagnostic-"+KEYS[i]);
            result.add(wrap(ctx,owner,build.invoke(b)));
        }
        JSONObject signs=report.optJSONObject("signTasks");
        if(signs!=null)for(java.util.Iterator<String> it=signs.keys();it.hasNext();){
            String name=it.next();String value=prefs.getBoolean("enabled",true)&&prefs.getBoolean("auto_sign",true)?signs.optString(name,"未执行"):"未启用";
            Object b=row(ctx,name+" · "+value,3,TAG+"-sign-"+name);result.add(wrap(ctx,owner,build.invoke(b)));
        }
        if(result.isEmpty())throw new IllegalStateException("module provider list empty");
        return result;
    }
    private void navigate(Context ctx){
        try{
            while(ctx instanceof ContextWrapper&&!(ctx instanceof android.app.Activity)){
                Context next=((ContextWrapper)ctx).getBaseContext();if(next==ctx)throw new IllegalStateException("context cycle");ctx=next;
            }
            Bundle args=new Bundle();args.putBoolean(TAG,true);
            ctx.getClass().getMethod("addSecondFragment",Class.class,Bundle.class).invoke(ctx,page,args);
        }catch(Throwable t){Log.e("UMusic","native settings navigation failed",t);}
    }
    private static void renameTitle(View root){
        if(root instanceof TextView&&"设置".contentEquals(((TextView)root).getText()))((TextView)root).setText("U 音设置");
        if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++)renameTitle(g.getChildAt(i));}
    }
    private void rollback(){for(int i=handles.size()-1;i>=0;i--)try{handles.get(i).unhook();}catch(Throwable ignored){}handles.clear();}
}
