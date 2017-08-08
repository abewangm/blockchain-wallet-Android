package piuk.blockchain.android.ui.onboarding;

import android.content.Intent;

import piuk.blockchain.android.ui.base.View;

interface OnboardingView extends View {

    Intent getPageIntent();

    void showFingerprintPrompt();

    void showEmailPrompt();

    void showFingerprintDialog(String pincode);

    void showEnrollFingerprintsDialog();
}
