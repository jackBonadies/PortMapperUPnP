package com.shinjiindustrial.portmapper

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.util.logging.Level
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(@ApplicationContext context: Context): FirebaseCrashlytics =
        initializeAndGetFirebaseInstances(context)

    private fun initializeAndGetFirebaseInstances(context: Context) : FirebaseCrashlytics
    {
        FirebaseApp.initializeApp(context)
        return FirebaseCrashlytics.getInstance()
    }

    @Provides
    @IntoSet
    @Singleton
    fun providesCrashlyticsSink(crashlytics: FirebaseCrashlytics): ILogSink = CrashlyticsSink(crashlytics)

    class CrashlyticsSink(
        private val crashlytics: FirebaseCrashlytics,
    ) : ILogSink() {
        override fun isFirebaseLogger() : Boolean {
            return true
        }

        override fun log(level: Level, msg: String, t: Throwable?, opts: LogOptions) {
            var firebaseLevel = opts.firebase
            if (firebaseLevel == FirebaseRoute.SILENT) return
            if (level == Level.SEVERE && firebaseLevel == FirebaseRoute.NO_OPINION)
            {
                firebaseLevel = FirebaseRoute.NON_FATAL
            }

            when (firebaseLevel) {
                FirebaseRoute.BREADCRUMB -> {
                    // Breadcrumb-style line (Crashlytics keeps a rolling buffer)
                    crashlytics.log("${level.name}: $msg")
                }
                FirebaseRoute.NON_FATAL -> {
                    // Non-fatal crash report
                    if (t != null)
                    {
                        crashlytics.log("${level.name}: $msg")
                        crashlytics.recordException(t)
                    }
                    else
                    {
                        crashlytics.recordException(RuntimeException("NonFatal: $msg"))
                    }
                }
                else -> Unit
            }
        }
    }
}