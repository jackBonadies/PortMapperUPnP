package com.shinjiindustrial.portmapper.common

import com.shinjiindustrial.portmapper.toIntOrMaxValue

data class ValidationResult(
    val hasError: Boolean,
    val errorMessage: String = ""
)

fun validateDescription(description : String) : ValidationResult
{
    if(description.isEmpty())
    {
        return ValidationResult(true, "Cannot be empty")
    }
    return ValidationResult(false, "")
}

fun validateStartPort(startPort : String) : ValidationResult
{
    if(startPort.isEmpty())
    {
        return ValidationResult(true, "Port cannot be empty")
    }
    val portInt = startPort.toIntOrMaxValue()
    if(portInt < MIN_PORT || portInt > MAX_PORT)
    {
        return ValidationResult(true, "Port must be between $MIN_PORT and $MAX_PORT")
    }
    return ValidationResult(false, "")
}

fun validateEndPort(startPort : String, endPort : String) : ValidationResult
{
    if(startPort.isEmpty())
    {
        // let them deal with that error first
        return ValidationResult(false, "")
    }
    if(endPort.isEmpty())
    {
        //valid
        return ValidationResult(false, "")
    }

    val startPortInt = startPort.toIntOrMaxValue()
    val endPortInt = endPort.toIntOrMaxValue()

    if(endPortInt < MIN_PORT || endPortInt > MAX_PORT)
    {
        return ValidationResult(true, "Port must be between $MIN_PORT and $MAX_PORT")
    }

    if(startPortInt > endPortInt)
    {
        return ValidationResult(true, "End port must be after start")
    }

    return ValidationResult(false, "")
}

fun validateInternalIp(ip : String) : ValidationResult
{
    val regexIPv4 = """^(25[0-5]|2[0-4]\d|[0-1]?\d?\d)(\.(25[0-5]|2[0-4]\d|[0-1]?\d?\d)){3}$""".toRegex()
    return if(regexIPv4.matches(ip)) ValidationResult(false, "") else ValidationResult(true, "Must be valid IP address")
}

