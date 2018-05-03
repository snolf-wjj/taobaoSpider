package com.taobaospider.spider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * <p>Title: FileUtil </p>
 * <p>Description  </p>
 * <p>Company: http://www.hnxianyi.com </p>
 *
 * @author Wjj
 * @CreateDate 2018/4/26 17:58
 */
public class FileUtil {
	/**
	 * 私有构造方法，防止类的实例化，因为工具类不需要实例化。
	 */
	private FileUtil() {

	}

	/**
	 * 判断指定的文件是否存在。
	 * @param fileName 要判断的文件的文件名
	 * @return 存在时返回true，否则返回false。
	 * @since  1.0
	 */
	public static boolean isFileExist(String fileName) {
		return new File(fileName).isFile();
	}

	/**
	 * 检查给定目录的存在性
	 * 保证指定的路径可用，如果指定的路径不存在，那么建立该路径，可以为多级路径
	 * @param path
	 * @return 真假值
	 * @since  1.0
	 */
	public static final boolean pathValidate(String path)
	{
		//String path="d:/web/www/sub";
		//System.out.println(path);
		//path = getUNIXfilePath(path);

		//path = ereg_replace("^\\/+", "", path);
		//path = ereg_replace("\\/+$", "", path);
		String[] arraypath = path.split("/");
		String tmppath = "";
		for (int i = 0; i < arraypath.length; i++)
		{
			tmppath += "/" + arraypath[i];
			File d = new File(tmppath.substring(1));
			if (!d.exists()) {
				//检查Sub目录是否存在
				System.out.println(tmppath.substring(1));
				if (!d.mkdir()) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 创建指定的目录。
	 * 如果指定的目录的父目录不存在则创建其目录书上所有需要的父目录。
	 * <b>注意：可能会在返回false的时候创建部分父目录。</b>
	 * @param file 要创建的目录
	 * @return 完全创建成功时返回true，否则返回false。
	 * @since  1.0
	 */
	public static boolean makeDirectory(File file) {
		File parent = file.getParentFile();
		if (parent != null) {
			return parent.mkdirs();
		}
		return false;
	}

	/**
	 * 根据内容生成文件
	 * @since  1.0
	 */
	public static final boolean genModuleTpl(String path, String modulecontent)  throws IOException
	{

		path = getUNIXfilePath(path);
		String[] patharray = path.split("\\/");
		String modulepath = "";
		for (int i = 0; i < patharray.length - 1; i++) {
			modulepath += "/" + patharray[i];
		}
		File d = new File(modulepath.substring(1));
		if (!d.exists()) {
			if (!pathValidate(modulepath.substring(1))) {
				return false;
			}
		}
		try {
			FileWriter fw = new FileWriter(path); //建立FileWriter对象，并实例化fw
			//将字符串写入文件
			fw.write(modulecontent);
			fw.close();
		}
		catch (IOException e) {
			throw e;
		}
		return true;
	}

	/**
	 * 将DOS/Windows格式的路径转换为UNIX/Linux格式的路径。
	 * 其实就是将路径中的"\"全部换为"/"，因为在某些情况下我们转换为这种方式比较方便，
	 * 某中程度上说"/"比"\"更适合作为路径分隔符，而且DOS/Windows也将它当作路径分隔符。
	 * @param filePath 转换前的路径
	 * @return 转换后的路径
	 * @since  1.0
	 */
	public static String toUNIXpath(String filePath) {
		return filePath.replace('\\', '/');
	}

	/**
	 * 从文件名得到UNIX风格的文件绝对路径。
	 * @param fileName 文件名
	 * @return 对应的UNIX风格的文件路径
	 * @since  1.0
	 * @see #toUNIXpath(String filePath) toUNIXpath
	 */
	public static String getUNIXfilePath(String fileName) {
		File file = new File(fileName);
		return toUNIXpath(file.getAbsolutePath());
	}
}
