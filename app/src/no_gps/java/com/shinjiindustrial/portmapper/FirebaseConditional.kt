package com.shinjiindustrial.portmapper

import android.content.Context

class FirebaseConditional {

    companion object {
        fun Initialize(context : Context) {
                PortForwardApplication.crashlyticsEnabled = false;
        }
    }
}