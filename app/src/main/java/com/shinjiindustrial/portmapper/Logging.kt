package com.shinjiindustrial.portmapper

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import com.example.myapplication.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
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

data class LogEntry(
    val seq: Long,
    val level: Level,
    val message: String,
    val stackTrace: String?,
    val wallClockMs: Long,
    val threadName: String,
)

object LogFormatter {

    private val viewTimeFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    }

    private val exportTimeFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US)
    }

    fun levelPrefix(level: Level): String {
        return when (level) {
            Level.FINE -> "D"
            Level.INFO -> "I"
            Level.WARNING -> "W"
            Level.SEVERE -> "E"
            else -> level.name.take(1)
        }
    }

    fun viewTimestamp(entry: LogEntry): String {
        return viewTimeFormat.get()!!.format(Date(entry.wallClockMs))
    }

    fun forView(entry: LogEntry): String {
        val head = "${levelPrefix(entry.level)}: ${entry.message}"
        return if (entry.stackTrace == null) head else "$head\n${entry.stackTrace}"
    }

    fun forExport(entry: LogEntry): String {
        val timestamp = exportTimeFormat.get()!!.format(Date(entry.wallClockMs))
        val head = "$timestamp ${levelPrefix(entry.level)} [${entry.threadName}] ${entry.message}"
        return if (entry.stackTrace == null) head else "$head\n${entry.stackTrace}"
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
    private val seq = AtomicLong(0)

    private val lock = Any()
    private val pending = ArrayDeque<LogEntry>()
    private var drainScheduled = false

    override fun log(level: Level, msg: String, t: Throwable?, opts: LogOptions) {
        val entry = LogEntry(
            seq = seq.getAndIncrement(),
            level = level,
            message = msg,
            stackTrace = t?.stackTraceToString(),
            wallClockMs = System.currentTimeMillis(),
            threadName = Thread.currentThread().name,
        )

        // One main-thread hop per burst rather than one per line
        synchronized(lock) {
            pending.addLast(entry)
            if (drainScheduled) {
                return
            }
            drainScheduled = true
        }
        main.post { drain() }
    }

    private fun drain() {
        val batch = synchronized(lock) {
            drainScheduled = false
            val copy = ArrayList(pending)
            pending.clear()
            copy
        }
        logStoreRepository.addAll(batch)
    }
}

@Singleton
class LogStoreRepository @Inject constructor(
) {
    private val _entries = mutableStateListOf<LogEntry>()

    // Read-only view of the live snapshot list, so Compose reads still observe changes.
    val entries: List<LogEntry> get() = _entries

    fun addAll(newEntries: List<LogEntry>) {
        _entries.addAll(newEntries)
        if (_entries.size > MAX_ENTRIES) {
            _entries.removeRange(0, _entries.size - TRIM_TO)
        }
    }

    fun clear() {
        _entries.clear()
    }

    fun getLogsAsText(): String
    {
        return _entries.toList().joinToString("\n") { LogFormatter.forExport(it) }
    }

    companion object {
        // these values are different bc a trim is O(n) and that way we only trim every 500 at most
        const val MAX_ENTRIES = 10000
        const val TRIM_TO = 9500
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
