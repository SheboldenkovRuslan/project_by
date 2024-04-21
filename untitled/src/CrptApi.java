import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final HttpClient httpClient;
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.semaphore = new Semaphore(requestLimit, true);
        scheduleSemaphoreReplenishment(timeUnit, requestLimit);
    }

    private void scheduleSemaphoreReplenishment(TimeUnit timeUnit, int requestLimit) {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(timeUnit.toMillis(1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                semaphore.release(requestLimit);
            }
        }).start();
    }

    public HttpResponse<String> createDocument(Object document, String signature) throws IOException, InterruptedException {
        semaphore.acquire();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .POST(HttpRequest.BodyPublishers.ofString(document.toString()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + signature)
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);
        HttpResponse<String> response = null;
        try {
            response = api.createDocument(new Object(), "signature");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(response.body());
    }
}