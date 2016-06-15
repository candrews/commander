package com.integralblue.commander.plugins.kodi.rpc;

import com.googlecode.jsonrpc4j.JsonRpcMethod;

public interface GUI {

	@JsonRpcMethod("GUI.ShowNotification")
	String showNotification(String title, String message);

	@JsonRpcMethod("GUI.ShowNotification")
	String showNotification(String title, String message, String image);

	@JsonRpcMethod("GUI.ShowNotification")
	String showNotification(String title, String message, String image, int displaytime);
}
