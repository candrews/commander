package com.integralblue.commander.plugins.kodi.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum IncrementOrDecrement {
	@JsonProperty("increment")
	INCREMEMENT,

	@JsonProperty("decrement")
	DECREMENT
}
