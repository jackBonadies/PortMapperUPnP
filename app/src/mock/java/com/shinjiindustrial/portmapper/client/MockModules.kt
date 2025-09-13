package com.shinjiindustrial.portmapper.client

import com.shinjiindustrial.portmapper.client.MockUpnpClientConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UpnpClientModule {
    @Provides
    @Singleton
    fun provideMockUpnpClient(): IUpnpClient {
        return MockUpnpClient(MockUpnpClientConfig(Speed.Medium, RuleSet.Full))
    }
}