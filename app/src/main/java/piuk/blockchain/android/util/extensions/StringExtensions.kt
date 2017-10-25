package piuk.blockchain.android.util.extensions

import com.fasterxml.jackson.databind.ObjectMapper
import org.json.JSONException

/**
 * Converts a deserialized object from a [String] without needing the KCLass passed to it.
 *
 * @throws JSONException
 */
@Throws(JSONException::class)
inline fun <reified T> String.toKotlinObject(): T {
    val mapper = ObjectMapper()
    return mapper.readValue(this, T::class.java)
}