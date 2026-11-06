package io.github.tancan1998.umusic;

/** Compact UI status; detailed registration/call counters remain in diagnostics. */
final class StatusText {
    static String format(String feature,int installed,int expected,boolean enabled,long hits,String sign,int days){
        if(!enabled)return "未启用";
        if(expected<=0||installed!=expected)return "不可用";
        if("auto_sign".equals(feature)){
            String suffix="";
            if(sign.contains("失败"))suffix="签到失败";
            else if(sign.contains("等待登录"))return "等待登录";
            else if(sign.contains("请求中"))suffix="签到中";
            if(days>0)return "连续签到 "+days+" 天"+(suffix.isEmpty()?"":" · "+suffix);
            if(!suffix.isEmpty())return suffix;
            return sign.contains("已签到")||sign.contains("成功")?"今日已签到":"可用";
        }
        boolean intercept=!"local_vip".equals(feature)&&!"sound_effects".equals(feature);
        return intercept&&hits>0?"已拦截":"可用";
    }
}
