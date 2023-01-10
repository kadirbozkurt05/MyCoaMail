package com.kadirbozkurt.postchecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class UpdateStatus extends Worker {
    private String vNum;
    private SharedPreferences sharedPreferences;
    private Context context;
    private boolean notificationSent;
    private NotificationManagerCompat notificationManagerCompat;
    private Notification notification;
    public UpdateStatus(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;

    }

    @NonNull
    @Override
    public Result doWork() {
        Data data = getInputData();
        vNum = data.getString("vNum");
        updateStatus();
        return Result.success();
    }

    private void updateStatus(){

        GetUpdate getUpdate = new GetUpdate();
        getUpdate.execute();

    }

    private class GetUpdate extends AsyncTask<Void, Void, Void> {
        public Elements element;
        @Override
        protected Void doInBackground(Void... voids) {
            sharedPreferences = context.getSharedPreferences("com.kadirbozkurt.postchecker",Context.MODE_PRIVATE);
            notificationSent = sharedPreferences.getBoolean("notify",true);
            vNum = sharedPreferences.getString("vNum","");
            Document document = null;
            try {
                document = Jsoup.connect("https://www.mycoa.nl/tr/content/posta?field_post_v_nummer_value="+vNum+"&submit_me=1").get();
                element = document.getElementsByAttributeValue("alt","Post");//No Post, Post
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            if (element.size()>0){

                if (notificationSent){
                    sendNotification("COA MAIL", (String) context.getText(R.string.have_mail));
                    notificationSent = false;
                    sharedPreferences.edit().putBoolean("notify",notificationSent).commit();
                }

            }else {
                notificationSent = true;
                sharedPreferences.edit().putBoolean("notify",notificationSent).commit();
            }


        }
    }

    private void sendNotification(String title,String text){
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel("not","Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,"not")
                .setSmallIcon(R.drawable.fullmailbox)
                .setContentTitle(title)
                .setContentText(text);
        notification = builder.build();
        notificationManagerCompat = NotificationManagerCompat.from(context);

        notificationManagerCompat.notify(1,notification);
    }
}
