package piuk.blockchain.android.util.helperfunctions

import android.support.v4.view.ViewPager
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

/**
 * Allow us to use a functional interface in place of implementing members that we might not need to.
 */
inline fun OnPageChangeListener(
        crossinline function: (position: Int, positionOffset: Float) -> Unit
): ViewPager.OnPageChangeListener = object : ViewPager.OnPageChangeListener {

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        // Pass the object position and offset to the supplied function
        function.invoke(position, positionOffset)
    }

    override fun onPageSelected(position: Int) {
        // No-op
    }

    override fun onPageScrollStateChanged(state: Int) {
        // No-op
    }
}
