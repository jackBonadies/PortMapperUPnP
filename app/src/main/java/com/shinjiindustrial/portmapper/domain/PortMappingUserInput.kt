package com.shinjiindustrial.portmapper.domain

import com.shinjiindustrial.portmapper.PortMappingRequest
import com.shinjiindustrial.portmapper.Protocol
import com.shinjiindustrial.portmapper.common.toIntOrMaxValue

// this is the information the user gives us to create a rule. i.e. what the router needs + any preference info (autorenew)
data class PortMappingUserInput(
    val description: String,
    val internalIp: String,
    val internalRange: String,
    val externalIp: String,
    val externalRange: String,
    val protocol: String,
    val leaseDuration: String,
    val enabled: Boolean,
    val autoRenew: Boolean
) {
    fun requestWith(
        internalPortSpecified: String,
        externalPortSpecified: String,
        portocolSpecified: String
    ): PortMappingRequest {
        return PortMappingRequest(
            description,
            internalIp,
            internalPortSpecified,
            externalIp,
            externalPortSpecified,
            portocolSpecified,
            leaseDuration,
            enabled,
            ""
        )
    }

    fun validateRange(): String {
        // many 1 to 1 makes sense
        // many different external to 1 internal
        // 1 external to many internal - this isnt a thing and it causes a contradiction with upnp port retrieval api

        val (inStart, inEnd) = getRange(true)
        val (outStart, outEnd) = getRange(false)

        val inSize = inEnd - inStart + 1 // inclusive
        val outSize = outEnd - outStart + 1
        val manyToOne = (inSize == 1) // many out to 1 in
        if (inSize != outSize && !manyToOne) {
            return "Internal and External Ranges do not match up."
        } else {
            return ""
        }
    }

    fun getRange(internal: Boolean): Pair<Int, Int> {
        val rangeInQuestion = if (internal) internalRange else externalRange
        if (rangeInQuestion.contains('-')) {
            val inRange = rangeInQuestion.split('-')
            return Pair(inRange[0].toInt(), inRange[1].toInt())
        } else {
            return Pair(rangeInQuestion.toInt(), rangeInQuestion.toInt())
        }
    }

    fun getProtocols(): List<String> {
        return when (protocol) {
            Protocol.BOTH.str() -> listOf("TCP", "UDP")
            else -> listOf(protocol)
        }
    }

    fun splitIntoRules(): MutableList<PortMappingRequest> {
        val portMappingRequests: MutableList<PortMappingRequest> = mutableListOf()
        val (inStart, inEnd) = this.getRange(true)
        val (outStart, outEnd) = this.getRange(false)
        val protocols = this.getProtocols()

        val errorString = this.validateRange()
        if (errorString.isNotEmpty()) {
            throw java.lang.Exception(errorString)
        }

        val inSize = inEnd - inStart + 1 // inclusive
        val outSize = outEnd - outStart + 1
        val sizeOfRange = maxOf(inSize, outSize) - 1 // if just 1 element then size is 0

        for (i in 0..sizeOfRange) {
            val inPort = if (inSize == 1) inStart else inStart + i
            val outPort = if (outSize == 1) outStart else outStart + i
            for (protocol in protocols) {
                portMappingRequests.add(
                    this.requestWith(
                        inPort.toString(),
                        outPort.toString(),
                        protocol
                    )
                )
            }
        }
        return portMappingRequests
    }

    fun validateAutoRenew(): String {
        if (this.autoRenew && this.leaseDuration.toIntOrMaxValue() != 0 && this.leaseDuration.toIntOrMaxValue() < 90) {
            return "If auto renew is enabled, lease duration must at least be 90 seconds"
        }
        return ""
    }

}
