package piuk.blockchain.android.ui.account;

import android.os.Parcel;
import android.os.Parcelable;

public class PaymentConfirmationDetails implements Parcelable {

    public String fromLabel;
    public String toLabel;
    public String btcUnit;
    public String fiatUnit;
    public String btcAmount;
    public String fiatAmount;
    public String btcFee;
    public String fiatFee;
    public String btcTotal;
    public String fiatTotal;
    public String btcSuggestedFee;
    public String fiatSymbol;
    public boolean isLargeTransaction;
    public boolean hasConsumedAmounts;

    public PaymentConfirmationDetails() {
        // Default empty constructor
    }

    protected PaymentConfirmationDetails(Parcel in) {
        fromLabel = in.readString();
        toLabel = in.readString();
        btcUnit = in.readString();
        fiatUnit = in.readString();
        btcAmount = in.readString();
        fiatAmount = in.readString();
        btcFee = in.readString();
        fiatFee = in.readString();
        btcTotal = in.readString();
        fiatTotal = in.readString();
        btcSuggestedFee = in.readString();
        fiatSymbol = in.readString();
        isLargeTransaction = in.readByte() != 0x00;
        hasConsumedAmounts = in.readByte() != 0x00;
    }

    public static final Creator<PaymentConfirmationDetails> CREATOR = new Creator<PaymentConfirmationDetails>() {
        @Override
        public PaymentConfirmationDetails createFromParcel(Parcel in) {
            return new PaymentConfirmationDetails(in);
        }

        @Override
        public PaymentConfirmationDetails[] newArray(int size) {
            return new PaymentConfirmationDetails[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fromLabel);
        dest.writeString(toLabel);
        dest.writeString(btcUnit);
        dest.writeString(fiatUnit);
        dest.writeString(btcAmount);
        dest.writeString(fiatAmount);
        dest.writeString(btcFee);
        dest.writeString(fiatFee);
        dest.writeString(btcTotal);
        dest.writeString(fiatTotal);
        dest.writeString(btcSuggestedFee);
        dest.writeString(fiatSymbol);
        dest.writeByte((byte) (isLargeTransaction ? 0x01 : 0x00));
        dest.writeByte((byte) (hasConsumedAmounts ? 0x01 : 0x00));
    }

    @Override
    public String toString() {
        return "PaymentConfirmationDetails{" +
                "fromLabel='" + fromLabel + '\'' +
                ", toLabel='" + toLabel + '\'' +
                ", btcUnit='" + btcUnit + '\'' +
                ", fiatUnit='" + fiatUnit + '\'' +
                ", btcAmount='" + btcAmount + '\'' +
                ", fiatAmount='" + fiatAmount + '\'' +
                ", btcFee='" + btcFee + '\'' +
                ", fiatFee='" + fiatFee + '\'' +
                ", btcTotal='" + btcTotal + '\'' +
                ", fiatTotal='" + fiatTotal + '\'' +
                ", btcSuggestedFee='" + btcSuggestedFee + '\'' +
                ", fiatSymbol='" + fiatSymbol + '\'' +
                ", isLargeTransaction=" + isLargeTransaction +
                ", hasConsumedAmounts=" + hasConsumedAmounts +
                '}';
    }

}
