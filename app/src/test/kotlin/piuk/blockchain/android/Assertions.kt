package piuk.blockchain.android

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

infix fun Any?.equals(expected: Any) = assertEquals(expected, this)
infix fun Any?.doesNotEqual(expected: Any) = assertNotEquals(expected, this)

infix fun <K, V> Map<K, V>.shouldContainKey(expected: K) = assertTrue { this.containsKey(expected) }
infix fun <K, V> Map<K, V>.shouldNotContainKey(expected: K) = assertFalse { this.containsKey(expected) }

infix fun <K, V> Map<K, V>.shouldContainValue(expected: V) = assertTrue { this.values.contains(expected) }
infix fun <K, V> Map<K, V>.shouldNotContainValue(expected: V) = assertFalse { this.values.contains(expected) }