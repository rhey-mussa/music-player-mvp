package com.mussaldynerhey.meuleitorrhey;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

import Model.Musica;
import Model.Model;

/**
 * Serviço responsável por gerenciar a reprodução de músicas em segundo plano.
 */
public class MusicService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private static final String TAG = "MediaPlayerService";
    public static final String ACTION_SEEK_TO = "ACTION_SEEK_TO";
    public static final String ACTION_SEEK_FORWARD = "ACTION_SEEK_FORWARD";
    public static final String ACTION_SEEK_BACKWARD = "ACTION_SEEK_BACKWARD";
    public static final String BROADCAST_UPDATE = "com.mussaldynerhey.meuleitorrhey.BROADCAST_UPDATE";
    public static final String EXTRA_IS_PLAYING = "isPlaying";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_DURATION = "duration";
    public static final String EXTRA_TITLE = "titulo";
    public static final String EXTRA_ARTIST = "artista";
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREV = "ACTION_PREV";
    private static final String CHANNEL_ID = "MediaPlayerChannel";
    private static final int NOTIFICATION_ID = 1;
    private MediaPlayer mediaPlayer; // Instância do MediaPlayer para reprodução de áudio
    private List<Musica> musicas; // Lista de músicas a serem reproduzidas
    private int indiceActual = 0; // Índice da música atual
    private boolean isPlaying = false; // Estado de reprodução
    private int currentPosition = 0; // Posição atual da música em milissegundos
    private int currentDuration = 0; // Duração total da música em milissegundos
    private Model model; // Modelo de dados associado ao serviço
    private int currentSongIndex = -1; // Índice da música atual (redundante, mantido por compatibilidade)
    private boolean repeatState = false; // Estado do modo de repetição
    private boolean shuffleState = false; // Estado do modo aleatório
    private Handler progressHandler = new Handler(); // Handler para atualizações de progresso
    private Runnable progressRunnable; // Runnable para atualizações periódicas do progresso
    private MediaSessionCompat mediaSession; // Sessão de mídia para controles do sistema
    private final IBinder binder = new LocalBinder(); // Binder para comunicação com o serviço

    /**
     * Classe interna para fornecer acesso ao serviço via binding.
     */
    public class LocalBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    /**
     * Toca uma música específica com base no índice fornecido.
     * @param indice Índice da música na lista.
     */
    private void tocarMusica(int indice) {
        Log.d(TAG, "tocarMusica() CHAMADO - Índice: " + indice);

        if (musicas == null || musicas.isEmpty() || indice < 0 || indice >= musicas.size()) {
            Log.e(TAG, "FALHA: Lista inválida ou índice fora dos limites");
            return;
        }

        this.indiceActual = indice;
        Musica musicaParaTocar = musicas.get(indice);
        Log.d(TAG, "Preparando música: " + musicaParaTocar.getTitulo());

        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            } else {
                mediaPlayer.reset();
            }

            if (musicaParaTocar.isDeviceMusic()) {
                String contentUriString = musicaParaTocar.getContentUri();
                if (contentUriString != null && !contentUriString.isEmpty()) {
                    Uri songUri = Uri.parse(contentUriString);
                    Log.d(TAG, "Tocando da Content URI: " + songUri);
                    mediaPlayer.setDataSource(getApplicationContext(), songUri);
                } else {
                    Log.w(TAG, "Content URI vazio, tentando path: " + musicaParaTocar.getFilePath());
                    mediaPlayer.setDataSource(musicaParaTocar.getFilePath());
                }
            } else {
                Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + musicaParaTocar.getNumeroFaixa());
                Log.d(TAG, "Tocando do resource: " + uri);
                mediaPlayer.setDataSource(this, uri);
            }

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "ONPREPARED - Música PRONTA!");
                currentDuration = mp.getDuration();
                currentPosition = 0;
                isPlaying = true;
                mp.start();
                startProgressUpdates();
                updateMediaSessionState();
                showNotification();
                sendSongUpdateBroadcast();
            });

            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);

            Log.d(TAG, "Chamando prepareAsync()...");
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "ERRO FATAL em tocarMusica", e);
            Toast.makeText(this, "Erro ao tocar música: " + e.getMessage(), Toast.LENGTH_LONG).show();
            isPlaying = false;
            if (mediaPlayer != null) {
                mediaPlayer.reset();
            }
        }
    }

    /**
     * Manipula erros do MediaPlayer.
     * @param mp MediaPlayer que gerou o erro.
     * @param what Tipo de erro.
     * @param extra Informações adicionais sobre o erro.
     * @return True para indicar que o erro foi tratado.
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer Error - What: " + what + " Extra: " + extra);
        isPlaying = false;

        if (mediaPlayer != null) {
            mediaPlayer.reset();
        }

        return true;
    }

    /**
     * Pausa a reprodução da música atual.
     */
    private void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            currentPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            isPlaying = false;
            stopProgressUpdates();

            sendSongUpdateBroadcast();
            showNotification();
            updateMediaSessionState();

            Log.d(TAG, "Música pausada em: " + formatTime(currentPosition) +
                    " - isPlaying: " + isPlaying);
        } else {
            Log.w(TAG, "MediaPlayer não está tocando - não é possível pausar");
        }
    }

    /**
     * Toca a próxima música na lista, considerando o modo aleatório.
     */
    private void next() {
        if (musicas == null || musicas.isEmpty()) return;
        if (shuffleState) {
            indiceActual = (int) (Math.random() * musicas.size());
        } else {
            indiceActual = (indiceActual + 1) % musicas.size();
        }
        tocarMusica(indiceActual);
    }

    /**
     * Toca a música anterior ou reinicia a atual, considerando o modo aleatório.
     */
    private void previous() {
        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 3000) {
            tocarMusica(indiceActual);
        } else {
            if (musicas == null || musicas.isEmpty()) return;
            if (shuffleState) {
                indiceActual = (int) (Math.random() * musicas.size());
            } else {
                indiceActual = (indiceActual - 1 + musicas.size()) % musicas.size();
            }
            tocarMusica(indiceActual);
        }
    }

    /**
     * Vincula o serviço a um cliente.
     * @param intent Intent usado para binding.
     * @return IBinder para comunicação com o serviço.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Inicializa o serviço, criando o canal de notificação e configurando atualizações de progresso.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        setupProgressUpdater();
        setupMediaSession();
        Log.d(TAG, "MediaPlayerService criado");
    }

    /**
     * Configura a MediaSessionCompat para integração com controles do sistema.
     */
    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(this, "MusicPlayer");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                resume();
            }

            @Override
            public void onPause() {
                super.onPause();
                pause();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                next();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                previous();
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
                seekTo((int) pos);
            }

            @Override
            public void onFastForward() {
                super.onFastForward();
                seekForward();
            }

            @Override
            public void onRewind() {
                super.onRewind();
                seekBackward();
            }
        });

        mediaSession.setActive(true);
        Log.d(TAG, "MediaSession configurado");
    }

    /**
     * Manipula comandos recebidos via Intent.
     * @param intent Intent com a ação a ser executada.
     * @param flags Flags adicionais.
     * @param startId ID de início do comando.
     * @return Modo de reinicialização do serviço (START_STICKY).
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "Intent ou Action NULL");
            return START_STICKY;
        }

        String action = intent.getAction();
        Log.d(TAG, "onStartCommand - Ação: " + action);

        switch (action) {
            case ACTION_PLAY:
                Log.d(TAG, "ACTION_PLAY - Processando...");

                if (intent.hasExtra("SONG_LIST")) {
                    ArrayList<Musica> receivedList = (ArrayList<Musica>) intent.getSerializableExtra("SONG_LIST");
                    if (receivedList != null && !receivedList.isEmpty()) {
                        this.musicas = receivedList;
                        Log.d(TAG, "Lista de músicas recebida no Service: " + receivedList.size() + " músicas");
                    }
                }

                if (intent.hasExtra("SONG_INDEX")) {
                    int novoIndice = intent.getIntExtra("SONG_INDEX", -1);
                    Log.d(TAG, "Tocando música no índice: " + novoIndice);

                    if (musicas != null && !musicas.isEmpty() && novoIndice >= 0 && novoIndice < musicas.size()) {
                        tocarMusica(novoIndice);
                    } else {
                        Log.w(TAG, "Lista de músicas não disponível no Service");
                        showNotification();
                    }
                } else {
                    Log.d(TAG, "Play/Resume da música atual");
                    play();
                }
                break;

            case ACTION_PAUSE:
                Log.d(TAG, "ACTION_PAUSE - Processando...");
                pause();
                break;

            case ACTION_SEEK_TO:
                Log.d(TAG, "ACTION_SEEK_TO RECEBIDO NO SERVICE");

                Bundle extras = intent.getExtras();
                if (extras != null) {
                    Log.d(TAG, "Extras disponíveis no ACTION_SEEK_TO:");
                    for (String key : extras.keySet()) {
                        Log.d(TAG, "   " + key + " = " + extras.get(key));
                    }
                }

                if (intent.hasExtra("SEEK_POSITION")) {
                    int seekPosition = intent.getIntExtra("SEEK_POSITION", 0);
                    Log.d(TAG, "Processando seek para: " + seekPosition + "ms (" + formatTime(seekPosition) + ")");
                    seekTo(seekPosition);
                } else {
                    Log.e(TAG, "ACTION_SEEK_TO SEM SEEK_POSITION EXTRA!");
                }
                break;

            case ACTION_NEXT:
                next();
                break;

            case ACTION_PREV:
                previous();
                break;

            case "SYNC_STATE":
                handleSyncState(intent);
                break;

            default:
                Log.w(TAG, "Ação desconhecida: " + action);
        }

        return START_STICKY;
    }

    /**
     * Manipula a conclusão de uma música, considerando o modo de repetição.
     * @param mp MediaPlayer que concluiu a reprodução.
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "Música concluída - Repeat: " + repeatState + " - Shuffle: " + shuffleState);

        if (repeatState) {
            Log.d(TAG, "Repeat ativado - Reiniciando mesma música");
            currentPosition = 0;
            tocarMusica(indiceActual);
        } else {
            Log.d(TAG, "Repeat desativado - Indo para próxima música");
            currentPosition = 0;
            next();
        }
    }

    /**
     * Avança a reprodução para uma posição específica.
     * @param position Posição em milissegundos para avançar.
     */
    private void seekTo(int position) {
        Log.d(TAG, "SERVICE.seekTo() INICIADO - Posição: " + position);

        if (mediaPlayer == null) {
            Log.e(TAG, "MediaPlayer é NULL - não é possível fazer seek");
            return;
        }

        if (position < 0) {
            Log.w(TAG, "Posição negativa, corrigindo para 0");
            position = 0;
        }

        try {
            progressHandler.removeCallbacks(progressRunnable);
            Log.d(TAG, "Progress updates PARADOS para seek");

            int currentDuration = mediaPlayer.getDuration();
            if (position > currentDuration) {
                Log.w(TAG, "Posição maior que duração, corrigindo para: " + currentDuration);
                position = currentDuration;
            }

            Log.d(TAG, "Executando mediaPlayer.seekTo(" + position + ")");

            mediaPlayer.seekTo(position);
            this.currentPosition = position;

            Log.d(TAG, "SEEK EXECUTADO COM SUCESSO - Nova posição: " + position +
                    " (" + formatTime(position) + ")");

            Intent seekIntent = new Intent(BROADCAST_UPDATE);
            seekIntent.putExtra(EXTRA_PROGRESS, position);
            seekIntent.putExtra(EXTRA_DURATION, currentDuration);
            seekIntent.putExtra(EXTRA_IS_PLAYING, isPlaying);
            seekIntent.putExtra("is_seeking", true);
            seekIntent.putExtra("seek_manual", true);
            seekIntent.putExtra("current_song_index", indiceActual);

            if (musicas != null && !musicas.isEmpty() && indiceActual < musicas.size()) {
                seekIntent.putExtra("song_title", musicas.get(indiceActual).getTitulo());
                seekIntent.putExtra("song_artist", musicas.get(indiceActual).getArtista());
            }

            sendBroadcast(seekIntent);
            Log.d(TAG, "BROADCAST DE SEEK ENVIADO: " + formatTime(position));

            new Handler().postDelayed(() -> {
                if (isPlaying && mediaPlayer != null) {
                    startProgressUpdates();
                    Log.d(TAG, "Progress updates REINICIADOS após seek");
                }
            }, 2000);

        } catch (Exception e) {
            Log.e(TAG, "ERRO CRÍTICO no seekTo", e);
            if (isPlaying) {
                startProgressUpdates();
            }
        }
    }

    /**
     * Avança a reprodução em 15 segundos.
     */
    private void seekForward() {
        if (mediaPlayer != null) {
            int newPosition = currentPosition + 15000;
            if (newPosition > currentDuration) {
                newPosition = currentDuration;
            }
            seekTo(newPosition);
            Log.d(TAG, "Seek forward para: " + newPosition);
        }
    }

    /**
     * Retrocede a reprodução em 15 segundos.
     */
    private void seekBackward() {
        if (mediaPlayer != null) {
            int newPosition = currentPosition - 15000;
            if (newPosition < 0) {
                newPosition = 0;
            }
            seekTo(newPosition);
            Log.d(TAG, "Seek backward para: " + newPosition);
        }
    }

    /**
     * Obtém o ID do áudio com base no caminho do arquivo.
     * @param filePath Caminho do arquivo de áudio.
     * @return ID do áudio ou -1 se não encontrado.
     */
    private long getAudioIdFromPath(String filePath) {
        Cursor cursor = null;
        try {
            ContentResolver resolver = getApplicationContext().getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = { MediaStore.Audio.Media._ID };
            String selection = MediaStore.Audio.Media.DATA + " = ?";
            String[] selectionArgs = { filePath };

            cursor = resolver.query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                return cursor.getLong(idColumn);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar ID do áudio", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    /**
     * Atualiza o estado da MediaSession com base na reprodução atual.
     */
    private void updateMediaSessionState() {
        if (mediaSession != null && mediaPlayer != null) {
            android.support.v4.media.session.PlaybackStateCompat.Builder stateBuilder =
                    new android.support.v4.media.session.PlaybackStateCompat.Builder()
                            .setActions(android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY |
                                    android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE |
                                    android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                    android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                    android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO |
                                    android.support.v4.media.session.PlaybackStateCompat.ACTION_FAST_FORWARD |
                                    android.support.v4.media.session.PlaybackStateCompat.ACTION_REWIND)
                            .setState(isPlaying ?
                                            android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING :
                                            android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED,
                                    mediaPlayer.getCurrentPosition(), 1.0f);

            mediaSession.setPlaybackState(stateBuilder.build());

            if (musicas != null && !musicas.isEmpty() && indiceActual < musicas.size()) {
                android.support.v4.media.MediaMetadataCompat.Builder metadataBuilder =
                        new android.support.v4.media.MediaMetadataCompat.Builder()
                                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE,
                                        musicas.get(indiceActual).getTitulo())
                                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST,
                                        musicas.get(indiceActual).getArtista())
                                .putLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION,
                                        mediaPlayer.getDuration());

                mediaSession.setMetadata(metadataBuilder.build());
            }
        }
    }

    /**
     * Configura o atualizador de progresso para enviar atualizações periódicas.
     */
    private void setupProgressUpdater() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying && mediaPlayer.isPlaying()) {
                    try {
                        int newPosition = mediaPlayer.getCurrentPosition();
                        int newDuration = mediaPlayer.getDuration();

                        if (newDuration > 0) {
                            currentPosition = newPosition;
                            currentDuration = newDuration;

                            if (currentPosition % 3000 < 50) {
                                Log.d(TAG, "SEEKBAR UPDATE - Pos: " + formatTime(currentPosition) +
                                        "/" + formatTime(currentDuration));
                            }

                            Intent updateIntent = new Intent(BROADCAST_UPDATE);
                            updateIntent.putExtra(EXTRA_PROGRESS, currentPosition);
                            updateIntent.putExtra(EXTRA_DURATION, currentDuration);
                            updateIntent.putExtra(EXTRA_IS_PLAYING, isPlaying);
                            updateIntent.putExtra("is_seeking", false);

                            if (musicas != null && !musicas.isEmpty() && indiceActual < musicas.size()) {
                                updateIntent.putExtra("song_title", musicas.get(indiceActual).getTitulo());
                                updateIntent.putExtra("song_artist", musicas.get(indiceActual).getArtista());
                                updateIntent.putExtra("current_song_index", indiceActual);
                            }

                            sendBroadcast(updateIntent);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Erro no progress updater", e);
                    }

                    progressHandler.postDelayed(this, 1000);
                } else {
                    Log.d(TAG, "Progress updates pausados - Estado: " +
                            "MediaPlayer: " + (mediaPlayer != null) +
                            ", isPlaying: " + isPlaying +
                            ", isActuallyPlaying: " + (mediaPlayer != null && mediaPlayer.isPlaying()));
                }
            }
        };
    }

    /**
     * Atualiza a notificação com o progresso atual da música.
     */
    private void updateNotificationProgress() {
        if (isPlaying && currentDuration > 0) {
            showNotification();
        }
    }

    /**
     * Inicia as atualizações periódicas do progresso da música.
     */
    private void startProgressUpdates() {
        Log.d(TAG, "Tentando iniciar progress updates...");

        progressHandler.removeCallbacks(progressRunnable);

        boolean canStart = (mediaPlayer != null) && isPlaying;

        if (canStart) {
            progressHandler.post(progressRunnable);
            Log.d(TAG, "PROGRESS UPDATES INICIADO COM SUCESSO");
        } else {
            Log.w(TAG, "Não foi possível iniciar progress updates - " +
                    "MediaPlayer: " + (mediaPlayer != null) +
                    ", isPlaying: " + isPlaying);
        }
    }

    /**
     * Para as atualizações periódicas do progresso da música.
     */
    private void stopProgressUpdates() {
        Log.d(TAG, "Parando progress updates...");
        progressHandler.removeCallbacks(progressRunnable);
        Log.d(TAG, "Progress updates parados");
    }

    /**
     * Envia um broadcast com informações atualizadas da música.
     */
    private void sendSongUpdateBroadcast() {
        try {
            if (musicas != null && !musicas.isEmpty() && indiceActual < musicas.size()) {
                Intent intent = new Intent(BROADCAST_UPDATE);

                intent.putExtra("current_song_index", indiceActual);
                intent.putExtra("song_title", musicas.get(indiceActual).getTitulo());
                intent.putExtra("song_artist", musicas.get(indiceActual).getArtista());

                intent.putExtra("is_playing", isPlaying);
                intent.putExtra(EXTRA_IS_PLAYING, isPlaying);

                intent.putExtra("current_position", currentPosition);
                intent.putExtra("current_duration", currentDuration);
                intent.putExtra(EXTRA_PROGRESS, currentPosition);
                intent.putExtra(EXTRA_DURATION, currentDuration);

                sendBroadcast(intent);
                Log.d(TAG, "BROADCAST ENVIADO - Música: " + musicas.get(indiceActual).getTitulo() +
                        ", Índice: " + indiceActual);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao enviar broadcast", e);
        }
    }

    /**
     * Cria o canal de notificação para Android 8.0+.
     */
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Media Player Notification");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            Log.d(TAG, "Canal de notificação criado");
        }
    }

    /**
     * Exibe uma notificação com controles de mídia e progresso.
     */
    @SuppressLint("ForegroundServiceType")
    private void showNotification() {
        if (musicas == null || musicas.isEmpty() || indiceActual >= musicas.size()) return;

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("from_notification", true);
        intent.putExtra("current_song_index", indiceActual);
        intent.putExtra("is_playing", isPlaying);
        intent.putExtra("current_position", currentPosition);
        intent.putExtra("current_duration", currentDuration);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(this, MusicService.class);
        playPauseIntent.setAction(isPlaying ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 1, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 2, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent = new Intent(this, MusicService.class);
        prevIntent.setAction(ACTION_PREV);
        PendingIntent prevPendingIntent = PendingIntent.getService(this, 3, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent seekForwardIntent = new Intent(this, MusicService.class);
        seekForwardIntent.setAction(ACTION_SEEK_FORWARD);
        PendingIntent seekForwardPendingIntent = PendingIntent.getService(this, 4, seekForwardIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent seekBackwardIntent = new Intent(this, MusicService.class);
        seekBackwardIntent.setAction(ACTION_SEEK_BACKWARD);
        PendingIntent seekBackwardPendingIntent = PendingIntent.getService(this, 5, seekBackwardIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String currentTimeText = formatTime(currentPosition);
        String totalTimeText = formatTime(currentDuration);
        String progressText = currentTimeText + " / " + totalTimeText;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(musicas.get(indiceActual).getTitulo())
                .setContentText(musicas.get(indiceActual).getArtista())
                .setSubText(progressText)
                .setSmallIcon(R.drawable.ic_music)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setOngoing(isPlaying)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2, 3, 4));

        if (currentDuration > 0) {
            builder.setProgress(currentDuration, currentPosition, false);
        }

        builder.addAction(R.drawable.ic_replay_10, "-15s", seekBackwardPendingIntent);
        builder.addAction(R.drawable.ic_skip_previous, "Previous", prevPendingIntent);
        builder.addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play,
                isPlaying ? "Pause" : "Play", playPausePendingIntent);
        builder.addAction(R.drawable.ic_skip_next, "Next", nextPendingIntent);
        builder.addAction(R.drawable.ic_forward_10, "+15s", seekForwardPendingIntent);

        Notification notification = builder.build();

        startForeground(NOTIFICATION_ID, notification);
        Log.d(TAG, "Notificação atualizada - Progresso: " + currentPosition + "/" + currentDuration);
    }

    /**
     * Formata o tempo em milissegundos para minutos e segundos.
     * @param millis Tempo em milissegundos.
     * @return String formatada no formato "MM:SS".
     */
    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds %= 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Libera recursos ao destruir o serviço.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopProgressUpdates();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        stopForeground(true);
        Log.d(TAG, "MediaPlayerService destruído");
    }

    /**
     * Verifica se uma música está sendo reproduzida.
     * @return True se a música está tocando, false caso contrário.
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Obtém a posição atual da música em reprodução.
     * @return Posição atual em milissegundos.
     */
    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : currentPosition;
    }

    /**
     * Obtém a duração total da música atual.
     * @return Duração em milissegundos.
     */
    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : currentDuration;
    }

    /**
     * Obtém a música atual.
     * @return Objeto Musica da música atual ou null se não disponível.
     */
    public Musica getCurrentSong() {
        return musicas != null && !musicas.isEmpty() && indiceActual < musicas.size() ?
                musicas.get(indiceActual) : null;
    }

    /**
     * Obtém o índice da música atual.
     * @return Índice da música atual.
     */
    public int getCurrentSongIndex() {
        return indiceActual;
    }

    /**
     * Define a posição atual da música.
     * @param position Posição em milissegundos.
     */
    public void setCurrentPosition(int position) {
        this.currentPosition = position;
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    /**
     * Define o modelo de dados para o serviço.
     * @param model Modelo de dados a ser usado.
     */
    public void setModel(Model model) {
        this.model = model;
        if (model != null && musicas != null) {
            this.indiceActual = model.getIndiceActual();
            Log.d(TAG, "Model definido no Service - Índice: " + indiceActual);
        }
    }

    /**
     * Obtém informações de depuração do serviço.
     * @return String com informações de estado.
     */
    public String getDebugInfo() {
        return "Service Debug - Repeat: " + repeatState +
                ", Shuffle: " + shuffleState +
                ", Índice: " + indiceActual +
                ", Playing: " + isPlaying;
    }

    /**
     * Sincroniza o estado do serviço com base em um Intent.
     * @param intent Intent com informações de sincronização.
     */
    private void handleSyncState(Intent intent) {
        try {
            boolean shuffleState = intent.getBooleanExtra("SHUFFLE_STATE", false);
            boolean repeatState = intent.getBooleanExtra("REPEAT_STATE", false);
            int currentIndex = intent.getIntExtra("CURRENT_INDEX", 0);

            Log.d(TAG, "Sincronizando estado - Shuffle: " + shuffleState +
                    ", Repeat: " + repeatState + ", Índice: " + currentIndex);

            this.repeatState = repeatState;
            this.shuffleState = shuffleState;

            if (currentIndex != this.indiceActual) {
                this.indiceActual = currentIndex;
                Log.d(TAG, "Índice atualizado para: " + currentIndex);
            }

            updateMediaSessionState();
            showNotification();

            Log.d(TAG, "Estado sincronizado com sucesso");

        } catch (Exception e) {
            Log.e(TAG, "Erro ao sincronizar estado", e);
        }
    }

    /**
     * Inicia ou retoma a reprodução da música atual.
     */
    private void play() {
        Log.d(TAG, "play() chamado - Estado atual: " + isPlaying);

        if (!isPlaying) {
            showNotification();
            Log.d(TAG, "Notificação de foreground exibida para evitar ANR");
        }

        if (mediaPlayer != null && !isPlaying) {
            resume();
        } else {
            if (musicas != null && !musicas.isEmpty()) {
                tocarMusica(indiceActual);
            } else {
                Log.e(TAG, "Não é possível tocar - lista de músicas vazia");
                showNotification();
            }
        }
    }

    /**
     * Retoma a reprodução da música pausada.
     */
    private void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;

            sendSongUpdateBroadcast();
            startProgressUpdates();
            showNotification();
            updateMediaSessionState();

            Log.d(TAG, "Música retomada - isPlaying: " + isPlaying);
        } else {
            Log.w(TAG, "Não é possível retomar - MediaPlayer: " +
                    (mediaPlayer != null) + ", Playing: " + (mediaPlayer != null && mediaPlayer.isPlaying()));
        }
    }

    /**
     * Define a lista de músicas para o serviço.
     * @param musicList Lista de músicas a ser definida.
     */
    public void setMusicList(List<Musica> musicList) {
        if (musicList != null && !musicList.isEmpty()) {
            this.musicas = new ArrayList<>(musicList);
            Log.d(TAG, "Lista de músicas definida no Service: " + musicList.size() + " músicas");
        } else {
            Log.w(TAG, "Tentativa de definir lista vazia no Service");
        }
    }
}