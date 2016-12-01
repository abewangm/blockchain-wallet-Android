package piuk.blockchain.android.data.metadata;

import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public class MetaDataUri {

    private static final String SCHEME = "http://blockchain.info/";

    private String from;
    private String message;
    private String inviteId;
    private UriType uriType;

    public enum UriType {
        // There may be more types in the future
        INVITE("invite"),
        REQUEST_PAYMENT("request_payment");

        String type;

        UriType(String type) {
            this.type = type;
        }

        public static UriType fromString(String text) {
            if (text != null) {
                for (UriType uriType : UriType.values()) {
                    if (text.equalsIgnoreCase(uriType.type)) {
                        return uriType;
                    }
                }
            }
            return null;
        }
    }

    MetaDataUri() {
        // Package local constructor
    }

    public Uri encode() {
        StringBuilder stringBuilder = new StringBuilder(SCHEME);

        stringBuilder.append(uriType.type)
                .append("?");

        if (message != null) {
            stringBuilder.append("message=")
                    .append(Uri.encode(message))
                    .append("&");
        }

        // TODO: 01/12/2016 This probably needs to be mandatory in future
        if (from != null) {
            stringBuilder.append("from=")
                    .append(Uri.encode(from));
        }

        stringBuilder.append("&invite_id=")
                .append(inviteId);

        return Uri.parse(stringBuilder.toString());
    }

    public static MetaDataUri decode(String data) {

        MetaDataUri metaDataUri = new MetaDataUri();

        // android.net.Uri to get authority
        Uri uri = Uri.parse(data);
        metaDataUri.setUriType(UriType.fromString(uri.getAuthority()));

        // Use UrlQuerySanitizer for everything else, as android.net.Uri.getQueryParameter() is broken pre-Jelly Bean
        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(data);

        String mdid = sanitizer.getValue("invite_id");
        metaDataUri.setInviteId(mdid);
        String from = sanitizer.getValue("from");
        metaDataUri.setFrom(Uri.decode(from));
        String message = sanitizer.getValue("message");
        metaDataUri.setMessage(message != null ? Uri.decode(message) : null);

        return metaDataUri;
    }

    @NonNull
    public String getFrom() {
        return from;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @NonNull
    public String getInviteId() {
        return inviteId;
    }

    public UriType getUriType() {
        return uriType;
    }

    void setFrom(String from) {
        this.from = from;
    }

    void setMessage(String message) {
        this.message = message;
    }

    void setInviteId(String inviteId) {
        this.inviteId = inviteId;
    }

    void setUriType(UriType uriType) {
        this.uriType = uriType;
    }

    public static class Builder {

        private MetaDataUri metaDataUri;

        public Builder() {
            metaDataUri = new MetaDataUri();
        }

        public Builder setFrom(String from) {
            metaDataUri.setFrom(from);
            return this;
        }

        public Builder setMessage(String message) {
            metaDataUri.setMessage(message);
            return this;
        }

        public Builder setInviteId(String mdid) {
            metaDataUri.setInviteId(mdid);
            return this;
        }

        public Builder setUriType(UriType uriType) {
            metaDataUri.setUriType(uriType);
            return this;
        }

        /**
         * Generates a MetaDataUri object from the Builder object. Please note that this will throw
         * an {@link AssertionError} if the URI type or MDID are not set
         *
         * @return A fully formed {@link MetaDataUri} object
         */
        @SuppressWarnings("ConstantConditions")
        public MetaDataUri create() {
            if (metaDataUri.getUriType() == null) throw new AssertionError("URI type must be set");
            if (metaDataUri.getInviteId() == null) throw new AssertionError("Invite ID must be set");
            if (metaDataUri.getFrom() == null) throw new AssertionError("From must be set");

            return metaDataUri;
        }
    }

}
