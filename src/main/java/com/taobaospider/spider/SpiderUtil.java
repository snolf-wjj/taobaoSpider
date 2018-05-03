package com.taobaospider.spider;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobaospider.config.SpiderConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: SpiderUtil </p>
 * <p>Description  </p>
 * <p>Company: http://www.hnxianyi.com </p>
 *
 * @author Wjj
 * @CreateDate 2018/4/26 17:38
 */
public class SpiderUtil {
	private Logger logger = LoggerFactory.getLogger(SpiderUtil.class);

	public static void login() {
		try {
			String loginHtml = HttpClientUtil.getHTML(SpiderConfig.LOGINPAGEURL);
			Document doc = Jsoup.parse(loginHtml);
			Elements ele = doc.getElementsByClass("submit");
			Elements inputs = null;
			for (Element element : ele) {
				inputs = element.getElementsByAttributeValue("type", "hidden");
			}
			for (int i = 0; i < inputs.size(); i++) {
				String name = inputs.get(i).attr("name");
				if ("loginType".equals(name)) {
					String s = name;
				}
				String val = inputs.get(i).attr("value");
				System.out.println(name + "=" + val);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public String getData(String keyword, String cookie) {
		Map<String, String> headers = new HashMap<>();
		System.out.println(cookie);
		headers.put("Cookie", cookie);
		headers.put("Connection", "keep-alive");
		headers.put("Host", "sycm.taobao.com");
		headers.put("Referer", "https://sycm.taobao.com/mq/words/search_words.htm?spm=a21ag.7749213.LeftMenu.d1078.49b415168eF6W3");
		headers.put("origin", "https://sycm.taobao.com");
		headers.put("Upgrade-Insecure-Requests", "1");
		// 获取数据参数
		String dateRange = DateUtil.getDateFormat(DateUtil.getDateByInt(-7)) + "%7C" + DateUtil.getDateFormat(DateUtil.getDateByInt(-1));
		String dateType = "recent7";
		String device = "0";
		String token = "6863e723e";
		long time = System.currentTimeMillis();
		Map<String, String> params = new HashMap<>();
		params.put("dateRange", dateRange);
		params.put("dateType", dateType);
		params.put("device", device);
		params.put("keyword", keyword);
		params.put("token", token);
		params.put("_", time+"");

		String resultData = "";
		try {
			// 关联词获取
			String url1= SpiderConfig.ASSOCIATEDMODIFIERSURL;
			Result result1 = HttpClientUtil.get(url1, headers, params);
			HttpEntity httpEntity1 = result1.getHttpEntity();
			String str1 = EntityUtils.toString(httpEntity1);
			logger.info("关键词：" + str1);
			String str1Format = formatData(str1);
			if (StringUtils.isBlank(str1Format)) {
				System.out.println(str1);
				System.out.println("修饰词未获取到");
			} else {
				resultData +=str1Format;
			}
			Thread.sleep(2000);
			// 热词获取
			String url2= SpiderConfig.RELATEDHOTWORDSURL;
			Result result2 = HttpClientUtil.get(url2, headers, params);
			HttpEntity httpEntity2 = result2.getHttpEntity();
			String str2 = EntityUtils.toString(httpEntity2);
			logger.info("热词：" + str2);
			String str2Format = formatData(str2);
			if (StringUtils.isBlank(str2Format)) {
				System.out.println(str2);
				System.out.println("热词未获取到");
			} else {
				resultData +=str2Format;
			}
			return resultData;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultData;
	}

	/**
	 * 处理返回的数据，生成文件用
	 * @return
	 */
	private String formatData(String data) {
		JSONObject resultJson = JSON.parseObject(data);
		if (null == resultJson.getJSONObject("content") || !"0".equals(resultJson.getJSONObject("content").getString("code"))) {
			return null;
		}
		JSONArray dataJsonArray = resultJson.getJSONObject("content").getJSONArray("data");
		String str = "";
		int num = 0;
		for (int i = 0; i < dataJsonArray.size(); i++) {
			if (num >100) {
				break;
			}
			JSONObject jo = dataJsonArray.getJSONObject(i);
			String keyword = jo.getString("keyword");
			if (checkStr(keyword)) {
				str+=jo.getString("keyword");
				str+="\r\n";
				num++;
			}
		}
		return str;
	}

	/**
	 * 校验字符串是否可取,可取返回TRUE
	 * @param str
	 * @return
	 */
	private boolean checkStr(String str) {
		boolean b = true;
		if (StringUtil.isABC(str)) {
			b = false;
		} else if (StringUtil.isNubmer(str)) {
			b = false;
		} else if (StringUtil.isSpecialChar(str)) {
			b = false;
		}
		return b;
	}

}
