package piuk.blockchain.android.data.transactions

import java.util.*

class DisplayableDateComparator : Comparator<Displayable> {

    override fun compare(t1: Displayable, t2: Displayable): Int {

        val BEFORE = -1
        val EQUAL = 0
        val AFTER = 1

        return when {
            t1.timeStamp > t2.timeStamp -> BEFORE
            t1.timeStamp < t2.timeStamp -> AFTER
            else -> EQUAL
        }
    }

}
