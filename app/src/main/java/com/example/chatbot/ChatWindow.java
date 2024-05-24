package com.example.chatbot;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class ChatWindow extends AppCompatActivity {

    // UI elements for input and sending messages
    EditText et_message_input;
    ImageView button_send;

    // Variables to hold the user's name and a list of messages
    String name;
    List<String[]> messageList;

    // RecyclerView for displaying the chat messages
    RecyclerView rv_chat;

    // JSON array to store the chat history
    JSONArray chatHistoryArray;

    // Adapter for managing the chat messages in the RecyclerView
    MessageAdapter msgAdapter;

    // Base URL of the server
    private static final String BASE_URL = "http://10.0.2.2:5000/";

    // Retrofit interface for sending chat messages
    interface ChatService {
        @POST("chat")
        Call<JsonObject> sendMessage(@Body JsonObject payload);
    }

    // Retrofit service instance
    private ChatService chatService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_window);

        // Initializing UI elements
        et_message_input = findViewById(R.id.et_message_input);
        et_message_input.setInputType(InputType.TYPE_NULL);
        button_send = findViewById(R.id.button_send);
        rv_chat = findViewById(R.id.rv_chat);

        // Get the user's name from the intent
        name = getIntent().getStringExtra("name");

        // Initialize chat history and message list
        chatHistoryArray = new JSONArray();
        messageList = new ArrayList<>();

        // Set up the RecyclerView with a linear layout manager and the message adapter
        msgAdapter = new MessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv_chat.setLayoutManager(layoutManager);
        rv_chat.setAdapter(msgAdapter);

        // Set up the HTTP logging interceptor for debugging
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Build the OkHttpClient with the logging interceptor
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .readTimeout(10, TimeUnit.MINUTES)
                .build();

        // Build the Retrofit instance with the Gson converter
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        // Create the chat service using Retrofit
        chatService = retrofit.create(ChatService.class);

        // Set the send button click listener
        button_send.setOnClickListener(v -> {
            String input = et_message_input.getText().toString();
            String[] newMsg = new String[]{input, "user"};
            msgAdapter.addItem(newMsg); // Add the user's message to the message list
            et_message_input.setText(""); // Clear the input field
            JSONObject payload = createPayload(input); // Create the JSON payload
            sendRequest(ChatWindow.this, payload, input); // Send the request to the server
        });
    }

    // Method to create the JSON payload for the request
    private JSONObject createPayload(String query) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("userMessage", query);
            payload.put("chatHistory", chatHistoryArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }

    // Method to send the request using Retrofit
    private void sendRequest(Context context, JSONObject payload, String query) {
        // Convert the JSONObject to a JsonObject
        JsonObject gsonPayload = new Gson().fromJson(payload.toString(), JsonObject.class);
        Call<JsonObject> call = chatService.sendMessage(gsonPayload);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject responseBody = response.body();
                        String aiResponse = responseBody.get("message").getAsString();
                        Log.d("ChatWindow", "aiResponse: " + aiResponse);
                        JSONObject chatHistoryItem = new JSONObject();
                        chatHistoryItem.put("User", query);
                        chatHistoryItem.put("Llama", aiResponse);
                        chatHistoryArray.put(chatHistoryItem);
                        String[] newMsg = new String[]{aiResponse, "ai"};
                        msgAdapter.addItem(newMsg); // Add the AI's response to the message list
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("ChatWindow", "Response unsuccessful: " + response.errorBody().toString());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("ChatWindow", "Request failed: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    // Adapter class for managing the chat messages in the RecyclerView
    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.TaskViewHolder> {

        List<String[]> messages;

        public MessageAdapter(List<String[]> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public MessageAdapter.TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new MessageAdapter.TaskViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageAdapter.TaskViewHolder holder, int position) {
            String[] msg = messages.get(position);
            holder.bind(msg);
        }

        public void addItem(String[] newItem) {
            messages.add(newItem);
            notifyItemInserted(messages.size() - 1);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        public class TaskViewHolder extends RecyclerView.ViewHolder {
            TextView tv_user_name, tv_message;
            ImageView img_ai;
            CardView cv_text;

            public TaskViewHolder(@NonNull View itemView) {
                super(itemView);
                img_ai = itemView.findViewById(R.id.img_ai);
                tv_user_name = itemView.findViewById(R.id.tv_user_name);
                tv_message = itemView.findViewById(R.id.tv_message);
                cv_text = itemView.findViewById(R.id.cv_text);
            }

            // Method to bind the message data to the UI elements
            public void bind(String[] msg) {
                tv_user_name.setText((name.charAt(0) + "").toUpperCase());
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) cv_text.getLayoutParams();
                if (msg[1].equals("ai")) {
                    img_ai.setVisibility(View.VISIBLE);
                    tv_user_name.setVisibility(View.INVISIBLE);
                } else {
                    img_ai.setVisibility(View.INVISIBLE);
                    tv_user_name.setVisibility(View.VISIBLE);
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
                    cv_text.setLayoutParams(params);
                }
                tv_message.setText(msg[0]);
            }
        }
    }
}
