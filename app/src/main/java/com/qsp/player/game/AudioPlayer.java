package com.qsp.player.game;

import android.media.MediaPlayer;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.util.FileUtil;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

class AudioPlayer {

    private static final String TAG = AudioPlayer.class.getName();

    private final ConcurrentHashMap<String, Sound> sounds = new ConcurrentHashMap<>();

    private DocumentFile gameDir;
    private boolean soundEnabled;

    void setGameDirectory(DocumentFile dir) {
        gameDir = dir;
    }

    void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    void play(final String path, int volume) {
        if (!soundEnabled) {
            return;
        }
        float sysVolume = getSystemVolume(volume);

        Sound prevSound = sounds.get(path);
        if (prevSound != null) {
            prevSound.player.setVolume(sysVolume, sysVolume);
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
        for (Sound sound : sounds.values()) {
            if (!sound.player.isPlaying()) {
                float volume = getSystemVolume(sound.volume);
                sound.player.setVolume(volume, volume);
                sound.player.start();
            }
        }
    }

    void pauseAll() {
        for (Sound sound : sounds.values()) {
            if (sound.player.isPlaying()) {
                sound.player.pause();
            }
        }
    }

    void stop(String path) {
        Sound sound = sounds.remove(path);
        if (sound != null) {
            if (sound.player.isPlaying()) {
                sound.player.stop();
            }
            sound.player.release();
        }
    }

    void stopAll() {
        for (Sound sound : sounds.values()) {
            if (sound.player.isPlaying()) {
                sound.player.stop();
            }
            sound.player.release();
        }
        sounds.clear();
    }

    private static class Sound {
        private int volume;
        private MediaPlayer player;
    }
}
