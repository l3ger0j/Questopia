package com.qsp.player.model.service;

import static com.qsp.player.utils.StringUtil.isNotEmpty;
import static com.qsp.player.utils.ThreadUtil.throwIfNotMainThread;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class AudioPlayer {
    private final String TAG = this.getClass().getSimpleName();

    private final ConcurrentHashMap<String, Sound> sounds = new ConcurrentHashMap<>();

    private Thread audioThread;
    private volatile Handler audioHandler;
    private volatile boolean audioThreadInited;
    private boolean soundEnabled;
    private boolean paused;

    public void start() {
        throwIfNotMainThread();

        audioThread = new Thread(() -> {
            try {
                Looper.prepare();
                audioHandler = new Handler();
                audioThreadInited = true;
                Looper.loop();
            } catch (Throwable t) {
                Log.e(TAG,"Audio thread has stopped exceptionally", t);
            }
        });
        audioThread.start();
    }

    public void stop() {
        throwIfNotMainThread();
        pause();

        if (audioThread == null) return;

        if (audioThreadInited) {
            Handler handler = audioHandler;
            if (handler != null) {
                handler.getLooper().quitSafely();
            }
            audioThreadInited = false;
        } else {
            Log.w(TAG,"Audio thread has been started, but not initialized");
        }
        audioThread = null;
    }

    public void playFile(final String path, final int volume) {
        runOnAudioThread(() -> {
            Sound sound = sounds.get(path);
            if (sound != null) {
                sound.volume = volume;
            } else {
                sound = new Sound();
                sound.path = path;
                sound.volume = volume;
                sounds.put(path, sound);
            }
            if (soundEnabled && !paused) {
                doPlay(sound);
            }
        });
    }

    private void runOnAudioThread(final Runnable runnable) {
        if (audioThread == null) {
            Log.w(TAG,"Audio thread has not been started");
            return;
        }
        if (!audioThreadInited) {
            Log.w(TAG,"Audio thread has not been initialized");
            return;
        }
        Handler handler = audioHandler;
        if (handler != null) {
            handler.post(runnable);
        }
    }

    private void doPlay(final Sound sound) {
        float sysVolume = getSystemVolume(sound.volume);

        if (sound.player != null) {
            sound.player.setVolume(sysVolume, sysVolume);
            if (!sound.player.isPlaying()) {
                sound.player.start();
            }
            return;
        }

        final String normPath = sound.path.replace("\\", "/");
        File file = new File(normPath);
        if (!file.exists()) {
            Log.e(TAG,"Sound file not found: " + normPath);
            return;
        }

        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource(file.getAbsolutePath());
            player.prepare();
        } catch (IOException ex) {
            Log.e(TAG,"Failed to initialize media player", ex);
            return;
        }
        player.setOnCompletionListener(mp -> sounds.remove(sound.path));
        player.setVolume(sysVolume, sysVolume);
        player.start();

        sound.player = player;
    }

    private float getSystemVolume(int volume) {
        return volume / 100.f;
    }

    public void closeAllFiles() {
        runOnAudioThread(() -> {
            for (Sound sound : sounds.values()) {
                doClose(sound);
            }
            sounds.clear();
        });
    }

    private void doClose(Sound sound) {
        if (sound.player == null) {
            return;
        }
        if (sound.player.isPlaying()) {
            sound.player.stop();
        }
        sound.player.release();
    }

    public void closeFile(final String path) {
        runOnAudioThread(() -> {
            Sound sound = sounds.remove(path);
            if (sound != null) {
                doClose(sound);
            }
        });
    }

    public void pause() {
        if (paused) return;

        paused = true;

        runOnAudioThread(() -> {
            for (Sound sound : sounds.values()) {
                if (sound.player != null && sound.player.isPlaying()) {
                    sound.player.pause();
                }
            }
        });
    }

    public void resume() {
        if (!paused) return;

        paused = false;

        if (!soundEnabled) return;

        runOnAudioThread(() -> {
            for (Sound sound : sounds.values()) {
                doPlay(sound);
            }
        });
    }

    public boolean isPlayingFile(String path) {
        return isNotEmpty(path) && sounds.containsKey(path);
    }

    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    private static class Sound {
        private String path;
        private int volume;
        private MediaPlayer player;
    }
}
