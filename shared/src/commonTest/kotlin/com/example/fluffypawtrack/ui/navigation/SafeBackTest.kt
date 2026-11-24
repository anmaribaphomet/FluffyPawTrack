package com.example.fluffypawtrack.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class SafeBackTest {

    @Test
    fun popsWhenThereIsAPreviousEntry() {
        val calls = mutableListOf<String>()
        navigateBack(hasPreviousEntry = true, pop = { calls += "pop" }, exit = { calls += "exit" })
        assertEquals(listOf("pop"), calls)
    }

    @Test
    fun neverPopsTheLastEntryAndExitsTheAppInstead() {
        val calls = mutableListOf<String>()
        navigateBack(hasPreviousEntry = false, pop = { calls += "pop" }, exit = { calls += "exit" })
        assertEquals(listOf("exit"), calls)
    }
}
