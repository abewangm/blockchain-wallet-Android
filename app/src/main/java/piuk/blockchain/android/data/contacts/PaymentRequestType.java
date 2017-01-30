package piuk.blockchain.android.data.contacts;

public enum PaymentRequestType {
    SEND("send"),
    REQUEST("request");

    private String name;

    PaymentRequestType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static PaymentRequestType fromString(String string) {
        if (string != null) {
            for (PaymentRequestType type : PaymentRequestType.values()) {
                if (type.getName().equalsIgnoreCase(string)) {
                    return type;
                }
            }
        }
        return null;
    }
}