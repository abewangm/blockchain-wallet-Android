/**
 * TODO: Remove me once a decision has been made
 *
 * This is used only in {@link piuk.blockchain.android.ui.directory.SuggestMerchantActivity} which
 * is currently hidden from the user. By commenting it out, we can remove a dependency but leave
 * the code for when a decision is made as to what to do with the Merchant Directory.
 */
//package piuk.blockchain.android.ui.directory;
//
//
//import java.io.DataOutputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import org.apache.commons.io.IOUtils;
//
///**
// * Temporary class for SuggestMerchantActivity.
// * Not adding API to jar until decided if Merchant map is getting removed or not
// */
//class WebUtil {
//
//    private static final int DEFAULT_REQUEST_TIMEOUT = 60000;
//
//    private static WebUtil instance = null;
//
//    private WebUtil() {
//    }
//
//    public static WebUtil getInstance() {
//
//        if (instance == null) {
//            instance = new WebUtil();
//        }
//
//        return instance;
//    }
//
//    public String postURLJson(String request, String urlParameters) throws Exception {
//        return this.postURLCall(request, urlParameters, 2, "application/json");
//    }
//
//    private String postURLCall(String request, String urlParameters, int requestRetry, String contentType) throws Exception {
//
//        String error = null;
//
//        for (int ii = 0; ii < requestRetry; ++ii) {
//            URL url = new URL(request);
//
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            try {
//                connection.setDoOutput(true);
//                connection.setDoInput(true);
//                connection.setInstanceFollowRedirects(false);
//                connection.setRequestMethod("POST");
//                connection.setRequestProperty("Content-Type", contentType);
//                connection.setRequestProperty("Origin", "http://localhost:8080");
//                connection.setRequestProperty("charset", "utf-8");
//                connection.setRequestProperty("Accept", "application/json");
//                connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
//                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");
//
//                connection.setUseCaches(false);
//
//                connection.setConnectTimeout(DEFAULT_REQUEST_TIMEOUT);
//                connection.setReadTimeout(DEFAULT_REQUEST_TIMEOUT);
//
//                connection.connect();
//
//                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
//                wr.writeBytes(urlParameters);
//                wr.flush();
//                wr.close();
//
//                connection.setInstanceFollowRedirects(false);
//
//                if (connection.getResponseCode() == 200) {
//                    return IOUtils.toString(connection.getInputStream(), "UTF-8");
//                } else {
//                    error = "Error code:"
//                        + connection.getResponseCode()
//                        + "\n"
//                        + IOUtils.toString(connection.getErrorStream(), "UTF-8");
//                }
//
//                // Sleep unless last request
//                if (ii != requestRetry - 1) {
//                    Thread.sleep(5000);
//                }
//            } catch (Exception e) {
//                throw new Exception("Network error" + e.getMessage());
//            } finally {
//                connection.disconnect();
//            }
//        }
//
//        throw new Exception("Invalid Response " + error);
//    }
//}