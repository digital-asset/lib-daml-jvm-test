package com.digitalasset.testing.junit4;

import com.google.common.io.Files;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static java.util.Collections.singletonList;

public class MavenDownloader {
    protected static final String MAVEN_REPO_TYPE_DEFAULT = "default";
    protected static final String DAML_REPO_ID = "DAML_REPO";

    public static Optional<File> downloadDarFileFromMaven(
            MavenCoordinates mavenCoordinates, File localRepository) {
        return downloadMavenArtifact(
                mavenCoordinates.getGroup(),
                mavenCoordinates.getDarArtifact(),
                mavenCoordinates.getVersion(),
                "dar",
                mavenCoordinates.getRepoUrl(),
                mavenCoordinates.getMavenCredentials(),
                localRepository);
    }

    public static Optional<File> downloadDamlYamlFileFromMaven(
            MavenCoordinates mavenCoordinates, File localRepository) {
        return mavenCoordinates
                .getYamlArtifact()
                .flatMap(
                        yamlArtifact ->
                                downloadMavenArtifact(
                                        mavenCoordinates.getGroup(),
                                        yamlArtifact,
                                        mavenCoordinates.getVersion(),
                                        "yaml",
                                        mavenCoordinates.getRepoUrl(),
                                        mavenCoordinates.getMavenCredentials(),
                                        localRepository))
                .map(file -> {
                    final File tempDamlRoot = createTempDamlRoot();
                    try {
                        Files.copy(file, new File(tempDamlRoot, "daml.yaml"));
                        return tempDamlRoot;
                    } catch (IOException e) {
                        throw new IllegalStateException("Cannot copy DAML yaml file to temporary DAML root: " + tempDamlRoot.getAbsolutePath(), e);
                    }
                });
    }

    private static File createTempDamlRoot() {
        return Optional.of(Files.createTempDir())
                .map(f -> {
                    final File damlroot = new File(f, "damlroot");
                    if (damlroot.mkdirs())
                        return damlroot;
                    else
                        throw new IllegalStateException("Unable to create temporary DAML root: " + damlroot.getAbsolutePath());
                }).get();

    }

    public static Optional<File> downloadMavenArtifact(
            String groupId,
            String artifactId,
            String version,
            String extension,
            String mavenRepoUrl,
            Optional<MavenCredentials> mavenCredentials,
            File localRepository) {
        RemoteRepository remoteRepository =
                mavenCredentials
                        .map(
                                credentials ->
                                        new RemoteRepository.Builder(
                                                DAML_REPO_ID, MAVEN_REPO_TYPE_DEFAULT, mavenRepoUrl)
                                                .setAuthentication(buildMavenAuthentication(credentials))
                                                .build())
                        .orElse(
                                new RemoteRepository.Builder(DAML_REPO_ID, MAVEN_REPO_TYPE_DEFAULT, mavenRepoUrl)
                                        .build());

        ArtifactRequest artifactRequest =
                new ArtifactRequest()
                        .setArtifact(new DefaultArtifact(groupId, artifactId, extension, version))
                        .setRepositories(singletonList(remoteRepository));

        try {
            RepositorySystem repositorySystem =
                    MavenRepositorySystemUtils.newServiceLocator()
                            .addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class)
                            .addService(TransporterFactory.class, FileTransporterFactory.class)
                            .addService(TransporterFactory.class, HttpTransporterFactory.class)
                            .getService(RepositorySystem.class);

            DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
            session.setLocalRepositoryManager(
                    repositorySystem.newLocalRepositoryManager(
                            session, new LocalRepository(localRepository.toString())));
            ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest);
            return Optional.ofNullable(artifactResult.getArtifact().getFile());
        } catch (ArtifactResolutionException e) {
            throw new IllegalStateException(
                    "Unable to download artifact " + groupId + ":" + artifactId + ":" + version, e);
        }
    }

    private static Authentication buildMavenAuthentication(MavenCredentials credentials) {
        return new AuthenticationBuilder()
                .addUsername(credentials.getUserName())
                .addPassword(credentials.getPassword())
                .build();
    }

}
