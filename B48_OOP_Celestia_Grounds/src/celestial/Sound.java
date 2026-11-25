package celestial;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.SourceDataLine;

/**
 * Music reuses PlayPokemon's music.wav / battle.wav. Sound effects (Banga "tok", temple chimes,
 * the Bayanihan drum) are synthesised on a small mixer thread so no extra files are needed.
 * Everything fails silently when no audio device exists.
 */
public final class Sound {

	public enum Wave { SINE, WOOD, NOISE, DRUM, CHIME }

	private static boolean enabled = true;
	private static Clip town, battle;
	private static Clip current;
	private static SourceDataLine line;
	private static final List<Voice> voices = new ArrayList<>();
	private static final int RATE = 44100;

	private Sound() {}

	public static void disable() { enabled = false; }

	public static void init() {
		if (!enabled) return;
		Thread loader = new Thread(() -> {
			town = load("res/music.wav");
			battle = load("res/battle.wav");
			if (pendingMusic != null) music(pendingMusic);
		}, "music-loader");
		loader.setDaemon(true);
		loader.start();
		Thread mixer = new Thread(Sound::mixLoop, "sfx-mixer");
		mixer.setDaemon(true);
		mixer.start();
	}

	private static Clip load(String path) {
		try {
			AudioInputStream in = AudioSystem.getAudioInputStream(new File(path));
			Clip c = AudioSystem.getClip();
			c.open(in);
			return c;
		} catch (Exception e) {
			return null;
		}
	}

	private static String pendingMusic;

	/** "town", "battle" or "none". */
	public static synchronized void music(String which) {
		pendingMusic = which;
		if (!enabled) return;
		Clip next = "battle".equals(which) ? battle : "town".equals(which) ? town : null;
		if (next == current && next != null) return;
		if (current != null) current.stop();
		current = next;
		if (current != null) {
			current.setFramePosition(0);
			current.loop(Clip.LOOP_CONTINUOUSLY);
		}
	}

	// ------------------------------------------------------------------ synth
	private static final class Voice {
		double freq, pan, vol, t;
		int samples, total;
		Wave wave;
		long seed = 12345;
	}

	public static void tone(double freq, int ms, double pan, double vol, Wave wave) {
		if (!enabled) return;
		Voice v = new Voice();
		v.freq = freq; v.pan = Math.max(-1, Math.min(1, pan)); v.vol = vol; v.wave = wave;
		v.total = v.samples = RATE * ms / 1000;
		synchronized (voices) { voices.add(v); }
	}

	public static void tok() { tone(880, 90, 0, 0.5, Wave.WOOD); }
	public static void blip() { tone(660, 40, 0, 0.18, Wave.SINE); }
	public static void hit() { tone(140, 120, 0, 0.45, Wave.DRUM); }
	public static void chime(double pan) { tone(1318, 900, pan, 0.4, Wave.CHIME); tone(1976, 700, pan, 0.18, Wave.CHIME); }

	private static void mixLoop() {
		try {
			AudioFormat fmt = new AudioFormat(RATE, 16, 2, true, false);
			line = AudioSystem.getSourceDataLine(fmt);
			line.open(fmt, RATE / 10 * 4);
			line.start();
		} catch (Exception e) {
			return;
		}
		int n = RATE / 100; // 10 ms chunks
		byte[] buf = new byte[n * 4];
		while (true) {
			double[] l = new double[n], r = new double[n];
			synchronized (voices) {
				for (int vi = voices.size() - 1; vi >= 0; vi--) {
					Voice v = voices.get(vi);
					for (int i = 0; i < n && v.samples > 0; i++, v.samples--) {
						double prog = 1.0 - (double) v.samples / v.total;
						double env;
						double s;
						v.t += v.freq / RATE;
						switch (v.wave) {
							case WOOD: env = Math.exp(-prog * 9); s = Math.sin(2 * Math.PI * v.t) * 0.7 + Math.sin(2 * Math.PI * v.t * 2.7) * 0.3; break;
							case NOISE: env = Math.exp(-prog * 4); v.seed = v.seed * 6364136223846793005L + 1; s = ((v.seed >>> 33) / (double) (1L << 31)) - 1; break;
							case DRUM: env = Math.exp(-prog * 6); s = Math.sin(2 * Math.PI * v.t * (1.0 - prog * 0.5)); break;
							case CHIME: env = Math.exp(-prog * 3.5); s = Math.sin(2 * Math.PI * v.t) * 0.8 + Math.sin(2 * Math.PI * v.t * 3.01) * 0.2; break;
							default: env = Math.min(1, (1 - prog) * 6) * Math.min(1, prog * 40); s = Math.sin(2 * Math.PI * v.t); break;
						}
						double a = s * env * v.vol;
						l[i] += a * (v.pan <= 0 ? 1 : 1 - v.pan);
						r[i] += a * (v.pan >= 0 ? 1 : 1 + v.pan);
					}
					if (v.samples <= 0) voices.remove(vi);
				}
			}
			for (int i = 0; i < n; i++) {
				int sl = (int) (Math.max(-1, Math.min(1, l[i])) * 20000);
				int sr = (int) (Math.max(-1, Math.min(1, r[i])) * 20000);
				buf[i * 4] = (byte) sl; buf[i * 4 + 1] = (byte) (sl >> 8);
				buf[i * 4 + 2] = (byte) sr; buf[i * 4 + 3] = (byte) (sr >> 8);
			}
			line.write(buf, 0, buf.length);
		}
	}
}
