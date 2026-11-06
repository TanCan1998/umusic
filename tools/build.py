#!/usr/bin/env python3
"""Build using the Java/D8/signing toolchain already used by U闲."""
import os,pathlib,subprocess,shutil,sys
ROOT=pathlib.Path(__file__).resolve().parent.parent
os.chdir(ROOT)
out=ROOT/'build';out.mkdir(exist_ok=True)
dl=pathlib.Path(os.environ.get('DL',ROOT/'.tools'))
def run(args,log):
 with (out/log).open('w') as f:p=subprocess.run([str(a) for a in args],stdout=f,stderr=subprocess.STDOUT)
 if p.returncode:print((out/log).read_text()[-6000:]);raise SystemExit(p.returncode)
 print(log,'OK',flush=True)
for dep in ['android-all.jar','lxaar/classes.jar','r8.jar','signer.jar']:
 if not (dl/dep).is_file():raise SystemExit('Missing '+str(dl/dep))
classes=out/'classes';shutil.rmtree(classes,ignore_errors=True);classes.mkdir()
cp=os.pathsep.join(str(dl/x) for x in ['android-all.jar','lxaar/classes.jar'])
sources=list((ROOT/'app/src/main/java').rglob('*.java'))
run(['javac','-source','17','-target','17','-encoding','UTF-8','-classpath',cp,'-d',classes]+sources,'javac.log')
run(['jar','cf',out/'code.jar','-C',classes,'.'],'jar.log')
dex=out/'dex';shutil.rmtree(dex,ignore_errors=True);dex.mkdir()
run(['java','-Xmx768m','-cp',dl/'r8.jar','com.android.tools.r8.D8','--min-api','28','--lib',dl/'android-all.jar','--lib',pathlib.Path(shutil.which('javac')).resolve().parent.parent,'--classpath',dl/'lxaar/classes.jar','--output',dex,out/'code.jar'],'d8.log')
subprocess.run(['python3','tools/manifest.py',out/'AndroidManifest.xml'],check=True)
sys.path.insert(0,str(ROOT/'tools'))
import arsc,pack
(out/'resources.arsc').write_bytes(arsc.build('com.xiandehuang.umusic',values=('com.tencent.qqmusic',)))
meta=out/'META-INF/xposed';meta.mkdir(parents=True,exist_ok=True)
(meta/'java_init.list').write_text('com.xiandehuang.umusic.Entry\n')
(meta/'scope.list').write_text('com.tencent.qqmusic\n')
(meta/'module.prop').write_text('minApiVersion=102\ntargetApiVersion=102\nstaticScope=true\n')
from PIL import Image,ImageDraw
icon=ROOT/'app/src/main/res/drawable-nodpi/ic_launcher.png'
im=Image.new('RGB',(192,192),'#0c473a');d=ImageDraw.Draw(im);d.rounded_rectangle((34,32,158,160),radius=28,fill='#25d4a1');d.ellipse((67,115,94,142),fill='#ffffff');d.rectangle((87,59,97,128),fill='#ffffff');d.polygon([(88,58),(131,48),(131,66),(88,77)],fill='#ffffff');im.save(icon)
files=['AndroidManifest.xml='+str(out/'AndroidManifest.xml'),'classes.dex='+str(dex/'classes.dex'),'resources.arsc='+str(out/'resources.arsc'),'res/drawable-nodpi/ic_launcher.png='+str(icon)]
files += ['META-INF/xposed/'+p.name+'='+str(p) for p in meta.iterdir()]
pack.main(str(out/'unsigned.apk'),[(item.split('=',1)[1],item.split('=',1)[0]) for item in files])
signed=out/'signed';shutil.rmtree(signed,ignore_errors=True)
os.environ['DL']=str(dl)
run(['python3','tools/sign.py',out/'unsigned.apk',signed],'sign.log')
apks=list(signed.glob('*.apk'))
if len(apks)!=1:raise SystemExit('Expected one signed APK')
shutil.copy2(apks[0],out/'umusic.apk')
run(['java','-jar',dl/'signer.jar','-a',out/'umusic.apk','-y','--skipZipAlign'],'verify.log')
print('BUILD SUCCESSFUL',out/'umusic.apk',flush=True)
