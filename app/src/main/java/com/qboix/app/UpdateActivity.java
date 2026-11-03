package com.qboix.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateActivity extends Activity {
    private final int GOLD=Color.rgb(215,181,91), BLUE=Color.rgb(52,136,255),
            GREEN=Color.rgb(55,190,125), WHITE=Color.WHITE, GREY=Color.rgb(165,165,175);
    private TextView status, version;
    private Button checkButton;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private String apkUrl;
    private File downloadedApk;
    private static final int INSTALL_PERMISSION_REQUEST=81;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(7,10,18));
        getWindow().setNavigationBarColor(Color.BLACK);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(24),dp(20),dp(24));
        root.setBackgroundColor(Color.rgb(8,10,16));
        ScrollView scroll=new ScrollView(this);
        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        TextView brand=label("Q-BOIX",GOLD,25,true);
        content.addView(brand);
        TextView heading=label("APP UPDATE",WHITE,23,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(22);
        content.addView(heading,hp);
        TextView intro=label("Check GitHub for the latest Q-BOIX release. Your workouts and preferences stay on this phone during a normal in-place update.",GREY,13,false);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);ip.topMargin=dp(8);
        content.addView(intro,ip);
        LinearLayout panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18),dp(18),dp(18),dp(18));
        panel.setBackground(card());
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.topMargin=dp(20);
        content.addView(panel,pp);
        panel.addView(label("INSTALLED VERSION",GOLD,12,true));
        version=label("Version "+installedVersion(),WHITE,18,true);
        LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,-2);vp.topMargin=dp(6);panel.addView(version,vp);
        status=label("Ready to check for updates.",GREY,13,false);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(18);panel.addView(status,sp);
        checkButton=new Button(this);checkButton.setText("CHECK FOR UPDATE");checkButton.setAllCaps(false);
        checkButton.setTextColor(Color.BLACK);checkButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(52));bp.topMargin=dp(18);panel.addView(checkButton,bp);
        checkButton.setOnClickListener(v->checkForUpdate());
        Button back=new Button(this);back.setText("BACK TO Q-BOIX");back.setAllCaps(false);
        back.setOnClickListener(v->finish());
        LinearLayout.LayoutParams backp=new LinearLayout.LayoutParams(-1,dp(48));backp.topMargin=dp(12);content.addView(back,backp);
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private String installedVersion(){
        try { PackageInfo p=getPackageManager().getPackageInfo(getPackageName(),0);return p.versionName==null?"0":p.versionName; }
        catch(Exception e){return "unknown";}
    }
    private GradientDrawable card(){GradientDrawable d=new GradientDrawable();d.setColor(Color.rgb(27,27,34));d.setCornerRadius(dp(18));d.setStroke(dp(1),Color.rgb(55,65,78));return d;}
    private TextView label(String value,int color,float size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextColor(color);t.setTextSize(size);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}

    private void checkForUpdate(){
        checkButton.setEnabled(false);status.setText("Checking the latest GitHub release…");
        worker.execute(()->{
            HttpURLConnection connection=null;
            try{
                URL url=new URL("https://api.github.com/repos/kipchumbangenoh90-beep/QBOIX/releases/latest");
                connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept","application/vnd.github+json");connection.setRequestProperty("User-Agent","Q-BOIX-Android");
                int code=connection.getResponseCode();
                if(code==404)throw new Exception("No published GitHub release is available yet.");
                if(code<200||code>=300)throw new Exception("GitHub check failed (HTTP "+code+").");
                InputStream in=connection.getInputStream();java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
                byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);in.close();
                JSONObject release=new JSONObject(bytes.toString("UTF-8"));
                String tag=release.optString("tag_name","").trim();
                JSONArray assets=release.optJSONArray("assets");
                String found=null;
                if(assets!=null)for(int i=0;i<assets.length();i++){JSONObject asset=assets.optJSONObject(i);if(asset==null)continue;String name=asset.optString("name","");if(name.toLowerCase(Locale.ROOT).endsWith(".apk")){found=asset.optString("browser_download_url",null);if(found!=null&&!found.isEmpty())break;}}
                final String download=found;
                final String latest=tag.isEmpty()?release.optString("name","Latest release"):tag;
                runOnUiThread(()->{
                    checkButton.setEnabled(true);
                    if(download==null){status.setText("A GitHub release exists ("+latest+"), but it has no APK asset yet. No update can be installed.");return;}
                    apkUrl=download;
                    if(compareVersions(latest,installedVersion())<=0){status.setText("No update yet. Installed version "+installedVersion()+" is up to date with release "+latest+".");}
                    else{
                        status.setText("New version available: "+latest+".\n\nRelease notes:\n"+release.optString("body","No release notes provided."));
                        checkButton.setText("DOWNLOAD AND INSTALL "+latest);
                        checkButton.setOnClickListener(v->downloadAndInstall());
                    }
                });
            }catch(Exception e){runOnUiThread(()->{checkButton.setEnabled(true);status.setText(e.getMessage()==null?"Could not check GitHub. Check your internet connection.":e.getMessage());});}
            finally{if(connection!=null)connection.disconnect();}
        });
    }

    private int compareVersions(String remote,String local){
        String a=remote.replaceFirst("^[vV]","").split("[-+]",2)[0];
        String b=local.replaceFirst("^[vV]","").split("[-+]",2)[0];
        String[] aa=a.split("\\."),bb=b.split("\\.");
        try{for(int i=0;i<Math.max(aa.length,bb.length);i++){int x=i<aa.length?Integer.parseInt(aa[i].replaceAll("[^0-9].*$","")):0;int y=i<bb.length?Integer.parseInt(bb[i].replaceAll("[^0-9].*$","")):0;if(x!=y)return Integer.compare(x,y);}}catch(Exception ignored){return remote.equals(local)?0:1;}
        return 0;
    }

    private void downloadAndInstall(){
        if(apkUrl==null){checkForUpdate();return;}
        checkButton.setEnabled(false);status.setText("Downloading the update APK…");
        worker.execute(()->{
            HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL(apkUrl).openConnection();connection.setConnectTimeout(20000);connection.setReadTimeout(30000);
                connection.setRequestProperty("User-Agent","Q-BOIX-Android");
                if(connection.getResponseCode()<200||connection.getResponseCode()>=300)throw new Exception("APK download failed.");
                File dir=getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);if(dir==null)throw new Exception("Phone storage is unavailable.");
                if(!dir.exists()&&!dir.mkdirs())throw new Exception("Could not create the update folder.");
                File target=new File(dir,"qboix-update.apk");
                try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(target)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);out.getFD().sync();}
                downloadedApk=target;
                runOnUiThread(()->{status.setText("Download complete. Opening Android's secure installer…");installDownloadedApk();});
            }catch(Exception e){runOnUiThread(()->{checkButton.setEnabled(true);status.setText(e.getMessage()==null?"Could not download update.":e.getMessage());});}
            finally{if(connection!=null)connection.disconnect();}
        });
    }

    private void installDownloadedApk(){
        if(downloadedApk==null||!downloadedApk.exists()){status.setText("Downloaded APK could not be found.");checkButton.setEnabled(true);return;}
        if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
            Intent permission=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()));
            startActivityForResult(permission,INSTALL_PERMISSION_REQUEST);return;
        }
        try{
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",downloadedApk);
            Intent install=new Intent(Intent.ACTION_VIEW);install.setDataAndType(uri,"application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
            status.setText("Android installer opened. Confirm the update there. Do not uninstall Q-BOIX.");
        }catch(Exception e){status.setText("Could not open Android installer: "+e.getMessage());checkButton.setEnabled(true);}
    }

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==INSTALL_PERMISSION_REQUEST){if(Build.VERSION.SDK_INT<26||getPackageManager().canRequestPackageInstalls())installDownloadedApk();else{status.setText("Allow Q-BOIX to install updates in Android settings, then return here.");checkButton.setEnabled(true);}}
    }
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
