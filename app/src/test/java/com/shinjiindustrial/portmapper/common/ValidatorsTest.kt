package com.shinjiindustrial.portmapper.common

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test
    fun `test description validation`() {
        assertFalse(validateDescription("Description is required").hasError);
        assertTrue(validateDescription("").hasError);
    }

    @Test
    fun `test start port validation`() {
        assertTrue(validateStartPort("").hasError);
        assertTrue(validateStartPort("70000").hasError);
        assertTrue(validateStartPort("-100").hasError);
        assertFalse(validateDescription("4000").hasError);
    }

    @Test
    fun `test end port validation`() {
        assertFalse(validateEndPort("", "").hasError);
        assertFalse(validateEndPort("1000", "").hasError);
        assertFalse(validateEndPort("1000", "").hasError);
        assertTrue(validateEndPort("1000", "70000").hasError);
        assertTrue(validateEndPort("1000", "-100").hasError);
        assertTrue(validateEndPort("1000", "100").hasError);
        assertFalse(validateEndPort("4000", "4040").hasError);
    }

    @Test
    fun `test validate internal ip`() {
        assertFalse(validateInternalIp("192.168.10.100").hasError)
        assertTrue(validateInternalIp("-192.168.10.100").hasError)
        assertTrue(validateInternalIp("-19216810100").hasError)
    }
}