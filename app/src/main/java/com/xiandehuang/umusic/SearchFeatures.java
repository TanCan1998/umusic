package com.xiandehuang.umusic;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Search-only ads and AI entrance hooks, leaving history, queries and organic results intact. */
final class SearchFeatures {
    private static final String ONLINE="com.tencent.qqmusic.fragment.search.OnlineSearchFragment";
    private static final String HOT="com.tencent.qqmusic.fragment.search.q";
    private static final String BASE="com.tencent.qqmusic.fragment.SearchBaseListFragmentNew";
    private static final String BANNER="com.tencent.qqmusic.business.ad.search.banner.k";
    private static final String EASTER="com.tencent.qqmusic.business.ad.search.b";
    static void install(Entry m,ClassLoader cl){
        // P4 binds the actual C0 button. z5 is a separate asynchronous PAG appearance path.
        UiFeatures.register("yuanbao",ONLINE,"P4",void.class,c->{
            Object result=c.proceed();hideAi(c.getThisObject());return result;
        });
        UiFeatures.register("yuanbao",ONLINE,"z5",void.class,c->{
            if(!m.enabled("yuanbao"))return c.proceed();
            m.entered("yuanbao");hideAi(c.getThisObject());UiFeatures.invoke(c.getThisObject(),"I4");m.hit("yuanbao");return null;
        },"boolean");
        UiFeatures.register("search_ads",HOT,"j1",void.class,c->{
            if(!m.enabled("search_ads"))return c.proceed();
            m.entered("search_ads");Object layout=UiFeatures.field(c.getThisObject(),"m");
            if(layout!=null)UiFeatures.hide(UiFeatures.field(layout,"d"),"search_ads");m.hit("search_ads");return null;
        },"boolean");
        // Skip the ad header's insertion and its associated empty placeholder as a unit.
        UiFeatures.register("search_ads",BASE,"e3",void.class,c->{
            if(!m.enabled("search_ads"))return c.proceed();
            m.entered("search_ads");Object self=c.getThisObject();UiFeatures.hide(UiFeatures.invoke(self,"J3"),"search_ads");
            UiFeatures.invoke(self,"o3");UiFeatures.hide(UiFeatures.field(self,"p"),"search_ads");m.hit("search_ads");return null;
        },"com.tencent.qqmusic.business.online.response.gson.SearchResultMixBodyGson","com.tencent.qqmusic.business.online.response.gson.SearchResultAdvertObjectGson");
        UiFeatures.register("search_ads","com.tencent.qqmusic.fragment.search.view.SearchTopAdView","setData",void.class,c->{
            if(!m.enabled("search_ads"))return c.proceed();
            m.entered("search_ads");UiFeatures.hide(c.getThisObject(),"search_ads");m.hit("search_ads");return null;
        },"com.tencent.qqmusic.business.online.response.gson.SearchResultAdvertGson","java.lang.String");
        // Match the host's disabled-position branch: finish the banner listener without requesting an ad.
        UiFeatures.register("search_ads",BANNER,"E",void.class,c->{
            if(!m.enabled("search_ads"))return c.proceed();
            m.entered("search_ads");UiFeatures.hide(UiFeatures.field(c.getThisObject(),"h"),"search_ads");
            Object listener=UiFeatures.field(c.getThisObject(),"f");if(listener!=null)UiFeatures.invoke(listener,"a");m.hit("search_ads");return null;
        });
        UiFeatures.register("search_ads",BANNER,"h",void.class,c->{
            if(!m.enabled("search_ads"))return c.proceed();
            m.entered("search_ads");Object layout=c.getArg(0);
            if(layout!=null)for(String field:new String[]{"g","h","i"})UiFeatures.hide(UiFeatures.field(layout,field),"search_ads");
            UiFeatures.hide(UiFeatures.field(c.getThisObject(),"h"),"search_ads");m.hit("search_ads");return null;
        },"com.tencent.qqmusic.fragment.search.u4$e");
        UiFeatures.register("search_ads",EASTER,"h",void.class,c->{
            m.entered("search_ads");if(!m.enabled("search_ads"))return c.proceed();m.hit("search_ads");return null;
        },"java.lang.String");
        UiFeatures.register("search_ads",EASTER,"j",void.class,c->{
            if(!m.enabled("search_ads"))return c.proceed();
            m.entered("search_ads");UiFeatures.hide(c.getArg(0),"search_ads");m.hit("search_ads");return null;
        },"android.view.ViewGroup","int");
        // All families below were read through MT including their inner classes.
        for(String owner:new String[]{ONLINE,HOT,BASE,BANNER,EASTER,"com.tencent.qqmusic.fragment.search.SearchMixFragmentNew","com.tencent.qqmusic.business.ad.search.f"})try{
            Class<?> type=Class.forName(owner,false,cl);
            for(Method method:type.getDeclaredMethods())if(!Modifier.isAbstract(method.getModifiers())&&!Modifier.isNative(method.getModifiers()))m.deoptimize(method);
            for(java.lang.reflect.Constructor<?> ctor:type.getDeclaredConstructors())m.deoptimize(ctor);
        }catch(Throwable t){m.warning("search_ads",owner+" caller deopt",t);}
    }
    private static void hideAi(Object page)throws Throwable{
        UiFeatures.hide(UiFeatures.field(page,"C0"),"yuanbao");
        UiFeatures.hide(UiFeatures.field(page,"E0"),"yuanbao");
    }
}
