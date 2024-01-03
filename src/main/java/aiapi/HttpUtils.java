package aiapi;

import com.google.gson.Gson;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;

import java.util.Map;

public class HttpUtils {

    private final Gson gson;
    public HttpUtils() {
        gson = new Gson();
    }

    public String getLlmResponse(String url, String inputText, int maxTokens) {

        String llmResponse = null;
        try {

            String inputString = "[INST] " + inputText + " [/INST]";
            String requestString = "{\"inputs\":\"" + inputString + "\",\"parameters\":{\"max_new_tokens\":" + maxTokens + "}}";
            HttpClient client = new HttpClient();
            client.setFollowRedirects(false);
            client.start();

            Request request = client.POST(url);
            request.header(HttpHeader.CONTENT_TYPE, "application/json");

            request.content(new StringContentProvider(requestString,"utf-8"));
            ContentResponse response = request.send();
            llmResponse = String.valueOf(gson.fromJson(response.getContentAsString(), Map.class).get("generated_text"));
            client.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return llmResponse;
    }

}
