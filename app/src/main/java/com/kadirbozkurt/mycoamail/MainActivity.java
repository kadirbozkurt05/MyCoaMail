package com.kadirbozkurt.mycoamail;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private EditText vNumEditText;
    private ProgressBar progressBar;
    private String vNum;
    private Dialog vNumDialog;
    private ImageView vNumSendButton;
    private View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        textView.setVisibility(View.GONE);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        vNumDialog = new Dialog(this);
        openDialog();


    }

    protected class MailPage extends AsyncTask<Void, Void, Void> {
            public Elements element;
            @Override
            protected Void doInBackground(Void... voids) {
                Document document = null;
                try {
                    document = Jsoup.connect("https://www.mycoa.nl/tr/content/posta?field_post_v_nummer_value="+vNum+"&submit_me=1").get();
                    element = document.getElementsByAttributeValue("alt","No post");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                textView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                super.onPostExecute(unused);
                if (element==null){
                    textView.setText("Yo have a mail!");
                }else{
                    textView.setText("You don't have a mail!");
                }

            }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);

        }
    }
    private void openDialog(){
        vNumDialog.setContentView(R.layout.vnum_alert);
        vNumDialog.setCancelable(false);
        vNumEditText = vNumDialog.findViewById(R.id.vNumEditText);
        vNumSendButton = vNumDialog.findViewById(R.id.vNumSendButton);
        vNumDialog.show();

        vNumSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vNum = vNumEditText.getText().toString();
                if (vNum.length()<10){
                    Snackbar.make(findViewById(android.R.id.content).getRootView(),"V-Number must be 10 digits!",Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                        }
                    }).show();
                }else{
                vNumDialog.dismiss();
                MailPage mailPage = new MailPage();
                mailPage.execute();
                }
            }
        });


    }
}