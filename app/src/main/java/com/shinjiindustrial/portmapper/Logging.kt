package com.shinjiindustrial.portmapper

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.myapplication.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

enum class FirebaseRoute
{
    // all loggers should log as normally, firebase should log SEVERE as exception
    NO_OPINION,
    // all loggers should log as normally, but firebase should not log
    SILENT,
    // all loggers should log as normally, but firebase should log breadcrumb
    BREADCRUMB,
    // all loggers should log as normally, firebase should log as exception
    NON_FATAL
}

data class LogOptions(
    val firebase: FirebaseRoute = FirebaseRoute.NO_OPINION,
)

interface ILogger {
    fun log(
        level: Level,
        msg: String,
        t: Throwable? = null,
        opts: LogOptions = LogOptions()
    )
    fun logBreadcrumb(obj : Any)
}

abstract class ILogSink {
    abstract fun log(level: Level, msg: String, t: Throwable?, opts: LogOptions)
    open fun isFirebaseLogger() : Boolean {
        return false
    }
}

class LogcatSink : ILogSink() {
    val logger: Logger = Logger.getLogger("PortMapper")
    override fun log(level: Level, msg: String, t: Throwable?, opts: LogOptions) {
        logger.log(level, msg)
    }
}

class LogStringBuilderSink(
    private val logStoreRepository: LogStoreRepository,
) : ILogSink() {
    private val main = Handler(Looper.getMainLooper())
    override fun log(level: Level, msg: String, t: Throwable?, opts: LogOptions) {
        main.post {
            val prefix = when (level) {
                Level.FINE -> "D: "
                Level.INFO -> "I: "
                Level.WARNING -> "W: "
                Level.SEVERE -> "E: "
                else -> return@post // i.e. do not log
            }
            logStoreRepository.logs.add(prefix + msg)
        }
    }
}

@Singleton
class LogStoreRepository @Inject constructor(
) {
    var logs: SnapshotStateList<String> = mutableStateListOf<String>()

    fun getLogsAsText(): String
    {
        return logs.joinToString("\n")
    }
}

class CompositeLogger(
    private val sinks: Set<ILogSink>
) : ILogger {
    override fun log(level: Level, msg: String, t: Throwable?, opts: LogOptions) {
        if (level.intValue() < threshold.intValue()) {
            return
        }
        sinks.forEach { it.log(level, msg, t, opts) }
    }

    override fun logBreadcrumb(obj: Any) {
        sinks.filter { it.isFirebaseLogger() }.forEach { it.log(Level.INFO, obj.toString(), null, LogOptions(FirebaseRoute.BREADCRUMB)) }
    }

    val threshold: Level = if (BuildConfig.DEBUG) Level.ALL else Level.INFO
}


@Module
@InstallIn(SingletonComponent::class)
object BaseLoggingModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideLogcatLogger() : ILogSink = LogcatSink()

    @Provides
    @IntoSet
    @Singleton
    fun provideInMemoryStringBuilderLogger(store : LogStoreRepository) : ILogSink = LogStringBuilderSink(store)

    @Provides @Singleton
    fun provideLogger(
        sinks: Set<@JvmSuppressWildcards ILogSink>
    ): ILogger = CompositeLogger(sinks)
}
