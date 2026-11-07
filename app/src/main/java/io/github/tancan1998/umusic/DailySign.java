package io.github.tancan1998.umusic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Account/day guarded real H5 day-sign protocol via the host's authenticated CGI transport. */
final class DailySign {
    private static final String SERVICE="music.actCenter.DaysignactSvr";
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static Attempt active;
    private static final Map<String,Long> attempts=new HashMap<>();
    private static String accountHash;
    private static long accountGeneration;
    private static Context appContext;
    private static Object accountListener;
    private static Method accountManager,addAccountListener;
    private static volatile boolean accountListenerReady;
    private static final Runnable scheduledCheck=()->{if(appContext!=null)start(appContext);};
    private static final class Attempt {
        final String key;final long generation;final AtomicBoolean completed=new AtomicBoolean();Runnable timeout;
        Attempt(String k,long g){key=k;generation=g;}
    }
    private static volatile boolean ready;
    private static volatile String status="未执行";

    private static Method isLogin,getUin,paramString,paramJson,factory,build,dispatch,respCode,respBytes,moduleLookup;
    private static Constructor<?> params;
    private static Class<?> listener;
    private static Entry module;
    static void install(Entry m,ClassLoader cl){
        module=m;m.expected("auto_sign");
        try{
            Class<?> user=Class.forName("com.tencent.qqmusic.business.user.UserHelper",false,cl);
            isLogin=method(user,"isLogin",boolean.class);getUin=method(user,"getUin",String.class);
            Class<?> p=Class.forName("com.tencent.qqmusiccommon.cgi.request.JsonRequest",false,cl);params=p.getConstructor();
            paramString=method(p,"h",p,String.class,String.class);paramJson=method(p,"d",p,String.class);
            Class<?> args=Class.forName("com.tencent.qqmusiccommon.cgi.request.ModuleRequestArgs",false,cl);
            factory=method(Class.forName("com.tencent.qqmusiccommon.cgi.request.e",false,cl),"n",args,String.class,String.class,p);
            Class<?> w=Class.forName("com.tencent.qqmusicplayerprocess.network.w",false,cl);build=method(args,"o",w);
            listener=Class.forName("com.tencent.qqmusicplayerprocess.network.OnResultListener",false,cl);
            dispatch=method(Class.forName("com.tencent.qqmusicplayerprocess.network.k",false,cl),"n",int.class,w,listener);
            Class<?> rsp=Class.forName("com.tencent.qqmusicplayerprocess.network.g",false,cl);
            respCode=method(rsp,"e",int.class);respBytes=method(rsp,"d",byte[].class);
            Class<?> mr=Class.forName("com.tencent.qqmusiccommon.cgi.response.ModuleResp",false,cl);
            Class<?> mb=Class.forName("com.tencent.qqmusiccommon.cgi.response.ModuleResp$b",false,cl);
            moduleLookup=method(mr,"b",mb,String.class,String.class);
            mr.getDeclaredField("b");rsp.getDeclaredField("g");mb.getDeclaredField("c");mb.getDeclaredField("a");
            ready=true;m.installed("auto_sign");
            installAccountListener(cl);
            SignTasks.install(m,cl);
        }catch(Throwable t){status="接口不可用";m.warning("auto_sign","日签协议",t);}
    }
    private static Method method(Class<?> cls,String name,Class<?> ret,Class<?>...types)throws Exception{Method m=cls.getDeclaredMethod(name,types);if(m.getReturnType()!=ret)throw new NoSuchMethodException(cls.getName()+"#"+name+" return");m.setAccessible(true);return m;}
    static String status(){return SignTasks.status(status);}
    private static volatile int streakDays;
    static int streak(){return streakDays;}
    // Count confirmed calendar days for the current account, including earlier U 音 versions.
    static int confirmedStreak(SharedPreferences preferences,String key){
        if(key.length()<11)return 0;
        int dateStart=key.length()-10;
        String prefix=key.substring(0,dateStart),date=key.substring(dateStart);
        try{
            SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);fmt.setLenient(false);
            Calendar day=Calendar.getInstance();day.setTime(fmt.parse(date));
            if(!preferences.getBoolean(prefix+fmt.format(day.getTime()),false))day.add(Calendar.DATE,-1);
            int count=0;
            while(count<36600&&preferences.getBoolean(prefix+fmt.format(day.getTime()),false)){count++;day.add(Calendar.DATE,-1);}
            return count;
        }catch(java.text.ParseException e){return 0;}
    }
    private static void installAccountListener(ClassLoader cl){
        module.expected("auto_sign");
        try{
            Class<?> manager=Class.forName("com.tencent.qqmusic.business.user.y0",false,cl);
            Class<?> events=Class.forName("com.tencent.qqmusic.business.user.u0",false,cl);
            Class<?> result=Class.forName("com.tencent.qqmusic.business.user.login.loginreport.a",false,cl);
            method(events,"onLogin",void.class,int.class,result);method(events,"onLogout",void.class);
            accountManager=method(manager,"Y",manager);addAccountListener=method(manager,"w",void.class,events);
            accountListener=Proxy.newProxyInstance(cl,new Class<?>[]{events},(proxy,m,args)->{
                if(m.getDeclaringClass()==Object.class){if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);if(m.getName().equals("equals"))return proxy==args[0];return "UMusicAccountListener";}
                if(m.getName().equals("onLogout"))main.post(()->{accountChanged(null);status="等待登录";module.diagnosticChanged();});
                else if(m.getName().equals("onLogin"))main.post(()->{main.removeCallbacks(scheduledCheck);main.postDelayed(scheduledCheck,3000);});
                return null;
            });
        }catch(Throwable t){module.warning("auto_sign","账号事件接口",t);}
    }
    private static void ensureAccountListener(){
        if(accountListenerReady||accountListener==null)return;
        try{addAccountListener.invoke(accountManager.invoke(null),accountListener);accountListenerReady=true;module.installed("auto_sign");}
        catch(Throwable t){module.warning("auto_sign","账号事件注册",t);}
    }
    static boolean accountEventsReady(){return accountListenerReady;}
    private static void accountChanged(String hash){
        if(Objects.equals(accountHash,hash))return;
        accountHash=hash;accountGeneration++;streakDays=0;status=hash==null?"等待登录":"等待签到";
        if(active!=null){active.completed.set(true);if(active.timeout!=null)main.removeCallbacks(active.timeout);active=null;}
    }
    static String currentAccount()throws Exception{
        if(!Boolean.TRUE.equals(isLogin.invoke(null)))return null;
        String uin=(String)getUin.invoke(null);return uin==null||uin.isEmpty()||uin.equals("0")?null:hash(uin);
    }
    static void maybe(Context context){
        if(!ready||module==null||context==null)return;
        appContext=context.getApplicationContext();
        SignTasks.maybe(appContext);
        main.post(()->{ensureAccountListener();if(!module.enabled("auto_sign"))return;main.removeCallbacks(scheduledCheck);main.postDelayed(scheduledCheck,3000);});
    }
    private static void start(Context app){
        SignTasks.maybe(app);
        if(!module.enabled("auto_sign"))return;
        Attempt pending=null;
        try{
            String hash=currentAccount();accountChanged(hash);
            if(hash==null){status="等待登录";module.diagnosticChanged();return;}
            String date=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());
            String key="daily-sign-"+hash+"-"+date;
            SharedPreferences preferences=module.preferences(app);
            streakDays=confirmedStreak(preferences,key);
            if(preferences.getBoolean(key,false)){status="今日已签到";module.diagnosticChanged();return;}
            if(active!=null)return;
            long now=android.os.SystemClock.elapsedRealtime();Long previous=attempts.get(key);
            if(previous!=null&&now-previous<60000)return;
            final Attempt request=new Attempt(key,accountGeneration);pending=request;active=request;
            attempts.put(key,now);status="签到请求中";module.entered("auto_sign");
            request.timeout=()->finish(app,request,false,"签到失败：请求超时");main.postDelayed(request.timeout,30000);
            Object param=params.newInstance();paramString.invoke(param,"date","");
            Object req=build.invoke(factory.invoke(null,SERVICE,"doSignIn",param));
            Object callback=Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},(proxy,m,args)->{
                if(m.getDeclaringClass()==Object.class){if(m.getName().equals("toString"))return "UMusicDailySignCallback";if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);if(m.getName().equals("equals"))return proxy==args[0];}
                if(m.getName().equals("onResult")){
                    Result r;try{r=parseResponse(args[0]);}catch(Throwable e){r=new Result(false,"签到失败：回执无法解析");}
                    final Result result=r;main.post(()->finish(app,request,result.success,result.message));
                }return null;
            });
            if(!Objects.equals(hash,currentAccount())){discard(request);maybe(app);return;}
            int id=(Integer)dispatch.invoke(null,req,callback);
            if(id<=0)finish(app,request,false,"签到失败：请求未派发");
        }catch(Throwable t){
            if(pending!=null)finish(app,pending,false,"签到失败："+t.getClass().getSimpleName());
            else {status="签到失败："+t.getClass().getSimpleName();notify(app,status);}
            module.warning("auto_sign","签到执行",t);
        }
    }
    private static void discard(Attempt request){
        request.completed.set(true);if(request.timeout!=null)main.removeCallbacks(request.timeout);
        if(active==request)active=null;
    }
    private static String hash(String uin)throws Exception{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(uin.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:digest)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    private static final class Result {final boolean success;final String message;Result(boolean s,String m){success=s;message=m;}}
    private static Result parseResponse(Object response)throws Throwable{
        int network=(Integer)respCode.invoke(response);if(network!=0)return new Result(false,"签到失败：网络码 "+network);
        // Prefer decoded module response; raw response is used only if the transport did not convert it.
        Object mr=UiFeatures.field(response,"g");
        if(mr!=null){
            int outer=(Integer)UiFeatures.field(mr,"b");if(outer!=0)return new Result(false,"签到失败：接口码 "+outer);
            Object sub=moduleLookup.invoke(mr,SERVICE,"doSignIn");
            if(sub==null)return new Result(false,"签到失败：缺少签到回执");
            int code=(Integer)UiFeatures.field(sub,"c");Object data=UiFeatures.field(sub,"a");
            if(data==null){Object parsed=UiFeatures.invoke(sub,"d");if(parsed!=null)data=parsed;}
            return classify(code,data==null?null:new JSONObject(data.toString()));
        }
        byte[] bytes=(byte[])respBytes.invoke(response);if(bytes==null||bytes.length==0)return new Result(false,"签到失败：回执为空");
        JSONObject all=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        if(!all.has("code")||all.optInt("code",-1)!=0)return new Result(false,"签到失败：顶层回执异常");
        JSONObject sub=all.optJSONObject("req_0");if(sub==null)return new Result(false,"签到失败：缺少签到回执");
        return classify(sub.optInt("code",-1),sub.optJSONObject("data"));
    }
    static Result classify(int code,JSONObject data){
        if(code!=0)return new Result(false,code==1000?"签到失败：请重新登录":"签到失败：接口码 "+code);
        if(data==null||!data.has("code"))return new Result(false,"签到失败：缺少业务结果");
        int business=data.optInt("code",-1);
        if(business==0)return new Result(true,"U 音：日签签到成功");
        if(business==2)return new Result(true,"U 音：今日已签到");
        return new Result(false,"签到失败：业务码 "+business);
    }
    private static void finish(Context app,Attempt request,boolean success,String message){
        if(request.completed.get())return;
        try{
            String hash=currentAccount();
            if(request.generation!=accountGeneration||!Objects.equals(hash,accountHash)){
                discard(request);accountChanged(hash);maybe(app);return;
            }
        }catch(Exception error){discard(request);status="等待登录";streakDays=0;module.diagnosticChanged();return;}
        if(!request.completed.compareAndSet(false,true))return;
        if(request.timeout!=null)main.removeCallbacks(request.timeout);if(active==request)active=null;status=message;
        if(success){SharedPreferences p=module.preferences(app);p.edit().putBoolean(request.key,true).apply();streakDays=confirmedStreak(p,request.key);module.hit("auto_sign");}
        notify(app,message);module.diagnosticChanged();
    }
    interface Reply {void result(int code,JSONObject data,String error);}
    static String uin()throws Exception{return (String)getUin.invoke(null);}
    static void request(String service,String operation,JSONObject values,Reply reply)throws Exception{
        Object param=paramJson.invoke(null,values.toString());
        Object req=build.invoke(factory.invoke(null,service,operation,param));
        method(req.getClass(),"F",req.getClass(),String.class).invoke(req,new Object[]{null});
        method(req.getClass(),"a",req.getClass(),String.class,String.class).invoke(req,"uin",uin());
        Object callback=Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},(proxy,m,args)->{
            if(m.getDeclaringClass()==Object.class){if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);if(m.getName().equals("equals"))return proxy==args[0];return "UMusicSignTaskCallback";}
            if(m.getName().equals("onResult")){
                int code=-1;JSONObject data=null;String error=null;
                try{
                    Object response=args[0];int network=(Integer)respCode.invoke(response);
                    if(network!=0)error="网络码 "+network;
                    else{
                        Object mr=UiFeatures.field(response,"g");
                        if(mr!=null){
                            int outer=(Integer)UiFeatures.field(mr,"b");
                            if(outer!=0)error="顶层码 "+outer;
                            else{
                                Object sub=moduleLookup.invoke(mr,service,operation);
                                if(sub==null)error="缺少回执";
                                else{code=(Integer)UiFeatures.field(sub,"c");Object raw=UiFeatures.field(sub,"a");if(raw==null)raw=UiFeatures.invoke(sub,"d");if(raw!=null)data=new JSONObject(raw.toString());}
                            }
                        }else{
                            byte[] raw=(byte[])respBytes.invoke(response);
                            if(raw==null)error="空回执";
                            else{JSONObject all=new JSONObject(new String(raw,StandardCharsets.UTF_8));if(all.optInt("code",-1)!=0)error="顶层回执异常";else{JSONObject sub=all.optJSONObject("req_0");if(sub==null)error="缺少回执";else{code=sub.optInt("code",-1);data=sub.optJSONObject("data");}}}
                        }
                    }
                }catch(Throwable e){error="回执解析失败";}
                final int c=code;final JSONObject d=data;final String e=error;main.post(()->reply.result(c,d,e));
            }return null;
        });
        if((Integer)dispatch.invoke(null,req,callback)<=0)main.post(()->reply.result(-1,null,"请求未派发"));
    }
    private static void notify(Context app,String message){if(!module.enabled("auto_sign"))return;main.post(()->Toast.makeText(app,message,Toast.LENGTH_LONG).show());}
}
