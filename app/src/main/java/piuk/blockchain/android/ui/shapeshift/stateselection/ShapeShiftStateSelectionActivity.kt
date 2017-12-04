package piuk.blockchain.android.ui.shapeshift.stateselection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_shapeshift_state_selection.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.util.americanStatesMap
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.invisible
import piuk.blockchain.android.util.extensions.visible
import javax.inject.Inject
import android.widget.TextView
import android.view.ViewGroup
import piuk.blockchain.android.util.helperfunctions.consume


class ShapeShiftStateSelectionActivity : BaseMvpActivity<
        ShapeShiftStateSelectionView, ShapeShiftStateSelectionPresenter>(), ShapeShiftStateSelectionView {

    override fun finishActivityWithResult(resultCode: Int) {
        setResult(resultCode)
        finish()
    }

    override fun createPresenter() = shapeShiftStateSelectionPresenter

    override fun getView() = this

    @Suppress("MemberVisibilityCanPrivate")
    @Inject lateinit var shapeShiftStateSelectionPresenter: ShapeShiftStateSelectionPresenter

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shapeshift_state_selection)
        setupToolbar(toolbar_general, R.string.shapeshift_exchange)

        stateSelectError.invisible()

        btnConfirm.setOnClickListener { finishActivityWithResult(Activity.RESULT_CANCELED) }

        var states = americanStatesMap.keys.toTypedArray().sortedArray().toMutableList()
        states.add(getString(R.string.shapeshift_select_state))

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, states) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val v =  super.getView(position, convertView, parent)

                if (position == count) {
                    (v.findViewById<TextView>(android.R.id.text1) as TextView).text = ""
                    (v.findViewById<TextView>(android.R.id.text1) as TextView).hint = getItem(count)
                }

                return v
            }

            override fun getCount(): Int {
                return super.getCount() - 1 // Dont display last item. It is used as hint.
            }

        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerState.adapter = adapter
        spinnerState.setSelection(adapter.count)

        spinnerState.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View, position: Int, id: Long) {

                if (position == adapter.count) return

                stateSelectError.invisible()
                btnConfirm.gone()

                val selectedState = parent.getItemAtPosition(position).toString()
                presenter.updateAmericanState(americanStatesMap.get(selectedState)!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No-op
            }
        }

        onViewReady()
    }

    override fun onSupportNavigateUp() = consume { onBackPressed() }

    override fun onError(message: Int) {
        stateSelectError.visible()
        btnConfirm.visible()
        stateSelectError.setText(message)
    }

    companion object {

        //TODO we need a request code handler/incrementer to avoid potential collisions
        val STATE_SELECTION_REQUEST_CODE = 54021

        @JvmStatic
        fun start(context: Activity, requestCode: Int) {
            context.startActivityForResult(Intent(context, ShapeShiftStateSelectionActivity::class.java), requestCode)
        }

    }
}
