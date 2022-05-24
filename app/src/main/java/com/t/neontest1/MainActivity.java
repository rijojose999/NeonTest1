package com.t.neontest1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public static String test = "error";
    EditText editText;
    Button fetchButton, clearButton;
    String videoId;
    TextView json_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.edit_text);
        fetchButton = findViewById(R.id.fetch_button);
        clearButton = findViewById(R.id.clear_button);
        json_text = findViewById(R.id.json_text);

        fetchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoId = editText.getText().toString();
                if (videoId.equals("")){
                    editText.setError("Please Enter a valid video id");
                }else if (!isOnline()){
                    json_text.setText(R.string.no_internet);
                }else {
                    JSONObject jsonObject = grab(videoId);
                    if (jsonObject==null){
                        json_text.setText(R.string.error_mssg);
                        return;
                    }
                    String jsonString = jsonObject.toString();
                    jsonString = jsonString.trim();
                    if (!jsonString.equals("")){
                        json_text.setText(jsonString);
                    }else {
                        json_text.setText(R.string.error_mssg);
                        Log.d("TAG", "onCreate: Error");
                    }
                }
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText.getText().clear();
                json_text.setText("");
            }
        });

    }

    public JSONObject grab(String video_id){
        final JSONObject[] videoJson = new JSONObject[1];
        String url = "https://www.youtube.com/watch?v=" + video_id;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        // added to wait for the json fetching to complete and return it in the same function
        CountDownLatch countDownLatch = new CountDownLatch(1);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Error:", e.toString());
                videoJson[0] = new JSONObject();
                countDownLatch.countDown();
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String html_data = Objects.requireNonNull(response.body()).string();
                    checkValidId(html_data);

                    if (checkValidId(html_data)) {
                        videoJson[0] = new JSONObject();
                        try {
                            videoJson[0].put("title", getVideoTitle(html_data));
                            videoJson[0].put("views", getVideoViews(html_data));
                            videoJson[0].put("channel_name", getChannelName(html_data));
                            videoJson[0].put("channel_subscribers", getSubscribers(html_data));

                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }else {
                        Log.d("MainActivity", "onResponse: Invalid video id");
                    }

                }else{
                    Log.d("MainActivity", "onResponse: Something went wrong. Please try again");
                }

                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return videoJson[0];
    }

    public String getVideoTitle(String html_data){
        String videoTitle = "" ;
        final Pattern pattern = Pattern.compile("\"title\" content=\"(.+?)\">", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(html_data);
        boolean b = matcher.find();

        if (b){
            videoTitle = matcher.group(1);
        }
        return videoTitle;
    }
    public String getVideoViews(String html_data){
        String videoTitle = "" ;
        final Pattern pattern = Pattern.compile("\"viewCount\":\"(.+?)\",\"author\"", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(html_data);
        boolean b = matcher.find();

        if (b){
            videoTitle = matcher.group(1);
        }
        return videoTitle;
    }
    public String getChannelName(String html_data){
        String videoTitle = "" ;
        final Pattern pattern = Pattern.compile("\"author\":\"(.+?)\",\"", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(html_data);
        boolean b = matcher.find();

        if (b){
            videoTitle = matcher.group(1);
        }
        return videoTitle;
    }
    public String getSubscribers(String html_data){
        String videoTitle = "" ;
        final Pattern pattern = Pattern.compile("\"subscriberCountText\":\\{\"accessibility\":\\{\"accessibilityData\":\\{\"label\":\"(.+?)\\ssubscribers\"", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(html_data);
        boolean b = matcher.find();

        if (b){
            videoTitle = matcher.group(1);
        }
        return videoTitle;
    }
    public boolean checkValidId(String html_data){
        final Pattern pattern = Pattern.compile("\\{\"backgroundPromoRenderer\":\\{\"title\":\\{\"runs\":\\[\\{\"text\":\"(.+?)\"", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(html_data);
        boolean b = matcher.find();

        return !b;
    }

    public boolean isOnline() {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager)
                getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetworkInfo != null &&
                activeNetworkInfo.isConnectedOrConnecting();
    }
}