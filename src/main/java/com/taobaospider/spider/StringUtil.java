package com.taobaospider.spider;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * <p>Title: StringUtil </p>
 * <p>Description  </p>
 * <p>Company: http://www.hnxianyi.com </p>
 *
 * @author Wjj
 * @CreateDate 2018/4/28 10:48
 */
public class StringUtil {

	/**
	 * 判断字符串是否为数字，是返回true
	 * @param str
	 * @return
	 */
	public static boolean isNubmer(String str) {
		Pattern pattern = compile("^[-\\+]?[\\d]*$");
		return pattern.matcher(str).matches();
	}

	/**
	 * 判断是否是英文，是返回true
	 * @param str
	 * @return
	 */
	public static boolean isABC(String str) {
		Pattern pattern = compile("[(A-Za-z)]");
		return pattern.matcher(str).find();
	}

	/**
	 * 特殊字符过滤
	 * @param str
	 * @return
	 */
	public static boolean isSpecialChar(String str) {
		String regEx="^[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]*$";
		Pattern pattern = compile(regEx);
		return pattern.matcher(str).matches();
	}
}
