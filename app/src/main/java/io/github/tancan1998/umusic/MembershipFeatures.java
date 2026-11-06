package io.github.tancan1998.umusic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import io.github.libxposed.api.XposedInterface;

/** Local decisions only; never changes account records or network responses. */
final class MembershipFeatures {
    private static final String USER="com.tencent.qqmusic.business.user.";
    private static final String SOUND="com.tencent.qqmusic.activity.soundfx.supersound.";
    static void install(Entry module,ClassLoader loader){
        module.replace("vip_popup","com.tencent.qqmusic.dialog.vipremind.f","q",void.class,null,
            "java.lang.String","com.tencent.qqmusic.dialog.universal.response.c","java.lang.String","com.tencent.qqmusiccommon.xray.dialog.b");
        module.replace("vip_popup","com.tencent.qqmusic.business.playernew.vipguide.s","x",boolean.class,false,"boolean");
        // Both UserHelper and direct LocalUser calls occur in the reviewed host.
        for(String name:new String[]{"isVip","isGreenVip","isSuperGreen","isSuperVip"})
            memberQuery(module,loader,USER+"UserHelper",name);
        for(String name:new String[]{"n2","L1","i2","j2"})
            memberQuery(module,loader,USER+"a",name);
        module.replace("sound_effects",SOUND+"kuikly.u6","V",boolean.class,true,
            "com.tencent.qqmusicplayerprocess.audio.supersound.AepEffect","android.app.Activity","java.lang.String");
        installRights(module,loader);
    }
    private static void memberQuery(Entry m,ClassLoader loader,String owner,String name){
        m.expected("local_vip");m.expected("sound_effects");
        try{
            Method method=Class.forName(owner,false,loader).getDeclaredMethod(name);
            if(method.getReturnType()!=boolean.class)throw new NoSuchMethodException("boolean return required");
            m.hook(method).setId("umusic-member-"+owner+"-"+name).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(c->{
                boolean soundCall=inSoundCall();
                // Sound callers obey only their own switch, including when local VIP is on.
                String feature=soundCall?"sound_effects":"local_vip";
                m.entered(feature);
                if(m.enabled(feature)){m.hit(feature);return true;}
                return c.proceed();
            });
            m.installed("local_vip");m.installed("sound_effects");
            try{m.deoptimize(method);}catch(Throwable t){m.warning("local_vip",owner+"#"+name+" deoptimize",t);}
        }catch(Throwable t){m.warning("local_vip",owner+"#"+name,t);m.warning("sound_effects",owner+"#"+name,t);}
    }
    static boolean inSoundCall(){
        for(StackTraceElement frame:Thread.currentThread().getStackTrace()){
            String name=frame.getClassName();
            if(name.startsWith("com.tencent.qqmusic.activity.soundfx.")||name.startsWith("com.tencent.qqmusicplayerprocess.audio.supersound."))return true;
        }
        return false;
    }
    private static void installRights(Entry m,ClassLoader loader){
        final String owner=SOUND+"rights.SoundEffectRightsManager";
        // Register as a unit: the synchronous and Rx entry points share the same result contract.
        m.expected("sound_effects");m.expected("sound_effects");
        List<XposedInterface.HookHandle> handles=new ArrayList<>();
        try{
            Class<?> result=Class.forName(SOUND+"rights.g",false,loader);
            Class<?> success=Class.forName(SOUND+"rights.g$d",false,loader);
            if(!result.isAssignableFrom(success))throw new IllegalStateException("rights result hierarchy changed");
            Constructor<?> constructor=success.getConstructor();
            Class<?> rx=Class.forName("rx.d",false,loader);
            Method just=rx.getDeclaredMethod("a0",Object.class);
            if(just.getReturnType()!=rx)throw new NoSuchMethodException("Rx just return changed");
            Class<?> manager=Class.forName(owner,false,loader);
            Method async=manager.getDeclaredMethod("R",int.class,int.class),sync=manager.getDeclaredMethod("e0",int.class,int.class);
            if(async.getReturnType()!=rx||sync.getReturnType()!=result)throw new NoSuchMethodException("rights entry return changed");
            for(Method method:new Method[]{async,sync}){
                handles.add(m.hook(method).setId("umusic-sound-rights-"+method.getName()).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(c->{
                    m.entered("sound_effects");if(!m.enabled("sound_effects"))return c.proceed();
                    Object value=constructor.newInstance();
                    if(method==async)value=just.invoke(null,value);
                    m.hit("sound_effects");return value;
                }));
            }
            m.installed("sound_effects");m.installed("sound_effects");
        }catch(Throwable t){for(int i=handles.size()-1;i>=0;i--)handles.get(i).unhook();m.warning("sound_effects",owner,t);}
    }
}
