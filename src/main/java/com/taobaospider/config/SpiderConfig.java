package com.taobaospider.config;

/**
 * <p>Title: SpiderConfig </p>
 * <p>Description  </p>
 * <p>Company: http://www.hnxianyi.com </p>
 *
 * @author Wjj
 * @CreateDate 2018/4/26 16:30
 */
public class SpiderConfig {
	public final static String[] CATEGORY = {"母婴玩具", "服饰箱包", "家纺家居", "家居生活", "数码电器"};

	public final static String LOGINPAGEURL = "https://login.taobao.com/member/login.jhtml";

	public final static String LOGINURL = "https://login.taobao.com/member/login.jhtml";
	/**
	 * 关联修饰词URL
	 */
	public final static String ASSOCIATEDMODIFIERSURL = "https://sycm.taobao.com/mq/searchword/relatedProperty.json";
	/**
	 * 关联热词URL
	 */
	public final static String RELATEDHOTWORDSURL = "https://sycm.taobao.com/mq/searchword/relatedHotWord.json";
	/**
	 * 品牌词
	 */
	public final static String BRANDSWORD = "特步耐克阿迪达斯安踏李宁三叶草瞄准镜瞄准器貂皮裘皮辣椒水防狼喷雾旗舰店官方";

}
