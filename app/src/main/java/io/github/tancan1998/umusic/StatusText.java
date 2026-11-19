package io.github.tancan1998.umusic;

/** Compact UI status; detailed registration/call counters remain in diagnostics. */
final class StatusText {
    static String startupNotice(boolean showStartupToast, boolean hasBusinessHooks) {
        if (!hasBusinessHooks) return "U 音加载失败\n所有功能均不可用";
        return showStartupToast ? "U 音已加载\n入口：我的 → 右上角更多 → U 音设置" : null;
    }

    static String format(String feature,int installed,int expected,boolean enabled,long hits,String sign,int days){
        if(!enabled)return "未启用";
        if(installed<=0)return "不可用";
        boolean partial=installed<expected;
        if("auto_sign".equals(feature)){
            if(sign.startsWith("普通日签：")){
                if(sign.contains("等待登录"))return "等待登录";
                if(sign.contains("查询中")||sign.contains("请求中"))return "签到中";
                if(sign.contains("失败")||sign.contains("未知")||sign.contains("未确认")||sign.contains("缺失")||sign.contains("待领奖"))return "部分未完成 · 查看详情";
                return sign.contains("今日已确认")||sign.contains("奖励已确认")?"今日签到已确认":"等待签到";
            }
            String suffix="";
            if(sign.contains("失败"))suffix="签到失败";
            else if(sign.contains("等待登录"))return "等待登录";
            else if(sign.contains("请求中"))suffix="签到中";
            if(days>0)return "连续签到 "+days+" 天"+(suffix.isEmpty()?"":" · "+suffix);
            if(!suffix.isEmpty())return suffix;
            if(sign.contains("已签到")||sign.contains("成功"))return "今日已签到";
            return partial?"部分可用":"可用";
        }
        boolean intercept=!"local_vip".equals(feature)&&!"sound_effects".equals(feature);
        if(partial){
            if(hits<=0)return "部分可用";
            return intercept?"已拦截（部分兼容）":"已生效（部分兼容）";
        }
        return intercept&&hits>0?"已拦截":"可用";
    }
}
