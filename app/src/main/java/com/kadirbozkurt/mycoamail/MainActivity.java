package com.kadirbozkurt.mycoamail;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.snackbar.Snackbar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
//banner ad : ca-app-pub-8301793912429911/4443825316
//intersititial ad: ca-app-pub-8301793912429911/8053620032

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private EditText vNumEditText;
    private EditText customTime;
    private ProgressBar progressBar;
    private String vNum;
    private Dialog vNumDialog;
    private ImageView vNumSendButton;
    private int timeToRefresh;
    private SharedPreferences sharedPreferences;
    private ImageView mailBox;
    private ImageView changeVnumButton;
    private ImageView changeUpdateTimeButton;
    private ImageView refreshButton;
    private AdView mAdView;
    AdRequest adRequest;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //loadAd();
        //bannerAd();
        changeVnumButton = findViewById(R.id.changeVnumButton);
        changeUpdateTimeButton = findViewById(R.id.changeUpdateTimeButton);
        refreshButton = findViewById(R.id.refreshButton);
        sharedPreferences = this.getSharedPreferences("com.kadirbozkurt.mycoamail", Context.MODE_PRIVATE);
        vNum = sharedPreferences.getString("vNum","");
        timeToRefresh = sharedPreferences.getInt("timeToRefresh",60);
        mailBox = findViewById(R.id.mailBox);
        textView = findViewById(R.id.textView);
        textView.setVisibility(View.GONE);
        mailBox.setVisibility(View.INVISIBLE);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        vNumDialog = new Dialog(this);
        openDialog();


    }


    protected class MailPage extends AsyncTask<Void, Void, Void> {
            public Elements element= null;
            @Override
            protected Void doInBackground(Void... voids) {
                Document document = null;
                try {
                    document = Jsoup.connect("https://www.mycoa.nl/tr/content/posta?field_post_v_nummer_value="+vNum+"&submit_me=1").get();
                    element = document.getElementsByAttributeValue("alt","Post");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                //showAd();
                textView.setVisibility(View.VISIBLE);
                refreshButton.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                mailBox.setVisibility(View.VISIBLE);
                changeUpdateTimeButton.setVisibility(View.VISIBLE);
                changeVnumButton.setVisibility(View.VISIBLE);
                if(element.size()==0){
                    textView.setText("You don't have a mail!");
                }else{
                    textView.setText("You have a mail!");
                    mailBox.setImageResource(R.drawable.fullmailbox);
                }

            }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //loadAd();
            progressBar.setVisibility(View.VISIBLE);
            mailBox.setVisibility(View.INVISIBLE);

        }
    }
    private void openDialog(){
        if (vNum.isEmpty()){
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
                            ignoreBatteryOptimization();


                        sharedPreferences.edit().putString("vNum",vNum).commit();
                        vNumDialog.dismiss();
                        MailPage mailPage = new MailPage();
                        mailPage.execute();
                        executeWorkManager();

                    }
                }
            });
        }else {
            vNumDialog.dismiss();
            MailPage mailPage = new MailPage();
            mailPage.execute();
            executeWorkManager();

        }
    }
    private void executeWorkManager(){

        @SuppressLint("RestrictedApi") Data data = new Data.Builder().put("vNum",vNum).build();
        Constraints constraints  = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build();

        WorkRequest workRequest = new PeriodicWorkRequest.Builder(UpdateStatus.class,timeToRefresh, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(data).build();
        WorkManager.getInstance(MainActivity.this).enqueue(workRequest);
    }
    public void changeVnum(View v){
        AlertDialog.Builder alert =new AlertDialog.Builder(MainActivity.this);
        alert.setMessage("Your previous V-Number will be deleted. Do you want to change your V-Number?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        vNum ="";
                        sharedPreferences.edit().putString("vNum","").commit();
                        openDialog();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).show();
    }
    public void selectUpdateTime(View v) {
        Dialog timeDialog = new Dialog(MainActivity.this);
        timeDialog.setContentView(R.layout.update_time_alert);
        timeDialog.setCancelable(false);

        customTime = timeDialog.findViewById(R.id.customTime);
        RadioGroup radioGroup = timeDialog.findViewById(R.id.timeRadioGroup);
        RadioButton minutes15 = timeDialog.findViewById(R.id.minutes15);
        RadioButton minutes30 = timeDialog.findViewById(R.id.minutes30);
        RadioButton minutes45 = timeDialog.findViewById(R.id.minutes45);
        RadioButton never = timeDialog.findViewById(R.id.never);
        ImageView approveTimeButton = timeDialog.findViewById(R.id.timeApproveButton);
        timeDialog.show();

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (minutes15.isChecked()){
                    timeToRefresh = 15;
                }else if (minutes30.isChecked()){
                    timeToRefresh = 30;
                }else if (minutes45.isChecked()){
                    timeToRefresh = 45;
                }else if (never.isChecked()){
                    timeToRefresh = Integer.MAX_VALUE;
                }
            }
        });

        customTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                radioGroup.clearCheck();
            }
        });
        

        approveTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!customTime.getText().toString().isEmpty()){
                    timeToRefresh = Integer.parseInt(customTime.getText().toString());
                }
                sharedPreferences.edit().putInt("timeToRefresh",timeToRefresh).commit();

                timeDialog.dismiss();
            }
        });
    }
    public void refresh(View v){
        refreshButton.setVisibility(View.INVISIBLE);
        changeVnumButton.setVisibility(View.INVISIBLE);
        textView.setVisibility(View.INVISIBLE);
        changeUpdateTimeButton.setVisibility(View.INVISIBLE);
        MailPage mailPage = new MailPage();
        mailPage.execute();
        executeWorkManager();
    }
    public void loadAd(){
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this,"ca-app-pub-8301793912429911/8053620032", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        fulscrn();
                        //Log.i(TAG, "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        //Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;
                    }
                });


    }
    public void showAd(){
        if (mInterstitialAd != null) {
            mInterstitialAd.show(MainActivity.this);
        } else {
            // Log.d("TAG", "The interstitial ad wasn't ready yet.");
        }
    }
    public void fulscrn(){
        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                // Called when a click is recorded for an ad.
                //Log.d(TAG, "Ad was clicked.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                //Log.d(TAG, "Ad dismissed fullscreen content.");
                mInterstitialAd = null;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // Called when ad fails to show.
                //Log.e(TAG, "Ad failed to show fullscreen content.");
                mInterstitialAd = null;
            }

            @Override
            public void onAdImpression() {
                // Called when an impression is recorded for an ad.
                //Log.d(TAG, "Ad recorded an impression.");
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when ad is shown.
                //Log.d(TAG, "Ad showed fullscreen content.");
            }
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void ignoreBatteryOptimization() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("You must allow permission this app to check your posts in background. Otherwise" +
                "you will not get notification even if you received a post!");
        alert.setNegativeButton("Don't allow", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent();
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }
            }
        });
        alert.show();



    }
    public void runBackGround(View v){
        ignoreBatteryOptimization();
    }
}
