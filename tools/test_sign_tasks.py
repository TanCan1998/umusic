#!/usr/bin/env python3
"""Run production multi-sign scheduler with scripted transport, real JSON, and deterministic Android time."""
import pathlib,subprocess,tempfile
ROOT=pathlib.Path(__file__).resolve().parent.parent
scope={'__file__':str(ROOT/'tools/test_ui_daily.py')}
exec(compile((ROOT/'tools/test_ui_daily.py').read_text().split('with tempfile.TemporaryDirectory(',1)[0],scope['__file__'],'exec'),scope)
files=scope['files'];src=scope['src']
files.pop('org/json/JSONObject.java');files.pop('io/github/tancan1998/umusic/UiDailyTest.java')
src('io.github.tancan1998.umusic.DailySign','''package io.github.tancan1998.umusic;
import org.json.*;import java.util.*;final class DailySign{
static String account="a";static String currentAccount(){return account;}static String uin(){return "123";}static String status(){return "普通日签已确认";}
interface Reply{void result(int code,JSONObject data,String error);}static class Request{String op;JSONObject data;Reply reply;Request(String o,JSONObject d,Reply r){op=o;data=d;reply=r;}}
static List<Request> sent=new ArrayList<>();static void request(String s,String o,JSONObject p,Reply r){sent.add(new Request(o,p,r));}
}''')
src('io.github.tancan1998.umusic.SignTasksTest','''package io.github.tancan1998.umusic;
import org.json.*;import android.os.*;import android.content.*;import java.util.*;
public class SignTasksTest{
static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
static JSONObject j(String s)throws Exception{return new JSONObject(s);}
static DailySign.Request request(String op,String act){for(int i=DailySign.sent.size()-1;i>=0;i--){DailySign.Request r=DailySign.sent.get(i);if(r.op.equals(op)&&r.data.optString("ActID","").equals(act))return r;}throw new AssertionError("missing "+op+act);}
static void reply(String op,String act,String data)throws Exception{request(op,act).reply.result(0,j(data),null);}
static String state(boolean signed,int reward){return "{\\"retCode\\":0,\\"Info\\":{\\"IsSignIn\\":"+signed+"},\\"TaskListInfo\\":{\\"TaskList\\":{\\"ContinueTaskList\\":{\\"1\\":{\\"ID\\":5,\\"State\\":"+reward+"}}}}}";}
public static void main(String[] args)throws Exception{
Entry m=new Entry();Context ctx=new Context();SignTasks.install(m,SignTasksTest.class.getClassLoader());m.toggles.put("auto_sign",false);SignTasks.maybe(ctx);Handler.tick();check(DailySign.sent.isEmpty(),"disabled no network");m.toggles.put("auto_sign",true);SignTasks.maybe(ctx);Handler.tick();check(DailySign.sent.size()==3,"three independent initial queries");
DailySign.Request oldVip=request("EveryDaySignLvzScore","");oldVip.reply.result(0,j("{\\"Ret\\":20028}"),null);check(request("EveryDaySignLvzScore","").data.optString("Cmd").equals("get"),"qry before get");reply("EveryDaySignLvzScore","","{\\"Ret\\":0}");check(request("EveryDaySignLvzScore","").data.optString("Cmd").equals("qry"),"get success requires qry readback");reply("EveryDaySignLvzScore","","{\\"Ret\\":20019}");check(m.prefs.d.size()==1,"VIP confirmed only after server readback");
reply("GetSignInSummary","Z25hHGi",state(false,1));reply("GetSignInTaskList","Z25hHGi",state(false,1));check(request("SignIn","Z25hHGi").data.optString("ScenesID").equals("2"),"coin scene preserved");reply("SignIn","Z25hHGi","{\\"retCode\\":0}");check(m.prefs.d.size()==1,"sign ack alone not persisted");reply("GetSignInSummary","Z25hHGi",state(true,2));reply("GetSignInTaskList","Z25hHGi",state(true,2));check(request("AwardPrize","Z25hHGi").data.optString("TaskID").equals("5"),"server-issued pending reward only");reply("AwardPrize","Z25hHGi","{\\"retCode\\":0}");check(m.prefs.d.size()==1,"award ack alone not persisted");DailySign.Request verify=request("GetSignInTaskList","Z25hHGi");verify.reply.result(0,j(state(true,3).replace("true","1")),null);check(m.prefs.d.size()==2,"coin persisted after sign and reward readback");verify.reply.result(0,j(state(false,2)),null);check(m.prefs.d.size()==2,"duplicate callback ignored");
reply("GetSignInSummary","Z156KEu",state(true,2));reply("GetSignInTaskList","Z156KEu",state(true,2));reply("AwardPrize","Z156KEu","{\\"retCode\\":17}");check(m.prefs.d.size()==2&&SignTasks.status("").contains("领奖失败 17"),"reward failure is not sign success");
SystemClock.now+=61000;SignTasks.maybe(ctx);Handler.tick();DailySign.Request late=request("GetSignInSummary","Z156KEu");DailySign.account="b";SignTasks.maybe(ctx);Handler.tick();late.reply.result(0,j(state(true,3)),null);check(m.prefs.d.size()==2,"old account late response ignored");reply("EveryDaySignLvzScore","","{\\"Ret\\":99}");check(m.prefs.d.size()==2,"unknown business code is failure");
reply("GetSignInSummary","Z25hHGi",state(true,3));reply("GetSignInTaskList","Z25hHGi","{\\"retCode\\":0,\\"Info\\":{\\"IsSignIn\\":true}}");check(m.prefs.d.size()==2,"missing rewards is not confirmed");Handler.tick();check(SignTasks.status("").contains("超时"),"timeout remains failure");check(m.prefs.d.keySet().stream().allMatch(k->k.contains("-a-")),"no account b success leaked");System.out.println("PASS: per-account tasks, disabled no requests, VIP readback, coin sign+award readback, unknown/missing/reward failure, duplicates, old-account late reply, timeout");
}}
''')
with tempfile.TemporaryDirectory(prefix='umusic-multisign-') as d:
 work=pathlib.Path(d)
 for n,t in files.items():
  p=work/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(t)
 for n in ['SignTasks','UiFeatures']:(work/f'io/github/tancan1998/umusic/{n}.java').write_text((ROOT/f'app/src/main/java/io/github/tancan1998/umusic/{n}.java').read_text())
 out=work/'classes';out.mkdir()
 import os
 jar=str(pathlib.Path(os.environ.get('DL',ROOT/'.tools'))/'android-all.jar')
 p=subprocess.run(['javac','-encoding','UTF-8','-cp',jar,'-d',str(out)]+[str(f) for f in work.rglob('*.java')],capture_output=True,text=True)
 if p.returncode:print(p.stderr[-3500:]);raise SystemExit(p.returncode)
 subprocess.run(['java','-cp',str(out)+':'+jar,'io.github.tancan1998.umusic.SignTasksTest'],check=True)
