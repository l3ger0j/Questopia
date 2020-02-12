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

    void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    void setGameDirectory(DocumentFile dir) {
        gameDir = dir;
    }

    void close() {
        if (audioHandler != null) {
            audioHandler.getLooper().quitSafely();
        }
    }

    void play(final String path, final int volume) {
        if (!soundEnabled) {
            return;
        }
        final float sysVolume = getSystemVolume(volume);
        runOnAudioThread(new Runnable() {
            @Override
            public void run() {
                Sound prevSound = sounds.get(path);
                if (prevSound != null) {
                    prevSound.player.setVolume(sysVolume, sysVolume);
                    if (!prevSound.player.isPlaying()) {
                        prevSound.player.start();
                    }
                    return;
                }

                DocumentFile file = FileUtil.findFileByPath(gameDir, path);
                if (file == null) {
                    Log.e(TAG, "Sound file not found: " + path);
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
                        sounds.remove(path);
                    }
                });
                player.setVolume(sysVolume, sysVolume);
                player.start();

                Sound sound = new Sound();
                sound.volume = volume;
                sound.player = player;
                sounds.put(path, sound);
            }
        });
    }

    private void runOnAudioThread(final Runnable r) {
        if (!audioThreadRunning) {
            startAudioThread();
        }
        audioHandler.post(r);
    }

    private float getSystemVolume(int volume) {
        return volume / 100.f;
    }

    boolean isPlaying(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        return sounds.containsKey(path);
    }

    void resumeAll() {
        if (!soundEnabled) {
            return;
        }
        runOnAudioThread(new Runnable() {
            @Override
            public void run() {
                for (Sound sound : sounds.values()) {
                    if (!sound.player.isPlaying()) {
                        float volume = getSystemVolume(sound.volume);
                        sound.player.setVolume(volume, volume);
                        sound.player.start();
                    }
                }
            }
        });
    }

    void pauseAll() {
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

    void stop(final String path) {
        runOnAudioThread(new Runnable() {
            @Override
            public void run() {
                Sound sound = sounds.remove(path);
                if (sound != null) {
                    if (sound.player.isPlaying()) {
                        sound.player.stop();
                    }
                    sound.player.release();
                }
            }
        });
    }

    void stopAll() {
        runOnAudioThread(new Runnable() {
            @Override
            public void run() {
                for (Sound sound : sounds.values()) {
                    if (sound.player.isPlaying()) {
                        sound.player.stop();
                    }
                    sound.player.release();
                }
                sounds.clear();
            }
        });
    }

    private static class Sound {
        private int volume;
        private MediaPlayer player;
    }
}
