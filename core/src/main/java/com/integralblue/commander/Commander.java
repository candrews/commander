package com.integralblue.commander;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.integralblue.commander.PluginLoader.PluginLoaderBuilder;
import com.integralblue.commander.api.Plugin;
import com.integralblue.commander.api.PluginDefinition;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException.Missing;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Commander {

	private static final ConfigParseOptions DEFAULT_CONFIG_PARSE_OPTIONS = ConfigParseOptions.defaults()
			.setAllowMissing(false);

	/**
	 * Acts as a cache so if the same config is loaded multiple times, only 1
	 * PluginLoader is used
	 */
	private static final ConcurrentMap<Config, PluginLoader> configToPluginLoader = new ConcurrentHashMap<>();

	public static Brain getBrain(@NonNull URL configurationURL, Map<String, String> configurationVariables) {
		log.debug("Loading configuration: {}", configurationURL);
		Config config = ConfigFactory.load(ConfigFactory.parseMap(configurationVariables, "configuration variables"))
				.withFallback(ConfigFactory.parseURL(configurationURL, DEFAULT_CONFIG_PARSE_OPTIONS))
				.withFallback(ConfigFactory.parseResources("default.conf", DEFAULT_CONFIG_PARSE_OPTIONS)).resolve();

		return getBrainBuilder(config).build();
	}

	private static Brain.BrainBuilder getBrainBuilder(@NonNull Config config) {

		final PluginLoader pluginLoader = configToPluginLoader.computeIfAbsent(config,
				Commander::getPluginLoaderFromConfig);

		final Brain.BrainBuilder brainBuilder = Brain.builder();
		config.getObject("plugins").entrySet().stream().forEach((pluginEntry) -> {
			String pluginName = pluginEntry.getKey();
			Config pluginConfig = config.getConfig("plugins." + pluginName);
			String pluginClassName = pluginConfig.getString("className");
			String loaderExpression = pluginConfig.getString("loader");
			try {
				Class<? extends Plugin> pluginClass = pluginLoader.loadPluginClass(pluginClassName, loaderExpression);

				Config pluginSettingsConfig;
				try {
					pluginSettingsConfig = pluginConfig.getConfig("config");
				} catch (Missing e) {
					// no config specified, so use an empty configuration
					pluginSettingsConfig = ConfigFactory.empty();
				}
				URL defaultPluginConfigurationURL = pluginClass
						.getResource("/" + pluginClass.getName().replace(".", "/") + ".conf");
				if (defaultPluginConfigurationURL != null) {
					// plugin has default configuration, so merge it in
					pluginSettingsConfig = pluginSettingsConfig
							.withFallback(ConfigFactory.parseURL(defaultPluginConfigurationURL,
									DEFAULT_CONFIG_PARSE_OPTIONS.setClassLoader(pluginClass.getClassLoader())));
				}

				brainBuilder.pluginNameToDefinition(pluginName,
						PluginDefinition.builder().clazz(pluginClass).config(pluginSettingsConfig).build());
			} catch (Exception e) {
				throw new RuntimeException("Error loading plugin with name: " + pluginName, e);
			}
		});

		return brainBuilder;

	}

	private static PluginLoader getPluginLoaderFromConfig(Config config) {
		PluginLoaderBuilder pluginLoaderBuilder = PluginLoader.builder();

		config.getObject("loaders").entrySet().stream().forEach((loaderEntry) -> {
			String loaderName = loaderEntry.getKey();
			Config loaderConfig = config.getConfig("loaders." + loaderName);
			String loaderClassName = loaderConfig.getString("className");
			String loaderExpression = loaderConfig.getString("loader");
			Config loaderSettingsConfig;
			try {
				loaderSettingsConfig = loaderConfig.getConfig("config");
			} catch (Missing e) {
				// no config specified, so use an empty configuration
				loaderSettingsConfig = ConfigFactory.empty();
			}
			pluginLoaderBuilder.nameToLoaderDefinition(loaderName, LoaderDefinition.builder().className(loaderClassName)
					.config(loaderSettingsConfig).loaderLoader(loaderExpression).build());
		});

		return pluginLoaderBuilder.build();
	}

	@SneakyThrows
	public static void main(String[] args) {
		String configurationPath = System.getProperty("commander.configuration", "classpath:/demo.conf");
		URL configurationURL;
		if (configurationPath.startsWith("classpath:/")) {
			configurationURL = Commander.class.getClassLoader()
					.getResource(configurationPath.substring("classpath:/".length()));
		} else {
			URI uri = URI.create(configurationPath);
			if (uri.getScheme() == null) {
				// assume it's a file path
				configurationURL = new URL("file://" + configurationPath);
			} else {
				configurationURL = new URL(configurationPath);
			}
		}
		if (configurationURL == null) {
			throw new IllegalArgumentException("Could not find specified configuration: " + configurationPath);
		}
		getBrain(configurationURL, Collections.emptyMap()).run();
	}

}
