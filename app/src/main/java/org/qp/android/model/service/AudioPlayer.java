package org.qp.android.model.service;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioPlayer {

    private final ConcurrentHashMap<Uri, MediaPlayer> sounds = new ConcurrentHashMap<>();
    private ExecutorService audioExecutor;
    private volatile boolean soundEnabled = true;
    private volatile boolean isPaused = false;

    private boolean isExecutorExistNotDown() {
        return audioExecutor != null && !audioExecutor.isShutdown();
    }

    public void start() {
        isPaused = false;
        audioExecutor = Executors.newSingleThreadExecutor();
    }

    public void stop() {
        if (isExecutorExistNotDown()) {
            closeAllFiles();
            audioExecutor.shutdown();
        }
    }

    public CompletableFuture<Void> playFile(final Context context,
                                            final Uri soundFileUri,
                                            final int volume) {
        if (isExecutorExistNotDown()) {
            return CompletableFuture.runAsync(() -> {
                final var sound = sounds.get(soundFileUri);
                final var sysVolume = getSystemVolume(volume);
                if (sound == null) {
                    final var newSound = createNewSound(context, soundFileUri, sysVolume);
                    if (soundEnabled && !isPaused) {
                        if (!newSound.isPlaying()) {
                            newSound.start();
                        }
                    }
                } else {
                    sound.setVolume(sysVolume, sysVolume);
                    if (soundEnabled && !isPaused) {
                        if (!sound.isPlaying()) {
                            sound.start();
                        }
                    }
                }
            }, audioExecutor);
        }
        return CompletableFuture.runAsync(() -> {});
    }

    private MediaPlayer createNewSound(final Context context,
                                       final Uri filePath,
                                       final float sysVolume) {
        var filePlayer = new MediaPlayer();

        try {
            filePlayer.setDataSource(context, filePath);
            filePlayer.prepare();
        } catch (IOException ex) {
            throw new CompletionException(ex);
        }

        filePlayer.setOnCompletionListener(mediaPlayer -> {
            sounds.remove(filePath);
            mediaPlayer.reset();
            mediaPlayer.release();
        });
        filePlayer.setVolume(sysVolume, sysVolume);

        sounds.put(filePath, filePlayer);
        return filePlayer;
    }

    private float getSystemVolume(int volume) {
        return volume / 100.f;
    }

    public CompletableFuture<Void> closeAllFiles() {
        if (isExecutorExistNotDown()) {
            return CompletableFuture.runAsync(() -> {
                sounds.values().stream()
                        .filter(Objects::nonNull)
                        .forEach(player -> {
                            if (player.isPlaying()) {
                                player.stop();
                            }
                            player.reset();
                            player.release();
                        });
                sounds.clear();
            }, audioExecutor);
        }
        return CompletableFuture.runAsync(() -> {});
    }

    public CompletableFuture<Void> closeFile(final Uri filePath) {
        if (isExecutorExistNotDown() && sounds.containsKey(filePath)) {
            return CompletableFuture.runAsync(() -> {
                final var player = sounds.get(filePath);
                if (player != null) {
                    if (player.isPlaying()) {
                        player.stop();
                    }
                    player.reset();
                    player.release();
                }
                sounds.remove(filePath);
            }, audioExecutor);
        }
        return CompletableFuture.runAsync(() -> {});
    }

    public void pause() {
        if (isPaused) return;
        if (isExecutorExistNotDown()) {
            isPaused = true;
            CompletableFuture
                    .runAsync(() ->
                            sounds.values().stream()
                                    .filter(player -> player != null && player.isPlaying())
                                    .forEach(MediaPlayer::pause),
                            audioExecutor);
        }
    }

    public void resume() {
        if (!soundEnabled) return;
        if (!isPaused) return;
        if (isExecutorExistNotDown()) {
            isPaused = false;
            CompletableFuture
                    .runAsync(() ->
                            sounds.values().stream()
                                    .filter(player -> player != null && !player.isPlaying())
                                    .forEach(MediaPlayer::start),
                            audioExecutor);
        }
    }

    public boolean isPlayingFile(final Uri filePath) {
        return filePath != null && !filePath.equals(Uri.EMPTY) && sounds.containsKey(filePath);
    }

    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }
}