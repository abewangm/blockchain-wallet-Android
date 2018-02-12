package piuk.blockchain.android.ui.account;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.annotation.ColorRes;
import android.support.annotation.FloatRange;
import android.support.v4.content.ContextCompat;

import piuk.blockchain.android.BR;
import piuk.blockchain.android.util.ViewUtils;

public class AccountEditModel extends BaseObservable {

    private int transferFundsVisibility;
    private float transferFundsAlpha;
    private boolean transferFundsClickable;

    private String labelHeader;
    private String label;
    private float labelAlpha;
    private boolean labelClickable;

    private int defaultAccountVisibility;
    private String defaultText;
    private int defaultTextColor;
    private float defaultAlpha;
    private boolean defaultClickable;

    private int scanPrivateKeyVisibility;
    private float xprivAlpha;
    private boolean xprivClickable;

    private String xpubText;
    private int xpubDescriptionVisibility;
    private float xpubAlpha;
    private boolean xpubClickable;

    private int archiveVisibility;
    private String archiveHeader;
    private String archiveText;
    private float archiveAlpha;
    private boolean archiveClickable;

    private Context context;

    AccountEditModel(Context context) {
        this.context = context;
    }

    @Bindable
    public int getTransferFundsVisibility() {
        return transferFundsVisibility;
    }

    void setTransferFundsVisibility(@ViewUtils.Visibility int visibility) {
        transferFundsVisibility = visibility;
        notifyPropertyChanged(BR.transferFundsVisibility);
    }

    @Bindable
    public float getTransferFundsAlpha() {
        return transferFundsAlpha;
    }

    void setTransferFundsAlpha(@FloatRange(from = 0.0, to = 1.0) float transferFundsAlpha) {
        this.transferFundsAlpha = transferFundsAlpha;
        notifyPropertyChanged(BR.transferFundsAlpha);
    }

    @Bindable
    boolean getTransferFundsClickable() {
        return transferFundsClickable;
    }

    void setTransferFundsClickable(boolean transferFundsClickable) {
        this.transferFundsClickable = transferFundsClickable;
        notifyPropertyChanged(BR.transferFundsClickable);
    }

    @Bindable
    public String getLabelHeader() {
        return labelHeader;
    }

    void setLabelHeader(String labelHeader) {
        this.labelHeader = labelHeader;
        notifyPropertyChanged(BR.labelHeader);
    }

    @Bindable
    public float getLabelAlpha() {
        return labelAlpha;
    }

    void setLabelAlpha(@FloatRange(from = 0.0, to = 1.0) float labelAlpha) {
        this.labelAlpha = labelAlpha;
        notifyPropertyChanged(BR.labelAlpha);
    }

    @Bindable
    public boolean getLabelClickable() {
        return labelClickable;
    }

    void setLabelClickable(boolean labelClickable) {
        this.labelClickable = labelClickable;
        notifyPropertyChanged(BR.labelClickable);
    }

