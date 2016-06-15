package com.integralblue.commander.plugins.watson;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.ibm.watson.developer_cloud.http.ServiceCall;
import com.ibm.watson.developer_cloud.http.ServiceCallback;
import com.integralblue.commander.api.AbstractPlugin;

public class AbstractWatsonPlugin extends AbstractPlugin {

	protected String username;
	protected String password;

	@Override
	public void initialize() throws Exception {
		super.initialize();
		if (!config.hasPath("username") || !config.hasPath("password")) {
			throw new IllegalArgumentException(
					"username and/or password is not set in the configuration - please provide Watson credentials. Sign up at https://console.ng.bluemix.net/pricing/");
		}
		username = config.getString("username");
		password = config.getString("password");
	}

	protected <T> CompletionStage<T> serviceCallToCompletionStage(ServiceCall<T> serviceCall) {
		final CompletableFuture<T> ret = new CompletableFuture<>();
		serviceCall.enqueue(new ServiceCallback<T>() {

			@Override
			public void onFailure(Exception e) {
				ret.completeExceptionally(e);
			}

			@Override
			public void onResponse(T response) {
				ret.complete(response);
			}
		});
		return ret;
	}

}
