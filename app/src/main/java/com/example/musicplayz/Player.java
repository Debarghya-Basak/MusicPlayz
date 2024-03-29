package com.example.musicplayz;

import static android.graphics.Color.argb;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatDrawableManager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import io.alterac.blurkit.BlurKit;
import io.alterac.blurkit.BlurLayout;


public class Player extends AppCompatActivity {

    TextView musicNameShow, musicProgressTime, musicEndTime;
    static ImageView blurBackground;
    ImageView musicDisc;
    MediaPlayer mediaPlayer;
    SeekBar musicSeekbar;
    ImageButton playPauseButton, nextButton, prevButton, shuffleButton, repeatButton;
    Thread updateSeekbar;
    LooperThreadMusicDisc threadMusicDisc;
    boolean musicPlaying;
    boolean firstMusic;
    int repeatMode = 0;
    int shuffleMode = 0;
    ArrayList<String> musicList, musicName;
    ArrayList<String> tempMusicList, tempMusicName;
    int position;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        //initialization
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        Intent intent = getIntent();
        Bundle contents = intent.getExtras();
        musicList = contents.getStringArrayList("musicList");
        musicName = contents.getStringArrayList("musicName");
        position = contents.getInt("position");

        tempMusicList = new ArrayList<>();
        tempMusicName = new ArrayList<>();
        BlurKit.init(this);
        firstMusic = true;

        blurBackground = findViewById(R.id.blurBackground);
        musicNameShow = findViewById(R.id.playing_music_name);
        musicProgressTime = findViewById(R.id.music_progressTime);
        musicEndTime = findViewById(R.id.music_endTime);
        musicSeekbar = findViewById(R.id.seekbar_music);
        playPauseButton = findViewById(R.id.playButton_music);
        nextButton = findViewById(R.id.nextButton_music);
        prevButton = findViewById(R.id.prevButton_music);
        musicDisc = findViewById(R.id.music_disc);
        shuffleButton = findViewById(R.id.shuffleMode_music);
        repeatButton = findViewById(R.id.repeatMode_music);

        updateUIMusic();
        musicStartFlag();
        updatePlayPauseButtonBg();
        musicDiscRotateAnimation();
        seekBarFn();
        mediaPlayerListener();
        updateBackground();