    @Bindable
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        notifyPropertyChanged(BR.label);
    }

    @Bindable
    public int getDefaultAccountVisibility() {
        return defaultAccountVisibility;
    }

    void setDefaultAccountVisibility(@ViewUtils.Visibility int visibility) {
        defaultAccountVisibility = visibility;
        notifyPropertyChanged(BR.defaultAccountVisibility);
    }

    @Bindable
    public String getDefaultText() {
        return defaultText;
    }

    void setDefaultText(String defaultText) {
        this.defaultText = defaultText;
        notifyPropertyChanged(BR.defaultText);
    }

    @Bindable
    public int getDefaultTextColor() {
        return defaultTextColor;
    }

    void setDefaultTextColor(@ColorRes int defaultTextColor) {
        this.defaultTextColor = ContextCompat.getColor(context, defaultTextColor);
        notifyPropertyChanged(BR.defaultTextColor);
    }

    @Bindable
    public float getDefaultAlpha() {
        return defaultAlpha;
    }

    void setDefaultAlpha(@FloatRange(from = 0.0, to = 1.0) float defaultAlpha) {
        this.defaultAlpha = defaultAlpha;
        notifyPropertyChanged(BR.defaultAlpha);
    }

    @Bindable
    public boolean getDefaultClickable() {
        return defaultClickable;
    }

    void setDefaultClickable(boolean defaultClickable) {
        this.defaultClickable = defaultClickable;
        notifyPropertyChanged(BR.defaultClickable);
    }

    @Bindable
    public int getScanPrivateKeyVisibility() {
        return scanPrivateKeyVisibility;
    }

    void setScanPrivateKeyVisibility(@ViewUtils.Visibility int visibility) {
        scanPrivateKeyVisibility = visibility;
        notifyPropertyChanged(BR.scanPrivateKeyVisibility);
    }

    @Bindable
    public float getXprivAlpha() {
        return xprivAlpha;
    }

    void setXprivAlpha(@FloatRange(from = 0.0, to = 1.0) float xprivAlpha) {
        this.xprivAlpha = xprivAlpha;
        notifyPropertyChanged(BR.xprivAlpha);
    }

    @Bindable
    public boolean getXprivClickable() {
        return xprivClickable;
    }

    void setXprivClickable(boolean xprivClickable) {
        this.xprivClickable = xprivClickable;
        notifyPropertyChanged(BR.xprivClickable);
    }

    @Bindable
    public String getXpubText() {
        return xpubText;
    }

    void setXpubText(String xpubText) {
        this.xpubText = xpubText;
        notifyPropertyChanged(BR.xpubText);
    }

    @Bindable
    public int getXpubDescriptionVisibility() {
        return xpubDescriptionVisibility;
    }

    void setXpubDescriptionVisibility(int xpubDescriptionVisibility) {
        this.xpubDescriptionVisibility = xpubDescriptionVisibility;
        notifyPropertyChanged(BR.xpubDescriptionVisibility);
    }

    @Bindable
    public float getXpubAlpha() {
        return xpubAlpha;
    }

    void setXpubAlpha(@FloatRange(from = 0.0, to = 1.0) float xpubAlpha) {
        this.xpubAlpha = xpubAlpha;
        notifyPropertyChanged(BR.xpubAlpha);
    }

    @Bindable
    public boolean getXpubClickable() {
        return xpubClickable;
    }

    void setXpubClickable(boolean xpubClickable) {
        this.xpubClickable = xpubClickable;
        notifyPropertyChanged(BR.xpubClickable);
    }

    @Bindable
    public int getArchiveVisibility() {
        return archiveVisibility;
    }

    void setArchiveVisibility(@ViewUtils.Visibility int visibility) {
        archiveVisibility = visibility;
        notifyPropertyChanged(BR.archiveVisibility);
    }

    @Bindable
    public String getArchiveHeader() {
        return archiveHeader;
    }

    void setArchiveHeader(String archiveHeader) {
        this.archiveHeader = archiveHeader;
        notifyPropertyChanged(BR.archiveHeader);
    }

    @Bindable
    public String getArchiveText() {
        return archiveText;
    }

    void setArchiveText(String archiveText) {
        this.archiveText = archiveText;
        notifyPropertyChanged(BR.archiveText);
    }

    @Bindable
    public float getArchiveAlpha() {
        return archiveAlpha;
    }

    void setArchiveAlpha(@FloatRange(from = 0.0, to = 1.0) float archiveAlpha) {
        this.archiveAlpha = archiveAlpha;
        notifyPropertyChanged(BR.archiveAlpha);
    }

    @Bindable
    public boolean getArchiveClickable() {
        return archiveClickable;
    }

    void setArchiveClickable(boolean archiveClickable) {
        this.archiveClickable = archiveClickable;
        notifyPropertyChanged(BR.archiveClickable);
    }
}
