package java.com.shinjiindustrial.portmapper

import android.content.Context
import com.shinjiindustrial.portmapper.domain.AndroidUpnpServiceConfigurationImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.fourthline.cling.UpnpService
import org.fourthline.cling.UpnpServiceImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClingModule {
//    @Singleton
//    @Provides
//    fun providesUpnpService(@ApplicationContext context: Context): UpnpService {
//        return UpnpServiceImpl(AndroidUpnpServiceConfigurationImpl(context))
//    }
}
