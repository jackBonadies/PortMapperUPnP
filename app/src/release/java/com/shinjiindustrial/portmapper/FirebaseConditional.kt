package com.shinjiindustrial.portmapper

import android.content.Context
import com.google.firebase.FirebaseApp

class FirebaseConditional {

    companion object {
        fun Initialize(context : Context) {
            var app = FirebaseApp.initializeApp(context)
            if (app == null)
            {
                PortForwardApplication.crashlyticsEnabled = false;
            }
        }

    }
}