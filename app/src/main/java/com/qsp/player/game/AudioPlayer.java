package com.qsp.player.game;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static com.qsp.player.util.StringUtil.isNotEmpty;

public class AudioPlayer {
    private static final Logger logger = LoggerFactory.getLogger(AudioPlayer.class);

    private final ConcurrentHashMap<String, Sound> sounds = new ConcurrentHashMap<>();

    private Thread audioThread;
    private volatile Handler audioHandler;
    private volatile boolean audioThreadInited;
    private boolean soundEnabled;
    private boolean paused;

    private void runOnAudioThread(final Runnable runnable) {
        // Запустить аудио-поток, если он ещё не был запущен
        if (audioThread == null) {
            startAudioThread(runnable);
            return;
        }

        // Выйти если поток был запущен, но не был проинициализирован
        if (!audioThreadInited) {
            logger.warn("Audio thread has been started, but not initialized");
            return;
        }

        Handler handler = audioHandler;
        if (handler != null) {
            handler.post(runnable);
        }
    }

    private void startAudioThread(final Runnable runnable) {
        audioThread = new Thread(() -> {
            try {
                Looper.prepare();
                audioHandler = new Handler();
                audioThreadInited = true;

                runnable.run();
                Looper.loop();

            } catch (Throwable t) {
                logger.error("Audio thread has been stopped exceptionally", t);
            }
        });
        audioThread.start();
    }

    public void close() {
        stopAudioThread();
    }

    private void stopAudioThread() {
        if (audioThread == null) return;

        if (audioThreadInited) {
            Handler handler = audioHandler;
            if (handler != null) {
                handler.getLooper().quitSafely();
            }
            audioThreadInited = false;
        } else {
            logger.warn("Audio thread has been started, but not initialized");
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
