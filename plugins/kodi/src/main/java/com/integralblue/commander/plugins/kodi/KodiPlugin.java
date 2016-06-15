package com.integralblue.commander.plugins.kodi;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.integralblue.commander.Manager.MenuConfiguration;
import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.plugins.kodi.rpc.Application;
import com.integralblue.commander.plugins.kodi.rpc.GUI;
import com.integralblue.commander.plugins.kodi.rpc.Input;
import com.integralblue.commander.plugins.kodi.rpc.Player;
import com.integralblue.commander.plugins.kodi.rpc.Player.ActivePlayer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KodiPlugin extends AbstractPlugin {
	
	private Player player;
	private Input input;
	private GUI gui;
	private Application application;

	private void handleErrors(Runnable r) {
		try {
			r.run();
		} catch (Exception e) {
			manager.say("Sorry, an error occurred");
			log.info("Error", e);
		}
	}

	@Override
	public void initialize() throws Exception {
		super.initialize();

		if (!config.hasPath("username") || !config.hasPath("password")) {
			throw new IllegalArgumentException(
					"username and/or password is not set in the configuration - please provide the Kodi credentials.");
		}
		if (!config.hasPath("address") || !config.hasPath("port")) {
			throw new IllegalArgumentException(
					"address and/or/port is not set in the configuration - please provide the Kodi connection information.");
		}
		final String username = config.getString("username");
		final String password = config.getString("password");
		final String address = config.getString("address");
		final int port = config.getInt("port");
		final int connectionTimeoutMillis = config.getInt("connectionTimeoutMillis");

		final URL jsonRpcUrl = new URL("http://" + "@" + address + ":" + port + "/jsonrpc");

		JsonRpcHttpClient client = new JsonRpcHttpClient(jsonRpcUrl,
				Collections.singletonMap("Authorization", "Basic " + Base64.getEncoder()
						.encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1))));
		client.setConnectionTimeoutMillis(connectionTimeoutMillis);

		player = ProxyUtil.createClientProxy(getClass().getClassLoader(), Player.class, client);
		input = ProxyUtil.createClientProxy(getClass().getClassLoader(), Input.class, client);
		gui = ProxyUtil.createClientProxy(getClass().getClassLoader(), GUI.class, client);
		application = ProxyUtil.createClientProxy(getClass().getClassLoader(), Application.class, client);

		manager.onSay(next -> text -> {
			try {
				gui.showNotification("Commander", text);
			} catch (Exception e) {
				log.info("Unable to contact kodi", e);
			}
			next.accept(text);
		});
		manager.onMainMenuOption("play (kodi | cody)", (mainMenuController, mainMenuResult) -> {
			play();
		});
		manager.onMainMenuOption("pause (kodi | cody)", (mainMenuController, mainMenuResult) -> {
			pause();
		});
		manager.onMainMenuOption("stop (kodi | cody)", (mainMenuController, mainMenuResult) -> {
			stop();
		});
		manager.onMainMenuOption("quit (kodi | cody)", (mainMenuController, mainMenuResult) -> {
			handleErrors(() -> {
				application.quit();
			});
		});
		manager.onMainMenuOption("mute (kodi | cody)", (mainMenuController, mainMenuResult) -> {
			handleErrors(() -> {
				application.setMute(true);
			});
		});
		manager.onMainMenuOption("((un mute) | unmute) (kodi | cody)", (mainMenuController, mainMenuResult) -> {
			handleErrors(() -> {
				application.setMute(false);
			});
		});

		manager.onMainMenuOption("control (kodi | cody)", (mainMenuController, mainMenuResult) -> {
			manager.menu(MenuConfiguration.builder().prompt(() -> {
				/* Do nothing */
			}).jsgfToConsumer("play", (mc, grammerResult) -> {
				play();
				mc.repeatMenu();
			}).jsgfToConsumer("pause", (mc, grammerResult) -> {
				pause();
				mc.repeatMenu();
			}).jsgfToConsumer("stop", (mc, grammerResult) -> {
				stop();
				mc.repeatMenu();
			}).jsgfToConsumer("quit", (mc, grammerResult) -> {
				handleErrors(() -> {
					application.quit();
				});
				mc.repeatMenu();
			}).jsgfToConsumer("info", (mc, grammerResult) -> {
				handleErrors(() -> {
					input.info();
				});
				mc.repeatMenu();
			}).jsgfToConsumer("back", (mc, grammerResult) -> {
				handleErrors(() -> {
					input.back();
				});
				mc.repeatMenu();
			}).jsgfToConsumer("up", (mc, grammerResult) -> {
				handleErrors(() -> {
					input.up();
				});
				mc.repeatMenu();
			}).jsgfToConsumer("down", (mc, grammerResult) -> {
				handleErrors(() -> {
					input.down();
				});
				mc.repeatMenu();
			}).jsgfToConsumer("left", (mc, grammerResult) -> {
				handleErrors(() -> {
					input.left();
				});
				mc.repeatMenu();
			}).jsgfToConsumer("right", (mc, grammerResult) -> {
				handleErrors(() -> {
					input.right();
				});
				mc.repeatMenu();
			}).jsgfToConsumer("select", (mc, grammerResult) -> {
				handleErrors(() -> {
					input.select();
				});
				mc.repeatMenu();
			}).jsgfToConsumer("home", (mc, grammerResult) -> {
				handleErrors(() -> {
					input.home();
				});
				mc.repeatMenu();
			}).jsgfToConsumer("mute", (mc, grammerResult) -> {
				handleErrors(() -> {
					application.setMute(true);
				});
				mc.repeatMenu();
			}).jsgfToConsumer("(un mute) | unmute", (mc, grammerResult) -> {
				handleErrors(() -> {
					application.setMute(false);
				});
				mc.repeatMenu();
			}).jsgfToConsumer("done", (mc, grammerResult) -> {
				// do nothing
			}).build());
		});
	}

	private void pause() {
		handleErrors(() -> {
			for (ActivePlayer activePlayer : player.getActivePlayers()) {
				if (activePlayer.getSpeed() > 0) {
					player.playPause(activePlayer.getPlayerid(), false);
					break;
				}
			}
			manager.say("Nothing is playing");
		});
	}

	private void play() {
		handleErrors(() -> {
			for (ActivePlayer activePlayer : player.getActivePlayers()) {
				if (activePlayer.getSpeed() == 0) {
					player.playPause(activePlayer.getPlayerid(), true);
					break;
				}
			}
			manager.say("Nothing is paused");
		});
	}

	private void stop() {
		handleErrors(() -> {
			for (ActivePlayer activePlayer : player.getActivePlayers()) {
				player.stop(activePlayer.getPlayerid());
				break;
			}
			manager.say("Nothing is playing");
		});
	}

}
