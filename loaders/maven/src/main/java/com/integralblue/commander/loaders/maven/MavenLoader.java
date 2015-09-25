package com.integralblue.commander.loaders.maven;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;

import com.integralblue.commander.api.AbstractLoader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MavenLoader extends AbstractLoader {

	private RepositorySystem system;

	private RepositorySystemSession session;

	private List<RemoteRepository> remoteRepositories;

	@Override
	public ClassLoader getClassLoader(String parameters, @NonNull ClassLoader parent) {
		if (parameters == null || parameters.isEmpty()) {
			throw new IllegalArgumentException(
					"Error loading classes with the maven loader. The parameter must be specified to specify the coordinates of the maven artifact to load.");
		}
		return URLClassLoader.newInstance(getDependenciesForArtifact(parameters).stream().toArray(URL[]::new), parent);
	}

	@SneakyThrows
	private List<URL> getDependenciesForArtifact(@NonNull String coordinates) {
		log.debug("Resolving dependencies for: {}", coordinates);

		Artifact artifact = new DefaultArtifact(coordinates);

		DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);

		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
		collectRequest.setRepositories(remoteRepositories);

		DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFilter);

		List<ArtifactResult> artifactResults = system.resolveDependencies(session, dependencyRequest)
				.getArtifactResults();
		List<URL> ret = new ArrayList<>(artifactResults.size());
		for (ArtifactResult artifactResult : artifactResults) {
			ret.add(artifactResult.getArtifact().getFile().toURI().toURL());
		}
		log.debug("Finished resolving dependencies for {}, result is: {}", coordinates, ret);
		return ret;
	}

	@Override
	public void initialize() throws Exception {
		system = newRepositorySystem();

		session = newRepositorySystemSession(system);

		remoteRepositories = new ArrayList<RemoteRepository>();
		for (Entry<String, ConfigValue> loaderEntry : config.getObject("repositories").entrySet()) {
			String repositoryName = loaderEntry.getKey();
			Config repositoryConfig = config.getConfig("repositories." + repositoryName);
			String repositoryUrl = repositoryConfig.getString("url");
			remoteRepositories.add(new RemoteRepository.Builder(repositoryName, "default", repositoryUrl).build());
		}
	}

	private RepositorySystem newRepositorySystem() {
		/*
		 * Aether's components implement org.eclipse.aether.spi.locator.Service
		 * to ease manual wiring and using the prepopulated
		 * DefaultServiceLocator, we only need to register the repository
		 * connector and transporter factories.
		 */
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

		locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
			@Override
			public void serviceCreationFailed(@NonNull Class<?> type, @NonNull Class<?> impl,
					@NonNull Throwable exception) {
				log.error("serviceCreationFailed: type: {}, impl: {}", type, impl, exception);
			}
		});

		return locator.getService(RepositorySystem.class);
	}

	private DefaultRepositorySystemSession newRepositorySystemSession(@NonNull RepositorySystem system) {
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

		// since com.integralblue.commander:api is already loaded, there's no
		// need to load it again, so exclude it
		session.setDependencySelector(
				new AndDependencySelector(session.getDependencySelector(), new ExclusionDependencySelector(
						Collections.singletonList(new Exclusion("com.integralblue.commander", "api", "*", "*")))));

		LocalRepository localRepo = new LocalRepository(config.getString("localRepository"));
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

		session.setReadOnly();

		return session;
	}

}
