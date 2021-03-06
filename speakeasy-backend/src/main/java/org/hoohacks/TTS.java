package org.hoohacks;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1.Voice;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class TTS {

    public static final String DEF_LANG = "en-US";

    private static final TTS instance = new TTS(DEF_LANG);

    public static TTS getInstance() {
        return instance;
    }

    private final TextToSpeechClient client;
    private final String language;
    private final Map<String, Voice> voiceMap;

    public TTS(String language) {
        try {
            this.language = language;
            GoogleCredentials credentials = CredentialFactory.getCredentials();

            var settings = TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();
            client = TextToSpeechClient.create(settings);
            var response = client.listVoices(language);
            List<Voice> voices = response.getVoicesList();
            voiceMap = new HashMap<>(voices.size());
            for (Voice v : voices) {
                String name = v.getName();
                if (name.toLowerCase().contains("wavenet")) {
                    voiceMap.put(v.getName(), v);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<VoiceData> getVoices() {
        return voiceMap.values().stream()
                .map(v -> new VoiceData(v.getName(), v.getSsmlGender().toString()))
                .collect(Collectors.toList());
    }

    public AudioInputStream getSpeechRaw(String voiceName, String text) {
        byte[] bytes = getSpeechRawBytes(voiceName, text);
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            return AudioSystem.getAudioInputStream(in);
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getSpeechRawBytes(String voiceName, String text) {
        if (!voiceMap.containsKey(voiceName)) {
            throw new IllegalArgumentException("Unrecognized voice name: " + voiceName);
        }

        SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
        AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16).build();
        var voiceParams = VoiceSelectionParams.newBuilder()
                .setLanguageCode(language)
                .setName(voiceName)
                .build();

        var response = client.synthesizeSpeech(input, voiceParams, audioConfig);
        return response.getAudioContent().toByteArray();
    }

    public byte[] getSpeechMP3Bytes(String voiceName, String text) {
        if (!voiceMap.containsKey(voiceName)) {
            throw new IllegalArgumentException("Unrecognized voice name: " + voiceName);
        }

        SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
        AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build();
        var voiceParams = VoiceSelectionParams.newBuilder()
                .setLanguageCode(language)
                .setName(voiceName)
                .build();

        return client.synthesizeSpeech(input, voiceParams, audioConfig).getAudioContent().toByteArray();
    }

    public static class VoiceData {
        public final String name;
        public final String gender;

        public VoiceData(String name, String gender) {
            this.name = name;
            this.gender = gender;
        }

        public String toString() {
            return String.format("VoiceData[name=%s, gender=%s]", name, gender);
        }
    }
}
