/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinax.aws.lambda.generator.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.plugins.CompilerLifecycleEventContext;
import io.ballerina.projects.plugins.CompilerLifecycleTask;
import io.ballerina.projects.plugins.CompilerPluginException;
import org.ballerinax.aws.lambda.generator.Constants;
import org.ballerinax.aws.lambda.generator.DockerBuildException;
import org.ballerinax.aws.lambda.generator.LambdaFunctionInfo;
import org.ballerinax.aws.lambda.generator.LambdaUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Contains the code generation part of the lambda functions.
 *
 * @since 2.0.0
 */
public class LambdaCodeGeneratedTask implements CompilerLifecycleTask<CompilerLifecycleEventContext> {

    private static final PrintStream OUT = System.out;
    private static final int TERMINATION_TIMEOUT_SECONDS = 10;
    private static final int BUILDX_PROBE_TIMEOUT_SECONDS = 10;

    @Override
    public void perform(CompilerLifecycleEventContext lifecycleEventContext) {

        Project project = lifecycleEventContext.currentPackage().project();
        Path lambdaJson = project.targetDir().resolve("aws-lambda.json");
        Gson gson = new Gson();
        try (FileReader file = new FileReader(lambdaJson.toAbsolutePath().toString(),
                StandardCharsets.UTF_8)) {
            LambdaFunctionInfo[] functionInfo = gson.fromJson(file, LambdaFunctionInfo[].class);
            file.close();
            List<LambdaFunctionInfo> functions = functionInfo == null
                    ? Collections.emptyList() : Arrays.asList(functionInfo);
            List<String> generatedFunctions = functions.stream()
                    .map(LambdaFunctionInfo::getName).collect(Collectors.toList());
            BuildOptions buildOptions = project.buildOptions();
            boolean isNative = buildOptions.nativeImage();
            boolean isContainerImage = Constants.CLOUD_AWS_LAMBDA_IMAGE.equals(buildOptions.cloud());
            Optional<Path> generatedArtifactPath = lifecycleEventContext.getGeneratedArtifactPath();
            if (generatedArtifactPath.isPresent()) {
                Path executablePath = generatedArtifactPath.get();
                try {
                    Path functionsDir = LambdaUtils.getFunctionsDir(project, executablePath);
                    LambdaUtils.deleteDirectory(functionsDir);
                    Files.createDirectories(functionsDir);

                    String fileName = executablePath.getFileName().toString();
                    String balxName = fileName.substring(0, fileName.lastIndexOf('.'));
                    String imageName = null;
                    if (isContainerImage) {
                        imageName = this.buildContainerImage(functionsDir, executablePath, isNative,
                                lifecycleEventContext.currentPackage());
                    } else if (isNative) {
                        this.generateNativeZipFile(functionsDir, executablePath);
                    } else {
                        this.generateZipFile(functionsDir, executablePath, false);
                    }
                    OUT.println("\t@aws.lambda:Function: " + String.join(", ", generatedFunctions));
                    if (isContainerImage) {
                        this.printImageInstructions(imageName, balxName);
                    } else {
                        this.printZipInstructions(functionsDir, balxName, isNative);
                    }
                    this.printDestinationInstructions(functions);
                } catch (IOException e) {
                    throw new CompilerPluginException("Error generating AWS lambda zip file: " + e.getMessage(), e);
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            // A stale or partially written aws-lambda.json fails to deserialise rather than to read,
            // so the syntax error is reported the same way instead of escaping uncaught.
            OUT.println("Internal error occurred. Unable to read target/aws-lambda.json " + e.getMessage());
        }
    }

    private void printZipInstructions(Path functionsDir, String balxName, boolean isNative) throws IOException {
        String layer = "";
        if (!isNative) {
            String version = getResourceFileAsString("layer-version.txt");
            layer = " --layers arn:aws:lambda:$REGION_ID:367134611783:layer:ballerina-jre21:" + version;
        }
        // Quoted, because the target directory sits under the project path and the shell would
        // otherwise split the argument for anyone whose path contains a space.
        String zipArgument = "\"fileb://" + functionsDir + File.separator
                + Constants.LAMBDA_OUTPUT_ZIP_FILENAME + "\"";
        OUT.println("\n\tRun the following command to deploy each Ballerina AWS Lambda function:");
        OUT.println("\taws lambda create-function --function-name $FUNCTION_NAME --zip-file "
                + zipArgument +
                " --handler " +
                balxName + ".$FUNCTION_NAME --runtime provided.al2023 --role $LAMBDA_ROLE_ARN" + layer +
                " --memory-size 512 --timeout 10");
        OUT.println("\n\tRun the following command to re-deploy an updated Ballerina AWS Lambda function:");
        OUT.println("\taws lambda update-function-code --function-name $FUNCTION_NAME --zip-file "
                + zipArgument + "\n\n");
    }

    /**
     * Prints the command that configures destinations, for the functions that declared any.
     * Destinations are set separately from the function itself, so this is emitted alongside the
     * deploy commands rather than folded into them.
     */
    private void printDestinationInstructions(List<LambdaFunctionInfo> functions) {
        List<LambdaFunctionInfo> withDestinations = functions.stream()
                .filter(f -> f.getDestinations() != null && !f.getDestinations().isEmpty())
                .collect(Collectors.toList());
        if (withDestinations.isEmpty()) {
            return;
        }
        OUT.println("\tRun the following command to configure the destinations of each function. " +
                "Destinations apply to asynchronous invocations only:");
        for (LambdaFunctionInfo function : withDestinations) {
            LambdaFunctionInfo.Destinations destinations = function.getDestinations();
            StringBuilder config = new StringBuilder("{");
            if (destinations.getOnSuccess() != null) {
                config.append("\"OnSuccess\":{\"Destination\":\"").append(destinations.getOnSuccess()).append("\"}");
            }
            if (destinations.getOnFailure() != null) {
                if (config.length() > 1) {
                    config.append(',');
                }
                config.append("\"OnFailure\":{\"Destination\":\"").append(destinations.getOnFailure()).append("\"}");
            }
            config.append('}');
            OUT.println("\taws lambda put-function-event-invoke-config --function-name " + function.getName() +
                    " --destination-config '" + config + "'");
        }
        OUT.println();
    }

    private void printImageInstructions(String imageName, String balxName) {
        String repositoryName = imageName.substring(0, imageName.lastIndexOf(':'));
        OUT.println("\n\tBuilt the container image " + imageName + ".");
        OUT.println("\n\tRun the following commands to push the image to Amazon ECR:");
        OUT.println("\taws ecr create-repository --repository-name " + repositoryName +
                " --region $REGION_ID");
        OUT.println("\taws ecr get-login-password --region $REGION_ID | docker login --username AWS " +
                "--password-stdin $ACCOUNT_ID.dkr.ecr.$REGION_ID.amazonaws.com");
        OUT.println("\tdocker tag " + imageName +
                " $ACCOUNT_ID.dkr.ecr.$REGION_ID.amazonaws.com/" + imageName);
        OUT.println("\tdocker push $ACCOUNT_ID.dkr.ecr.$REGION_ID.amazonaws.com/" + imageName);
        OUT.println("\n\tRun the following command to deploy each Ballerina AWS Lambda function. The handler is " +
                "selected with --image-config, so a single image can serve every function in the package:");
        OUT.println("\taws lambda create-function --function-name $FUNCTION_NAME --package-type Image" +
                " --code ImageUri=$ACCOUNT_ID.dkr.ecr.$REGION_ID.amazonaws.com/" + imageName +
                // Double quoted so the shell expands $FUNCTION_NAME. Single quotes would pass it
                // through literally and Lambda would look for a handler of that name.
                " --role $LAMBDA_ROLE_ARN --image-config \"{\\\"Command\\\":[\\\"" + balxName +
                ".$FUNCTION_NAME\\\"]}\"" +
                " --memory-size 512 --timeout 10");
        OUT.println("\n\tRun the following command to re-deploy an updated Ballerina AWS Lambda function:");
        OUT.println("\taws lambda update-function-code --function-name $FUNCTION_NAME" +
                " --image-uri $ACCOUNT_ID.dkr.ecr.$REGION_ID.amazonaws.com/" + imageName + "\n\n");
    }

    /**
     * Packages the built artifact as a Lambda container image. The Ballerina runtime already implements the
     * Lambda Runtime API, so the image only has to start the artifact and let it poll for invocations.
     *
     * @param functionsDir   the directory the deployment artifacts are written to
     * @param binaryPath     the built jar or native executable
     * @param isNative       whether the build produced a native executable
     * @param currentPackage the package being built, used to name the image
     * @return the name and tag of the image that was built
     * @throws IOException if writing the Dockerfile fails
     */
    private String buildContainerImage(Path functionsDir, Path binaryPath, boolean isNative, Package currentPackage)
            throws IOException {
        String jarFileName = binaryPath.getFileName().toString();
        Files.copy(binaryPath, functionsDir.resolve(jarFileName), StandardCopyOption.REPLACE_EXISTING);
        String artifactName = jarFileName;
        if (isNative) {
            // The native builder reads and writes the jar directory, so the jar has to be in place first.
            buildRemoteArtifacts(functionsDir, jarFileName);
            artifactName = stripJarExtension(jarFileName);
            Files.deleteIfExists(functionsDir.resolve(jarFileName));
        }
        String imageName = toImageRepository(currentPackage.packageName().value()) + ":"
                + toImageTag(currentPackage.packageVersion().value().toString());
        Files.write(functionsDir.resolve(Constants.DOCKERFILE),
                dockerFileContent(artifactName, isNative).getBytes(StandardCharsets.UTF_8));
        OUT.println("\t@aws.lambda:Building the container image using Docker. This may take a while.\n");
        runDockerBuild(functionsDir, imageName);
        return imageName;
    }

    /**
     * Strips the {@code .jar} extension from a built artifact's file name.
     *
     * <p>The {@code .} is escaped and the match anchored to the end, because the unanchored
     * {@code ".jar"} pattern this replaced treats {@code .} as a wildcard: for {@code myjar.jar} it
     * matches {@code yjar} at index 1 and yields {@code m.jar}. The native builder and the image
     * build derive the executable name independently, so an unanchored match there produced a name
     * that did not exist on disk.
     *
     * @param jarFileName the file name of the built jar
     * @return the file name without its extension
     */
    private static String stripJarExtension(String jarFileName) {
        return jarFileName.replaceFirst("\\.jar$", "");
    }

    /**
     * Converts a package name into something Docker accepts as a repository name. Docker requires
     * lowercase alphanumerics with separators only between them, so a name that is valid in
     * Ballerina.toml but leads or trails with an underscore, such as {@code orders_}, is rejected
     * with "invalid reference format" in the same way an unsanitised version would be.
     *
     * @param packageName the package name
     * @return the package name as a valid Docker repository name
     */
    private String toImageRepository(String packageName) {
        String repository = packageName.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("^[._-]+", "")
                .replaceAll("[._-]+$", "");
        if (repository.isEmpty()) {
            repository = Constants.DEFAULT_IMAGE_REPOSITORY;
        }
        if (!repository.equals(packageName)) {
            OUT.println("\t@aws.lambda:Naming the image repository " + repository + ", as the package name " +
                    packageName + " is not a valid Docker repository name.");
        }
        return repository;
    }

    /**
     * Reports whether buildx is available, since {@code --provenance} and {@code --sbom} are
     * BuildKit-only flags and {@code docker build} aborts with "unknown flag" without it. The
     * classic builder attaches no attestations in the first place, so the flags are only needed
     * where they are also understood.
     *
     * @return whether the flags can be passed to docker build
     */
    private boolean isBuildxAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "buildx", "version")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(BUILDX_PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                terminate(process);
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException | RuntimeException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Converts a package version into something Docker accepts as a tag. A Ballerina version may
     * carry SemVer build metadata, and the {@code +} that introduces it is rejected by Docker with
     * "invalid reference format".
     *
     * <p>The {@code +} becomes an underscore rather than a hyphen, so that two versions cannot map
     * onto one tag: an underscore is valid in a Docker tag but never appears in a SemVer version,
     * whereas mapping onto a hyphen would make 1.0.0+linux and 1.0.0-linux the same image, and
     * pushing one would overwrite the other. Anything else outside a tag is replaced defensively.
     *
     * @param version the package version
     * @return the version as a valid Docker tag
     */
    private String toImageTag(String version) {
        String tag = version.replace('+', '_').replaceAll("[^a-zA-Z0-9._-]", "-");
        if (!tag.equals(version)) {
            OUT.println("\t@aws.lambda:Tagging the image " + tag + ", as the package version " + version +
                    " is not a valid Docker tag.");
        }
        return tag;
    }

    /**
     * Ends a docker process that is no longer wanted. {@code destroy} only asks it to stop, so the
     * process is given a moment to go and killed if it does not, otherwise the build carries on
     * after the interruption has been reported.
     *
     * @param process the process to end, which may be null if it never started
     */
    private static void terminate(Process process) {
        if (process == null) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly().waitFor(TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Builds the Dockerfile. Lambda passes the image config command as arguments to the entrypoint
     * rather than in _HANDLER, and the generated Ballerina main accepts no operands, so the
     * entrypoint moves the first argument into _HANDLER before starting the artifact. That keeps the
     * handler out of the image, so one image can serve every function in the package.
     *
     * @param artifactName the jar or native executable the image starts
     * @param isNative     whether the build produced a native executable
     * @return the contents of the Dockerfile
     */
    private String dockerFileContent(String artifactName, boolean isNative) {
        String taskRoot = Constants.LAMBDA_TASK_ROOT;
        String start = isNative
                ? taskRoot + "/" + artifactName
                : Constants.JVM_JAVA_PATH + " -jar " + taskRoot + "/" + artifactName;
        String baseImage = isNative ? Constants.NATIVE_BASE_IMAGE : Constants.JVM_BASE_IMAGE;
        return "FROM " + baseImage + "\n" +
                "COPY " + artifactName + " " + taskRoot + "/\n" +
                "ENTRYPOINT [\"/bin/sh\", \"-c\", \"export _HANDLER=$1; exec " + start + "\", \"sh\"]\n";
    }

    private void runDockerBuild(Path contextDir, String imageName) {
        List<String> command = new ArrayList<>(Arrays.asList("docker", "build", Constants.DOCKER_PLATFORM_FLAG,
                Constants.LAMBDA_REMOTE_COMPATIBLE_ARCHITECTURE));
        if (isBuildxAvailable()) {
            command.add(Constants.DOCKER_NO_PROVENANCE_FLAG);
            command.add(Constants.DOCKER_NO_SBOM_FLAG);
        }
        command.add("-t");
        command.add(imageName);
        command.add(".");
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(contextDir.toFile());
        pb.inheritIO();
        Process process = null;
        try {
            process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new DockerBuildException("Container image generation failed with exit code " + exitCode +
                        ". Refer to the above build log for information");
            }
        } catch (DockerBuildException e) {
            // DockerBuildException is a RuntimeException, so without this it would be caught below
            // and rethrown without the exit code.
            throw e;
        } catch (InterruptedException e) {
            // Docker outlives this thread otherwise, carrying on building an image nothing wants.
            terminate(process);
            Thread.currentThread().interrupt();
            throw new DockerBuildException("Container image generation was interrupted");
        } catch (IOException | RuntimeException e) {
            throw new DockerBuildException(
                    "Container image generation failed. Refer to the above build log for information");
        }
    }

    private void generateZipFile(Path functionsDir, Path binaryPath, boolean isNative) throws IOException {
        Path path = functionsDir.toAbsolutePath().resolve(Constants.LAMBDA_OUTPUT_ZIP_FILENAME);
        Files.deleteIfExists(path);
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:file:" + path.toUri().getPath());
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Path pathInZipfile = zipfs.getPath("/" + binaryPath.getFileName());
            if (isNative) {
                Set<PosixFilePermission> ownerWritable = PosixFilePermissions.fromString("rwxr-xr-x");
                Path bootstrapPath = functionsDir.resolve("bootstrap");
                Files.write(bootstrapPath, Constants.BOOTSTRAP_CONTENT.getBytes(StandardCharsets.UTF_8));
                Files.setPosixFilePermissions(bootstrapPath, ownerWritable);
                Path bootstrapZipPath = zipfs.getPath("/bootstrap");
                Files.copy(bootstrapPath, bootstrapZipPath, StandardCopyOption.COPY_ATTRIBUTES);
            }
            Files.copy(binaryPath, pathInZipfile, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    private void generateNativeZipFile(Path functionsDir, Path binaryPath) throws IOException {

        String jarFileName = binaryPath.getFileName().toString();
        Path jarPath = functionsDir.resolve(jarFileName);
        Files.copy(binaryPath, jarPath, StandardCopyOption.REPLACE_EXISTING);
        buildRemoteArtifacts(functionsDir, jarFileName);
        String executableName = stripJarExtension(jarFileName);
        generateZipFile(functionsDir, functionsDir.resolve(executableName), true);

    }

    public void buildRemoteArtifacts(Path jarPath, String jarFileName) {
        OUT.println("\t@aws.lambda:Building native image compatible for the Cloud using Docker. " +
                "This may take a while.\n");
        String executableName = stripJarExtension(jarFileName);
        String volumeMount = jarPath.toAbsolutePath() + Constants.CONTAINER_OUTPUT_PATH;
        ProcessBuilder pb = new ProcessBuilder("docker", "run", "--rm", Constants.DOCKER_PLATFORM_FLAG,
                Constants.LAMBDA_REMOTE_COMPATIBLE_ARCHITECTURE, "-v", volumeMount, Constants.NATIVE_BUILDER_IMAGE,
                jarFileName, executableName);

        pb.inheritIO();

        Process process = null;
        try {
            process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new DockerBuildException(
                        "Native executable generation for cloud using docker failed with exit code " + exitCode +
                                ". Refer to the above build log for information");
            }
        } catch (DockerBuildException e) {
            throw e;
        } catch (InterruptedException e) {
            // The native build is long running, so an interrupt left it going for minutes after the
            // compiler had already reported failure.
            terminate(process);
            Thread.currentThread().interrupt();
            throw new DockerBuildException("Native executable generation for cloud using docker was interrupted");
        } catch (IOException | RuntimeException e) {
            throw new DockerBuildException(
                    "Native executable generation for cloud using docker failed. Refer to the above build log for " +
                            "information");
        }
    }

    /**
     * Reads given resource file as a string.
     *
     * @param fileName path to the resource file
     * @return the file's contents
     * @throws IOException if read fails for any reason
     */
    private String getResourceFileAsString(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(fileName)) {
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}
