/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.jps.build;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.KotlinVersion;
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.compiler.runner.*;
import org.jetbrains.jet.jps.JpsKotlinCompilerSettings;
import org.jetbrains.jet.utils.PathUtil;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.*;
import org.jetbrains.jps.incremental.java.JavaBuilder;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.module.JpsModule;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity.*;

public class KotlinBuilder extends ModuleLevelBuilder {

    private static final String KOTLIN_BUILDER_NAME = "Kotlin Builder";
    private static final List<String> COMPILABLE_FILE_EXTENSIONS = Collections.singletonList("kt");

    private static final Function<JpsModule,String> MODULE_NAME = new Function<JpsModule, String>() {
        @Override
        public String fun(JpsModule module) {
            return module.getName();
        }
    };

    protected KotlinBuilder() {
        super(BuilderCategory.SOURCE_PROCESSOR);
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return KOTLIN_BUILDER_NAME;
    }

    @Override
    public ExitCode build(
            CompileContext context,
            ModuleChunk chunk,
            DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder,
            OutputConsumer outputConsumer
    ) throws ProjectBuildException, IOException {
        MessageCollector messageCollector = new MessageCollectorAdapter(context);
        // Workaround for Android Studio
        if (!isJavaPluginEnabled(context)) {
            messageCollector.report(INFO, "Kotlin JPS plugin is disabled", CompilerMessageLocation.NO_LOCATION);
            return ExitCode.NOTHING_DONE;
        }

        messageCollector.report(INFO, "Kotlin JPS plugin version " + KotlinVersion.VERSION, CompilerMessageLocation.NO_LOCATION);

        if (chunk.getModules().size() > 1) {
            // We do not support circular dependencies, but if they are present, we should not break the build,
            // so we simply yield a warning and report NOTHING_DONE
            messageCollector.report(
                    WARNING, "Circular dependencies are not supported. " +
                             "The following modules depend on each other: " + StringUtil.join(chunk.getModules(), MODULE_NAME, ", ") + ". " +
                             "Kotlin is not compiled for these modules",
                    CompilerMessageLocation.NO_LOCATION);
            return ExitCode.NOTHING_DONE;
        }

        ModuleBuildTarget representativeTarget = chunk.representativeTarget();

        // For non-incremental build: take all sources
        if (!dirtyFilesHolder.hasDirtyFiles()) {
            return ExitCode.NOTHING_DONE;
        }
        List<File> sourceFiles = KotlinSourceFileCollector.getAllKotlinSourceFiles(representativeTarget);
        //List<File> sourceFiles = KotlinSourceFileCollector.getDirtySourceFiles(dirtyFilesHolder);

        if (sourceFiles.isEmpty()) {
            return ExitCode.NOTHING_DONE;
        }

        File outputDir = representativeTarget.getOutputDir();

        CompilerEnvironment environment = CompilerEnvironment.getEnvironmentFor(PathUtil.getKotlinPathsForJpsPluginOrJpsTests(), outputDir);
        if (!environment.success()) {
            environment.reportErrorsTo(messageCollector);
            return ExitCode.ABORT;
        }

        assert outputDir != null : "CompilerEnvironment must have checked for outputDir to be not null, but it didn't";

        OutputItemsCollectorImpl outputItemCollector = new OutputItemsCollectorImpl(outputDir);

        JpsProject project = representativeTarget.getModule().getProject();

        if (JpsUtils.isJsKotlinModule(representativeTarget)) {
            K2JSCompilerArguments settings = JpsKotlinCompilerSettings.getMergedK2JsSettings(project);
            setupK2JsSettings(representativeTarget, sourceFiles, outputDir, settings);

            KotlinCompilerRunner.runK2JsCompiler(settings, messageCollector, environment, outputItemCollector);
        }
        else {
            File moduleFile = KotlinBuilderModuleScriptGenerator.generateModuleDescription(context, representativeTarget, sourceFiles);
            K2JVMCompilerArguments settings = JpsKotlinCompilerSettings.getMergedK2JvmSettings(project);
            setupK2JvmSettings(outputDir, moduleFile, settings);

            KotlinCompilerRunner.runK2JvmCompiler(settings, messageCollector, environment, outputItemCollector);
        }

        for (SimpleOutputItem outputItem : outputItemCollector.getOutputs()) {
            outputConsumer.registerOutputFile(
                    representativeTarget,
                    outputItem.getOutputFile(),
                    paths(outputItem.getSourceFiles()));
        }

        return ExitCode.OK;
    }

