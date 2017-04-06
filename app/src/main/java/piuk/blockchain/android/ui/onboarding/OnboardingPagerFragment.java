package piuk.blockchain.android.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import piuk.blockchain.android.R;

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

        View layoutView = inflater.inflate(R.layout.item_onboarding_page, container, false);

        TextView tvHeading1 = ((TextView) layoutView.findViewById(R.id.tv_heading_1));
        TextView tvHeading2 = ((TextView) layoutView.findViewById(R.id.tv_heading_2));
        TextView tvContent = ((TextView) layoutView.findViewById(R.id.tv_content));
        TextView tvLink = ((TextView) layoutView.findViewById(R.id.tv_link));
        ImageView ivIcon = ((ImageView) layoutView.findViewById(R.id.iv_icon));

        //Set text
        if (heading1 == null || heading1.isEmpty()) {
            tvHeading1.setVisibility(View.GONE);
        } else {
            tvHeading1.setText(heading1);
        }

        if (heading2 == null || heading2.isEmpty()) {
            tvHeading2.setVisibility(View.GONE);
        } else {
            tvHeading2.setText(heading2);
        }

        if (content == null || content.isEmpty()) {
            tvContent.setVisibility(View.GONE);
        } else {
            tvContent.setText(content);
        }

        if (link == null || link.isEmpty()) {
            tvLink.setVisibility(View.GONE);
        } else {
            tvLink.setText(link);
        }

        //Set icon
        ivIcon.setImageResource(iconResource);

        //Set color
        tvHeading1.setTextColor(ContextCompat.getColor(getActivity(), colorResource));
        tvHeading2.setTextColor(ContextCompat.getColor(getActivity(), colorResource));
        tvLink.setTextColor(ContextCompat.getColor(getActivity(), colorResource));

        tvLink.setOnClickListener(v -> sendBroadcast());

        return layoutView;
    }

    void sendBroadcast() {
        Intent intent = new Intent(linkAction);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }
}
