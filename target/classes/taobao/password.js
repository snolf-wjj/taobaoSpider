var UA_Opt = {};
  UA_Opt.ExTarget = ['TPL_password_1','TPL_password_2','J_Pwd1','J_PwdV'];
  UA_Opt.FormId = "J_Form";

  function initNC() {
		var nc = new noCaptcha();
			var opt = {
				renderTo : "nocaptcha",
				appkey : "CF_APP_TBLogin_PC",
				token : "f33520a81f289b249b9be36d1bddd8445d7455ca",
				elementID : [ "TPL_username_1" ],
				trans : {"behaviorTraceId": "null"},
				is_Opt : 1,
				language : "zh_CN",
				isEnabled : true,

				customWidth: 'J_StaticForm',
				customFloatHeight: 420,

				times : 3,
				callback: function (data) {
					var S = KISSY;
					S.one("#J_NcoSig").val( data.sig);
					S.one("#J_NcoSessionid").val(data.csessionid);
				},
				error: function (s) {
					window.console && console.log("error");
					window.console && console.log(s);
				},
				is_tbLogin : true
			};
			nc.init(opt);
	}

	initNC();
