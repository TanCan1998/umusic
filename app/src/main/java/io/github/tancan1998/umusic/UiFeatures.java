package io.github.tancan1998.umusic;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.*;

/** Business-scoped UI hooks; never scan all views by label or alter user/account data. */
final class UiFeatures {
    static final String VISIBILITY_LISTENER = "view_visibility";
    private static Entry module;
    private static ClassLoader loader;
    private static final Map<View,Hidden> hidden=Collections.synchronizedMap(new WeakHashMap<View,Hidden>());
    private static final Map<Object,Badge> badges=Collections.synchronizedMap(new WeakHashMap<Object,Badge>());
    private static final Map<TextView,CharSequence> titles=Collections.synchronizedMap(new WeakHashMap<TextView,CharSequence>());
    private static final ThreadLocal<Boolean> restoring=new ThreadLocal<>();
    private static final String MY="com.tencent.qqmusic.modular.module.musichall.views.viewholders.v21.MyMusicUserInfoViewHolder21st";
    private static final String USER_INFO="com.tencent.qqmusic.fragment.mymusic.userinfo.";
    private static final String USER_ENTRANCES=USER_INFO+"p0";
    private static final String LOGIN_ENTRANCES=USER_INFO+"d0";
    private static final String LOGIN_HOLDER="com.tencent.qqmusic.modular.module.musichall.views.viewholders.v13.MyMusicLoginInfoViewHolder21st";
    private static final String TOP="com.tencent.qqmusic.ui.desktopheader.BaseDesktopHeaderNew";
    private static final String PLAYER="com.tencent.qqmusic.business.playernew.view.v4";
    private static final class Hidden {String feature;int original;Hidden(String f,int v){feature=f;original=v;}}
    private static final class Badge {java.lang.ref.WeakReference<View> icons;java.lang.ref.WeakReference<TextView> text;Badge(View v,TextView t){icons=new java.lang.ref.WeakReference<>(v);text=new java.lang.ref.WeakReference<>(t);}}
    interface Work {Object run(XposedInterface.Chain c)throws Throwable;}
    static void install(Entry m,ClassLoader cl){
        module=m;loader=cl;
        register(VISIBILITY_LISTENER,View.class,"setVisibility",void.class,c->{
            Hidden h=hidden.get((View)c.getThisObject());
            if(h!=null&&!Boolean.TRUE.equals(restoring.get())){
                h.original=(Integer)c.getArg(0);
                if(m.enabled(h.feature))return c.proceed(new Object[]{View.GONE});
            }
            return c.proceed();
        },int.class);
        installMinePullCoins(m);
        boolean currentUserInfo=hasMethod(MY,"bindCoinEntrance",void.class,USER_INFO+"e2");
        installUserEntrances(currentUserInfo);
        Work hideHomeCoins=c->{
            Object result=c.proceed();
            for(String name:new String[]{"A0","B0","C0","D0"})hide(field(c.getThisObject(),name),"coins");
            return result;
        };
        register("coins",TOP,"I",void.class,hideHomeCoins,"int","boolean");
        String inflateCoins=hasMethod(TOP,"O3",void.class,"int","boolean")?"O3":"L3";
        register("coins",TOP,inflateCoins,void.class,hideHomeCoins,"int","boolean");
        register("yuanbao",TOP,"S",void.class,c->{
            if(!m.enabled("yuanbao"))return c.proceed();
            m.entered("yuanbao");invoke(c.getThisObject(),"u",new Class<?>[]{boolean.class},false);m.hit("yuanbao");return null;
        },"boolean");
        register("yuanbao",TOP,"p",boolean.class,c->{
            if(!m.enabled("yuanbao"))return c.proceed();invoke(c.getThisObject(),"r");m.entered("yuanbao");m.hit("yuanbao");return false;
        },"boolean");
        register("coins",PLAYER,"k4","android.widget.FrameLayout",c->{Object r=c.proceed();hide(r,"coins");return r;},PLAYER);
        // Type 3 earning, type 4 ad-VIP/reward, type 6 AI assistant; keep free-mode types 1/2/7.
        boolean dispatcher=register("coins",PLAYER,"b5",void.class,c->{
            int type=(Integer)c.getArg(0);String feature=type==6?"yuanbao":(type==3||type==4)?"coins":null;
            if(feature==null||!m.enabled(feature))return c.proceed();
            m.entered(feature);invoke(c.getThisObject(),"V1",new Class<?>[]{Class.forName("kotlin.jvm.functions.Function0",false,cl)},(Object)null);m.hit(feature);return null;
        },"int","kotlin.jvm.functions.Function0","boolean");
        m.expected("yuanbao");if(dispatcher)m.installed("yuanbao");else m.warning("yuanbao",PLAYER+"#b5",new NoSuchMethodException());
        register("local_vip",MY,"refreshNicknameColorAndVipIcons",void.class,c->{Object r=c.proceed();badge(c.getThisObject());return r;},"com.tencent.qqmusic.business.user.z","boolean");
        register("local_vip",MY,"setIconListData",void.class,c->{Object r=c.proceed();badge(c.getThisObject());return r;},"java.util.List","java.util.List","boolean");
        String alternate="com.tencent.qqmusic.modular.module.musichall.views.viewholders.v13.MyMusicUserInfoViewHolder20th";
        register("local_vip",alternate,"refreshNicknameColorAndVipIcons",void.class,c->{Object r=c.proceed();badge(c.getThisObject());return r;},"com.tencent.qqmusic.business.user.z","boolean");
        register("local_vip",alternate,"setIconListData",void.class,c->{Object r=c.proceed();badge(c.getThisObject());return r;},"java.util.List","java.util.List","boolean");
        register("local_vip","com.tencent.qqmusic.business.playernew.actionsheet.z0$m","a",void.class,c->{
            Object r=c.proceed();Object popupController=field(c.getThisObject(),"b");Object holder=invokeStatic("com.tencent.qqmusic.business.playernew.actionsheet.z0","H",new Class<?>[]{Class.forName("com.tencent.qqmusic.business.playernew.actionsheet.z0",false,cl)},popupController);hide(field(holder,"p"),"local_vip");return r;
        },"java.lang.String");
        register("local_vip","com.tencent.qqmusic.business.playernew.actionsheet.PlayerPopupMenu","getHolder","com.tencent.qqmusic.business.playernew.actionsheet.o1",c->{Object r=c.proceed();hide(field(r,"p"),"local_vip");return r;});
        String menuHeader="com.tencent.qqmusic.ui.newactionsheet.viewholder.ListActionSheetHeaderViewHolder";
        register("local_vip",menuHeader,"payMsgBtn_delegate$lambda$2","android.widget.TextView",c->{Object r=c.proceed();hide(r,"local_vip");return r;},"android.view.View");
        register("local_vip",menuHeader,"getPayMsgBtn","android.widget.TextView",c->{Object r=c.proceed();hide(r,"local_vip");return r;});
        // Reviewed caller families must be deoptimized too: a hooked callee may be AOT-inlined.
        for(String family:new String[]{MY,alternate,USER_ENTRANCES,menuHeader,TOP,PLAYER,"com.tencent.qqmusic.business.playernew.view.u4","com.tencent.qqmusic.ui.desktopheader.t1","com.tencent.qqmusic.business.playernew.actionsheet.z0"})deoptimizeFamily(family,"coins");
    }

