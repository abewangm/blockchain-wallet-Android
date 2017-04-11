package piuk.blockchain.android.ui.onboarding;

public class OnboardingPagerContent {

    public String heading1;
    public String heading2;
    public String content;
    public String link;
    public String linkAction;
    public int iconResource;
    public int colorResource;

    public OnboardingPagerContent(String heading1, String heading2, String content, String link, String linkAction, int colorResource, int iconResource) {
        this.heading1 = heading1;
        this.heading2 = heading2;
        this.content = content;
        this.link = link;
        this.linkAction = linkAction;
        this.colorResource  = colorResource;
        this.iconResource = iconResource;
    }
}
