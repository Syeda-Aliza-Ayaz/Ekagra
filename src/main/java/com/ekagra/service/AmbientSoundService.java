package com.ekagra.service;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.Random;

/**
 * Procedural ambient focus sound generator (FR-Sound).
 * Runs purely on standard Java Audio APIs with zero external dependencies.
 */
public final class AmbientSoundService {

    public enum SoundType {
        RAIN("Gentle Rain", "Soothing rainfall backdrop"),
        STREAM("Forest Stream", "Calm flowing water cadence"),
        BINAURAL("Deep Alpha Focus", "10 Hz binaural beats for flow state"),
        WHITE_NOISE("Soft Noise", "Broadband sound mask for quiet focus");

        private final String displayName;
        private final String description;

        SoundType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final AmbientSoundService INSTANCE = new AmbientSoundService();

    private final Object lock = new Object();
    private volatile boolean playing = false;
    private volatile boolean muted = false;
    private volatile double volume = 0.5; // 0.0 to 1.0
    private volatile SoundType currentType = SoundType.RAIN;
    private Thread audioThread;
    private SourceDataLine line;

    private AmbientSoundService() {
    }

    public static AmbientSoundService get() {
        return INSTANCE;
    }

    public void play(SoundType type) {
        synchronized (lock) {
            this.currentType = type;
            this.playing = true;
            if (audioThread == null || !audioThread.isAlive()) {
                startAudioThread();
            }
        }
    }

    public void play() {
        play(currentType);
    }

    public void pause() {
        synchronized (lock) {
            this.playing = false;
        }
    }

    public void togglePlay() {
        if (playing) {
            pause();
        } else {
            play(currentType);
        }
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public void toggleMute() {
        this.muted = !this.muted;
    }

    public void setVolume(double vol) {
        this.volume = Math.clamp(vol, 0.0, 1.0);
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isMuted() {
        return muted;
    }

    public double getVolume() {
        return volume;
    }

    public SoundType getCurrentType() {
        return currentType;
    }

    private void startAudioThread() {
        audioThread = new Thread(this::audioLoop, "Ekagra-AmbientSound");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    private void audioLoop() {
        final float sampleRate = 44100f;
        AudioFormat format = new AudioFormat(sampleRate, 16, 2, true, false);
        byte[] buffer = new byte[2048];
        Random rng = new Random();

        double phase1 = 0;
        double phase2 = 0;
        double lowpassLeft = 0;
        double lowpassRight = 0;

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            while (!Thread.currentThread().isInterrupted()) {
                if (!playing) {
                    Thread.sleep(100);
                    continue;
                }

                double effectiveVol = muted ? 0.0 : volume;
                SoundType type = currentType;
                int frames = buffer.length / 4;

                for (int i = 0; i < frames; i++) {
                    double sampleLeft = 0;
                    double sampleRight = 0;

                    switch (type) {
                        case RAIN -> {
                            // Filtered pink noise simulating rainfall
                            double whiteL = rng.nextDouble() * 2.0 - 1.0;
                            double whiteR = rng.nextDouble() * 2.0 - 1.0;
                            lowpassLeft = lowpassLeft * 0.86 + whiteL * 0.14;
                            lowpassRight = lowpassRight * 0.86 + whiteR * 0.14;
                            sampleLeft = lowpassLeft * 0.7;
                            sampleRight = lowpassRight * 0.7;
                        }
                        case STREAM -> {
                            // Gentle babbling forest stream
                            phase1 += (2.0 * Math.PI * 0.45) / sampleRate;
                            double mod = (Math.sin(phase1) * 0.3) + 0.7;
                            double whiteL = (rng.nextDouble() * 2.0 - 1.0) * mod;
                            double whiteR = (rng.nextDouble() * 2.0 - 1.0) * mod;
                            lowpassLeft = lowpassLeft * 0.78 + whiteL * 0.22;
                            lowpassRight = lowpassRight * 0.78 + whiteR * 0.22;
                            sampleLeft = lowpassLeft * 0.65;
                            sampleRight = lowpassRight * 0.65;
                        }
                        case BINAURAL -> {
                            // 216 Hz Left, 226 Hz Right (10 Hz alpha differential for flow state)
                            phase1 += (2.0 * Math.PI * 216.0) / sampleRate;
                            phase2 += (2.0 * Math.PI * 226.0) / sampleRate;
                            if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI;
                            if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI;
                            sampleLeft = Math.sin(phase1) * 0.45;
                            sampleRight = Math.sin(phase2) * 0.45;
                        }
                        case WHITE_NOISE -> {
                            // Warm filtered Gaussian sound mask
                            double wL = rng.nextDouble() * 2.0 - 1.0;
                            double wR = rng.nextDouble() * 2.0 - 1.0;
                            lowpassLeft = lowpassLeft * 0.65 + wL * 0.35;
                            lowpassRight = lowpassRight * 0.65 + wR * 0.35;
                            sampleLeft = lowpassLeft * 0.55;
                            sampleRight = lowpassRight * 0.55;
                        }
                    }

                    short sL = (short) Math.clamp(sampleLeft * effectiveVol * 30000.0, -32000, 32000);
                    short sR = (short) Math.clamp(sampleRight * effectiveVol * 30000.0, -32000, 32000);

                    int idx = i * 4;
                    buffer[idx] = (byte) (sL & 0xFF);
                    buffer[idx + 1] = (byte) ((sL >> 8) & 0xFF);
                    buffer[idx + 2] = (byte) (sR & 0xFF);
                    buffer[idx + 3] = (byte) ((sR >> 8) & 0xFF);
                }

                line.write(buffer, 0, buffer.length);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Audio line unavailable on this system - quiet failover
        } finally {
            if (line != null) {
                try {
                    line.stop();
                    line.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
