package org.jetbrains.jet.compiler.runner

import java.io.File

public trait KotlinModuleDescriptionBuilderFactory {
    fun create() : KotlinModuleDescriptionBuilder
    fun getFileExtension() : String
}

public trait KotlinModuleDescriptionBuilder {
    fun addModule(
            moduleName: String,
            outputDir: String,
            dependencyProvider: DependencyProvider,
            sourceFiles: List<File>,
            tests: Boolean,
            directoriesToFilterOut: Set<File>): KotlinModuleDescriptionBuilder

    fun asText(): CharSequence

    trait DependencyProvider {
        fun processClassPath(processor: DependencyProcessor)
    }

    trait DependencyProcessor {
        fun processClassPathSection(sectionDescription: String, files: Collection<File>)
        fun processAnnotationRoots(files: List<File>)
    }
}
