package com.faire.test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class ServiceIntegrationTest {
    @Test
    fun testWithTestUtility() {
        val testUtility = TestUtility()
        assertEquals("test utility helper", testUtility.helperFunction())
    }
}