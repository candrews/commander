package com.integralblue.commander.plugins.kodi.rpc;

import com.googlecode.jsonrpc4j.JsonRpcMethod;

import lombok.Value;

public interface Player {

	@Value
	public class ActivePlayer {
		private int playerid;
		private String type;
		private int speed;
	}

	@Value
	public class Speed {
		private int speed;
	}

	@JsonRpcMethod("Player.GetActivePlayers")
	ActivePlayer[] getActivePlayers();

	@JsonRpcMethod("Player.PlayPause")
	Speed playPause(int playerid);

	@JsonRpcMethod("Player.PlayPause")
	Speed playPause(int playerid, boolean play);

	@JsonRpcMethod("Player.Stop")
	String stop(int playerid);
}
