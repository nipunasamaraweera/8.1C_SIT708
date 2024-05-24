package com.example.chatbot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge content
        EdgeToEdge.enable(this);
        // Set the layout resource for this activity
        setContentView(R.layout.activity_main);

        // Get the EditText view by its ID
        EditText editText2 = findViewById(R.id.editText2);
        // Set an OnClickListener on the button to handle click events
        findViewById(R.id.btn_go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a new intent to start the ChatWindow activity
                Intent i = new Intent(MainActivity.this, ChatWindow.class);
                // Put the text from the EditText into the intent as an extra
                i.putExtra("name", editText2.getText().toString());
                // Start the ChatWindow activity
                startActivity(i);
            }
        });
    }
}
