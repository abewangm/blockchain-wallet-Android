package piuk.blockchain.android.ui.shapeshift.confirmation

import org.amshove.kluent.mock
import org.junit.Before
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.payments.SendDataManager
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.util.StringUtils

class ShapeShiftConfirmationPresenterTest {

    private lateinit var subject: ShapeShiftConfirmationPresenter
    private val view: ShapeShiftConfirmationView = mock()
    private val shapeShiftDataManager: ShapeShiftDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val sendDataManager: SendDataManager = mock()
    private val ethDataManager: EthDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val stringUtils: StringUtils = mock()

    @Before
    fun setUp() {
        subject = ShapeShiftConfirmationPresenter(
                shapeShiftDataManager,
                payloadDataManager,
                sendDataManager,
                ethDataManager,
                bchDataManager,
                stringUtils
        ).apply { initView(this@ShapeShiftConfirmationPresenterTest.view) }
    }


}