        //initialization end

    }

    public void mediaPlayerListener(){
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                nextButton.performClick();
            }
        });
    }

    public String toMin(long millis){
        long sec = (millis / 1000) % 60;
        long min = (millis / 1000) / 60;
        long hour = (millis / 1000) / (60*60);

        String seconds=sec+"",minutes=min+"",hours=hour+"";
        if(sec < 10)
            seconds = "0" + sec;
        if(min < 10)
            minutes = "0" + minutes;
        if(hour < 10)
            hours = "0" + hour;

        return hours + ":" + minutes + ":" + seconds;
    }

    public void seekBarFn(){
        musicSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo(seekBar.getProgress());
                musicSeekbar.setProgress(seekBar.getProgress());
                musicProgressTime.setText(toMin(mediaPlayer.getCurrentPosition()));
            }
        });

        updateSeekbarUI();
    }

    public void musicDiscRotateAnimation(){
        threadMusicDisc = new LooperThreadMusicDisc();

        threadMusicDisc.start();
        SystemClock.sleep(100);
        threadMusicDisc.looping = true;

        Handler handler = new Handler(threadMusicDisc.looper);
        handler.post(new Runnable() {
            @Override
            public void run() {
                do{
                    //if animate() is looped, it cannot run directly on a simple Thread. It needs a Looper Thread
                    musicDisc.animate().rotationBy(100).setDuration(5000);
                    musicDisc.setHasTransientState(true);
                    Log.d("Debug","Player : Rotation" + musicDisc.hasTransientState());
                    SystemClock.sleep(5000);
                    if(!musicPlaying)
                        break;
                }while (mediaPlayer.getCurrentPosition() < mediaPlayer.getDuration()-250);
                musicPlaying = false;
                Log.d("Debug","Stop Rotation");
            }
        });


    }

    public void musicStartFlag(){
        mediaPlayer.start();
        musicPlaying = true;
    }

    public void musicStopFlag(boolean flag){
        mediaPlayer.stop();
        if(flag)
            mediaPlayer.release();
        musicPlaying = false;
    }

    public void musicPauseFlag(){
        mediaPlayer.pause();
        musicPlaying = false;
    }

    public void updateSeekbarUI(){
        updateSeekbar = new Thread(){
            @Override
            public void run() {
                setName("SeekBarUIThread");
                int currentPosition = 0;

                try {
                    while (currentPosition < mediaPlayer.getDuration() - 250) {
                        currentPosition = mediaPlayer.getCurrentPosition();
                        //TODO: set text inside another thread does not work properly
                        int finalCurrentPosition = currentPosition;
                        Player.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                musicSeekbar.setProgress(finalCurrentPosition);
                                musicProgressTime.setText(toMin(mediaPlayer.getCurrentPosition()));

                            }
                        });

                        SystemClock.sleep(500);
                        Log.d("Debug","Duration : " + mediaPlayer.getDuration() + ", Progress : "+ mediaPlayer.getCurrentPosition());
                    }
                }
                catch (Exception e){}
                Log.d("Debug" , "Song End");

            }
        };
        updateSeekbar.start();
    }

    @SuppressLint("RestrictedApi")
    private void updatePlayPauseButtonBg() {
        if(musicPlaying){
            playPauseButton.setBackground(AppCompatDrawableManager.get().getDrawable(this, R.drawable.pause_button));
            playPauseButton.setBackgroundTintList(ColorStateList.valueOf(argb(127,255,255,255)));
        }
        else{
            playPauseButton.setBackground(AppCompatDrawableManager.get().getDrawable(this, R.drawable.play_button));
            playPauseButton.setBackgroundTintList(ColorStateList.valueOf(argb(127,255,255,255)));
        }


    }

    public void updateUIMusic(){
        mediaPlayer = MediaPlayer.create(this, Uri.parse(musicList.get(position)));
        musicNameShow.setText(musicName.get(position));
        musicNameShow.setSelected(true);
        musicSeekbar.setMax(mediaPlayer.getDuration()-250);
        musicSeekbar.setProgress(0);
        musicProgressTime.setText(toMin(mediaPlayer.getCurrentPosition()));
        musicEndTime.setText(toMin(mediaPlayer.getDuration()));
    }

    public void changeToPrevMusic(){
        musicStopFlag(false);
        updatePlayPauseButtonBg();
        position--;
        if(position == -1)
            position = musicList.size()-1;

        updateUIMusic();
        musicStartFlag();
        updatePlayPauseButtonBg();
//        updateSeekbarUI();
        mediaPlayerListener();
        updateBackground();
        if(!threadMusicDisc.looping && musicPlaying)
            threadStart();
    }

    public void changeToPrevMusic(View v){
       changeToPrevMusic();

    }

    public void changeToNextMusic(){
        if(repeatMode == 0){
            musicStopFlag(false);
            updatePlayPauseButtonBg();
            position++;
            if (position == musicList.size()){
                position--;
                musicStopFlag(false);
                threadQuit();
                updateUIMusic();
                updatePlayPauseButtonBg();
                mediaPlayerListener();
                updateBackground();
            }
            else{
                updateUIMusic();
                musicStartFlag();
                updatePlayPauseButtonBg();
//                updateSeekbarUI();
                mediaPlayerListener();
                updateBackground();
                if(!threadMusicDisc.looping && musicPlaying)
                    threadStart();
            }

        }
        else if(repeatMode == 1) {
            musicStopFlag(false);
            updatePlayPauseButtonBg();
            position++;
            if (position == musicList.size())
                position = 0;
            Log.d("Debug",musicName.get(position));

            updateUIMusic();
            musicStartFlag();
            updatePlayPauseButtonBg();
//            updateSeekbarUI();
            mediaPlayerListener();
            updateBackground();
            if(!threadMusicDisc.looping && musicPlaying)
                threadStart();
        }
        else if(repeatMode == 2){
            musicStopFlag(false);
            updatePlayPauseButtonBg();
            Log.d("Debug",musicName.get(position));

            updateUIMusic();
            musicStartFlag();
            updatePlayPauseButtonBg();
//            updateSeekbarUI();
            mediaPlayerListener();
            updateBackground();
            if(!threadMusicDisc.looping && musicPlaying)
                threadStart();
        }


    }

    public void changeToNextMusic(View v){
        changeToNextMusic();
        Log.d("Debug", "Next button Clicked");
    }

    public void playPauseMusic(){
        if(musicPlaying) {
            musicPauseFlag();
            updatePlayPauseButtonBg();
            threadQuit();
        }
        else {
            musicStartFlag();
            updatePlayPauseButtonBg();
            if(!threadMusicDisc.looping && musicPlaying)
                threadStart();
        }
    }

    public void playPauseMusic(View v){
       playPauseMusic();
    }

    public void shuffleArrayList(){
        for(int i = 0;i<musicList.size();i++){
            int rand = (int) Math.random() * musicList.size();
            String temp = musicList.get(rand);
            musicList.set(rand, musicList.get(i));
            musicList.set(i,temp);

            temp = musicName.get(rand);
            musicName.set(rand, musicName.get(i));
            musicName.set(i,temp);
        }
    }

    @SuppressLint("RestrictedApi")
    public void changeShuffleMode(View v){
        if(shuffleMode == 0){
            shuffleButton.setBackground(AppCompatDrawableManager.get().getDrawable(this, R.drawable.shuffle_on));
            shuffleMode = 1;
            musicStopFlag(true);
            threadQuit();


            tempMusicList = copyArrayList(musicList);
            tempMusicName = copyArrayList(musicName);
            shuffleArrayList();

            display(tempMusicName);

            position = 0;
            updatePlayPauseButtonBg();
            updateUIMusic();
            mediaPlayerListener();
            updateBackground();
        }
        else{
            shuffleButton.setBackground(AppCompatDrawableManager.get().getDrawable(this, R.drawable.shuffle_off));
            shuffleMode = 0;
            musicStopFlag(true);
            threadQuit();


            musicList = copyArrayList(tempMusicList);
            musicName = copyArrayList(tempMusicName);

            display(musicName);

            position = 0;
            updatePlayPauseButtonBg();
            updateUIMusic();
            mediaPlayerListener();
            updateBackground();
        }

    }

    private ArrayList<String> copyArrayList(ArrayList<String> musicName) {
        ArrayList<String> temp = new ArrayList<String>();
        for(String name: musicName)
            temp.add(name);

        return temp;
    }

    @SuppressLint("RestrictedApi")
    public void changeRepeatMode(View v){
        if(repeatMode == 0){
            repeatButton.setBackground(AppCompatDrawableManager.get().getDrawable(this, R.drawable.repeat_playlist_on));
            repeatMode = 1;
        }
        else if(repeatMode == 1){
            repeatButton.setBackground(AppCompatDrawableManager.get().getDrawable(this, R.drawable.repeat_one_on));
            repeatMode = 2;
        }
        else if(repeatMode == 2){
            repeatButton.setBackground(AppCompatDrawableManager.get().getDrawable(this, R.drawable.repeat_off));
            repeatMode = 0;
        }

    }

