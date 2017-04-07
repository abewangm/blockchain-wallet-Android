package piuk.blockchain.android.ui.onboarding;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentOnboardingPagerBinding;

public class OnboardingPagerFragment extends Fragment {

    private String heading1;
    private String heading2;
    private String content;
    private String link;
    private int iconResource;
    private int colorResource;
    private String linkAction;

    static OnboardingPagerFragment newInstance(OnboardingPagerContent pageContent) {
        OnboardingPagerFragment frag = new OnboardingPagerFragment();
        Bundle args = new Bundle();
        args.putString("heading1", pageContent.heading1);
        args.putString("heading2", pageContent.heading2);
        args.putString("content", pageContent.content);
        args.putString("link", pageContent.link);
        args.putInt("icon", pageContent.iconResource);
        args.putInt("color", pageContent.colorResource);
        args.putString("linkAction", pageContent.linkAction);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        heading1 = getArguments() != null ? getArguments().getString("heading1") : "";
        heading2 = getArguments() != null ? getArguments().getString("heading2") : "";
        content = getArguments() != null ? getArguments().getString("content") : "";
        link = getArguments() != null ? getArguments().getString("link") : "";
        linkAction = getArguments() != null ? getArguments().getString("linkAction") : "";
        iconResource = getArguments() != null ? getArguments().getInt("icon") : 0;
        colorResource = getArguments() != null ? getArguments().getInt("color") : 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final FragmentOnboardingPagerBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_onboarding_pager,
                container,
                false);

        //Set text
        if (heading1 == null || heading1.isEmpty()) {
            binding.tvHeading1.setVisibility(View.GONE);
        } else {
            binding.tvHeading1.setText(heading1);
        }

        if (heading2 == null || heading2.isEmpty()) {
            binding.tvHeading2.setVisibility(View.GONE);
        } else {
            binding.tvHeading2.setText(heading2);
        }

        if (content == null || content.isEmpty()) {
            binding.tvContent.setVisibility(View.GONE);
        } else {
            binding.tvContent.setText(content);
        }

        if (link == null || link.isEmpty()) {
            binding.tvLink.setVisibility(View.GONE);
        } else {
            binding.tvLink.setText(link);
        }

        //Set icon
        binding.ivIcon.setImageResource(iconResource);

        //Set color
        binding.tvHeading1.setTextColor(ContextCompat.getColor(getActivity(), colorResource));
        binding.tvHeading2.setTextColor(ContextCompat.getColor(getActivity(), colorResource));
        binding.tvLink.setTextColor(ContextCompat.getColor(getActivity(), colorResource));

        binding.getRoot().setOnClickListener(v -> sendBroadcast());

        return binding.getRoot();
    }

    private void sendBroadcast() {
        Intent intent = new Intent(linkAction);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }
}
