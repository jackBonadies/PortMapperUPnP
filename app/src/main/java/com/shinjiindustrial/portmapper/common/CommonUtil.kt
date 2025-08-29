package com.shinjiindustrial.portmapper.common

import com.shinjiindustrial.portmapper.toIntOrMaxValue


// v1 - max is ui4 maxvalue (which is 100+ years, so just cap at signed int4 max)
// v2 - max is 1 week (604800)
fun capLeaseDur(leaseDurString: String, v1: Boolean): String {
    return if (v1) {
        leaseDurString.toIntOrMaxValue().toString()
    } else {
        val leaseInt = leaseDurString.toIntOrMaxValue()
        minOf(leaseInt, 604800).toString()
    }
}

class Event<T> {
    private val observers = mutableSetOf<(T) -> Unit>()

    operator fun plusAssign(observer: (T) -> Unit) {
        observers.add(observer)
    }

    operator fun minusAssign(observer: (T) -> Unit) {
        observers.remove(observer)
    }

    operator fun invoke(value: T) {
        for (observer in observers)
            observer(value)
    }
}


enum class NetworkType(val networkTypeString: String) {
    NONE("None"),
    WIFI("Wifi"),
    DATA("Data");

    override fun toString(): String {
        return networkTypeString
    }
}