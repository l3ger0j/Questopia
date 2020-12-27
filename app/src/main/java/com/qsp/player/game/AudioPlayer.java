package com.qsp.player.game;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

class AudioPlayer {
    private static Logger logger = LoggerFactory.getLogger(AudioPlayer.class);

    private final ConcurrentHashMap<String, Sound> sounds = new ConcurrentHashMap<>();

    private volatile boolean audioThreadRunning;
    private volatile Handler audioHandler;
    private boolean soundEnabled;
    private boolean paused;

    void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
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
        runOnAudioThread(() -> {
            for (Sound sound : sounds.values()) {
                doPlay(sound);
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

        new Thread(() -> {
            audioThreadRunning = true;
            Looper.prepare();
            audioHandler = new Handler();
            latch.countDown();
            Looper.loop();
            audioThreadRunning = false;
        })
                .start();

        try {
            latch.await();
        } catch (InterruptedException ex) {
            logger.error("Wait failed", ex);
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

        File file = new File(sound.path);
        if (!file.exists()) {
            logger.error("Sound file not found: " + sound.path);
            return;
        }

        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource(file.getAbsolutePath());
            player.prepare();
        } catch (IOException ex) {
            logger.error("Failed to initialize media player", ex);
            return;
        }
        player.setOnCompletionListener(mp -> sounds.remove(sound.path));
        player.setVolume(sysVolume, sysVolume);
        player.start();

        sound.player = player;
    }

    void pause() {
        paused = true;

        runOnAudioThread(() -> {
            for (Sound sound : sounds.values()) {
                if (sound.player != null && sound.player.isPlaying()) {
                    sound.player.pause();
                }
            }
        });
    }

    void playFile(final String path, final int volume) {
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
        runOnAudioThread(() -> {
            Sound sound = sounds.remove(path);
            if (sound != null) {
                doClose(sound);
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
        runOnAudioThread(() -> {
            for (Sound sound : sounds.values()) {
                doClose(sound);
            }
            sounds.clear();
        });
    }

    private static class Sound {
        private String path;
        private int volume;
        private MediaPlayer player;
    }
}
