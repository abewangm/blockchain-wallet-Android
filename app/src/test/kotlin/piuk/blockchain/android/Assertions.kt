package piuk.blockchain.android

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

infix fun Any.equals(expected : Any) = assertEquals(expected, this)
infix fun Any.doesNotEqual(expected : Any) = assertNotEquals(expected, this)

