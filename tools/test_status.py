#!/usr/bin/env python3
"""Status truth table and calendar-day streak from the actual production classes."""
import pathlib,tempfile,subprocess
ROOT=pathlib.Path(__file__).resolve().parent.parent
scope={'__file__':str(ROOT/'tools/test_ui_daily.py')}
exec(compile((ROOT/'tools/test_ui_daily.py').read_text().split('with tempfile.TemporaryDirectory(',1)[0],scope['__file__'],'exec'),scope)
src=scope['src'];files=scope['files']
src('com.xiandehuang.umusic.StatusTest','''package com.xiandehuang.umusic;public class StatusTest{
static void check(boolean b,String s){if(!b)throw new AssertionError(s);}static String text(String f,int i,int e,boolean on,long h){return StatusText.format(f,i,e,on,h,"今日已签到",1);}
public static void main(String[] args){
check(text("search_ads",7,7,true,0).equals("可用"),"registered but no interception");check(text("search_ads",7,7,true,1).equals("已拦截"),"actual intercept");check(text("search_ads",7,7,false,9).equals("未启用"),"disabled despite old hits");check(text("search_ads",6,7,true,9).equals("不可用"),"partial cannot claim available");check(text("local_vip",18,18,true,100).equals("可用"),"member status not interception");check(text("sound_effects",11,11,true,100).equals("可用"),"sound status not interception");check(text("auto_sign",1,1,true,1).equals("连续签到 1 天"),"compact sign days");check(StatusText.format("auto_sign",1,1,true,0,"签到失败：超时",2).contains("签到失败"),"failure retained");check(StatusText.format("auto_sign",1,1,true,0,"等待登录",0).equals("等待登录"),"logged out truthful");
Entry.Prefs p=new Entry.Prefs();String prefix="daily-sign-hash-";check(DailySign.confirmedStreak(p,prefix+"2026-11-06")==0,"no record no invented day");p.d.put(prefix+"2026-11-06",true);check(DailySign.confirmedStreak(p,prefix+"2026-11-06")==1,"legacy today imported");p.d.put(prefix+"2026-11-05",true);p.d.put(prefix+"2026-11-04",true);check(DailySign.confirmedStreak(p,prefix+"2026-11-06")==3,"consecutive calendar days");check(DailySign.confirmedStreak(p,prefix+"2026-11-07")==3,"yesterday streak before today sign");check(DailySign.confirmedStreak(p,prefix+"2026-11-08")==0,"missed day breaks streak");p.d.put(prefix+"2026-11-08",true);check(DailySign.confirmedStreak(p,prefix+"2026-11-08")==1,"new streak resets");check(DailySign.confirmedStreak(p,"daily-sign-other-2026-11-06")==0,"accounts isolated");Entry.Prefs leap=new Entry.Prefs();leap.d.put(prefix+"2024-02-29",true);leap.d.put(prefix+"2024-03-01",true);check(DailySign.confirmedStreak(leap,prefix+"2024-03-01")==2,"leap date boundary");System.out.println("PASS: compact status truth table, no false intercept, real daily records, legacy import, missed-day reset, account isolation, leap calendar boundary");}}
''')
with tempfile.TemporaryDirectory(prefix='umusic-status-') as d:
 work=pathlib.Path(d)
 for n,t in files.items():
  p=work/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(t)
 for n in ['DailySign','UiFeatures','StatusText']:(work/f'com/xiandehuang/umusic/{n}.java').write_text((ROOT/f'app/src/main/java/com/xiandehuang/umusic/{n}.java').read_text())
 out=work/'classes';out.mkdir()
 p=subprocess.run(['javac','-encoding','UTF-8','-d',str(out)]+[str(f) for f in work.rglob('*.java')],capture_output=True,text=True)
 if p.returncode:print(p.stderr[-2500:]);raise SystemExit(p.returncode)
 subprocess.run(['java','-cp',str(out),'com.xiandehuang.umusic.StatusTest'],check=True)
