package piuk.blockchain.android.data.metadata;

import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.URI;

public class MetaDataUri {

    private static final String SCHEME = "blockchain://";

    private String from;
    private String message;
    private String mdid;
    private UriType uriType;

    public enum UriType {

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

        stringBuilder.append("from=")
                .append(Uri.encode(from))
                .append("&mdid_hash=")
                .append(mdid);

        return Uri.parse(stringBuilder.toString());
    }

    public static MetaDataUri decode(String uri) {

        MetaDataUri metaDataUri = new MetaDataUri();

        // Use java.net.URI to get authority
        URI uri1 = URI.create(uri);
        metaDataUri.setUriType(UriType.fromString(uri1.getAuthority()));

        // Use UrlQuerySanitizer for everything else, as URI.getQueryParameter() is broken pre-Jelly Bean
        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(uri);

        String mdid = sanitizer.getValue("mdid_hash");
        metaDataUri.setMdid(mdid);
        String from = sanitizer.getValue("from");
        metaDataUri.setFrom(from.replaceAll("_", " "));
        String message = sanitizer.getValue("message");
        metaDataUri.setMessage(message != null ? message.replaceAll("_", " ") : null);

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
    public String getMdid() {
        return mdid;
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

    void setMdid(String mdid) {
        this.mdid = mdid;
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

        public Builder setMdid(String mdid) {
            metaDataUri.setMdid(mdid);
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
            if (metaDataUri.getMdid() == null) throw new AssertionError("MDID must be set");
            if (metaDataUri.getFrom() == null) throw new AssertionError("From must be set");

            return metaDataUri;
        }
    }

}