//    private void updateBackgroundThread(){
//
//        LooperThreadMusicDisc looperThreadMusicDisc = new LooperThreadMusicDisc();
//        looperThreadMusicDisc.start();
//        SystemClock.sleep(200);
//
//        Handler handler = new Handler(looperThreadMusicDisc.looper);
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if(firstMusic) {
//                    Player.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                blurBackground.setImageBitmap(BlurKit.getInstance().blur(createAlbumArt(musicList.get(position)), 10));
//                            } catch (Exception e) {
//                                Bitmap placeholder = BitmapFactory.decodeResource(getResources(), R.drawable.background);
//                                blurBackground.setImageBitmap(BlurKit.getInstance().blur(placeholder, 10));
//                            }
//                        }
//                    });
//
//                    blurBackground.animate().alpha(1).setDuration(1000).start();
//
//                    firstMusic = false;
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            looperThreadMusicDisc.looper.quit();
//                        }
//                    },1000);
//
//                }
//                else{
//                    blurBackground.animate().alpha(0).setDuration(1000).setListener(new AnimatorListenerAdapter() {
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            super.onAnimationEnd(animation);
//
//                            Player.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    try {
//                                        blurBackground.setImageBitmap(BlurKit.getInstance().blur(createAlbumArt(musicList.get(position)), 10));
//                                    } catch (Exception e) {
//                                        Bitmap placeholder = BitmapFactory.decodeResource(getResources(), R.drawable.background);
//                                        blurBackground.setImageBitmap(BlurKit.getInstance().blur(placeholder, 10));
//                                    }
//                                }
//                            });
//
//
//                            blurBackground.animate().alpha(1).setDuration(2000).start();
//                        }
//                    }).start();
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            looperThreadMusicDisc.looper.quit();
//                        }
//                    },3000);
//                }
//
//            }
//        },100);
//
//
//    }

    private void updateBackground() {

//        blurBackground.setRenderEffect(RenderEffect.createBlurEffect(50, 50, Shader.TileMode.MIRROR));

        try {
            blurBackground.setImageBitmap(BlurKit.getInstance().blur(createAlbumArt(musicList.get(position)), 10));
        } catch (Exception e) {
            Bitmap placeholder = BitmapFactory.decodeResource(getResources(), R.drawable.background);
            blurBackground.setImageBitmap(BlurKit.getInstance().blur(placeholder, 10));
        }

    }

    public Bitmap createAlbumArt(String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            byte[] embedPic = retriever.getEmbeddedPicture();
            bitmap = BitmapFactory.decodeByteArray(embedPic, 0, embedPic.length);
            Log.d("Debug", "Found album art");
        } catch (Exception e) {
            Log.d("Debug", "Cannot find album art");
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return bitmap;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public void threadQuit(){

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {

                Log.d("Debug","Thread stopped");
                threadMusicDisc.looper.quit();
            }
        },5000);

    }

    public void threadStart(){
        if(threadMusicDisc.looping){
            new Handler().postDelayed(new Runnable(){
                @Override
                public void run() {
                    Log.d("Debug","Next Song");
                    musicDiscRotateAnimation();
                }
            },5100);
        }
        else{
            musicDiscRotateAnimation();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        musicStopFlag(true);
        updatePlayPauseButtonBg();
        threadQuit();

    }

    private void display(ArrayList<String> musicName) {
        for(String name : musicName)
            Log.d("Debug", name);
    }
}