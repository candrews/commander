package com.integralblue.commander.plugins.kodi.rpc;

import com.googlecode.jsonrpc4j.JsonRpcMethod;

public interface Input {

	@JsonRpcMethod("Input.Back")
	String back();

	@JsonRpcMethod("Input.Down")
	String down();

	@JsonRpcMethod("Input.Home")
	String home();

	@JsonRpcMethod("Input.Info")
	String info();

	@JsonRpcMethod("Input.Left")
	String left();

	@JsonRpcMethod("Input.Right")
	String right();

	@JsonRpcMethod("Input.Select")
	String select();

	@JsonRpcMethod("Input.Up")
	String up();
}