    private static void deoptimizeFamily(String family,String feature){
        try{
            Class<?> owner=type(family);
            for(Method method:owner.getDeclaredMethods())if(!java.lang.reflect.Modifier.isAbstract(method.getModifiers())&&!java.lang.reflect.Modifier.isNative(method.getModifiers()))module.deoptimize(method);
            for(java.lang.reflect.Constructor<?> ctor:owner.getDeclaredConstructors())module.deoptimize(ctor);
        }catch(Throwable t){module.warning(feature,family+" caller deopt",t);}
    }

    private static void installUserEntrances(boolean current){
        registerHolderEntrances(MY,current);
        register("coins",USER_ENTRANCES,current?"E0":"y0",void.class,c->{
            Object result=c.proceed();
            boolean hideReward=isReward(c.getArg(0),current);
            for(String name:current?new String[]{"h","J","K","M"}:new String[]{"E","F","H","g"}){
                updateRewardView(field(c.getThisObject(),name),hideReward);
            }
            return result;
        },USER_INFO+(current?"p2":"k2"),"boolean","boolean");
        register("local_vip",USER_ENTRANCES,current?"v0":"p0",void.class,c->{
            Object result=c.proceed();
            if(c.getArg(0)!=null){
                Object holder=c.getThisObject();
                setVipLabels(field(holder,current?"p":"l"),field(holder,current?"q":"m"),
                        field(holder,current?"r":"n"),field(holder,current?"s":"o"));
                if(current)hide(field(holder,"t"),"local_vip");
            }
            return result;
        },USER_INFO+"g");
        if(!current)return;
        registerHolderEntrances(LOGIN_HOLDER,true);
        register("coins",LOGIN_ENTRANCES,"E0",void.class,c->{
            Object result=c.proceed();
            updateRewardView(invoke(c.getThisObject(),"C"),isReward(c.getArg(0),true));
            return result;
        },USER_INFO+"p2","boolean","boolean");
        register("local_vip",LOGIN_ENTRANCES,"v0",void.class,c->{
            Object result=c.proceed();
            if(c.getArg(0)!=null){
                Object holder=c.getThisObject();
                setVipLabels(invoke(holder,"A"),invoke(holder,"z"),invoke(holder,"x"),invoke(holder,"w"));
                hide(field(holder,"t"),"local_vip");
            }
            return result;
        },USER_INFO+"g");
        deoptimizeFamily(LOGIN_HOLDER,"coins");
        deoptimizeFamily(LOGIN_ENTRANCES,"coins");
    }

