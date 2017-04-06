package piuk.blockchain.android.ui.onboarding;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class OnboardingPagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> fragments = new ArrayList<>();

    public OnboardingPagerAdapter(FragmentManager fm, List<OnboardingPagerContent> pages) {
        super(fm);
        notifyPagesChanged(pages);
    }

    public void notifyPagesChanged(List<OnboardingPagerContent> pages) {
        fragments.clear();
        for (OnboardingPagerContent page : pages) {
            fragments.add(OnboardingPagerFragment.newInstance(page));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }
}
