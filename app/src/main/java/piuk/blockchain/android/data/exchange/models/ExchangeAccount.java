package piuk.blockchain.android.data.exchange.models;

import java.util.List;

/**
 * Created by justin on 5/1/17.
 */

public interface ExchangeAccount {

    String getToken();

    List<TradeData> getTrades();
}
