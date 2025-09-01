package com.shinjiindustrial.portmapper.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.shinjiindustrial.portmapper.toIntOrMaxValue

data class ValidationResult(
    val hasError: Boolean,
    val validationError: ValidationError
) {
    companion object {
        val ok = ValidationResult(false, ValidationError.NONE)
        fun error(error: ValidationError) = ValidationResult(true, error)
    }
}

@Composable
fun ValidationError.toMessage(): String {
    return when (this) {
        ValidationError.EMPTY_DESCRIPTION -> stringResource(R.string.error_empty)
        ValidationError.EMPTY_PORT -> stringResource(R.string.error_empty)
        ValidationError.INVALID_PORT_RANGE ->
            stringResource(R.string.error_invalid_port_range, MIN_PORT, MAX_PORT)

        ValidationError.END_BEFORE_START -> stringResource(R.string.error_end_before_start)
        ValidationError.NONE -> ""
        ValidationError.INVALID_INTERNAL_IP -> stringResource(R.string.error_invalid_ip_address)
    }
}

enum class ValidationError {
    NONE,
    EMPTY_DESCRIPTION,
    EMPTY_PORT,
    INVALID_PORT_RANGE,
    END_BEFORE_START,
    INVALID_INTERNAL_IP
}

fun validateDescription(description: String): ValidationResult {
    if (description.isEmpty()) {
        return ValidationResult.error(ValidationError.EMPTY_DESCRIPTION)
    }
    return ValidationResult.ok
}

fun validateStartPort(startPort: String): ValidationResult {
    if (startPort.isEmpty()) {
        return ValidationResult.error(ValidationError.EMPTY_PORT)
    }
    val portInt = startPort.toIntOrMaxValue()
    if (portInt < MIN_PORT || portInt > MAX_PORT) {
        return ValidationResult.error(ValidationError.INVALID_PORT_RANGE)
    }
    return ValidationResult.ok
}

fun validateEndPort(startPort: String, endPort: String): ValidationResult {
    if (startPort.isEmpty()) {
        // let them deal with that error first
        return ValidationResult.ok
    }
    if (endPort.isEmpty()) {
        //valid
        return ValidationResult.ok
    }

    val startPortInt = startPort.toIntOrMaxValue()
    val endPortInt = endPort.toIntOrMaxValue()

    if (endPortInt < MIN_PORT || endPortInt > MAX_PORT) {
        return ValidationResult.error(ValidationError.INVALID_PORT_RANGE)
    }

    if (startPortInt > endPortInt) {
        return ValidationResult.error(ValidationError.END_BEFORE_START)
    }

    return ValidationResult.ok
}

fun validateInternalIp(ip: String): ValidationResult {
    val regexIPv4 =
        """^(25[0-5]|2[0-4]\d|[0-1]?\d?\d)(\.(25[0-5]|2[0-4]\d|[0-1]?\d?\d)){3}$""".toRegex()
    return if (regexIPv4.matches(ip)) ValidationResult.ok else ValidationResult.error(
        ValidationError.INVALID_INTERNAL_IP
    )
}

