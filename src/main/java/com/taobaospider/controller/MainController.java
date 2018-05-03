package com.taobaospider.controller;

import com.taobaospider.config.SpiderConfig;
import com.taobaospider.spider.DateUtil;
import com.taobaospider.spider.FileUtil;
import com.taobaospider.spider.SpiderUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * <p>Title: MainController.java </p>
 * <p>Description 接口 </p>
 * @author Wjj
 * @CreateDate 2018/4/20 18:16
 */
public class MainController implements Initializable{

	@FXML
	private TextField loginName;
	@FXML
	private PasswordField password;
//	@FXML
//	private Button login;
	@FXML
	private TextArea cookie;
	@FXML
	private ComboBox<String> category;
	@FXML
	private TextField keyword;


	/**
	 * 初始化，由JavaFX调用
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		System.out.println(location);
		ObservableList<String> roles = FXCollections.observableArrayList(SpiderConfig.CATEGORY);
		category.setItems(roles);
	}

	public void login(ActionEvent event) {
		SpiderUtil.login();

	}
	
	public void startSpider(ActionEvent event) {
		System.out.println("开始抓取数据");
		String categoryStr = this.category.getValue();
		if (StringUtils.isBlank(categoryStr)) {
			Alert err = new Alert(Alert.AlertType.WARNING);
			err.setContentText("请选择类目");
			err.show();
			return;
		}
		String keyword = this.keyword.getText();
		if (StringUtils.isBlank(keyword)) {
			Alert err = new Alert(Alert.AlertType.WARNING);
			err.setContentText("请输入关键词");
			err.show();
			return;
		}
		String cookie = this.cookie.getText();
		if (StringUtils.isBlank(cookie)) {
			Alert err = new Alert(Alert.AlertType.WARNING);
			err.setContentText("请输入cookie值");
			err.show();
			return;
		}

		System.out.println("keyword:" + keyword);
		SpiderUtil su = new SpiderUtil();
		String resultData = su.getData(keyword, cookie);
		if (StringUtils.isNotBlank(resultData)) {
			createFile(resultData);
		}

	}

	/**
	 * 生成数据文件
	 */
	private void createFile(String data) {
		String path = DateUtil.getDateFormat(new Date()) + "\\" + this.category.getValue() + "\\" + this.keyword.getText() + ".txt";
//		File f = new File(path);
//		FileUtil.makeDirectory(f);
		try {
			FileUtil.genModuleTpl(path, data);
			System.out.println(this.keyword + "热搜词获取成功啦:)");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
