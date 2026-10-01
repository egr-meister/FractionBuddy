package com.fractionbuddy.app.domain

import kotlin.random.Random

/** Injected time source so tests can control timestamps. */
fun interface Clock {
    fun now(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}

/** Injected randomness so question generation can be seeded in tests. */
fun interface RandomProvider {
    fun next(): Random

    companion object {
        val Default = RandomProvider { Random(java.lang.System.nanoTime()) }
    }
}
