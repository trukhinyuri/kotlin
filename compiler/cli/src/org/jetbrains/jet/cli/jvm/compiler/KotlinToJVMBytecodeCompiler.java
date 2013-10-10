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

package org.jetbrains.jet.cli.jvm.compiler;

import com.google.common.base.Predicates;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFile;
import jet.Function0;
import jet.modules.AllModules;
import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.CLIConfigurationKeys;
import org.jetbrains.jet.cli.common.CompilerPlugin;
import org.jetbrains.jet.cli.common.CompilerPluginContext;
import org.jetbrains.jet.cli.common.messages.*;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.Progress;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.parsing.JetScriptDefinition;
import org.jetbrains.jet.lang.parsing.JetScriptDefinitionProvider;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.ScriptNameUtil;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.JetMainDetector;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.utils.KotlinPaths;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class KotlinToJVMBytecodeCompiler {

    private static final boolean COMPILE_CHUNK_AS_ONE_MODULE = true;

    private KotlinToJVMBytecodeCompiler() {
    }

    @Nullable
    public static ClassFileFactory compileModule(CompilerConfiguration configuration, Module module, File directory) {
        List<String> sourceFiles = module.getSourceFiles();
        if (sourceFiles.isEmpty()) {
            throw new CompileEnvironmentException("No source files where defined in module " + module.getModuleName());
        }

        CompilerConfiguration compilerConfiguration = configuration.copy();
        for (String sourceFile : sourceFiles) {
            File source = new File(sourceFile);
            if (!source.isAbsolute()) {
                source = new File(directory, sourceFile);
            }

            if (!source.exists()) {
                throw new CompileEnvironmentException("'" + source + "' does not exist in module " + module.getModuleName());
            }

            compilerConfiguration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, source.getPath());
        }

        for (String classpathRoot : module.getClasspathRoots()) {
            compilerConfiguration.add(JVMConfigurationKeys.CLASSPATH_KEY, new File(classpathRoot));
        }

        for (String annotationsRoot : module.getAnnotationsRoots()) {
            compilerConfiguration.add(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, new File(annotationsRoot));
        }

        Disposable parentDisposable = CompileEnvironmentUtil.createMockDisposable();
        JetCoreEnvironment moduleEnvironment = null;
        try {
            moduleEnvironment = new JetCoreEnvironment(parentDisposable, compilerConfiguration);


            GenerationState generationState = analyzeAndGenerate(moduleEnvironment);
            if (generationState == null) {
                return null;
            }
            return generationState.getFactory();
        } finally {
            if (moduleEnvironment != null) {
                Disposer.dispose(parentDisposable);
            }
        }
    }

    private static void writeOutput(
            CompilerConfiguration configuration,
            ClassFileFactory moduleFactory,
            CompileEnvironmentUtil.OutputDirector outputDir,
            File jarPath,
            boolean jarRuntime,
            FqName mainClass
    ) {
        MessageCollector messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE);
        CompileEnvironmentUtil.writeOutputToDirOrJar(jarPath, outputDir, jarRuntime, mainClass, moduleFactory, messageCollector);
    }

    public static boolean compileModules(
            CompilerConfiguration configuration,
            @NotNull final ModuleChunk chunk,
            @NotNull File directory,
            @Nullable File jarPath,
            boolean jarRuntime
    ) {
        List<Module> modules = chunk.getModules();
        if (COMPILE_CHUNK_AS_ONE_MODULE && modules.size() > 1) {
            modules = Collections.<Module>singletonList(new ChunkAsOneModule(chunk));
        }
        for (Module module : modules) {
            ClassFileFactory moduleFactory = compileModule(configuration, module, directory);
            if (moduleFactory == null) {
                return false;
            }
            CompileEnvironmentUtil.OutputDirector outputDir = new CompileEnvironmentUtil.OutputDirector() {
                @NotNull
                @Override
                public File getOutputDirectory(@NotNull Collection<File> sourceFiles) {
                    for (File sourceFile : sourceFiles) {
                        // Note that here we track original modules:
                        Module module = chunk.findModuleBySourceFile(sourceFile);
                        if (module != null) {
                            return new File(module.getOutputDirectory());
                        }
                    }
                    throw new IllegalStateException("No module found for source files: " + sourceFiles);
                }
            };

            writeOutput(configuration, moduleFactory, outputDir, jarPath, jarRuntime, null);
        }
        return true;
    }

    @Nullable
    private static FqName findMainClass(@NotNull List<JetFile> files) {
        FqName mainClass = null;
        for (JetFile file : files) {
            if (JetMainDetector.hasMain(file.getDeclarations())) {
                if (mainClass != null) {
                    // more than one main
                    return null;
                }
                FqName fqName = JetPsiUtil.getFQName(file);
                mainClass = PackageClassUtils.getPackageClassFqName(fqName);
            }
        }
        return mainClass;
    }

    public static boolean compileBunchOfSources(
            JetCoreEnvironment environment,
            @Nullable File jar,
            @Nullable File outputDir,
            boolean includeRuntime
    ) {

        FqName mainClass = findMainClass(environment.getSourceFiles());

        GenerationState generationState = analyzeAndGenerate(environment);
        if (generationState == null) {
            return false;
        }

        try {
            CompileEnvironmentUtil.OutputDirector outputDirector = CompileEnvironmentUtil.singleDirectory(outputDir);
            writeOutput(environment.getConfiguration(), generationState.getFactory(), outputDirector, jar, includeRuntime, mainClass);
            return true;
        }
        finally {
            generationState.destroy();
        }
    }

    public static boolean compileAndExecuteScript(
            @NotNull KotlinPaths paths,
            @NotNull JetCoreEnvironment environment,
            @NotNull List<String> scriptArgs) {
        Class<?> scriptClass = compileScript(paths, environment, null);
        if(scriptClass == null)
            return false;

        try {
            scriptClass.getConstructor(String[].class).newInstance(new Object[]{scriptArgs.toArray(new String[0])});
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to evaluate script: " + e, e);
        }
        return true;
    }

    private static Class<?> compileScript(
            @NotNull KotlinPaths paths, @NotNull JetCoreEnvironment environment, @Nullable ClassLoader parentLoader) {

        GenerationState generationState = analyzeAndGenerate(environment);
        if (generationState == null) {
            return null;
        }

        GeneratedClassLoader classLoader = null;
        try {
            ClassFileFactory factory = generationState.getFactory();
            classLoader = new GeneratedClassLoader(factory,
                    new URLClassLoader(new URL[] {
                        // TODO: add all classpath
                        paths.getRuntimePath().toURI().toURL()
                    },
                    parentLoader == null ? AllModules.class.getClassLoader() : parentLoader));

            JetFile scriptFile = environment.getSourceFiles().get(0);
            return classLoader.loadClass(ScriptNameUtil.classNameForScript(scriptFile));
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to evaluate script: " + e, e);
        }
        finally {
            if (classLoader != null) {
                classLoader.dispose();
            }
            generationState.destroy();
        }
    }

    @Nullable
    public static GenerationState analyzeAndGenerate(
            JetCoreEnvironment environment
    ) {
        AnalyzeExhaust exhaust = analyze(environment);

        if (exhaust == null) {
            return null;
        }

        exhaust.throwIfError();

        return generate(environment, exhaust);
    }

    @Nullable
    private static AnalyzeExhaust analyze(final JetCoreEnvironment environment) {
        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(
                environment.getConfiguration().get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY));
        analyzerWithCompilerReport.analyzeAndReport(
                new Function0<AnalyzeExhaust>() {
                    @NotNull
                    @Override
                    public AnalyzeExhaust invoke() {
                        BindingTrace sharedTrace = CliLightClassGenerationSupport.getInstanceForCli(environment.getProject()).getTrace();
                        return AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                                environment.getProject(),
                                environment.getSourceFiles(),
                                sharedTrace,
                                environment.getConfiguration().getList(JVMConfigurationKeys.SCRIPT_PARAMETERS),
                                Predicates.<PsiFile>alwaysTrue(),
                                false
                        );
                    }
                }, environment.getSourceFiles()
        );

        return analyzerWithCompilerReport.hasErrors() ? null : analyzerWithCompilerReport.getAnalyzeExhaust();
    }

    @NotNull
    private static GenerationState generate(
            JetCoreEnvironment environment,
            AnalyzeExhaust exhaust
    ) {
        Project project = environment.getProject();
        CompilerConfiguration configuration = environment.getConfiguration();
        GenerationState generationState = new GenerationState(
                project, ClassBuilderFactories.BINARIES, Progress.DEAF, exhaust.getBindingContext(), environment.getSourceFiles(),
                configuration.get(JVMConfigurationKeys.GENERATE_NOT_NULL_ASSERTIONS, false),
                configuration.get(JVMConfigurationKeys.GENERATE_NOT_NULL_PARAMETER_ASSERTIONS, false),
                /*generateDeclaredClasses = */true
        );
        KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION);

        CompilerPluginContext context = new CompilerPluginContext(project, exhaust.getBindingContext(), environment.getSourceFiles());
        for (CompilerPlugin plugin : configuration.getList(CLIConfigurationKeys.COMPILER_PLUGINS)) {
            plugin.processFiles(context);
        }
        return generationState;
    }

    public static Class compileScript(
            @NotNull ClassLoader parentLoader,
            @NotNull KotlinPaths paths,
            @NotNull String scriptPath,
            @Nullable List<AnalyzerScriptParameter> scriptParameters,
            @Nullable List<JetScriptDefinition> scriptDefinitions) {
        MessageRenderer messageRenderer = MessageRenderer.PLAIN;
        GroupingMessageCollector messageCollector = new GroupingMessageCollector(new PrintingMessageCollector(System.err, messageRenderer, false));
        Disposable rootDisposable = CompileEnvironmentUtil.createMockDisposable();
        try {
            CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
            compilerConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector);
            compilerConfiguration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, getClasspath(parentLoader));
            compilerConfiguration.add(JVMConfigurationKeys.CLASSPATH_KEY, PathUtil.findRtJar());
            compilerConfiguration.addAll(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, Collections.singletonList(
                    paths.getJdkAnnotationsPath()));
            compilerConfiguration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, scriptPath);
            compilerConfiguration.addAll(CommonConfigurationKeys.SCRIPT_DEFINITIONS_KEY,
                                         scriptDefinitions != null ? scriptDefinitions : Collections.<JetScriptDefinition>emptyList());
            compilerConfiguration.put(JVMConfigurationKeys.SCRIPT_PARAMETERS, scriptParameters);

            JetCoreEnvironment environment = new JetCoreEnvironment(rootDisposable, compilerConfiguration);

            try {
                JetScriptDefinitionProvider.getInstance(environment.getProject()).markFileAsScript(environment.getSourceFiles().get(0));
                return compileScript(paths, environment, parentLoader);
            }
            catch (CompilationException e) {
                messageCollector.report(CompilerMessageSeverity.EXCEPTION, MessageRenderer.PLAIN.renderException(e),
                                        MessageUtil.psiElementToMessageLocation(e.getElement()));
                return null;
            }
            catch (Throwable t) {
                MessageCollectorUtil.reportException(messageCollector, t);
                return null;
            }

        }
        finally {
            messageCollector.flush();
            Disposer.dispose(rootDisposable);
        }
    }

    private static Collection<File> getClasspath(ClassLoader loader) {
        return getClasspath(loader, new LinkedList<File>());
    }

    private static Collection<File> getClasspath(ClassLoader loader, LinkedList<File> files) {
        ClassLoader parent = loader.getParent();
        if(parent != null)
            getClasspath(parent, files);

        if(loader instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) loader).getURLs()) {
                String urlFile = url.getFile();

                if (urlFile.contains("%")) {
                    try {
                        urlFile = url.toURI().getPath();
                    }
                    catch (URISyntaxException e) {
                        throw ExceptionUtils.rethrow(e);
                    }
                }

                File file = new File(urlFile);
                if(file.exists() && (file.isDirectory() || file.getName().endsWith(".jar"))) {
                    files.add(file);
                }
            }
        }
        return files;
    }
}
