from axml_base import Builder,TYPE_STRING
from pathlib import Path
import sys
PACKAGE='com.xiandehuang.umusic'
b=Builder()
b.open('manifest',[(0xffffffff,b.pool.get('package'),b.pool.get(PACKAGE),TYPE_STRING,b.pool.get(PACKAGE)),b._attr('versionCode',8,is_string=False),b._attr('versionName','0.8')])
b.simple('uses-sdk',[b._attr('minSdkVersion',28,is_string=False),b._attr('targetSdkVersion',34,is_string=False)])
b.open('queries')
b.simple('package',[b._attr('name','com.tencent.qqmusic')]);b.close()
b.open('application',[b._attr('label','U音'),b.ref('icon',0x7f030000),b._attr('allowBackup',False,is_string=False),b._attr('forceQueryable',True,is_string=False)])
for name,value in [('xposedmodule','true'),('xposeddescription','QQ音乐净化：广告与会员弹窗、本地会员和音效'),('xposedminversion','102')]:
 b.simple('meta-data',[b._attr('name',name),b._attr('value',value)])
b.simple('meta-data',[b._attr('name','xposedscope'),b.ref('resource',0x7f010000)])
# Pure injection module: no Activity, Provider, Service, Receiver or launcher.
b.close();b.close()
Path(sys.argv[1]).write_bytes(b.build(PACKAGE))
