package com.mussaldynerhey.meuleitorrhey;

import android.Manifest;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioCaptureManager {
    private static final String TAG = "AudioCaptureManager"; // Variável TAG armazena o nome para logs.

    private static final int SAMPLE_RATE = 44100; // Variável SAMPLE_RATE define a taxa de amostragem do áudio.
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO; // Variável CHANNEL_CONFIG define o canal de áudio como mono.
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // Variável AUDIO_FORMAT define o formato de codificação do áudio.
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT); // Variável BUFFER_SIZE define o tamanho do buffer de áudio.

    private AudioRecord audioRecord; // Variável audioRecord representa o objeto de gravação de áudio.
    private boolean isRecording = false; // Variável isRecording indica se a gravação está ativa.
    private Thread recordingThread; // Variável recordingThread gerencia a thread de gravação.

    public interface AudioCaptureListener {
        void onAudioCaptured(byte[] audioData); // Método onAudioCaptured retorna os dados de áudio capturados.
        void onCaptureError(String error); // Método onCaptureError lida com erros na captura de áudio.
    }

    @SuppressLint("MissingPermission")
    public boolean startCapture(AudioCaptureListener listener) { // Método startCapture inicia a captura de áudio.
        try {
            if (BUFFER_SIZE == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Tamanho do buffer inválido");
                return false;
            }

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE
            ); // Cria um novo objeto AudioRecord com as configurações definidas.

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord não inicializado");
                return false;
            }

            audioRecord.startRecording(); // Inicia a gravação de áudio.
            isRecording = true; // Define que a gravação está ativa.

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[BUFFER_SIZE]; // Variável buffer armazena os dados de áudio capturados.

                while (isRecording) {
                    int bytesRead = audioRecord.read(buffer, 0, BUFFER_SIZE); // Lê os dados de áudio no buffer.
                    if (bytesRead > 0 && listener != null) {
                        listener.onAudioCaptured(buffer); // Envia os dados capturados para o listener.
                    }
                }
            });

            recordingThread.start(); // Inicia a thread de gravação.
            Log.d(TAG, "Captura de áudio iniciada");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar captura de áudio", e);
            if (listener != null) {
                listener.onCaptureError(e.getMessage());
            }
            return false;
        }
    }

    public void stopCapture() { // Método stopCapture para a captura de áudio.
        isRecording = false; // Define que a gravação não está ativa.

        if (recordingThread != null) {
            try {
                recordingThread.join(1000); // Aguarda a thread de gravação terminar.
            } catch (InterruptedException e) {
                Log.e(TAG, "Erro ao parar thread", e);
            }
            recordingThread = null; // Limpa a referência da thread.
        }

        if (audioRecord != null) {
            try {
                audioRecord.stop(); // Para a gravação de áudio.
                audioRecord.release(); // Libera os recursos do AudioRecord.
            } catch (Exception e) {
                Log.e(TAG, "Erro ao liberar AudioRecord", e);
            }
            audioRecord = null; // Limpa a referência do AudioRecord.
        }

        Log.d(TAG, "Captura de áudio parada");
    }

    public boolean isRecording() { // Método isRecording verifica se a gravação está ativa.
        return isRecording;
    }
}