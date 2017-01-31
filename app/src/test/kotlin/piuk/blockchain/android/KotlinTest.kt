package piuk.blockchain.android

import org.junit.Test
import kotlin.test.assertEquals

class KotlinTest {

    infix fun Any.equals(expected : Any) {
        assertEquals(expected, this)
    }

    @Test
    @Throws(Exception::class)
    fun testAThing() : Unit {
        val bool = true

        bool equals true
    }

}

