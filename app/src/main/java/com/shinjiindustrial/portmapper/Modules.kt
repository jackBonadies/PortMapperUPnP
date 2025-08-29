package java.com.shinjiindustrial.portmapper

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ClingModule {
//    @Singleton
//    @Provides
//    fun providesUpnpService(@ApplicationContext context: Context): UpnpService {
//        return UpnpServiceImpl(AndroidUpnpServiceConfigurationImpl(context))
//    }
}
