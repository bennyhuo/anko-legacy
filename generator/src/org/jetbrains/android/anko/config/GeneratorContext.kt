package org.jetbrains.android.anko.config

import org.jetbrains.android.anko.annotations.*
import org.jetbrains.android.anko.sources.AndroidHomeSourceProvider
import org.jetbrains.android.anko.sources.SourceManager
import java.io.File

class GeneratorContext(
        val annotationManager: AnnotationManager,
        val sourceManager: SourceManager,
        val logger: Logger,
        val configuration: AnkoConfiguration
) {
    companion object {
        fun create(propsDir: File, logLevel: Logger.LogLevel, config: AnkoConfiguration): GeneratorContext {
            val zipFileProvider = ZipFileAnnotationProvider(File(propsDir, "kotlin-android-sdk-annotations-1.0.0.jar"))
            val directoryProvider = DirectoryAnnotationProvider(File(propsDir, "annotations"))

            val annotationManager = AnnotationManager(CompoundAnnotationProvider(
                    CachingAnnotationProvider(zipFileProvider), CachingAnnotationProvider(directoryProvider)))
            val sourceManager = SourceManager(AndroidHomeSourceProvider(config[ANDROID_SDK_LOCATION], 27))
            val logger = Logger(logLevel)

            return GeneratorContext(annotationManager, sourceManager, logger, config)
        }
    }
}

interface WithGeneratorContext {
    val context: GeneratorContext

    val annotationManager: AnnotationManager
        get() = context.annotationManager

    val sourceManager: SourceManager
        get() = context.sourceManager

    val logger: Logger
        get() = context.logger

    val config: AnkoConfiguration
        get() = context.configuration
}
