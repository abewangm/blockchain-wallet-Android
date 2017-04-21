package piuk.blockchain.android.ui.base;

public interface Presenter<VIEW extends View> {

    void onViewDestroyed();

    void onViewPaused();

    void initView(VIEW view);

    VIEW getView();

}
