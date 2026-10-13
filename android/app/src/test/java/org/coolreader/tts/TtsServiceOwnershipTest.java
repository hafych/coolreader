package org.coolreader.tts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Test;

/**
 * TTS service playback/audio-focus slots belong to one service generation.
 */
public class TtsServiceOwnershipTest {
	@Test
	public void playbackAndFocusBelongToOwnedStates() throws Exception {
		for (String name : new String[]{
				"playbackState", "audioFocusState", "audiobookState"}) {
			Field field = TTSControlService.class.getDeclaredField(name);
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		assertEquals(
				TtsPlaybackState.class,
				TTSControlService.class.getDeclaredField("playbackState").getType());
		assertEquals(
				TtsAudioFocusState.class,
				TTSControlService.class.getDeclaredField("audioFocusState").getType());
		assertEquals(
				TtsAudiobookState.class,
				TTSControlService.class.getDeclaredField("audiobookState").getType());
		for (Class<?> owner : new Class<?>[]{
				TtsPlaybackState.class,
				TtsAudioFocusState.class,
				TtsAudiobookState.class}) {
			assertTrue(Modifier.isFinal(owner.getModifiers()));
			for (Field field : owner.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers())) {
					assertTrue(Modifier.isFinal(field.getModifiers()));
					continue;
				}
				assertTrue(Modifier.isPrivate(field.getModifiers()));
			}
		}
		for (String legacy : new String[]{
				"mState",
				"mPrevState",
				"mContinuousErrors",
				"mPlaybackNowAuthorized",
				"mPlaybackDelayed",
				"mResumeOnFocusGain",
				"useAudioBook",
				"audioFile",
				"startTime"}) {
			for (Field field : TTSControlService.class.getDeclaredFields()) {
				assertFalse(
						"TTSControlService retains raw residual field " + legacy,
						field.getName().equals(legacy));
			}
		}
	}
}
