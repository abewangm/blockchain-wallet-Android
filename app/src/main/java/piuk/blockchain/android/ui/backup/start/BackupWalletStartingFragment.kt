package piuk.blockchain.android.ui.backup.start

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_backup_start.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.SecondPasswordHandler
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.util.extensions.inflate
import javax.inject.Inject

class BackupWalletStartingFragment : BaseFragment<BackupWalletStartingView, BackupWalletStartingPresenter>(),
        BackupWalletStartingView {

    @Inject lateinit var backupWalletStartingPresenter: BackupWalletStartingPresenter

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = container!!.inflate(R.layout.fragment_backup_start)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_start.setOnClickListener {
            if (presenter.isDoubleEncrypted()) {
                SecondPasswordHandler(activity).validate(object : SecondPasswordHandler.ResultListener {
                    override fun onNoSecondPassword() {
                        throw IllegalStateException("This point should never be reached")
                    }

                    override fun onSecondPasswordValidated(validateSecondPassword: String) {
                        val fragment = BackupWalletWordListFragment().apply {
                            arguments = Bundle().apply {
                                putString(
                                        BackupWalletWordListFragment.ARGUMENT_SECOND_PASSWORD,
                                        validateSecondPassword
                                )
                            }
                        }
                        loadFragment(fragment)
                    }
                })
            } else {
                loadFragment(BackupWalletWordListFragment())
            }
        }
    }

    override fun createPresenter() = backupWalletStartingPresenter

    override fun getMvpView() = this

    private fun loadFragment(fragment: Fragment) {
        activity?.run {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .addToBackStack(null)
                    .commit()
        }
    }

    companion object {
        const val TAG = "BackupWalletStartingFragment"
    }

}