    private static void registerHolderEntrances(String holder,boolean current){
        register("coins",holder,"bindCoinEntrance",void.class,c->{
            Object result=c.proceed();
            hide(invoke(c.getThisObject(),"getCoinContainer"),"coins");
            hide(invoke(c.getThisObject(),"getBgCoinImg"),"coins");
            return result;
        },USER_INFO+(current?"e2":"z1"));
        register("local_vip",holder,"bindVipEntrance",void.class,c->{
            Object result=c.proceed();
            if(c.getArg(0)!=null){
                Object self=c.getThisObject();
                setVipLabels(invoke(self,"getVipCenterTitle"),invoke(self,"getVipCenterSubtitle"),
                        invoke(self,"getVipCenterFlag"),invoke(self,"getVipCenterIcon"));
            }
            return result;
        },USER_INFO+(current?"i2":"d2"));
    }

    private static boolean isReward(Object data,boolean current)throws Throwable{
        if(data==null)return false;
        // Type 1 is the task center. Other placements may receive a cash or coin promotion.
        if(Integer.valueOf(1).equals(field(data,"a")))return true;
        if(current)return rewardTitle(invoke(data,"q"))||rewardTitle(invoke(data,"r"));
        return rewardTitle(field(data,"b"));
    }

    private static boolean rewardTitle(Object value){
        if(!(value instanceof CharSequence title))return false;
        String text=title.toString();
        return text.contains("金币")||text.contains("领现金");
    }

    private static void updateRewardView(Object view,boolean reward){
        if(reward)hide(view,"coins");else forget(view);
    }

    private static void setVipLabels(Object title,Object subtitle,Object flag,Object icon){
        if(title instanceof TextView text)rememberTitle(text,"超级会员");
        if(subtitle instanceof TextView text)rememberTitle(text,"本地会员 · 不含云端权益");
        hide(flag,"local_vip");
        hide(icon,"local_vip");
    }

    private static void installMinePullCoins(Entry m) {
        String feature = "mine_pull_coins";
        String owner = "com.tencent.qqmusic.fragment.mymusic.my.mytabpull.l";
        boolean eligibility = register(feature, owner, "i", boolean.class, c -> {
            if (!m.enabled(feature) || !isMinePullCoinTarget(c.getThisObject())) return c.proceed();
            m.entered(feature);
            m.hit(feature);
            return false;
        });
        boolean launch = register(feature, owner, "A", void.class, c -> {
            if (!m.enabled(feature) || !isMinePullCoinTarget(c.getThisObject())) return c.proceed();
            // A drag may already have started when the matching configuration arrives.
            invoke(c.getThisObject(), "r");
            m.entered(feature);
            m.hit(feature);
            return null;
        });
        if (!eligibility && !launch) return;
        try {
            Class<?> controller = type(owner);
            m.deoptimize(controller.getDeclaredMethod("onTouch", View.class, android.view.MotionEvent.class));
            m.deoptimize(controller.getDeclaredMethod("f"));
        } catch (Throwable error) {
            m.warning(feature, owner + " caller deopt", error);
        }
    }

    private static boolean isMinePullCoinTarget(Object controller) throws Throwable {
        Object entrance = invoke(controller, "l");
        Object config = invoke(entrance, "v");
        if (config == null) return false;
        Object type = invoke(config, "j");
        if ("VipEarning".equals(type)) return true;
        if ("CDShelf".equals(type) || "PenguinIsand".equals(type)) return false;
        String path = android.net.Uri.parse(String.valueOf(invoke(config, "e"))).getPath();
        return path != null && path.startsWith("/n3/client/pages/points_center/");
    }

