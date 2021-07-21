package com.example.covid_19_watson_chatbot;

/**
 * Created by Bhavya Pratap Singh on 28/06/21.
 */

import android.content.Context;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.assistant.v2.Assistant;
import com.ibm.watson.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.assistant.v2.model.DialogNodeOutputOptionsElement;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.RuntimeResponseGeneric;
import com.ibm.watson.assistant.v2.model.SessionResponse;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


  private RecyclerView recyclerView;
  private ChatAdapter mAdapter;
  private ArrayList messageArrayList;
  private EditText inputMessage;
  private ImageButton btnSend;
  private boolean initialRequest;
  private static String TAG = "MainActivity";
  private Context mContext;
  private Assistant watsonAssistant;
  private Response<SessionResponse> watsonAssistantSession;


  private void createServices() {
    watsonAssistant = new Assistant("2019-02-28", new IamAuthenticator(mContext.getString(R.string.assistant_apikey)));
    watsonAssistant.setServiceUrl(mContext.getString(R.string.assistant_url));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mContext = getApplicationContext();

    inputMessage = findViewById(R.id.message);
    btnSend = findViewById(R.id.btn_send);
    String customFont = "Montserrat-Regular.ttf";
    Typeface typeface = Typeface.createFromAsset(getAssets(), customFont);
    inputMessage.setTypeface(typeface);
    recyclerView = findViewById(R.id.recycler_view);

    messageArrayList = new ArrayList<>();
    mAdapter = new ChatAdapter(messageArrayList);


    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setStackFromEnd(true);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(mAdapter);
    this.inputMessage.setText("");
    this.initialRequest = true;

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkInternetConnection()) {
                    sendMessage();
                }
            }
        });

        createServices();
        sendMessage();
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { switch(item.getItemId()) {
        case R.id.refresh:
            finish();
            startActivity(getIntent());

    }
        return(super.onOptionsItemSelected(item));
    }




    // Sending a message to Watson Assistant Service
    private void sendMessage() {

                        final String inputmessage = this.inputMessage.getText().toString().trim();
                        if (!this.initialRequest) {
                            Message inputMessage = new Message();
                            inputMessage.setMessage(inputmessage);
                            inputMessage.setId("1");
                            messageArrayList.add(inputMessage);
                        } else {
                            Message inputMessage = new Message();
                            inputMessage.setMessage(inputmessage);
                            inputMessage.setId("100");
                            this.initialRequest = false;

                        }

                        this.inputMessage.setText("");
                        mAdapter.notifyDataSetChanged();

                        Thread thread = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    if (watsonAssistantSession == null) {
                                        ServiceCall<SessionResponse> call = watsonAssistant.createSession(new CreateSessionOptions.Builder()
                                                .assistantId(mContext.getString(R.string.assistant_id)).build());
                                        watsonAssistantSession = call.execute();
                                    }

                                    MessageInput input = new MessageInput.Builder()
                                            .text(inputmessage)
                                            .build();
                                    MessageOptions options = new MessageOptions.Builder()
                                            .assistantId(mContext.getString(R.string.assistant_id))
                                            .input(input)
                                            .sessionId(watsonAssistantSession.getResult().getSessionId())
                                            .build();
                                    Response<MessageResponse> response = watsonAssistant.message(options).execute();
                                    Log.i(TAG, "run: " + response.getResult());
                                    if (response != null &&
                                            response.getResult().getOutput() != null &&
                                            !response.getResult().getOutput().getGeneric().isEmpty()) {

                                        List<RuntimeResponseGeneric> responses = response.getResult().getOutput().getGeneric();

                                        for (RuntimeResponseGeneric r : responses) {
                                            Message outMessage;
                                            switch (r.responseType()) {
                                                case "text":
                                                    outMessage = new Message();
                                                    outMessage.setMessage(r.text());
                                                    outMessage.setId("2");

                                                    messageArrayList.add(outMessage);

                                                    // speak the message
                                                    //new SayTask().execute(outMessage.getMessage());
                                                    break;

                                                case "option":
                                                    outMessage =new Message();
                                                    String title = r.title();
                                                    String OptionsOutput = "";
                                                    for (int i = 0; i < r.options().size(); i++) {
                                                        DialogNodeOutputOptionsElement option = r.options().get(i);
                                                        OptionsOutput = OptionsOutput + option.getLabel() +"\n";

                                                    }
                                                    outMessage.setMessage(title + "\n" + OptionsOutput);
                                                    outMessage.setId("2");

                                                    messageArrayList.add(outMessage);

                                                    // speak the message
                                                    //new SayTask().execute(outMessage.getMessage());
                                                    break;

                                                case "image":
                                                    outMessage = new Message(r);
                                                    messageArrayList.add(outMessage);

                                                    // speak the description
                                                    //new SayTask().execute("You received an image: " + outMessage.getTitle() + outMessage.getDescription());
                                                    break;
                                                default:
                                                    Log.e("Error", "Unhandled message type");
                                            }
                                        }

                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                mAdapter.notifyDataSetChanged();
                                                if (mAdapter.getItemCount() > 1) {
                                                    recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);

                                                }

                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        thread.start();
                    }



    private boolean checkInternetConnection() {
        // get Connectivity Manager object to check connection
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        // Check for network connections
        if (isConnected) {
            return true;
        } else {
            Toast.makeText(this, " No Internet Connection available ", Toast.LENGTH_LONG).show();
            return false;
        }

    }

    private void showError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }





}



