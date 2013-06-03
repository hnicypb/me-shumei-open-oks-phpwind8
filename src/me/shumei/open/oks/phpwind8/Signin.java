package me.shumei.open.oks.phpwind8;

import java.io.IOException;
import java.util.HashMap;


import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.content.Context;

/**
 * 使签到类继承CommonData，以方便使用一些公共配置信息
 * @author wolforce
 *
 */
public class Signin extends CommonData {
	String resultFlag = "false";
	String resultStr = "未知错误！";
	
	/**
	 * <p><b>程序的签到入口</b></p>
	 * <p>在签到时，此函数会被《一键签到》调用，调用结束后本函数须返回长度为2的一维String数组。程序根据此数组来判断签到是否成功</p>
	 * @param ctx 主程序执行签到的Service的Context，可以用此Context来发送广播
	 * @param isAutoSign 当前程序是否处于定时自动签到状态<br />true代表处于定时自动签到，false代表手动打开软件签到<br />一般在定时自动签到状态时，遇到验证码需要自动跳过
	 * @param cfg “配置”栏内输入的数据
	 * @param user 用户名
	 * @param pwd 解密后的明文密码
	 * @return 长度为2的一维String数组<br />String[0]的取值范围限定为两个："true"和"false"，前者表示签到成功，后者表示签到失败<br />String[1]表示返回的成功或出错信息
	 */
	public String[] start(Context ctx, boolean isAutoSign, String cfg, String user, String pwd) {
		//把主程序的Context传送给验证码操作类，此语句在显示验证码前必须至少调用一次
		CaptchaUtil.context = ctx;
		//标识当前的程序是否处于自动签到状态，只有执行此操作才能在定时自动签到时跳过验证码
		CaptchaUtil.isAutoSign = isAutoSign;
		
		try{
			//存放Cookies的HashMap
			HashMap<String, String> cookies = new HashMap<String, String>();
			//Jsoup的Response
			Response res;
			
			//检查用户配置的URL，如果不以“http://”开头则帮用户补上
			//但在说明信息里面，还是需要跟用户明说要加“http://”，以免用户都不知道加还是不加的好
			String url = cfg.trim();
			if(url.startsWith("http://") == false && url.startsWith("https://") == false) {
				url = "http://" + url;
			}
			//登录URL
			String loginUrl = url + "/login.php?";
			//用户中心URL 
			String userCenterUrl = url +"/u.php";
			//签到URL
			String signUrl;
			
			//构造连接信息
			res = Jsoup.connect(loginUrl)
					.data("lgt", "0")//0->用户名登录，1->邮箱登录
					.data("pwuser", user)
					.data("pwpwd", pwd)
					.data("forward","")
					.data("jumpurl", url + "/u.php")
					.data("step", "2")
					.data("hideid", "0")
					.data("submit", "")
					.userAgent(UA_CHROME)
					.timeout(TIME_OUT)
					.ignoreContentType(true)
					.method(Method.POST)
					.execute();
			//保存Cookies
			cookies.putAll(res.cookies());
			
			//访问用户中心
			res = Jsoup.connect(userCenterUrl).cookies(cookies).userAgent(UA_CHROME).timeout(TIME_OUT).method(Method.GET).referrer(loginUrl).ignoreContentType(true).execute();
			cookies.putAll(res.cookies());
			
			//提取提交签到需要用的信息
			String verifyHash = res.parse().select("input[name=verify]").first().val();//获取Verify的值
			signUrl = url + "/jobcenter.php?action=punch&step=2&verify=" + verifyHash;//提交签到信息的URL
			
			//提交签到请求
			res = Jsoup.connect(signUrl).cookies(cookies).userAgent(UA_CHROME).timeout(TIME_OUT).method(Method.GET).referrer(loginUrl).ignoreContentType(true).execute();
			String signinReturnStr = res.parse().getElementsByTag("ajax").first().text().replace("[", "").replace("]", "");//获取返回的Json值：[{"message":'铜币+6 1',"flag":'1',"html":''}]
			
			//分析返回的数据
			try {
				JSONObject jsonObj = new JSONObject(signinReturnStr);
				String message = jsonObj.getString("message");
				String flag = jsonObj.getString("flag");
				if(flag.equals("1"))
				{
					resultFlag = "true";
					String[] msgArr = message.split(" ");
					message = msgArr[0] + "，连续" + msgArr[1] + "天打卡";
				}
				else
				{
					if(message.contains("已经打卡"))
						resultFlag = "true";
					else
						resultFlag = "false";
				}
				
				resultStr = message;
			} catch (JSONException e) {
				resultFlag = "false";
				resultStr = "提交签到信息出错";
				e.printStackTrace();
			}
			
		} catch (IOException e) {
			this.resultFlag = "false";
			this.resultStr = "连接超时";
			e.printStackTrace();
		} catch (Exception e) {
			this.resultFlag = "false";
			this.resultStr = "未知错误！";
			e.printStackTrace();
		}
		
		return new String[]{resultFlag, resultStr};
	}
	
	
}