    static boolean register(String f,String owner,String name,Object ret,Work body,String...args){
        try{Class<?>[] types=new Class<?>[args.length];for(int i=0;i<types.length;i++)types[i]=type(args[i]);return register(f,type(owner),name,ret instanceof Class?(Class<?>)ret:type((String)ret),body,types);}
        catch(Throwable t){module.expected(f);module.warning(f,owner+"#"+name,t);return false;}
    }
    private static boolean register(String f,Class<?> owner,String name,Class<?> ret,Work body,Class<?>...args){
        module.expected(f);try{Method method=owner.getDeclaredMethod(name,args);if(method.getReturnType()!=ret)throw new NoSuchMethodException("return mismatch");
            module.hook(method).setId("umusic-ui-"+owner.getName()+"-"+name).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(body::run);module.installed(f);try{module.deoptimize(method);}catch(Throwable ignored){}return true;
        }catch(Throwable t){module.warning(f,owner.getName()+"#"+name,t);return false;}
    }
    static boolean hasMethod(String owner,String name,Class<?> result,String...args){
        try{
            Class<?>[] types=new Class<?>[args.length];
            for(int i=0;i<args.length;i++)types[i]=type(args[i]);
            return type(owner).getDeclaredMethod(name,types).getReturnType()==result;
        }catch(ReflectiveOperationException|LinkageError unavailable){return false;}
    }
    private static Class<?> type(String name)throws ClassNotFoundException{if(name.equals("boolean"))return boolean.class;if(name.equals("int"))return int.class;return Class.forName(name,false,loader);}
    static void hide(Object obj,String feature){
        if(!(obj instanceof View))return;View v=(View)obj;Hidden h=hidden.get(v);if(h==null){h=new Hidden(feature,v.getVisibility());hidden.put(v,h);}else if(!module.enabled(feature))h.original=v.getVisibility();
        module.entered(feature);if(module.enabled(feature)){setVisibility(v,View.GONE);module.hit(feature);}
    }
    static Entry module(){return module;}
    static void forget(Object obj){if(!(obj instanceof View))return;View v=(View)obj;Hidden h=hidden.remove(v);if(h!=null)setVisibility(v,h.original);}
    private static void setVisibility(View v,int visibility){restoring.set(true);try{v.setVisibility(visibility);}finally{restoring.remove();}}
    private static void rememberTitle(TextView view,String local){CharSequence current=view.getText();if(!local.contentEquals(current))titles.put(view,current);localTitles.put(view,local);if(module.enabled("local_vip")){setTitle(view,local);module.entered("local_vip");module.hit("local_vip");}}
    private static void badge(Object holder)throws Throwable{
        View icons=(View)invoke(holder,"getVipIconListView");ViewGroup parent=(ViewGroup)icons.getParent();if(parent==null)return;
        TextView label=(TextView)parent.findViewWithTag("umusic-local-vip-badge");
        if(label==null){label=new TextView(icons.getContext());label.setTag("umusic-local-vip-badge");label.setText("超级会员");label.setContentDescription("超级会员（U 音本地状态）");label.setTextColor(Color.rgb(123,64,10));label.setTextSize(12);label.setTypeface(Typeface.DEFAULT,Typeface.BOLD);label.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int dp=(int)(icons.getResources().getDisplayMetrics().density*5);label.setPadding(dp,0,dp,0);GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(255,224,151));bg.setCornerRadius(dp);label.setBackground(bg);parent.addView(label,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.MATCH_PARENT));}
        badges.put(holder,new Badge(icons,label));hide(icons,"local_vip");label.setVisibility(module.enabled("local_vip")?View.VISIBLE:View.GONE);
    }
    static void refresh(){
        synchronized(hidden){for(Map.Entry<View,Hidden> e:hidden.entrySet())setVisibility(e.getKey(),module.enabled(e.getValue().feature)?View.GONE:e.getValue().original);}
        synchronized(titles){for(Map.Entry<TextView,CharSequence> e:titles.entrySet()){TextView v=e.getKey();setTitle(v,module.enabled("local_vip")?localTitle(v):e.getValue());}}
        synchronized(badges){for(Badge b:badges.values()){TextView t=b.text.get();if(t!=null)t.setVisibility(module.enabled("local_vip")?View.VISIBLE:View.GONE);}}
    }
    private static void setTitle(TextView view,CharSequence value){
        if((USER_INFO+"MyTabSubtitleMarqueeTextView").equals(view.getClass().getName())){
            try{invoke(view,"setMarqueeText",new Class<?>[]{CharSequence.class},value);return;}
            catch(Throwable error){module.warning("local_vip","setMarqueeText",error);}
        }
        view.setText(value);
    }
    private static final Map<TextView,String> localTitles=Collections.synchronizedMap(new WeakHashMap<TextView,String>());
    private static String localTitle(TextView v){return localTitles.getOrDefault(v,"超级会员");}
    static Object field(Object obj,String name)throws Exception{if(obj==null)return null;for(Class<?> cls=obj.getClass();cls!=null;cls=cls.getSuperclass())try{java.lang.reflect.Field f=cls.getDeclaredField(name);f.setAccessible(true);return f.get(obj);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    static Object invoke(Object obj,String name)throws Throwable{return invoke(obj,name,new Class<?>[0]);}
    static Object invoke(Object obj,String name,Class<?>[] types,Object...args)throws Throwable{for(Class<?> cls=obj.getClass();cls!=null;cls=cls.getSuperclass())try{Method method=cls.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(obj,args);}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(name);}
    static Object invokeStatic(String owner,String name,Class<?>[] types,Object...args)throws Throwable{Method method=type(owner).getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(null,args);}
}
