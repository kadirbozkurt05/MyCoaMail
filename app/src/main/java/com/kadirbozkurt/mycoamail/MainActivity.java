package com.kadirbozkurt.mycoamail;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.kadirbozkurt.mycoamail.databinding.ActivityMainBinding;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    //List of autostart packages for Chinese Rom
    public static List<Intent> POWER_MANAGER_INTENTS = Arrays.asList(
            new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            new Intent().setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
            new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")).setData(android.net.Uri.parse("mobilemanager://function/entry/AutoStart"))
    );

    private ActivityMainBinding binding;
    private String vNum;
    private Dialog vNumDialog;
    private int timeToRefresh;
    private SharedPreferences sharedPreferences;
    private MailPage mailPage;
    private String latestVersion;
    private String versionCode;

    //Dropdown Menu icin
    AutoCompleteTextView autoCompleteTextView;
    TextView dropdownControl;

    //Animasyonlar icin
    AnimationDrawable mailAnimation;


    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sharedPreferences = this.getSharedPreferences("com.kadirbozkurt.mycoamail", Context.MODE_PRIVATE);
        //After every 5 run, check for the update
        int day = sharedPreferences.getInt("day",0);
        System.out.println("DAY :"+day);
        sharedPreferences.edit().putInt("day",day+1).commit();
        if (day>4){
            checkVersion();
            sharedPreferences.edit().putInt("day",0).commit();
        }


        //get the v-num and duration time if they are already stored

        vNum = sharedPreferences.getString("vNum","");
        timeToRefresh = sharedPreferences.getInt("timeToRefresh",60);

        //Because it is checking the posts at that point set text and hide image
        binding.textView.setText("Your posts are being checked...");
        ImageView animationHolder = findViewById(R.id.animationHolder);
        animationHolder.setBackgroundResource(R.drawable.animation_searching);
        mailAnimation = (AnimationDrawable) animationHolder.getBackground();
        mailAnimation.start();
        vNumDialog = new Dialog(this); //Dialog for asking v-num at the beginning and changing

        openDialog();

        //  ↓   ↓   ↓   ↓   Dropdown Menu   ↓   ↓   ↓   ↓

        //Dropdown Menu icini doldurmak Icin
        autoCompleteTextView = findViewById(R.id.drop_items);
        dropdownControl = findViewById(R.id.itemSelected);

        String [] items = {getString(R.string.english_flag), getString(R.string.spanish_flag), getString(R.string.turkish_flag)};
        ArrayAdapter<String> itemAdapter= new ArrayAdapter<>(MainActivity.this, R.layout.items_list, items);
        autoCompleteTextView.setAdapter(itemAdapter);

        //Secilen dile gore duzenleme yapmak icin
        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                switch(String.valueOf(position)) {
                    case "0":
                        dropdownControl.setText("Ingilzice secildi");
                        break;
                    case "1":
                        dropdownControl.setText("Ispanyolca secildi");
                        break;
                    case "2":
                        dropdownControl.setText("Turkce secildi");
                        break;
                    default:
                        dropdownControl.setText((String)parent.getItemAtPosition(position));
                }
            }
        });

        //  ↑   ↑   ↑   ↑   Dropdown Menu   ↑   ↑   ↑   ↑

        //  ↓   ↓   ↓   ↓   Animasyonlar   ↓   ↓   ↓   ↓


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
                binding.progressBar.setVisibility(View.GONE);
                binding.refreshButton.setVisibility(View.VISIBLE);
                binding.changeUpdateTimeButton.setEnabled(true);
                binding.changeVnumButton.setEnabled(true);
                if(element.size()==0){
                    binding.textView.setText("You don't have a mail!");
                    ImageView animationHolder = findViewById(R.id.animationHolder);
                    animationHolder.setBackgroundResource(R.drawable.animation_no_mail);
                    mailAnimation = (AnimationDrawable) animationHolder.getBackground();
                    mailAnimation.start();
                }else{
                    binding.textView.setText("You have a mail!");
                    ImageView animationHolder = findViewById(R.id.animationHolder);
                    animationHolder.setBackgroundResource(R.drawable.animation_mail_found);
                    mailAnimation = (AnimationDrawable) animationHolder.getBackground();
                    mailAnimation.start();
                }

            }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.refreshButton.setVisibility(View.INVISIBLE);

        }
    }
    private void openDialog(){
        if (vNum.isEmpty()){//if it is the first time of running the app or user clicked the change v-num button
            vNumDialog.setContentView(R.layout.vnum_alert); //get view of dialog
            vNumDialog.setCancelable(false);
            EditText vNumEditText = vNumDialog.findViewById(R.id.vNumEditText);
            Button vNumSendButton = vNumDialog.findViewById(R.id.vNumSendButton);
            vNumDialog.show();

            vNumSendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    vNum = vNumEditText.getText().toString();
                    if (vNum.length()!=10){
                        Snackbar.make(findViewById(android.R.id.content).getRootView(),"V-Number must be 10 digits!",Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                            }
                        }).show();
                    }else{
                        ignoreBatteryOptimization(); //if user provides his 10 digit v-num, ask for permissions
                        sharedPreferences.edit().putString("vNum",vNum).commit(); // store the v-num
                        vNumDialog.dismiss();
                        binding.textView.setText("Your posts are being checked...");
                        ImageView animationHolder = findViewById(R.id.animationHolder);
                        animationHolder.setBackgroundResource(R.drawable.animation_searching);
                        mailAnimation = (AnimationDrawable) animationHolder.getBackground();
                        mailAnimation.start();
                        MailPage mailPage = new MailPage(); //create Mail page object to start background task
                        mailPage.execute(); // start background task
                        executeWorkManager();
                    }
                }
            });
        }else {
            vNumDialog.dismiss();
            mailPage=new MailPage();
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

        EditText customTime = timeDialog.findViewById(R.id.customTime);
        RadioGroup radioGroup = timeDialog.findViewById(R.id.timeRadioGroup);
        RadioButton minutes15 = timeDialog.findViewById(R.id.minutes15);
        RadioButton minutes30 = timeDialog.findViewById(R.id.minutes30);
        RadioButton minutes45 = timeDialog.findViewById(R.id.minutes45);
        RadioButton never = timeDialog.findViewById(R.id.never);
        Button approveTimeButton = timeDialog.findViewById(R.id.timeApproveButton);
        timeDialog.show();

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                if (minutes15.isChecked()){
                    customTime.setText("");
                    customTime.clearFocus();
                    ignoreBatteryOptimization();
                    timeToRefresh = 15;
                }else if (minutes30.isChecked()){
                    customTime.setText("");
                    customTime.clearFocus();
                    ignoreBatteryOptimization();
                    timeToRefresh = 30;
                }else if (minutes45.isChecked()){
                    customTime.setText("");
                    customTime.clearFocus();
                    ignoreBatteryOptimization();
                    timeToRefresh = 45;
                }else if (never.isChecked()){
                    customTime.setText("");
                    customTime.clearFocus();
                    timeToRefresh = Integer.MAX_VALUE;
                }
            }
        });


        customTime.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b){
                    radioGroup.clearCheck();
                }

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
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.refreshButton.setVisibility(View.INVISIBLE);
        binding.changeVnumButton.setEnabled(false);
        binding.textView.setText("Your posts are being checked...");
        ImageView animationHolder = findViewById(R.id.animationHolder);
        animationHolder.setBackgroundResource(R.drawable.animation_searching);
        mailAnimation = (AnimationDrawable) animationHolder.getBackground();
        mailAnimation.start();
        binding.changeUpdateTimeButton.setEnabled(false);
        mailPage=new MailPage();
        mailPage.execute();
        executeWorkManager();
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void ignoreBatteryOptimization() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager!=null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())){
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
                        startPowerSaverIntent(MainActivity.this);
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                }
            });
            alert.show();
        }
    }
    public static void startPowerSaverIntent(Context context) {
        SharedPreferences settings = context.getSharedPreferences("ProtectedApps", Context.MODE_PRIVATE);
        boolean skipMessage = settings.getBoolean("skipProtectedAppCheck", false);
        if (!skipMessage) {
            final SharedPreferences.Editor editor = settings.edit();
            boolean foundCorrectIntent = false;
            for (Intent intent : POWER_MANAGER_INTENTS) {
                if (isCallable(context, intent)) {
                    foundCorrectIntent = true;
                    final AppCompatCheckBox dontShowAgain = new AppCompatCheckBox(context);
                    dontShowAgain.setText("Do not show again");
                    dontShowAgain.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        editor.putBoolean("skipProtectedAppCheck", isChecked);
                        editor.apply();
                    });

                    new AlertDialog.Builder(context)
                            .setTitle(Build.MANUFACTURER + " Protected Apps")
                            .setMessage(String.format("Please Click 'GO TO SETTINGS' and enable 'POST CHECKER' app for autostart.", context.getString(R.string.app_name)))
                            .setView(dontShowAgain)
                            .setPositiveButton("Go to settings", (dialog, which) -> context.startActivity(intent))
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    break;
                }
            }
            if (!foundCorrectIntent) {
                editor.putBoolean("skipProtectedAppCheck", true);
                editor.apply();
            }
        }
    }
    private static boolean isCallable(Context context, Intent intent) {
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void checkVersion(){
        // Get the package manager
        PackageManager packageManager = getPackageManager();

        try {
            // Get the package info
            PackageInfo packageInfo = packageManager.getPackageInfo(getOpPackageName(), 0);

            // Get the version code
            versionCode = packageInfo.versionCode+"";

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("General").document("version").addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                latestVersion = (String) value.getData().get("version");

                if (!latestVersion.equals(versionCode)){
                    //SHOW POP UP

                    Dialog dialog = new Dialog(MainActivity.this);
                    dialog.setContentView(R.layout.new_version_popup);
                    Button yesButton = dialog.findViewById(R.id.newVersionYesButton);
                    Button laterButton = dialog.findViewById(R.id.newVersionLaterButton);

                    laterButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });

                    yesButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String url = "https://play.google.com/store/apps/details?id=com.kadirbozkurt.mycoamail";
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(url));
                            startActivity(intent);
                        }
                    });


                    dialog.show();
                }
            }
        });


    }
}
