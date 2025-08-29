package com.shinjiindustrial.portmapper.client

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UpnpClientModule {
    @Binds
    @Singleton
    abstract fun bindUpnpClient(impl: UpnpClient): IUpnpClient
}