    private static boolean isJavaPluginEnabled(@NotNull CompileContext context) {
        try {
            // Using reflection for backward compatibility with IDEA 12
            Field javaPluginIsEnabledField = JavaBuilder.class.getDeclaredField("IS_ENABLED");
            return Modifier.isPublic(javaPluginIsEnabledField.getModifiers()) ? JavaBuilder.IS_ENABLED.get(context, Boolean.TRUE) : true;
        }
        catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Cannot check if Java Jps Plugin is enabled", e);
        }
    }

    private static Collection<String> paths(Collection<File> files) {
        Collection<String> result = ContainerUtil.newArrayList();
        for (File file : files) {
            result.add(file.getPath());
        }
        return result;
    }

    public static class MessageCollectorAdapter implements MessageCollector {

        private final CompileContext context;

        public MessageCollectorAdapter(@NotNull CompileContext context) {
            this.context = context;
        }

        @Override
        public void report(
                @NotNull CompilerMessageSeverity severity,
                @NotNull String message,
                @NotNull CompilerMessageLocation location
        ) {
            String prefix = "";
            if (severity == EXCEPTION) {
                prefix = CompilerRunnerConstants.INTERNAL_ERROR_PREFIX;
            }
            context.processMessage(new CompilerMessage(
                    CompilerRunnerConstants.KOTLIN_COMPILER_NAME,
                    kind(severity),
                    prefix + message,
                    location.getPath(),
                    -1, -1, -1,
                    location.getLine(),
                    location.getColumn()
            ));
        }

        @NotNull
        private static BuildMessage.Kind kind(@NotNull CompilerMessageSeverity severity) {
            switch (severity) {
                case INFO:
                    return BuildMessage.Kind.INFO;
                case ERROR:
                case EXCEPTION:
                    return BuildMessage.Kind.ERROR;
                case WARNING:
                    return BuildMessage.Kind.WARNING;
                case LOGGING:
                    return BuildMessage.Kind.PROGRESS;
                default:
                    throw new IllegalArgumentException("Unsupported severity: " + severity);
            }
        }

    }

    @Override
    public List<String> getCompilableFileExtensions() {
        return COMPILABLE_FILE_EXTENSIONS;
    }

    private static void setupCommonSettings(CommonCompilerArguments settings) {
        settings.tags = true;
        settings.verbose = true;
        settings.version = true;
        settings.printArgs = true;
    }

    private static void setupK2JvmSettings(
            File outputDir,
            File moduleFile,
            K2JVMCompilerArguments settings
    ) {
        setupCommonSettings(settings);

        settings.module = moduleFile.getAbsolutePath();
        settings.outputDir = outputDir.getPath();
        settings.notNullAssertions = true;
        settings.notNullParamAssertions = true;
        settings.noStdlib = true;
        settings.noJdkAnnotations = true;
        settings.noJdk = true;
    }

    private static void setupK2JsSettings(
            ModuleBuildTarget representativeTarget,
            List<File> sourceFiles,
            File outputDir,
            K2JSCompilerArguments settings
    ) {
        setupCommonSettings(settings);

        List<String> sourceFilePaths = ContainerUtil.map(sourceFiles, new Function<File, String>() {
            @Override
            public String fun(File file) {
                return file.getPath();
            }
        });
        settings.sourceFiles = ArrayUtil.toStringArray(sourceFilePaths);

        File outputFile = new File(outputDir, representativeTarget.getModule().getName() + ".js");
        settings.outputFile = outputFile.getPath();

        Set<String> dependencies = JpsJsModuleUtils.getLibraryFilesAndDependencies(representativeTarget);
        settings.libraryFiles = ArrayUtil.toStringArray(dependencies);
    }
}
