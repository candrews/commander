package com.integralblue.commander.plugins.kodi.rpc;

import com.googlecode.jsonrpc4j.JsonRpcMethod;

public interface Application {

	@JsonRpcMethod("Application.Quit")
	String quit();

	@JsonRpcMethod("Application.SetMute")
	String setMute();

	@JsonRpcMethod("Application.SetMute")
	String setMute(boolean mute);

	@JsonRpcMethod("Application.SetVolume")
	String setVolume(IncrementOrDecrement volume);

	/**
	 * @param volume
	 *            0 to 100
	 * @return
	 */
	@JsonRpcMethod("Application.SetVolume")
	String setVolume(short volume);
}
