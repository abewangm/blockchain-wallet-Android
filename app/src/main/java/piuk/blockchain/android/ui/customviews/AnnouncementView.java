package piuk.blockchain.android.ui.customviews;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import piuk.blockchain.android.R;

public class AnnouncementView extends FrameLayout{

    TextView tvTitle;
    TextView tvContent;
    ImageView ivImage;
    ImageView ivEmoji;
    ImageView ivClose;
    TextView tvLink;

    public AnnouncementView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.item_announcement, this, true);

        FrameLayout fl = (FrameLayout) getChildAt(0);
        CardView cv = (CardView)fl.getChildAt(0);
        RelativeLayout rl = (RelativeLayout)cv.getChildAt(0);

        ivClose = (ImageView)rl.getChildAt(0);
        ivImage = (ImageView)rl.getChildAt(1);
        LinearLayout llmain = (LinearLayout)rl.getChildAt(2);
        LinearLayout ll = (LinearLayout)llmain.getChildAt(0);
        tvContent = (TextView)llmain.getChildAt(1);
        tvLink = (TextView)rl.getChildAt(3);

        tvTitle = (TextView)ll.getChildAt(0);
        ivEmoji = (ImageView)ll.getChildAt(1);
    }

    public void setTitle(int resourceId) {
        tvTitle.setText(resourceId);
    }

    public void setContent(int resourceId) {
        tvContent.setText(resourceId);
    }

    public void setLink(int resourceId) {
        tvLink.setText(resourceId);
    }

    public void setImage(int resourceId) {
        ivImage.setImageResource(resourceId);
    }

    public void setEmoji(int resourceId) {
        ivEmoji.setImageResource(resourceId);
    }

    public void setLinkOnclickListener(OnClickListener onClickListener) {
        tvLink.setOnClickListener(onClickListener);
    }

    public void setCloseOnclickListener(OnClickListener onClickListener) {
        ivClose.setOnClickListener(onClickListener);
    }
}
