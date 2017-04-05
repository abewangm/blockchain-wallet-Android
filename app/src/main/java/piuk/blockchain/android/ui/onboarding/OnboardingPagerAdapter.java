package piuk.blockchain.android.ui.onboarding;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

public class OnboardingPagerAdapter extends FragmentPagerAdapter{

    ArrayList<OnboardingPagerContent> pages;

    public OnboardingPagerAdapter(FragmentManager fm, ArrayList<OnboardingPagerContent> pages) {
        super(fm);
        this.pages = pages;
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public android.support.v4.app.Fragment getItem(int position) {
        return OnboardingPagerFragment.init(pages.get(position));
    }
}
