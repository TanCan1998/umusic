package io.github.tancan1998.umusic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;
import java.lang.reflect.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

/** Independent, account/day guarded sign-ins. Reward success requires a server read-back. */
final class SignTasks {
    private static final String ACT="music.actCenter.ActCenterSignNewSvr",VIP="music.lvz.MuFest13TaskSvr";
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static final Map<String,Task> tasks=new LinkedHashMap<>();
    private static final Map<String,String> results=new LinkedHashMap<>();
    private static final Map<String,Long> attempts=new HashMap<>();
    private static final Map<String,Run> active=new HashMap<>();
    private static Entry module;private static Context context;private static String account;private static long generation;
    private static final Runnable check=()->{if(context!=null)start(context);};
    private static final class Task {
        final String id,label,act,scene;final boolean vip;
        Task(String i,String l,String a,String s,boolean v){id=i;label=l;act=a;scene=s;vip=v;}
    }
    private static final class Run {
        final Task task;final String account,key;final long generation;final AtomicBoolean ended=new AtomicBoolean();
        Runnable timeout;int serial;Run(Task t,String a,String k,long g){task=t;account=a;key=k;generation=g;}
    }
    static void install(Entry m,ClassLoader cl){
        module=m;tasks.put("vip",new Task("vip","会员成长值","","",true));
        tasks.put("coin-Z25hHGi",new Task("coin-Z25hHGi","金币中心","Z25hHGi","2",false));
        tasks.put("coin-Z156KEu",new Task("coin-Z156KEu","抽奖页签到","Z156KEu","",false));
        // Observe the host's current activity IDs instead of assuming every future sign page shares a fixed ID.
        try{
            Class<?> p=Class.forName("com.tencent.qqmusiccommon.cgi.request.JsonRequest",false,cl);
            Method f=Class.forName("com.tencent.qqmusiccommon.cgi.request.e",false,cl).getDeclaredMethod("n",String.class,String.class,p);
            m.hook(f).setId("umusic-sign-discovery").intercept(c->{
                Object value=c.proceed();
                if(ACT.equals(c.getArg(0))){String operation=(String)c.getArg(1);
                    if(Arrays.asList("GetSignInSummary","GetSignInTaskList","SignIn").contains(operation)){
                        try{JSONObject json=new JSONObject(UiFeatures.invoke(c.getArg(2),"a").toString());String id=json.optString("ActID","");String scene=json.optString("ScenesID","");
                            if(!id.isEmpty()&&id.matches("[A-Za-z0-9_-]{1,100}"))main.post(()->discover(id,scene));
                        }catch(Throwable ignored){}
                    }
                }return value;
            });m.deoptimize(f);
            Class<?> args=Class.forName("com.tencent.qqmusiccommon.cgi.request.ModuleRequestArgs",false,cl);
            Method build=args.getDeclaredMethod("o");
            m.hook(build).setId("umusic-sign-batch-discovery").intercept(c->{
                try{Object entries=UiFeatures.invoke(c.getThisObject(),"j");if(entries instanceof Map)for(Object item:((Map<?,?>)entries).values()){
                    String service=String.valueOf(UiFeatures.field(item,"c")),operation=String.valueOf(UiFeatures.field(item,"d"));
                    if(ACT.equals(service)&&Arrays.asList("GetSignInSummary","GetSignInTaskList","SignIn").contains(operation)){
                        Object param=UiFeatures.field(item,"e");JSONObject json=new JSONObject(UiFeatures.invoke(param,"a").toString());String id=json.optString("ActID",""),scene=json.optString("ScenesID","");
                        if(!id.isEmpty()&&id.matches("[A-Za-z0-9_-]{1,100}"))main.post(()->discover(id,scene));
                    }
                }}catch(Throwable ignored){}return c.proceed();
            });m.deoptimize(build);
        }catch(Throwable e){m.warning("auto_sign","签到活动识别",e);}
    }
    private static void discover(String id,String scene){
        String key="coin-"+id;Task old=tasks.get(key);
        if(old==null||(!scene.isEmpty()&&!scene.equals(old.scene))){tasks.put(key,new Task(key,old==null?"活动签到 "+(tasks.size()-1):old.label,id,scene,false));if(context!=null)maybe(context);}
    }
    static void maybe(Context app){if(module==null||app==null)return;context=app.getApplicationContext();main.removeCallbacks(check);main.postDelayed(check,3500);}
    static String status(String daily){StringBuilder out=new StringBuilder("普通日签：").append(daily);for(Map.Entry<String,String> item:results.entrySet()){Task t=tasks.get(item.getKey());if(t!=null)out.append("；").append(t.label).append("：").append(item.getValue());}return out.toString();}
    static JSONObject diagnostics(){JSONObject out=new JSONObject();try{for(Map.Entry<String,String> e:results.entrySet()){Task t=tasks.get(e.getKey());out.put(t==null?e.getKey():t.label,e.getValue());}}catch(JSONException ignored){}return out;}
    private static void start(Context app){
        if(!module.enabled("auto_sign"))return;
        try{
            String nowAccount=DailySign.currentAccount();
            if(!Objects.equals(account,nowAccount)){account=nowAccount;generation++;for(Run r:active.values()){r.ended.set(true);main.removeCallbacks(r.timeout);}active.clear();results.clear();}
            if(account==null){for(Task t:tasks.values())results.put(t.id,"等待登录");module.diagnosticChanged();return;}
            String date=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());
            for(Task task:new ArrayList<>(tasks.values())){
                String key="sign-v2-"+task.id+"-"+account+"-"+date;
                if(module.preferences(app).getBoolean(key,false)){results.put(task.id,"今日已确认");continue;}
                if(active.containsKey(task.id))continue;long now=SystemClock.elapsedRealtime();Long prev=attempts.get(key);if(prev!=null&&now-prev<60000)continue;
                Run r=new Run(task,account,key,generation);r.timeout=()->done(r,false,"失败：请求超时");active.put(task.id,r);attempts.put(key,now);results.put(task.id,"查询中");
                main.postDelayed(r.timeout,90000);if(task.vip)vip(r,false);else activity(r,false);
            }module.diagnosticChanged();
        }catch(Exception e){module.warning("auto_sign","扩展签到启动",e);}
    }
    private static boolean valid(Run r){
        if(r.ended.get())return false;
        try{if(r.generation!=generation||!Objects.equals(r.account,DailySign.currentAccount())||!module.enabled("auto_sign")||!r.key.endsWith(new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date()))){r.ended.set(true);main.removeCallbacks(r.timeout);if(active.get(r.task.id)==r)active.remove(r.task.id);maybe(context);return false;}return true;}
        catch(Exception e){return false;}
    }
    private interface Data {void result(JSONObject data)throws Exception;}
    private static JSONObject values(String...pairs)throws JSONException{JSONObject j=new JSONObject();for(int i=0;i<pairs.length;i+=2)j.put(pairs[i],pairs[i+1]);return j;}
    private static void send(Run r,String service,String operation,JSONObject args,Data next){
        if(!valid(r))return;int serial=++r.serial;AtomicBoolean called=new AtomicBoolean();
        try{module.entered("auto_sign");DailySign.request(service,operation,args,(code,data,error)->{
            if(!called.compareAndSet(false,true)||!valid(r)||serial!=r.serial)return;
            if(error!=null||code!=0||data==null){done(r,false,"失败："+(error!=null?error:"接口码 "+code));return;}
            try{next.result(data);}catch(Exception e){done(r,false,"失败：回执字段异常");}
        });}catch(Exception e){done(r,false,"失败：请求异常");}
    }
    private static void vip(Run r,boolean verify)throws Exception{
        send(r,VIP,"EveryDaySignLvzScore",values("Uin",DailySign.uin(),"Cmd","qry"),data->{
            int ret=data.optInt("Ret",-1);
            if(ret==20019){done(r,true,"今日已确认");return;}
            if(ret!=0&&ret!=20028){done(r,false,"失败：业务码 "+ret+" "+data.optString("Msg","").replaceAll("[0-9]{5,}","*").substring(0,Math.min(60,data.optString("Msg","").replaceAll("[0-9]{5,}","*").length())));return;}
            if(verify){done(r,false,"失败：服务端仍未签到");return;}
            send(r,VIP,"EveryDaySignLvzScore",values("Uin",DailySign.uin(),"Cmd","get"),signed->{
                int code=signed.optInt("Ret",-1);if(code!=0&&code!=20019){done(r,false,"失败：业务码 "+code);return;}vip(r,true);
            });
        });
    }
    private static boolean signed(JSONObject info){Object value=info.opt("IsSignIn");return Boolean.TRUE.equals(value)||(value instanceof Number&&((Number)value).doubleValue()==1d)||"1".equals(String.valueOf(value))||"true".equalsIgnoreCase(String.valueOf(value));}
    private static boolean okay(JSONObject data){return data.has("retCode")&&data.optInt("retCode",-1)==0;}
    private static void activity(Run r,boolean verify)throws JSONException{
        send(r,ACT,"GetSignInSummary",values("ActID",r.task.act),summary->{
            if(!okay(summary)){done(r,false,"失败：状态码 "+summary.optInt("retCode",-1));return;}
            JSONObject args=values("ActID",r.task.act);if(!r.task.scene.isEmpty())args.put("ScenesID",r.task.scene);
            send(r,ACT,"GetSignInTaskList",args,data->{
                if(!okay(data)){done(r,false,"失败：任务码 "+data.optInt("retCode",-1));return;}
                JSONObject info=data.optJSONObject("Info");if(info==null)info=summary.optJSONObject("Info");
                if(info==null||!info.has("IsSignIn")){done(r,false,"失败：缺少签到状态");return;}
                if(!signed(info)){
                    if(verify){done(r,false,"失败：服务端仍未签到");return;}
                    send(r,ACT,"SignIn",args,signed->{if(!okay(signed)){done(r,false,"失败：签到码 "+signed.optInt("retCode",-1));return;}activity(r,true);});return;
                }
                JSONObject lists=data.optJSONObject("TaskListInfo");lists=lists==null?null:lists.optJSONObject("TaskList");
                JSONObject rewards=lists==null?null:lists.optJSONObject("ContinueTaskList");
                if(rewards==null){done(r,false,"已签，奖励状态未知");return;}
                ArrayList<String> pending=new ArrayList<>();for(Iterator<String> i=rewards.keys();i.hasNext();){JSONObject item=rewards.optJSONObject(i.next());if(item!=null&&item.optInt("State",-1)==2){String id=item.optString("ID","");if(id.isEmpty()){done(r,false,"已签，奖励编号缺失");return;}pending.add(id);}}
                award(r,pending,0);
            });
        });
    }
    private static void award(Run r,List<String> ids,int index)throws JSONException{
        if(index==ids.size()){if(ids.isEmpty())done(r,true,"已签，奖励已确认");else verifyRewards(r);return;}
        send(r,ACT,"AwardPrize",values("ActID",r.task.act,"TaskID",ids.get(index)),data->{int code=data.optInt("retCode",-1);if(code!=0&&code!=100004){done(r,false,"已签，领奖失败 "+code);return;}award(r,ids,index+1);});
    }
    private static void verifyRewards(Run r)throws JSONException{
        JSONObject args=values("ActID",r.task.act);if(!r.task.scene.isEmpty())args.put("ScenesID",r.task.scene);
        send(r,ACT,"GetSignInTaskList",args,data->{
            JSONObject info=data.optJSONObject("Info"),lists=data.optJSONObject("TaskListInfo");lists=lists==null?null:lists.optJSONObject("TaskList");JSONObject rewards=lists==null?null:lists.optJSONObject("ContinueTaskList");
            if(!okay(data)||info==null||!signed(info)||rewards==null){done(r,false,"已签，领奖未确认");return;}
            for(Iterator<String> i=rewards.keys();i.hasNext();){JSONObject item=rewards.optJSONObject(i.next());if(item!=null&&item.optInt("State",-1)==2){done(r,false,"已签，仍有待领奖励");return;}}
            done(r,true,"已签，奖励已确认");
        });
    }
    private static void done(Run r,boolean success,String message){
        if(!valid(r)||!r.ended.compareAndSet(false,true))return;main.removeCallbacks(r.timeout);if(active.get(r.task.id)==r)active.remove(r.task.id);
        results.put(r.task.id,message);if(success){module.preferences(context).edit().putBoolean(r.key,true).apply();module.hit("auto_sign");}
        module.diagnosticChanged();if(active.isEmpty())Toast.makeText(context,"U 音签到\n"+DailySign.status().replace("；","\n"),Toast.LENGTH_LONG).show();
    }
}
