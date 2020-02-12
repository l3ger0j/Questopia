package com.qsp.player.game;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.util.FileUtil;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

class AudioPlayer {

    private static final String TAG = AudioPlayer.class.getName();

    private final ConcurrentHashMap<String, Sound> sounds = new ConcurrentHashMap<>();

    private volatile boolean audioThreadRunning;
    private volatile Handler audioHandler;
    private boolean soundEnabled;
    private DocumentFile gameDir;
    private boolean paused;

    void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    void setGameDirectory(DocumentFile dir) {
        gameDir = dir;
    }

    void destroy() {
        if (audioHandler != null) {
            audioHandler.getLooper().quitSafely();
        }
    }

    void resume() {
        paused = false;

        if (!soundEnabled) {
            return;
        }
        runOnAudioThread(new Runnable() {
            @Override
            public void run() {
                for (Sound sound : sounds.values()) {
                    doPlay(sound);
                }
            }
        });
    }

    private void runOnAudioThread(final Runnable r) {
        if (!audioThreadRunning) {
            startAudioThread();
        }
        audioHandler.post(r);
    }

    private void startAudioThread() {
        final CountDownLatch latch = new CountDownLatch(1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                audioThreadRunning = true;
                Looper.prepare();
                audioHandler = new Handler();
                latch.countDown();
                Looper.loop();
                audioThreadRunning = false;
            }
        })
                .start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Wait failed", e);
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

        DocumentFile file = FileUtil.findFileByPath(gameDir, sound.path);
        if (file == null) {
            Log.e(TAG, "Sound file not found: " + sound.path);
            return;
        }

        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource(file.getUri().toString());
            player.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize media player", e);
            return;
        }
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                sounds.remove(sound.path);
            }
        });
        player.setVolume(sysVolume, sysVolume);
        player.start();

        sound.player = player;
    }

    void pause() {
        paused = true;

        runOnAudioThread(new Runnable() {
            @Override
            public void run() {
                for (Sound sound : sounds.values()) {
                    if (sound.player.isPlaying()) {
                        sound.player.pause();
                    }
                }
            }
        });
    }

    void playFile(final String path, final int volume) {
        runOnAudioThread(new Runnable() {
            @Override
            public void run() {
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
            }
        });
    }

    private float getSystemVolume(int volume) {
        return volume / 100.f;
    }

    boolean isPlayingFile(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        return sounds.containsKey(path);
    }

    void closeFile(final String path) {
        runOnAudioThread(new Runnable() {
            @Override
            public void run() {
                Sound sound = sounds.remove(path);
                if (sound != null) {
                    doClose(sound);
                }
            }
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

    void closeAllFiles() {
        runOnAudioThread(new Runnable() {
            @Override
            public void run() {
                for (Sound sound : sounds.values()) {
                    doClose(sound);
                }
                sounds.clear();
            }
        });
    }

    private static class Sound {
        private String path;
        private int volume;
        private MediaPlayer player;
    }
}
