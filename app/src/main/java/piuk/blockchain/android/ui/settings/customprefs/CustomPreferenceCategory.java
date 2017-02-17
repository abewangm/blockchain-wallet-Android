package piuk.blockchain.android.ui.settings.customprefs;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.preference.PreferenceCategory;
import android.util.AttributeSet;

import uk.co.chrisjenx.calligraphy.CalligraphyUtils;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

public class CustomPreferenceCategory extends PreferenceCategory {

    private Typeface typeface;

    public CustomPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public CustomPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CustomPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomPreferenceCategory(Context context) {
        super(context);
        init();
    }

    private void init() {
        typeface = TypefaceUtils.load(getContext().getAssets(), "fonts/Montserrat-Regular.ttf");
        // Forces setting fonts when Summary or Title are set via XMl
        setTitle(getTitle());
        setSummary(getSummary());
    }

    @Override
    public void setTitle(int titleResId) {
        setTitle(getContext().getString(titleResId));
    }

    @Override
    public void setTitle(CharSequence title) {
        CharSequence charSequence = CalligraphyUtils.applyTypefaceSpan(
                title,
                typeface);
        super.setTitle(charSequence);
    }

    @Override
    public void setSummary(int summaryResId) {
        setSummary(getContext().getString(summaryResId));
    }

    @Override
    public void setSummary(CharSequence summary) {
        CharSequence charSequence = CalligraphyUtils.applyTypefaceSpan(
                summary,
                typeface);
        super.setSummary(charSequence);
    }

}
