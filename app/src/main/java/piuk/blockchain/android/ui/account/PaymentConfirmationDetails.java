package piuk.blockchain.android.ui.account;

import android.os.Parcel;
import android.os.Parcelable;

public class PaymentConfirmationDetails implements Parcelable {

    public String fromLabel;
    public String toLabel;
    public String cryptoUnit;
    public String fiatUnit;
    public String cryptoAmount;
    public String fiatAmount;
    public String cryptoFee;
    public String fiatFee;
    public String cryptoTotal;
    public String fiatTotal;
    public String btcSuggestedFee;
    public String fiatSymbol;
    public boolean isLargeTransaction;
    public boolean hasConsumedAmounts;
    public String warningText;
    public String warningSubtext;

    public PaymentConfirmationDetails() {
        // Default empty constructor
    }

    protected PaymentConfirmationDetails(Parcel in) {
        fromLabel = in.readString();
        toLabel = in.readString();
        cryptoUnit = in.readString();
        fiatUnit = in.readString();
        cryptoAmount = in.readString();
        fiatAmount = in.readString();
        cryptoFee = in.readString();
        fiatFee = in.readString();
        cryptoTotal = in.readString();
        fiatTotal = in.readString();
        btcSuggestedFee = in.readString();
        fiatSymbol = in.readString();
        isLargeTransaction = in.readByte() != 0x00;
        hasConsumedAmounts = in.readByte() != 0x00;
        warningText = in.readString();
        warningSubtext = in.readString();
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
        dest.writeString(cryptoUnit);
        dest.writeString(fiatUnit);
        dest.writeString(cryptoAmount);
        dest.writeString(fiatAmount);
        dest.writeString(cryptoFee);
        dest.writeString(fiatFee);
        dest.writeString(cryptoTotal);
        dest.writeString(fiatTotal);
        dest.writeString(btcSuggestedFee);
        dest.writeString(fiatSymbol);
        dest.writeByte((byte) (isLargeTransaction ? 0x01 : 0x00));
        dest.writeByte((byte) (hasConsumedAmounts ? 0x01 : 0x00));
        dest.writeString(warningText);
        dest.writeString(warningSubtext);
    }

    @Override
    public String toString() {
        return "PaymentConfirmationDetails{" +
                "fromLabel='" + fromLabel + '\'' +
                ", toLabel='" + toLabel + '\'' +
                ", cryptoUnit='" + cryptoUnit + '\'' +
                ", fiatUnit='" + fiatUnit + '\'' +
                ", cryptoAmount='" + cryptoAmount + '\'' +
                ", fiatAmount='" + fiatAmount + '\'' +
                ", cryptoFee='" + cryptoFee + '\'' +
                ", fiatFee='" + fiatFee + '\'' +
                ", cryptoTotal='" + cryptoTotal + '\'' +
                ", fiatTotal='" + fiatTotal + '\'' +
                ", btcSuggestedFee='" + btcSuggestedFee + '\'' +
                ", fiatSymbol='" + fiatSymbol + '\'' +
                ", isLargeTransaction=" + isLargeTransaction +
                ", hasConsumedAmounts=" + hasConsumedAmounts +
                ", warningText=" + warningText +
                ", warningSubtext=" + warningSubtext +
                '}';
    }

}
