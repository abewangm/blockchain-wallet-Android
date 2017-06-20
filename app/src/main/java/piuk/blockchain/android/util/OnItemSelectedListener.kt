package piuk.blockchain.android.util

import android.view.View
import android.widget.AdapterView

/**
 * Allow us to use a functional interface in place of implementing members that we might not need to.
 */
inline fun OnItemSelectedListener(
        crossinline function: (position: Int) -> Unit
): AdapterView.OnItemSelectedListener = object : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        // Pass the object position to the supplied function
        function.invoke(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // No-op
    }

}
