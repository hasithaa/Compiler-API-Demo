/*
 * MIT License
 *
 * Copyright (c) 2023 Hasitha Aravinda. All rights reserved.
 *
 * This work is licensed under the terms of the MIT license.
 * For a copy, see <https://opensource.org/licenses/MIT>.
 */
package tips.bal.examples.compiler;

import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Basic Project API usage example.
 *
 * @since 1.0.0
 */
public class CompilerRunner {

    private static final String BALLERINA_HOME = "BALLERINA_HOME";
    private static final String BRE = "bre";
    private static final String DISTRIBUTIONS = "distributions";
    private static final String BALLERINA_VERSION = "ballerina-version";

    /**
     * Main method to run the compiler via Project API.
     *
     * @param sourcePath ballerina source path
     */
    public Project compile(Path sourcePath) {
        return compile(sourcePath, getActiveDistribution());
    }

    /**
     * Main method to run the compiler via Project API.
     *
     * @param sourcePath    ballerina source path
     * @param ballerinaHome ballerina distribution path
     */
    public Project compile(Path sourcePath, Path ballerinaHome) {
        Environment env = EnvironmentBuilder.getBuilder().setBallerinaHome(ballerinaHome).build();
        ProjectEnvironmentBuilder projEnvBuilder = ProjectEnvironmentBuilder.getBuilder(env);
        return ProjectLoader.loadProject(sourcePath, projEnvBuilder);
    }

    /**
     * Get the active Ballerina distribution.
     *
     * @return Path to the active distribution
     */
    protected Path getActiveDistribution() {
        final String getenv = System.getenv(BALLERINA_HOME);
        if (getenv == null) {
            throw new IllegalStateException("Ballerina home is not set");
        }
        final Path ballerinaHome = Paths.get((new File(getenv)).toURI());
        final File bre = ballerinaHome.resolve(BRE).toFile();
        if (bre.isDirectory()) {
            return ballerinaHome;
        }

        final File distro = ballerinaHome.resolve(DISTRIBUTIONS).toFile();
        if (!distro.isDirectory()) {
            throw new IllegalStateException("Ballerina home is invalid, distributions directory not found");
        }

        final File balVersion = ballerinaHome.resolve(Paths.get(DISTRIBUTIONS, BALLERINA_VERSION)).toFile();
        if (!balVersion.exists()) {
            throw new IllegalStateException("Ballerina home is invalid, ballerina-version file is missing");
        }

        StringBuilder sb = new StringBuilder();
        try (Scanner reader = new Scanner(balVersion, StandardCharsets.UTF_8)) {
            while (reader.hasNextLine()) {
                sb.append(reader.nextLine()).append("\n");
            }
        } catch (IOException e) { // Ignore
        }

        String version = sb.toString().trim();
        final Path activeDistro = ballerinaHome.resolve(Paths.get(DISTRIBUTIONS, version)).toAbsolutePath();
        if (activeDistro.toFile().isDirectory()) {
            return activeDistro;
        }
        throw new IllegalStateException("Ballerina home is invalid, active distribution not found");
    }
}
