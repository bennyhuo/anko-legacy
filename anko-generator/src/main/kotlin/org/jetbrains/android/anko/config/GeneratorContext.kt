package org.jetbrains.android.anko.config

import org.jetbrains.android.anko.annotations.AnnotationManager
import org.jetbrains.android.anko.annotations.CachingAnnotationProvider
import org.jetbrains.android.anko.annotations.CompoundAnnotationProvider
import org.jetbrains.android.anko.annotations.DirectoryAnnotationProvider
import org.jetbrains.android.anko.annotations.ZipFileAnnotationProvider
import org.jetbrains.android.anko.sources.AndroidHomeSourceProvider
import org.jetbrains.android.anko.sources.EmptySourceProvider
import org.jetbrains.android.anko.sources.SourceManager
import java.io.File

class GeneratorContext(
    val annotationManager: AnnotationManager,
    val sourceManager: SourceManager,
    val logger: Logger,
) {
    companion object {
        fun create(propsDir: File, logLevel: Logger.LogLevel): GeneratorContext {
            val zipFileProvider = ZipFileAnnotationProvider(File(propsDir, "kotlin-android-sdk-annotations-1.0.0.jar"))
            val directoryProvider = DirectoryAnnotationProvider(File(propsDir, "annotations"))

            val annotationManager = AnnotationManager(
                CompoundAnnotationProvider(
                    CachingAnnotationProvider(zipFileProvider), CachingAnnotationProvider(directoryProvider)
                )
            )
            val sourceManager = SourceManager(EmptySourceProvider())
            val logger = Logger(logLevel)

            return GeneratorContext(annotationManager, sourceManager, logger)
        }
    }
}