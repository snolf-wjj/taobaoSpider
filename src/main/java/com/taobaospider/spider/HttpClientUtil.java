package com.taobaospider.spider;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * HttpClient4工具类
 * @author Lanxiaowei
 * @create 2013-01-25 15:42
 */
@SuppressWarnings({"deprecation","unused"})
public class HttpClientUtil {
	/**连接池最大链接数*/
	private static final int MAX_TOTAL_CONNECTION = 1000;
	/**连接池每个路由最大链接并发数*/
	private static final int MAX_PRE_ROUTE = 5000;
	/**连接池HTTP请求80端口最大链接并发数*/
	private static final int MAX_HTTP_ROUTE = 500;
	/** 用户浏览器代理 */
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36";
	/**默认请求编码UTF-8*/
	private static final String HTTP_CONTENT_CHARSET = "UTF-8";
	/**Socket读取返回超时时间*/
	private static final int SO_TIMEOUT = 20000;
	/**链接超时时间*/
	public static final int CONNECT_TIMEOUT = 30000;
	/**获取连接的最大等待时间 */
	public static final int WAITING_TIMEOUT = 5000;
	/**Socket工厂注册器*/
	private static SchemeRegistry schemeRegistry;
	/**连接池管理器*/
	private static ThreadSafeClientConnManager connectionManager;
	/**HTTP请求参数(单例)*/
	private static HttpParams httpParams;
	/**HttpClient实例对象(单例)*/
	private static DefaultHttpClient httpClient;

	static {
		createConnectionManager();
	}

	/**
	 * 异常自动恢复处理, 使用HttpRequestRetryHandler接口实现请求的异常恢复
	 */
	private static HttpRequestRetryHandler requestRetryHandler = new HttpRequestRetryHandler() {
		// 自定义的恢复策略
		@Override
		public boolean retryRequest(IOException exception, int executionCount,
									HttpContext context) {
			// 设置恢复策略，在发生异常时候将自动重试5次
			if (executionCount >= 5) {
				// 如果连接次数超过了最大值则停止重试
				return false;
			}
			if (exception instanceof NoHttpResponseException) {
				// 如果服务器连接失败重试
				return true;
			}
			if (exception instanceof SSLHandshakeException) {
				//SSL连接异常 时不重试
				return false;
			}
			HttpRequest request = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
			boolean idempotent = (request instanceof HttpEntityEnclosingRequest);
			if (!idempotent) {
				// 请求内容相同则重试
				return true;
			}
			return false;
		}
	};

	/**
	 * 重写验证方法，取消检测ssl
	 */
	private static TrustManager truseAllManager = new X509TrustManager(){
		@Override
		public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws CertificateException {}
		@Override
		public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws CertificateException {}
		@Override
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	} ;

