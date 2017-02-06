package piuk.blockchain.android

import kotlin.test.assertEquals

infix fun Any.equals(expected : Any) {
    assertEquals(expected, this)
}

