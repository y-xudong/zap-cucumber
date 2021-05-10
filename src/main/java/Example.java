import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Example {
    private static final String ZAP_ADDRESS = "localhost";
    private static final int ZAP_PORT = 8080;
    private static final String ZAP_API_KEY = null;

    private static final String TARGET = "https://example.com/";

    private static final Path REPORT_DIRECTORY_PATH = Paths.get(System.getProperty("user.dir"), "reports");
    private static final Path HTML_REPORT_PATH = REPORT_DIRECTORY_PATH.resolve("html_report.html");

    public static void main(String[] args) throws ClientApiException {
        ClientApi clientApi = new ClientApi(ZAP_ADDRESS, ZAP_PORT, ZAP_API_KEY);

        try {
            ProcessBuilder pb = new ProcessBuilder("/Applications/OWASP ZAP.app/Contents/Java/zap.sh", "-daemon", "-config", "api.disablekey=true");
            pb.start();
            System.out.println("ZAP is starting");
            int statusCode = 0;
            while (statusCode != 200) {
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(String.format("http://%s:%d", ZAP_ADDRESS.replaceAll("(.*)/$", "$1"), ZAP_PORT));
                try {
                    try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
                        statusCode = httpResponse.getStatusLine().getStatusCode();
                    }
                } catch (ConnectException e) {
                    // ignore
                }
            }
            System.out.println("ZAP has started");

            System.out.println("Spider: " + TARGET);
            ApiResponse spiderResponse = clientApi.spider.scan(TARGET, null, null, null, null);
            String spiderScanId = ((ApiResponseElement) spiderResponse).getValue();
            int spiderProgress = 0;
            while (spiderProgress < 100) {
                Thread.sleep(1000);
                spiderProgress = Integer.parseInt(((ApiResponseElement) clientApi.spider.status(spiderScanId)).getValue());
                System.out.printf("Spider progress: %d%%%n", spiderProgress);
            }
            Thread.sleep(2000); // allow the passive scan of the spider to complete
            System.out.println("Spider finished");

            System.out.println("Active scan: " + TARGET);
            ApiResponse activeScanResponse = clientApi.ascan.scan(TARGET, "True", "False", null, null, null);
            String activeScanId = ((ApiResponseElement) activeScanResponse).getValue();
            int activeScanProgress = 0;
            while (activeScanProgress < 100) {
                Thread.sleep(1000);
                activeScanProgress = Integer.parseInt(((ApiResponseElement) clientApi.ascan.status(activeScanId)).getValue());
                System.out.printf("Active scan progress: %d%%%n", activeScanProgress);
            }
            System.out.println("Active scan finished");

            String htmlReportContent = new String(clientApi.core.htmlreport(), StandardCharsets.UTF_8);
            File reportDirectory = new File(REPORT_DIRECTORY_PATH.toString());
            if (!reportDirectory.exists()) {
                boolean successfullyCreated = reportDirectory.mkdirs();
                if (!successfullyCreated) {
                    System.out.printf("This directory is not successfully created: %s%n", reportDirectory.toString());
                }
            }
            FileWriter writer = new FileWriter(HTML_REPORT_PATH.toString());
            writer.write(htmlReportContent);
            writer.close();
            System.out.printf("HTML report: %s%s%n", System.getProperty("user.dir"), HTML_REPORT_PATH.toString());

        } catch (ClientApiException | InterruptedException | IOException e) {
            e.printStackTrace();

        } finally {
            clientApi.core.shutdown();
            System.out.println("ZAP has been shut down");
        }
    }
}