	/**
	 * 支持访问https的网站
	 * @param httpClient
	 */
	private static void enableSSL(DefaultHttpClient httpClient){
		//调用SSL
		try {
			SSLContext sslcontext = SSLContext.getInstance("TLS");
			sslcontext.init(null, new TrustManager[] { truseAllManager }, null);
			SSLSocketFactory sf = new SSLSocketFactory(sslcontext);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			Scheme https = new Scheme("https", sf, 443);
			httpClient.getConnectionManager().getSchemeRegistry().register(https);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 设置HTTP请求链接参数
	 */
	private static void setHttpParams(HttpParams httpParams,String charset) {
		if(null == httpParams) {
			return;
		}

		//设置支持的HTTP协议版本
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
		//HTTP请求内容字符编码
		HttpProtocolParams.setContentCharset(httpParams, HTTP_CONTENT_CHARSET);
		// 设置连接超时时间
		HttpConnectionParams.setConnectionTimeout(httpParams, CONNECT_TIMEOUT);
		// 设置读取超时时间
		HttpConnectionParams.setSoTimeout(httpParams, SO_TIMEOUT);
		// 模拟浏览器，解决一些服务器程序只允许浏览器访问的问题
		httpParams.setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
		httpParams.setParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
		httpParams.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET,charset == null ? HTTP_CONTENT_CHARSET : charset);
		//Cookie自动设置策略(BROWSER_COMPATIBILITY:最大程度的兼容各种浏览器供应商的Cookie规范)
		httpParams.setParameter(ClientPNames.COOKIE_POLICY,CookiePolicy.BROWSER_COMPATIBILITY);
		httpParams.setParameter("http.protocol.single-cookie-header", true);
		// 设置最大连接数
		ConnManagerParams.setMaxTotalConnections(httpParams, MAX_TOTAL_CONNECTION);
		/**从连接池中取连接的超时时间 */
		ConnManagerParams.setTimeout(httpParams, 5000);
		// 设置获取连接的最大等待时间
		ConnManagerParams.setTimeout(httpParams, WAITING_TIMEOUT);
		// 设置每个路由最大连接数
		ConnPerRouteBean connPerRoute = new ConnPerRouteBean(MAX_PRE_ROUTE);
		ConnManagerParams.setMaxConnectionsPerRoute(httpParams,connPerRoute);
	}

	/**
	 * 设置HTTP请求链接参数
	 */
	private static void setHttpParams(HttpParams httpParams) {
		setHttpParams(httpParams,HTTP_CONTENT_CHARSET);
	}

	/**
	 * Socket工厂注册
	 * @return
	 */
	private static SchemeRegistry getSchemeRegistry(){
		if(null == schemeRegistry) {
			schemeRegistry = new SchemeRegistry();
		}
		schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
		schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));
		return schemeRegistry;
	}

	/**
	 * 创建连接池
	 * @return
	 */
	private static ThreadSafeClientConnManager createConnectionManager() {
		if(null == connectionManager) {
			connectionManager = new ThreadSafeClientConnManager(getSchemeRegistry());
		}

		connectionManager.setDefaultMaxPerRoute(MAX_PRE_ROUTE);
		HttpHost localhost = new HttpHost("locahost", 80);
		connectionManager.setMaxForRoute(new HttpRoute(localhost), MAX_HTTP_ROUTE);
		connectionManager.setMaxTotal(MAX_TOTAL_CONNECTION);
		return connectionManager;
	}

	/**
	 * @Title: getHttpClientInstance
	 * @Description: 单实例模式创建DefaultHttpClicent对象
	 * @param charset  请求编码
	 * @return DefaultHttpClient
	 * @throws
	 */
	public static DefaultHttpClient getHttpClientInstance(String charset){
		if(null == httpClient) {
			synchronized(HttpClient.class){
				if(null == httpClient) {
					httpClient = new DefaultHttpClient(connectionManager);
				}
			}
		}
		httpParams = httpClient.getParams();
		setHttpParams(httpParams, charset);
		//定义重试策略
		//httpClient.setHttpRequestRetryHandler(requestRetryHandler);
		return httpClient;
	}

	/**
	 * @Title: getHttpClientInstance
	 * @Description: 单实例模式创建DefaultHttpClicent对象(重载)
	 *               默认请求编码为UTF-8
	 * @return DefaultHttpClient
	 * @throws
	 */
	public static DefaultHttpClient getHttpClientInstance() {
		return getHttpClientInstance(HTTP_CONTENT_CHARSET);
	}

	/**
	 * 创建HttpClient实例
	 * @param charset
	 * @return
	 */
	private static DefaultHttpClient getDefaultHttpClient(String charset) {
		return getHttpClientInstance(charset);
	}

	/**
	 * 创建HttpClient实例
	 * @return
	 */
	private static DefaultHttpClient getDefaultHttpClient(){
		return getDefaultHttpClient(HTTP_CONTENT_CHARSET);
	}

	/**
	 * 发送get请求
	 * @param url             请求URL
	 * @param headers         请求头信息
	 * @param params          请求参数
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static Result get(String url, Map<String,String> headers, Map<String,String> params) throws Exception {
		DefaultHttpClient httpClient = getDefaultHttpClient();
		url = url + (null == params ? "" : assemblyParameter(params));
		System.out.println(url);
		HttpGet httpGet = new HttpGet(url);
		//判断是否是https请求
		if(url.startsWith("https")){
			enableSSL(httpClient);
		}
		//设置请求头信息
		if(null != headers){
			httpGet.setHeaders(assemblyHeader(headers));
		}
		HttpResponse response = httpClient.execute(httpGet);
		Result result = produceResult(httpClient,response);
		//断开请求
		httpGet.abort();
		httpGet.releaseConnection();
		return result;
	}
	
//	public static void main(String[] args) {
//		String url = "https://sycm.taobao.com/custom/common/permission.json";
//		Map<String, String> headers = new HashMap<>();
//		headers.put("Cookie", "t=83c104ac3fbada7047236b773a050264; thw=cn; l=Al1db8Hf-fqf9ER2BHfQx8j2bTNW0pF/; isg=BHx8glA1CusXujw58hvr0nlnThnu3QDLyFl5Qlb8n2dDIR2rfISBL8z4BclZclj3; cna=pc1jEU8MZFcCAd6AQ0H7fdXu; um=C234BF9D3AFA6FE717103AE7C87A9C9D6F1FA8E312882A7F23072AF2539884C56784283A404FE79CCD43AD3E795C914C64824F43DC27ADAACFF6D68A1209D5AE; miid=1265412071578562620; _cc_=Vq8l%2BKCLiw%3D%3D; tg=0; x=e%3D1%26p%3D*%26s%3D0%26c%3D0%26f%3D0%26g%3D0%26t%3D0%26__ll%3D-1; hng=CN%7Czh-CN%7CCNY%7C156; UM_distinctid=15f8092ff771df-0216950da888a8-173b7740-100200-15f8092ff78268; enc=7Gml%2Bs70LiOAbs0v1Ye%2BlMkGtHI48GyL6SR0ft2rtNPlaELrkfBS1Qrl8pAS97gAe%2FzjFkK%2FgeryDnpa32mUZw%3D%3D; mt=np=&ci=9_1; uc3=nk2=3opRKu9jxkXP&id2=UonScg667Kj86w%3D%3D&vt3=F8dBz4FVO6htOn0zzBw%3D&lg2=V32FPkk%2Fw0dUvg%3D%3D; lgc=%5Cu6770%5Cu51FAs%5Cu751F%5Cu6D3B; tracknick=%5Cu6770%5Cu51FAs%5Cu751F%5Cu6D3B; tk_trace=oTRxOWSBNwn9dPy4KVJVbutfzK5InlkjwbWpxHegXyGxPdWTLVRjn23RuZzZtB1ZgD6Khe0jl%2BAoo68rryovRBE2Yp933GccTPwH%2FTbWVnqEfudSt0ozZPG%2BkA1iKeVv2L5C1tkul3c1pEAfoOzBoBsNsJyRfvSKelpwlhUE5pUFtoV5qDoMj%2BD9OUXji%2BJgNXgJK7R2SbQIgH5J%2FJxMcYsRMcY%2BtReOyjpl09UERtStsFbHhYud%2FH2x5Qf1dUfhfmFbzV4CSo9jXgo4SilGwrp5QNs0rWwhYRkrEIag7YlxUt9NZSc3xGa21YLX8yZv%2BZTbFd0HciRegqil7sazzA%3D%3D; v=0; cookie2=2a25a57fcde916691eaab9824ba36520; _tb_token_=eebe7e35933e8; JSESSIONID=70D5A5800C64CB5B23AB8ADD9F588EF6; uc1=cookie14=UoTeOoHAx7PGOA%3D%3D&lng=zh_CN&cookie16=UIHiLt3xCS3yM2h4eKHS9lpEOw%3D%3D&existShop=false&cookie21=WqG3DMC9EdFmJgke4t0pDw%3D%3D&tag=8&cookie15=W5iHLLyFOGW7aA%3D%3D&pas=0; existShop=MTUyNDkwODg3Mg%3D%3D; dnk=%5Cu6770%5Cu51FAs%5Cu751F%5Cu6D3B; sg=%E6%B4%BB41; csg=5a8a5ff9; cookie1=VT5dCdINfRE81rdgwhPsULHqJ29TXFHfeJmFFIbabcA%3D; unb=1898520184; skt=9b52fc428a496f06; _l_g_=Ug%3D%3D; _nk_=%5Cu6770%5Cu51FAs%5Cu751F%5Cu6D3B; cookie17=UonScg667Kj86w%3D%3D; _euacm_ac_l_uid_=1898520184; 1898520184_euacm_ac_c_uid_=1898520184; 1898520184_euacm_ac_rs_uid_=1898520184; _euacm_ac_rs_sid_=107639450; whl=-1%260%260%261524909489384; _portal_version_=new");
//		headers.put("Connection", "keep-alive");
//		headers.put("Host", "sycm.taobao.com");
//		headers.put("Referer", "https://sycm.taobao.com/mq/words/search_words.htm?spm=a21ag.7749213.LeftMenu.d1078.49b415168eF6W3");
//		headers.put("origin", "https://sycm.taobao.com");
//		headers.put("Upgrade-Insecure-Requests", "1");
//		// 获取数据参数
//		long time = System.currentTimeMillis();
//		Map<String, String> params = new HashMap<>();
//		params.put("modules", "sycm-bzb-new");
//		params.put("step", "prepare");
//		params.put("token", "41eb730cf");
//		params.put("_", time+"");
//
//		try {
//			Result result1 = HttpClientUtil.get(url, headers, params);
//			HttpEntity httpEntity1 = result1.getHttpEntity();
//			String str1 = null;
//			str1 = EntityUtils.toString(httpEntity1);
//			System.out.println(str1);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	/**
	 * 发送post请求
	 * @param url      请求URL
	 * @param headers  请求头信息
	 * @param params   请求参数
	 * @param host     代理主机IP
	 * @param port     代理端口号
	 * @param encoding 请求编码
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static Result post(String url,
							  Map<String,String> headers,Map<String,String> params,
							  String host,int port,String encoding) throws Exception {
		DefaultHttpClient httpClient = getDefaultHttpClient();
		HttpPost httpPost = new HttpPost(url);
		//判断是否是https请求
		if(url.startsWith("https")){
			enableSSL(httpClient);
		}
		//设置请求头信息
		if(null != headers){
			httpPost.setHeaders(assemblyHeader(headers));
		}
		//设置请求代理
		if(null != host && !"".equals(host) && port > 0){
			httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,new HttpHost(host,port));
		}
		//设置请求参数
		if(null == params || params.isEmpty()) {
			List<NameValuePair> list  = new ArrayList<NameValuePair>();
			for (String temp : params.keySet()) {
				list.add(new BasicNameValuePair(temp,params.get(temp)));
			}
			if(StringUtils.isEmpty(encoding)) {
				encoding = "UTF_8";
			}
			UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(list,encoding);
			urlEncodedFormEntity.setChunked(false);
			httpPost.setEntity(urlEncodedFormEntity);
		}
		HttpResponse response = httpClient.execute(httpPost);
		Result result = produceResult(httpClient,response);
		httpPost.abort();
		httpPost.releaseConnection();
		return result ;
	}

	/**
	 * 发送post请求(重载1)
	 * @param url      请求URL
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static Result post(String url) throws Exception {
		return post(url,null,null,null,0);
	}

	/**
	 * 发送post请求(重载2)
	 * @param url      请求URL
	 * @param params   请求参数
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static Result post(String url,Map<String,String> params) throws Exception {
		return post(url,null,params,null,0);
	}

	/**
	 * 发送post请求(重载3)
	 * @param url      请求URL
	 * @param headers  请求头信息
	 * @param params   请求参数
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static Result post(String url,Map<String,String> headers,Map<String,String> params) throws Exception {
		return post(url,headers,params,null,0);
	}

	/**
	 * 发送post请求(重载4)
	 * @param url      请求URL
	 * @param headers  请求头信息
	 * @param params   请求参数
	 * @param host     代理主机IP
	 * @param port     代理端口号
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static Result post(String url,Map<String,String> headers,Map<String,String> params,String host,int port) throws Exception {
		return post(url,headers,params,host,port,HTTP_CONTENT_CHARSET);
	}

	/**
	 * 发送post请求
	 * @param url      请求URL
	 * @param headers  请求头信息
	 * @param params   请求参数
	 * @param encoding 请求编码
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static Result post(String url,Map<String,String> headers,Map<String,String> params,String encoding) throws Exception {
		DefaultHttpClient httpClient = getDefaultHttpClient();
		HttpPost httpPost = new HttpPost(url);
		//判断是否是https请求
		if(url.startsWith("https")){
			enableSSL(httpClient);
		}
		//设置请求头信息
		if(null != headers){
			httpPost.setHeaders(assemblyHeader(headers));
		}
		//设置请求参数
		if(null == params || params.isEmpty()) {
			List<NameValuePair> list  = new ArrayList<NameValuePair>();
			for (String temp : params.keySet()) {
				list.add(new BasicNameValuePair(temp,params.get(temp)));
			}
			httpPost.setEntity(new UrlEncodedFormEntity(list,encoding));
		}
		HttpResponse response = httpClient.execute(httpPost);
		BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());

		Result result = new Result();
		//设置返回的cookie
		result.setCookie(assemblyCookie(httpClient.getCookieStore().getCookies()));
		//设置响应状态码
		result.setStatusCode(response.getStatusLine().getStatusCode());
		//设置响应头信息
		result.setHeaders(response.getAllHeaders());
		//设置响应体
		result.setHttpEntity(entity);
		httpPost.abort();
		httpPost.releaseConnection();
		return result ;
	}

	/**
	 * 发送post请求
	 * @param url      请求URL
	 * @param headers  请求头信息
	 * @param encoding 请求编码
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static Result post(String url,Map<String,String> headers,String payload,String contentType, String encoding) throws ClientProtocolException, IOException {
		if(contentType == null || "".equals(contentType) ||
				(!contentType.equalsIgnoreCase("application/json") &&
						!contentType.equalsIgnoreCase("application/xml"))) {
			throw new IllegalArgumentException("content-type must be application/json or application/xml");
		}
		DefaultHttpClient httpClient = getDefaultHttpClient();
		HttpPost httpPost = new HttpPost(url);
		//判断是否是https请求
		if(url.startsWith("https")){
			enableSSL(httpClient);
		}
		//设置请求头信息
		if(null != headers){
			httpPost.setHeaders(assemblyHeader(headers));
		}
		//设置请求参数
		if(null != payload && !payload.equals("")) {
			//StringEntity entity = new StringEntity(payload, contentType, encoding);
			ByteArrayEntity entity = new ByteArrayEntity(payload.getBytes(encoding));
			entity.setContentType(contentType);
			httpPost.setEntity(entity);
		}
		HttpResponse response = httpClient.execute(httpPost);
		BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());

		Result result = new Result();
		//设置返回的cookie
		result.setCookie(assemblyCookie(httpClient.getCookieStore().getCookies()));
		//设置响应状态码
		result.setStatusCode(response.getStatusLine().getStatusCode());
		//设置响应头信息
		result.setHeaders(response.getAllHeaders());
		//设置响应体
		result.setHttpEntity(entity);
		httpPost.abort();
		httpPost.releaseConnection();
		return result ;
	}

	/**
	 * 组装请求头信息
	 * @param headers
	 * @return
	 */
	public static Header[] assemblyHeader(Map<String,String> headers){
		Header[] allHeader= new BasicHeader[headers.size()];
		int i  = 0;
		for (String str :headers.keySet()) {
			allHeader[i] = new BasicHeader(str,headers.get(str));
			i++;
		}
		return allHeader;
	}

	/**
	 * 组装Cookie
	 * @param cookies
	 * @return
	 */
	public static String assemblyCookie(List<Cookie> cookies){
		StringBuffer buffer = new StringBuffer();
		for (Cookie cookie : cookies) {
			buffer.append(cookie.getName()).append("=").append(cookie.getValue()).append(";");
		}
		if(buffer.length()>0){
			buffer.deleteCharAt(buffer.length()-1);
		}
		return buffer.toString();
	}

	/**
	 * 组装请求参数
	 * @param parameters
	 * @return
	 */
	public static String assemblyParameter(Map<String,String> parameters){
		String para = "?";
		for (String str :parameters.keySet()) {
			String val = parameters.get(str);
			Pattern pattern = compile("[\\u4e00-\\u9fa5]+");
			Matcher matcher = pattern.matcher(val);
			if(matcher.find()) {
				try {
					val = URLEncoder.encode(val, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			para += str + "=" + val + "&";
		}
		return para.substring(0,para.length() - 1);
	}

	/**
	 * 获取HTML内容(默认get方式)
	 * @param url
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String getHTML(String url) throws Exception {
		return getHTML(url,null,true);
	}

	/**
	 * 获取HTML内容(默认get方式)
	 * @param url
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String getHTML(String url,Map<String,String> headers, boolean isGet) throws Exception {
		Result result = null;
		result = get(url, headers, null);
		HttpEntity entity = result.getHttpEntity();
		if(null == entity) {
			return null;
		}
		String respHtml = EntityUtils.toString(result.getHttpEntity());
		return respHtml;
	}

	/**
	 * 获取响应体
	 * @param url
	 * @param headers
	 * @return
	 */
	public static Object[] getResponse(String url,Map<String,String> headers) {
		DefaultHttpClient httpClient = getDefaultHttpClient();
		HttpHead httpHead = new HttpHead(url);
		//设置请求头信息
		if(null == headers || headers.isEmpty()) {
			for(Map.Entry<String, String> entry : headers.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();
				httpHead.addHeader(key, val);
			}
		}
		HttpResponse response = null;
		try {
			response = httpClient.execute(httpHead);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//获取HTTP响应状态码
		int statusCode = response.getStatusLine().getStatusCode();
		Object[] result = null;
		if(!(statusCode >= 200 && statusCode <= 399)) {
			result = new Object[] {statusCode,null};
		} else {
			Header[] headerArray = response.getAllHeaders();
			result = new Object[] {statusCode,headerArray};
		}
		httpHead.abort();
		httpHead.releaseConnection();
		return result;
	}

	/**
	 * 获取响应体
	 * @param url
	 * @return
	 */
	public static Object[] getResponse(String url) {
		return getResponse(url, null);
	}

	/**
	 * 获取Content-Disposition响应头
	 * @param url
	 * @return
	 */
	public static String getContentDisposition(String url) {
		DefaultHttpClient httpClient = getDefaultHttpClient();
		HttpHead httpHead = new HttpHead(url);
		HttpResponse response = null;
		try {
			response = httpClient.execute(httpHead);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String contentDisposition = null;
		if(response.getStatusLine().getStatusCode() == 200){
			Header[] headers = response.getHeaders("Content-Disposition");
			if(headers.length > 0) {
				contentDisposition = headers[0].getValue();
			}
		}
		httpHead.abort();
		httpHead.releaseConnection();
		return contentDisposition;
	}

	/**
	 * 获取响应头信息，如Content-Disposition、Content-Length
	 * @param url  请求URL
	 * @return
	 */
	public static Header[] getResponseHeaders(String url) {
		return getResponseHeaders(url, null);
	}

	/**
	 * 获取响应头信息，如Content-Disposition、Content-Length
	 * @param url  请求URL
	 * @headers    设置请求头信息
	 * @return
	 */
	public static Header[] getResponseHeaders(String url,Map<String,String> headers) {
		DefaultHttpClient httpClient = getDefaultHttpClient();
		HttpHead httpHead = new HttpHead(url);
		//设置请求头信息
		if(null == headers || headers.isEmpty()) {
			for(Map.Entry<String, String> entry : headers.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();
				httpHead.addHeader(key, val);
			}
		}
		HttpResponse response = null;
		try {
			response = httpClient.execute(httpHead);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//获取HTTP响应状态码
		int statusCode = response.getStatusLine().getStatusCode();
		if(!(statusCode >= 200 && statusCode <= 399)) {
			return null;
		}
		Header[] headerArray = response.getAllHeaders();
		httpHead.abort();
		httpHead.releaseConnection();
		return headerArray;
	}

	/**
	 * @Author: Lanxiaowei(736031305@qq.com)
	 * @Title: produceResult
	 * @Description: 生产Result
	 * @param @param httpClient
	 * @param @param response
	 * @param @return
	 * @param @throws Exception
	 * @return Result
	 * @throws
	 */
	private static Result produceResult(DefaultHttpClient httpClient,
										HttpResponse response) throws Exception {
		BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
		Result result = new Result();
		//设置返回的cookie
		result.setCookie(assemblyCookie(httpClient.getCookieStore().getCookies()));
		//设置响应状态码
		result.setStatusCode(response.getStatusLine().getStatusCode());
		//设置响应头信息
		result.setHeaders(response.getAllHeaders());
		//设置响应体
		result.setHttpEntity(entity);
		return result;
	}
}