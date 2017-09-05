package piuk.blockchain.android.util.helperfunctions

/**
 * Allows us to enforce correct arguments being supplied to methods.
 *
 * @throws IllegalArgumentException
 */
inline fun require(value: Boolean, lazyMessage: () -> Any) {
    if (!value) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    }
}

/**
 * Intended to be used to check that a method isn't called at an inappropriate time.
 *
 * @throws IllegalStateException
 */
inline fun check(value: Boolean, lazyMessage: () -> Any) {
    if (!value) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString())
    }
}