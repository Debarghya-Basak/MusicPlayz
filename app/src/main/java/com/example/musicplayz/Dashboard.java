package com.example.musicplayz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Dashboard extends AppCompatActivity {
    SearchView musicSearch;
    RecyclerView musicItemRecyclerView;
    ArrayList<String> musicList = new ArrayList<>();
    ArrayList<String> musicName = new ArrayList<>();
    boolean searchedOnceFlag = false;
    boolean searchingStatusFlag = false;
    Context context;
    ImageButton refresh;
    TextView refreshStatusText;
    final int REQUEST_CODE_TIRAMISU = 101, REQUEST_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        //initialization
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        musicItemRecyclerView = findViewById(R.id.music_item_recyclerView);
        musicSearch = findViewById(R.id.music_searchView);
        context = this;
        refresh = (ImageButton) findViewById(R.id.music_refresh) ;
        refreshStatusText = (TextView) findViewById(R.id.refresh_status_textView);
        //initialization end

        checkPerm();

    }

    public void checkPerm(){

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                loadMusicListFromDirectoryThread();
            }
            else {
                ActivityCompat.requestPermissions(Dashboard.this, new String[] { Manifest.permission.READ_MEDIA_AUDIO }, REQUEST_CODE_TIRAMISU);

                Log.d("Debug","Dashboard : Permission not granted > android 10");
                Toast.makeText(this, "Permission not granted > android 10", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                loadMusicListFromDirectoryThread();
            }
            else {
                ActivityCompat.requestPermissions(Dashboard.this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, REQUEST_CODE);

                Log.d("Debug","Dashboard : Permission not granted < android 10");
                Toast.makeText(this, "Permission not granted < android 10", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_CODE_TIRAMISU:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(context, "Media Audio permission granted", Toast.LENGTH_SHORT).show();
                    loadMusicListFromDirectoryThread();
                }
                break;

            case REQUEST_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(context, "External storage permission granted", Toast.LENGTH_SHORT).show();
                    loadMusicListFromDirectoryThread();
                }
                break;
        }

    }

    public void loadMusicListFromDirectoryThread(){

        Thread search = new Thread() {
            @Override
            public void run() {
                setName("LoadMusicToListThread");
                try {
                    Dashboard.this.runOnUiThread(new Runnable() {
                        public void run() {

                            refreshStatusText.setText("Refreshing music... Please Wait");

                        }
                    });

                    searchingStatusFlag = true;

                    loadMusicListFromDirectory(Environment.getExternalStorageDirectory().toString(), 0);
                }
                catch (IOException e){}
            }
        };

        search.start();

        if(!searchedOnceFlag) {
            searchedOnceFlag = true;
            startSearchListener();
        }
    }

    public void loadMusicListFromDirectory(String path,long recCounter) throws IOException{

        File directory = new File(path);
        File files[] = directory.listFiles();

        if(files != null){
            boolean containsMusicFlag = false;
            for (int i = 0; i < files.length; ++i){
                String fileList = files[i].getAbsolutePath();
                String fileName = files[i].getName();
                if(fileList.endsWith(".mp3") || fileList.endsWith(".m4a")){

                    if(!musicList.contains(fileList)) {
                        containsMusicFlag = true;
                        musicList.add(fileList);
                        musicName.add(fileName);
                        Log.d("Debug", "Dashboard: files = " + fileList);
                    }
                }
            }

            if(containsMusicFlag) {
                Dashboard.this.runOnUiThread(new Runnable() {
                    public void run() {

                        updateRecyclerView(musicList, musicName);

                    }
                });
            }


            for (int i = 0; i < files.length; ++i){
                if(files[i].isDirectory() && !files[i].isHidden()) {
                    loadMusicListFromDirectory(files[i].getAbsolutePath(), recCounter+1);
                }
            }

            Log.d("DebugRec", recCounter+"");
            if(recCounter == 0l) {
                Log.d("DebugRec", "End of recursion");

                searchingStatusFlag = false;
                Dashboard.this.runOnUiThread(new Runnable() {
                    public void run() {

                        Toast.makeText(context, "Search Complete", Toast.LENGTH_SHORT).show();
                        refreshStatusText.setText("Refresh music");

                    }
                });
            }

        }
        else{
            Log.d("Debug","No files found");
        }


    }

    public void refreshMusic(View v){
        if(!searchingStatusFlag)
            loadMusicListFromDirectoryThread();
    }

    public void startSearchListener(){

        musicSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                if(newText.equals("")){
                    updateRecyclerView(musicList, musicName);
                    Log.d("Debug", "Empty");
                }
                else {
                    Log.d("Debug", newText);

                    ArrayList<String> filteredMusicList = new ArrayList<String>();
                    ArrayList<String> filteredMusicName = new ArrayList<String>();
                    for (int i = 0; i < musicName.size(); i++) {
                        if (musicName.get(i).toLowerCase().contains(newText.toLowerCase())) {
                            filteredMusicName.add(musicName.get(i));
                            filteredMusicList.add(musicList.get(i));
                        }
                    }
                    display(filteredMusicName);
                    updateRecyclerView(filteredMusicList, filteredMusicName);
                }

                return false;
            }
        });

    }

    public void updateRecyclerView(ArrayList<String> musicList, ArrayList<String> musicName){
        display(musicName);
        MusicItemRecyclerViewAdapter adapter = new MusicItemRecyclerViewAdapter(this, musicList, musicName);
        musicItemRecyclerView.setAdapter(adapter);
        musicItemRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    }

    private void display(ArrayList<String> musicName) {
        for(String name : musicName)
            Log.d("Debug", name);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        musicList = new ArrayList<>();
        musicName = new ArrayList<>();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        musicList = new ArrayList<>();
        musicName = new ArrayList<>();
    }
}