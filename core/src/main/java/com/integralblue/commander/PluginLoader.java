package com.integralblue.commander;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.integralblue.commander.api.Loader;
import com.integralblue.commander.api.Plugin;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Builder
@Value
@Slf4j
class PluginLoader {
	private static final Pattern LOADER_EXPRESSION_PATTERN = Pattern
			.compile("(?<loaderName>[^\\[:]+)(?:\\[(?<loaderParameters>[^\\]]*)\\])?:");

	@Singular
	@NonNull
	Map<String, LoaderDefinition> nameToLoaderDefinitions;

	private final ConcurrentMap<String, Loader> nameToLoaders = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, ClassLoader> loaderExpressionToClassLoaders = new ConcurrentHashMap<>();

	private ClassLoader getClassLoader(String loaderExpression) {
		Matcher matcher = LOADER_EXPRESSION_PATTERN.matcher(loaderExpression + ":");
		if (!matcher.find()) {
			throw new IllegalArgumentException("Invalid loader expression: " + loaderExpression);
		}
		List<ClassLoader> classLoaders = new ArrayList<>();
		do {
			String loaderName = matcher.group("loaderName");
			String loaderParameters = matcher.group("loaderParameters");
			classLoaders.add(loaderExpressionToClassLoaders.computeIfAbsent(loaderExpression, (key) -> {
				ClassLoader classLoader;
				try {
					classLoader = getLoader(loaderName).getClassLoader(loaderParameters, getClass().getClassLoader());
					if (classLoader == null) {
						throw new IllegalStateException(
								"loader returned a null class loader, which should never happen.");
					}
					return classLoader;
				} catch (Exception e) {
					throw new RuntimeException("Error loading class loader with expression: " + loaderExpression, e);
				}
			}));
		} while (matcher.find());
		return new ListDelegatingClassLoader(classLoaders.stream().toArray(ClassLoader[]::new));
	}

	@SuppressWarnings("unchecked")
	private Loader getLoader(String loaderName) {
		return nameToLoaders.computeIfAbsent(loaderName, (key) -> {
			try {
				LoaderDefinition loaderDefinition = nameToLoaderDefinitions.get(loaderName);
				if (loaderDefinition == null) {
					throw new IllegalArgumentException("Undefined loader: " + loaderName);
				}
				Class<? extends Loader> loaderClass;
				if (loaderName.equals("root") && loaderDefinition.getLoaderLoader().equals("root")) {
					// the root loader cannot load itself (as it doesn't exist
					// yet
					// to load itself), so the root loader is a special case
					// where
					// the root loader class is loaded using the default class
					// loader
					// if the root class load has any configuration options,
					// they can be read from loaderDefinition here
					loaderClass = (Class<? extends Loader>) loadClass(loaderDefinition.getClassName());
				} else {
					loaderClass = (Class<? extends Loader>) loadClass(loaderDefinition.getClassName(),
							loaderDefinition.getLoaderLoader());
				}
				Loader loader = loaderClass.newInstance();
				loader.setConfig(loaderDefinition.getConfig());
				loader.initialize();
				return loader;
			} catch (Exception e) {
				throw new RuntimeException("failed to load loader with name: " + loaderName, e);
			}
		});
	}

	private Class<?> loadClass(String className) throws ClassNotFoundException {
		return Class.forName(className);
	}

	public Class<?> loadClass(String className, String loaderExpression) throws ClassNotFoundException {
		// the very first thing is to see if the default class loader has the
		// class. If it has the class, there's no reason to continue on -
		// because the parent class loader is always checked first, there's no
		// reason to keep looking if the top-most parent already has the class.
		// Besides being an optimization, this approach also allows dependencies
		// to be bundled with the core. For example, a plugin could be specified
		// as a Maven dependency of the core so it's always available - then
		// there's no need to also try to get it from Maven later using a loader
		// as specified in a configuration file.
		try {
			Class<?> clazz = Class.forName(className);
			log.debug(
					"className '{}' was to be loaded with loader expression '{}' but the class was found in the root classloader so the loader expression was not used",
					className, loaderExpression);
			return clazz;
		} catch (ClassNotFoundException e) {
			// default class loader doesn't have the class... so carry on
		}

		try {
			ClassLoader classLoader = getClassLoader(loaderExpression);
			// have to save then restore the context class loader for this
			// thread
			// plugin may use the contextClassLoader, and expect it to be able
			// to load their classes
			// for example, if a plugin uses java.util.ServiceLoader that won't
			// work unless the context class loader is set to the plugin's class
			// loader.
			final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(classLoader);
			try {
				return classLoader.loadClass(className);
			} finally {
				Thread.currentThread().setContextClassLoader(contextClassLoader);
			}
		} catch (ClassNotFoundException e) {
			throw new ClassNotFoundException("Error loading class " + className + " with loader " + loaderExpression,
					e);
		} catch (Exception e) {
			throw new RuntimeException("Error loading class " + className + " with loader " + loaderExpression, e);
		}
	}

	@SuppressWarnings("unchecked")
	public Class<? extends Plugin> loadPluginClass(String className, String loaderExpression)
			throws ClassNotFoundException {
		return (Class<? extends Plugin>) loadClass(className, loaderExpression);
	}
}