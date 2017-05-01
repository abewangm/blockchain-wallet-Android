package piuk.blockchain.android.data.exchange;

import java.util.List;

/**
 * Created by justin on 5/1/17.
 */

public interface ExchangeAccount {
    public String getToken();

    public List<TradeData> getTrades();
}
