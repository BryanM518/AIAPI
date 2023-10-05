package com.example.openaiapi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.textclassifier.TextLinks;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import domain.model.Message;
import domain.model.MessageAdapter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView lblMessage = null;
    private RecyclerView recyclerView = null;
    private EditText txtMessage = null;
    private Button btnSearch = null;
    private List<Message> messageList;
    private MessageAdapter messageAdapter;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf8");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageList = new ArrayList<>();

        initViews();
        initEvents();
    }

    public void initViews(){
        lblMessage = findViewById(R.id.lblMessage);
        txtMessage = findViewById(R.id.editMessage);
        btnSearch = findViewById(R.id.btnSearch);
        recyclerView = findViewById(R.id.recycler_view);
    }

    public void initEvents(){

        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        btnSearch.setOnClickListener( (v) -> {
            txtMessage.setEnabled(false);
            btnSearch.setEnabled(false);
            String question = txtMessage.getText().toString().trim();
            addToChat(question, Message.SENT_BY_ME);
            txtMessage.setText("");
            callAPI(question);
        });

    }

    void addToChat(String message, String sentBy){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message, sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
                txtMessage.setEnabled(true);
                btnSearch.setEnabled(true);
            }
        });
    }

    void addResponse(String response){
        addToChat(response, Message.SENT_BY_BOT);
    }

    void callAPI(String question){
        JSONObject jsonBody = new JSONObject();

        JSONArray array = new JSONArray();

        JSONObject object = new JSONObject();

        try {
            object.put("role", "user");
            object.put("content", question);
            array.put(object);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        try {
            jsonBody.put("model", "gpt-3.5-turbo");
            System.out.println("Array creado: " + array);
            jsonBody.put("messages", array);
            jsonBody.put("temperature", 0.7);
            /*
            "model": "gpt-3.5-turbo",
            "messages": [{"role": "user", "content": "Say this is a test!"}]
                        [{"role": "user", "content": "Hola"}]
            "temperature": 0.7
            */
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer")
                .post(body)
                .build();

        System.out.println("La request es: " + request);
        System.out.println("el body es: " + jsonBody.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to 1 " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()){
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        System.out.println("jsonArray obtenido: " + jsonArray.getJSONObject(0));
                        String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }else{
                    addResponse("Failed to load response due to 2 " + response.body().string());
                }

            }
        });
    }
}