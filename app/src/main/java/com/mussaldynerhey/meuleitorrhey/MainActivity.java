package com.mussaldynerhey.meuleitorrhey;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import Model.Musica;
import presenter.Presenter;
import view.Contrato;

public class MainActivity extends AppCompatActivity implements Contrato.View {
    private static final String TAG = "MainActivity"; // Variável TAG armazena o nome para logs.
    private Presenter presenter; // Variável presenter gerencia a lógica de negócios.

    private TextView songTitle, artistName, currentTime, totalTime; // Variáveis para exibir título, artista e tempos da música.
    private ImageButton playPauseBtn, prevBtn, nextBtn, searchButton, allSongsButton; // Botões para controle de reprodução e busca.
    private ImageView shuffleBtn, repeatBtn, albumArt; // Botões para embaralhar, repetir e exibir arte do álbum.
    private SeekBar seekBar; // Barra de progresso da música.
    private RecyclerView songListRecyclerView; // Lista de músicas exibida na tela.
    private MusicListAdapter songAdapter; // Adaptador para a lista de músicas.

    private boolean isReceiverRegistered = false; // Variável isReceiverRegistered indica se o receiver está registrado.

    private static final int PERMISSION_REQUEST_CODE = 100; // Código para solicitação de permissões gerais.
    private static final int MANAGE_STORAGE_REQUEST_CODE = 101; // Código para gerenciar armazenamento.
    private static final int MUSIC_LIST_REQUEST_CODE = 102; // Código para retorno da lista de músicas.
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 200; // Código para permissão de áudio.
    private static final int PLAYLIST_REQUEST_CODE = 103; // Código para retorno da tela de playlist.

    private boolean isWaitingForPermission = false; // Variável isWaitingForPermission indica espera por permissões.

    private boolean isSeeking = false; // Variável isSeeking indica se o usuário está manipulando a barra de progresso.

    private RotateAnimation rotateAnimation; // Animação de rotação para a arte do álbum.
    private boolean isAnimationRunning = false; // Variável isAnimationRunning indica se a animação está ativa.

    private Musica musicaAtual; // Variável musicaAtual armazena a música em reprodução.

    private boolean isPlaying = false; // Variável isPlaying indica se a música está tocando.

    private List<Musica> todasMusicas; // Lista todasMusicas contém todas as músicas disponíveis.
    private boolean isSearchVisible = false; // Variável isSearchVisible indica se a busca está visível.

    private Handler uiHandler = new Handler(); // Variável uiHandler gerencia atualizações na interface.

    private androidx.appcompat.widget.SearchView searchView; // Variável searchView representa o componente de busca.

    private ACRCloudRecognizer acrCloudRecognizer; // Variável acrCloudRecognizer gerencia o reconhecimento de músicas.
    private boolean isIdentifying = false; // Variável isIdentifying indica se o reconhecimento está ativo.

    private Handler identificationTimeoutHandler = new Handler(); // Variável identificationTimeoutHandler controla o tempo limite de identificação.
    private static final long IDENTIFICATION_TIMEOUT = 30000; // Variável IDENTIFICATION_TIMEOUT define 30 segundos para timeout.

    private BroadcastReceiver songUpdateReceiver; // Variável songUpdateReceiver recebe atualizações do serviço de música.

    private void updateSongInfo(String titulo, String artista) { // Método updateSongInfo atualiza título e artista na interface.
        if (titulo != null) {
            songTitle.setText(titulo);
        }
        if (artista != null) {
            artistName.setText(artista);
        }
    }

    @Override
    public void playNewList(ArrayList<Musica> songs, int index) { // Método playNewList inicia uma nova lista de reprodução.
        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_PLAY);
        serviceIntent.putExtra("SONG_LIST", songs);
        serviceIntent.putExtra("SONG_INDEX", index);
        startService(serviceIntent);
    }

    @Override
    public void showFilteredSongs(List<Musica> musicas) { // Método showFilteredSongs atualiza a lista de músicas filtradas.
        if (songAdapter != null) {
            songAdapter.updateSongs(musicas);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) { // Método onCreate inicializa a atividade principal.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        musicaAtual = null;
        todasMusicas = new ArrayList<>();

        ImageButton menuButton = findViewById(R.id.all_songs_button); // Botão menuButton carrega todas as músicas.
        menuButton.setOnClickListener(v -> {
            Log.d(TAG, "Botão hamburguer clicado - Carregando todas as músicas do dispositivo");
            loadAllDeviceSongs();
        });

        TextView mediaPlayerLabel = findViewById(R.id.media_player_label); // Botão mediaPlayerLabel exibe a playlist atual.
        mediaPlayerLabel.setOnClickListener(v -> {
            Log.d(TAG, "Botão '> Media Player' clicado - Mostrando músicas da playlist atual");
            showCurrentPlaylistSongs();
        });

        Toolbar toolbar = findViewById(R.id.toolbar); // Variável toolbar representa a barra de ferramentas.
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        songTitle = findViewById(R.id.song_title); // Variável songTitle exibe o título da música.
        artistName = findViewById(R.id.artist_name); // Variável artistName exibe o nome do artista.
        currentTime = findViewById(R.id.current_time); // Variável currentTime exibe o tempo atual da música.
        totalTime = findViewById(R.id.total_time); // Variável totalTime exibe a duração total da música.
        playPauseBtn = findViewById(R.id.play_pause_button); // Botão playPauseBtn controla play e pause.
        prevBtn = findViewById(R.id.prev_button); // Botão prevBtn vai para a música anterior.
        nextBtn = findViewById(R.id.next_button); // Botão nextBtn vai para a próxima música.
        shuffleBtn = findViewById(R.id.shuffleButton); // Botão shuffleBtn ativa/desativa o modo aleatório.
        repeatBtn = findViewById(R.id.repeatButton); // Botão repeatBtn ativa/desativa o modo repetição.
        seekBar = findViewById(R.id.seek_bar); // Variável seekBar controla o progresso da música.
        songListRecyclerView = findViewById(R.id.song_list); // Variável songListRecyclerView exibe a lista de músicas.
        albumArt = findViewById(R.id.imageView2); // Variável albumArt exibe a arte do álbum.

        searchButton = findViewById(R.id.search_button); // Botão searchButton ativa a busca.
        allSongsButton = findViewById(R.id.all_songs_button); // Botão allSongsButton carrega todas as músicas.

        songListRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Configura o layout da lista de músicas.

        setupRotationAnimation(); // Método setupRotationAnimation configura a animação de rotação.

        mediaPlayerLabel = findViewById(R.id.media_player_label); // Botão mediaPlayerLabel abre a lista de músicas.
        mediaPlayerLabel.setOnClickListener(v -> {
            openMusicListActivity();
        });

        allSongsButton.setOnClickListener(v -> {
            Log.d(TAG, "Botão 'Todas as Músicas' clicado - Carregando todas as músicas do dispositivo");
            loadAllDeviceSongs();
        });

        setupSearchButton(); // Método setupSearchButton configura o botão de busca.

        setupMusicIdentification(); // Método setupMusicIdentification configura o reconhecimento de música.

        checkAndRequestPermissions(); // Método checkAndRequestPermissions verifica e solicita permissões.

        checkNotificationIntent(getIntent()); // Método checkNotificationIntent verifica intenções de notificação.
        playlistActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        ArrayList<com.mussaldynerhey.meuleitorrhey.database.entities.Song> playlistSongs =
                                (ArrayList<com.mussaldynerhey.meuleitorrhey.database.entities.Song>) data.getSerializableExtra("SONG_LIST_RESULT");
                        int songIndex = data.getIntExtra("SONG_INDEX_RESULT", -1);

                        if (playlistSongs != null && !playlistSongs.isEmpty() && songIndex != -1) {
                            Log.d(TAG, "Recebido da playlist: " + playlistSongs.size() + " músicas, tocando a de índice " + songIndex);

                            List<Musica> musicasParaTocar = new ArrayList<>();
                            for (com.mussaldynerhey.meuleitorrhey.database.entities.Song song : playlistSongs) {
                                musicasParaTocar.add(new Musica(
                                        song.getTitle(),
                                        song.getArtist(),
                                        song.getPath(),
                                        song.getDuration(),
                                        song.getAlbumArtPath()
                                ));
                            }

                            if (presenter != null) {
                                presenter.playNewList(musicasParaTocar, songIndex);
                            }
                        }
                    }
                }
        );

        playPauseBtn.setOnClickListener(v -> { // Botão playPauseBtn alterna entre play e pause.
            Log.d(TAG, "Botão clicado - Estado ANTES: " + (isPlaying ? "PLAYING" : "PAUSED"));
            if (presenter != null) {
                if (isPlaying) {
                    Log.d(TAG, "Enviando PAUSE para presenter");
                    presenter.onPauseClicked();
                } else {
                    Log.d(TAG, "Enviando PLAY para presenter");
                    presenter.onPlayClicked();
                }
            }
            Log.d(TAG, "Comando enviado - Aguardando confirmação do Service");
        });

        nextBtn.setOnClickListener(v -> { // Botão nextBtn toca a próxima música.
            presenter.next();
            resetAlbumRotation();
        });

        prevBtn.setOnClickListener(v -> { // Botão prevBtn toca a música anterior.
            presenter.previous();
            resetAlbumRotation();
        });

        shuffleBtn.setOnClickListener(v -> presenter.toggleShuffle()); // Botão shuffleBtn alterna o modo aleatório.
        repeatBtn.setOnClickListener(v -> presenter.toggleRepeat()); // Botão repeatBtn alterna o modo repetição.

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // Configura a barra de progresso.
            private int seekStartProgress = 0;
            private long seekStartTime = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Log.d(TAG, "SEEKING - Progresso mudou para: " + progress + " (" + formatTime(progress) + ")");
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekStartProgress = seekBar.getProgress();
                seekStartTime = System.currentTimeMillis();
                isSeeking = true;
                Log.d(TAG, "SEEK INICIADO - Posição inicial: " + seekStartProgress +
                        " (" + formatTime(seekStartProgress) + ") | isSeeking = TRUE");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int finalProgress = seekBar.getProgress();
                long seekDuration = System.currentTimeMillis() - seekStartTime;
                Log.d(TAG, "SEEK FINALIZADO - Posição final: " + finalProgress +
                        " (" + formatTime(finalProgress) + ") | Duração: " + seekDuration + "ms");
                if (presenter != null) {
                    Log.d(TAG, "ENVIANDO SEEK PARA PRESENTER: " + finalProgress);
                    presenter.seekTo(finalProgress);
                } else {
                    Log.e(TAG, "PRESENTER É NULL - não é possível enviar seek");
                }
                new Handler().postDelayed(() -> {
                    isSeeking = false;
                    Log.d(TAG, "isSeeking = FALSE (updates automáticos retomados)");
                }, 1000);
            }
        });

        if (getIntent().getBooleanExtra("start_recognition", false)) {
            new Handler().postDelayed(() -> {
                showMusicRecognitionDialog();
            }, 500);
        }
        initializeBroadcastReceiver(); // Método initializeBroadcastReceiver configura o receiver de atualizações.
    }

    private void loadAllDeviceSongs() { // Método loadAllDeviceSongs carrega todas as músicas do dispositivo.
        Log.d(TAG, "Carregando todas as músicas do dispositivo...");
        if (presenter != null) {
            presenter.forceReloadSongs();
            Toast.makeText(this, "Carregando todas as músicas do dispositivo...", Toast.LENGTH_SHORT).show();
            updateSongsHeader("Todas as Músicas do Dispositivo");
            Log.d(TAG, "Comando para carregar todas as músicas enviado");
        } else {
            Log.e(TAG, "Presenter é null - não é possível carregar músicas");
            Toast.makeText(this, "Erro ao carregar músicas", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCurrentPlaylistSongs() { // Método showCurrentPlaylistSongs exibe a playlist atual.
        Log.d(TAG, "Mostrando músicas da playlist atual...");
        if (todasMusicas != null && !todasMusicas.isEmpty()) {
            if (songAdapter != null) {
                songAdapter.updateSongs(todasMusicas);
                songAdapter.notifyDataSetChanged();
                if (isFromPlaylist()) {
                    updateSongsHeader("Playlist Atual (" + todasMusicas.size() + " músicas)");
                    Toast.makeText(this, "Mostrando " + todasMusicas.size() + " músicas da playlist atual", Toast.LENGTH_SHORT).show();
                } else {
                    updateSongsHeader("Todas as Músicas (" + todasMusicas.size() + ")");
                    Toast.makeText(this, "Mostrando todas as " + todasMusicas.size() + " músicas", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "Lista atualizada com " + todasMusicas.size() + " músicas");
            }
        } else {
            Log.w(TAG, "Lista de músicas vazia - não é possível mostrar playlist atual");
            Toast.makeText(this, "Nenhuma música disponível", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSongsHeader(String title) { // Método updateSongsHeader atualiza o título da seção de músicas.
        TextView songsHeader = findViewById(R.id.songs_header);
        if (songsHeader != null) {
            songsHeader.setText(title);
        }
    }

    private boolean isFromPlaylist() { // Método isFromPlaylist verifica se a lista é uma playlist.
        return todasMusicas != null && todasMusicas.size() < getTotalDeviceSongs();
    }

    private int getTotalDeviceSongs() { // Método getTotalDeviceSongs retorna o total estimado de músicas.
        return 50;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() { // Método onResume registra o receiver de atualizações.
        super.onResume();
        if (!isReceiverRegistered) {
            try {
                IntentFilter filter = new IntentFilter(MusicService.BROADCAST_UPDATE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(songUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    registerReceiver(songUpdateReceiver, filter);
                }
                isReceiverRegistered = true;
                Log.d(TAG, "BroadcastReceiver registrado com sucesso para SeekBar updates");
            } catch (Exception e) {
                Log.e(TAG, "Erro ao registrar BroadcastReceiver", e);
            }
        }
    }

    @Override
    protected void onPause() { // Método onPause desregistra o receiver.
        super.onPause();
        if (isReceiverRegistered) {
            unregisterReceiver(songUpdateReceiver);
            isReceiverRegistered = false;
            Log.d(TAG, "BroadcastReceiver desregistado com sucesso.");
        }
    }

    private void initializeBroadcastReceiver() { // Método initializeBroadcastReceiver configura o receiver de atualizações.
        songUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || !MusicService.BROADCAST_UPDATE.equals(intent.getAction())) {
                    return;
                }
                Log.d(TAG, "Broadcast recebido - Atualizando UI");
                boolean serviceIsPlaying = intent.getBooleanExtra("is_playing", false);
                boolean extraIsPlaying = intent.getBooleanExtra(MusicService.EXTRA_IS_PLAYING, false);
                boolean isActuallyPlaying = serviceIsPlaying || extraIsPlaying;
                MainActivity.this.isPlaying = isActuallyPlaying;
                int currentSongIndex = intent.getIntExtra("current_song_index", -1);
                if (currentSongIndex != -1 && todasMusicas != null && currentSongIndex < todasMusicas.size()) {
                    musicaAtual = todasMusicas.get(currentSongIndex);
                    Log.d(TAG, "Música atual atualizada: " +
                            (musicaAtual != null ? musicaAtual.getTitulo() : "NULL"));
                }
                runOnUiThread(() -> {
                    if (isActuallyPlaying) {
                        playPauseBtn.setImageResource(R.drawable.ic_pause);
                        startAlbumRotation();
                        Log.d(TAG, "UI: Botão atualizado para PAUSE");
                    } else {
                        playPauseBtn.setImageResource(R.drawable.ic_play);
                        stopAlbumRotation();
                        Log.d(TAG, "UI: Botão atualizado para PLAY");
                    }
                });
                String titulo = intent.getStringExtra("song_title");
                String artista = intent.getStringExtra("song_artist");
                if (titulo != null && artista != null) {
                    runOnUiThread(() -> {
                        songTitle.setText(titulo);
                        artistName.setText(artista);
                        Log.d(TAG, "UI: Música atualizada - " + titulo);
                    });
                }
                handleProgressUpdate(intent); // Método handleProgressUpdate atualiza a barra de progresso.
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { // Método onCreateOptionsMenu cria o menu da toolbar.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // Método onOptionsItemSelected processa cliques no menu.
        int id = item.getItemId();
        if (id == R.id.menu_music_recognition) {
            showMusicRecognitionDialog();
            return true;
        } else if (id == R.id.menu_music_details) {
            showMusicDetailsDialog();
            return true;
        } else if (id == R.id.menu_playlists) {
            Intent playlistIntent = new Intent(this, PlaylistActivity.class);
            playlistActivityLauncher.launch(playlistIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMusicRecognitionDialog() { // Método showMusicRecognitionDialog exibe o diálogo de reconhecimento.
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Reconhecimento de Música");
        builder.setMessage("Deseja identificar uma música que está tocando? O aplicativo irá ouvir o ambiente por 30 segundos.");
        builder.setPositiveButton("IDENTIFICAR MÚSICA", (dialog, which) -> {
            startMusicIdentification();
            dialog.dismiss();
        });
        builder.setNegativeButton("CANCELAR", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.setNeutralButton("BUSCAR NA LISTA", (dialog, which) -> {
            if (searchView != null && !isSearchVisible) {
                toggleSearchVisibility();
            }
            searchView.setQuery("", false);
            searchView.requestFocus();
            dialog.dismiss();
        });
        builder.show();
    }

    private void showRecognitionProgressDialog() { // Método showRecognitionProgressDialog exibe o progresso do reconhecimento.
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Ouvindo...");
        builder.setMessage("Toque uma música para identificação.\n\nTempo restante: 30 segundos");
        builder.setCancelable(false);
        androidx.appcompat.app.AlertDialog progressDialog = builder.create();
        progressDialog.show();
        final int[] timeLeft = {30};
        Handler countdownHandler = new Handler();
        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (isIdentifying && timeLeft[0] > 0) {
                    progressDialog.setMessage("Toque uma música para identificação.\n\nTempo restante: " + timeLeft[0] + " segundos");
                    timeLeft[0]--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    progressDialog.dismiss();
                }
            }
        };
        countdownHandler.postDelayed(countdownRunnable, 1000);
        progressDialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE, "CANCELAR", (dialog, which) -> {
            stopMusicIdentification();
            progressDialog.dismiss();
        });
    }

    private void setupMusicIdentification() { // Método setupMusicIdentification configura o reconhecimento de música.
        acrCloudRecognizer = new ACRCloudRecognizer(this, new ACRCloudRecognizer.RecognitionListener() {
            @Override
            public void onRecognitionResult(String title, String artist, String album) {
                runOnUiThread(() -> {
                    showRecognitionResult(title, artist, album);
                });
            }

            @Override
            public void onRecognitionError(String error) {
                runOnUiThread(() -> {
                    showRecognitionError(error);
                });
            }

            @Override
            public void onBackToSearch() {
                runOnUiThread(() -> {
                    stopMusicIdentification();
                });
            }
        });
    }

    private void showRecognitionError(String error) { // Método showRecognitionError exibe erros do reconhecimento.
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Música Não Encontrada");
        builder.setMessage("Não foi possível identificar a música.\n\nErro: " + error +
                "\n\nTente:\n• Aumentar o volume\n• Aproximar do som\n• Tentar outra música");
        builder.setPositiveButton("TENTAR NOVAMENTE", (dialog, which) -> {
            Toast.makeText(this, "Continuando a ouvir...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.setNeutralButton("BUSCAR NA LISTA", (dialog, which) -> {
            stopMusicIdentification();
            if (searchView != null && !isSearchVisible) {
                toggleSearchVisibility();
            }
            if (searchView != null) {
                searchView.setQuery("", false);
                searchView.requestFocus();
            } else {
                activateSearchManually();
            }
            dialog.dismiss();
        });
        builder.setNegativeButton("VOLTAR AO PLAYER", (dialog, which) -> {
            stopMusicIdentification();
            dialog.dismiss();
        });
        builder.setCancelable(true);
        builder.show();
    }

    private void showRecognitionResult(String title, String artist, String album) { // Método showRecognitionResult exibe o resultado do reconhecimento.
        boolean musicaExiste = false;
        if (todasMusicas != null) {
            for (Musica musica : todasMusicas) {
                String tituloMusica = musica.getTitulo() != null ? musica.getTitulo().toLowerCase() : "";
                String artistaMusica = musica.getArtista() != null ? musica.getArtista().toLowerCase() : "";
                String buscaTitle = title != null ? title.toLowerCase() : "";
                String buscaArtist = artist != null ? artist.toLowerCase() : "";
                if ((tituloMusica.contains(buscaTitle) || tituloMusica.contains(buscaArtist)) ||
                        (artistaMusica.contains(buscaTitle) || artistaMusica.contains(buscaArtist))) {
                    musicaExiste = true;
                    break;
                }
            }
        }
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        if (musicaExiste) {
            builder.setTitle("Música Identificada!");
            builder.setMessage("Título: " + title +
                    "\nArtista: " + artist +
                    (album != null && !album.isEmpty() ? "\nÁlbum: " + album : ""));
        } else {
            builder.setTitle("Música Identificada (Não na Lista)");
            builder.setMessage("Identificamos a música, mas ela não está na sua lista local.\n\n" +
                    "Título: " + title +
                    "\nArtista: " + artist +
                    (album != null && !album.isEmpty() ? "\nÁlbum: " + album : ""));
        }
        builder.setPositiveButton("BUSCAR ESTA MÚSICA", (dialog, which) -> {
            stopMusicIdentification();
            searchIdentifiedMusic(title, artist);
            dialog.dismiss();
        });
        builder.setNeutralButton("IDENTIFICAR OUTRA", (dialog, which) -> {
            Toast.makeText(this, "Continue tocando outra música...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.setNegativeButton("VOLTAR AO PLAYER", (dialog, which) -> {
            stopMusicIdentification();
            dialog.dismiss();
        });
        builder.setCancelable(true);
        try {
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao mostrar diálogo de resultado", e);
            Toast.makeText(this, "Erro ao mostrar resultado", Toast.LENGTH_SHORT).show();
        }
    }

    private void startMusicIdentification() { // Método startMusicIdentification inicia o reconhecimento de música.
        if (checkAudioPermission()) {
            isIdentifying = true;
            acrCloudRecognizer.startRecognizing();
            showRecognitionProgressDialog();
            startIdentificationTimeout();
        } else {
            requestAudioPermission();
        }
    }

    private void startIdentificationTimeout() { // Método startIdentificationTimeout define o tempo limite para reconhecimento.
        identificationTimeoutHandler.postDelayed(() -> {
            if (isIdentifying) {
                Log.d(TAG, "Timeout de identificação alcançado");
                stopMusicIdentification();
                Toast.makeText(this, "Tempo de identificação esgotado", Toast.LENGTH_SHORT).show();
            }
        }, IDENTIFICATION_TIMEOUT);
    }

    private void stopMusicIdentification() { // Método stopMusicIdentification para o reconhecimento de música.
        isIdentifying = false;
        identificationTimeoutHandler.removeCallbacksAndMessages(null);
        if (acrCloudRecognizer != null) {
            acrCloudRecognizer.stopRecognizing();
        }
        Toast.makeText(this, "Identificação parada", Toast.LENGTH_SHORT).show();
    }

    private void searchIdentifiedMusic(String title, String artist) { // Método searchIdentifiedMusic busca a música identificada.
        Log.d(TAG, "Buscando música identificada: " + title + " - " + artist);
        stopMusicIdentification();
        String queryParaBusca = title + " " + artist;
        if (searchView != null && !isSearchVisible) {
            toggleSearchVisibility();
        }
        new Handler().postDelayed(() -> {
            if (searchView != null) {
                searchView.setQuery(queryParaBusca, true);
                Log.d(TAG, "Busca executada: " + queryParaBusca);
            } else {
                Log.w(TAG, "searchView é null, filtrando diretamente");
                filterSongs(queryParaBusca);
            }
        }, 500);
        Toast.makeText(this, "Buscando: " + title, Toast.LENGTH_SHORT).show();
    }

    private void showMusicNotFoundDialog(String title, String artist, String searchQuery) { // Método showMusicNotFoundDialog exibe diálogo quando a música não é encontrada.
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Música Não Encontrada");
        builder.setMessage("A música identificada não foi encontrada na sua lista local.\n\n" +
                "Título: " + title + "\n" +
                "Artista: " + artist + "\n\n" +
                "O que você gostaria de fazer?");
        builder.setPositiveButton("BUSCAR MESMO ASSIM", (dialog, which) -> {
            Log.d(TAG, "Usuário escolheu buscar mesmo assim: " + searchQuery);
            if (searchView != null && !isSearchVisible) {
                toggleSearchVisibility();
            }
            if (searchView != null) {
                searchView.setQuery(searchQuery, true);
            } else {
                filterSongs(searchQuery);
            }
            Toast.makeText(this, "Buscando músicas similares...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.setNeutralButton("VER TODAS AS MÚSICAS", (dialog, which) -> {
            Log.d(TAG, "Usuário escolheu ver todas as músicas");
            if (isSearchVisible) {
                hideSearchFromToolbar();
            }
            if (songAdapter != null && todasMusicas != null) {
                songAdapter.updateSongs(todasMusicas);
                songAdapter.notifyDataSetChanged();
                updateSearchResultsText(todasMusicas.size());
            }
            Toast.makeText(this, "Mostrando todas as " +
                    (todasMusicas != null ? todasMusicas.size() : 0) + " músicas", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.setNegativeButton("IDENTIFICAR OUTRA", (dialog, which) -> {
            Log.d(TAG, "Usuário escolheu identificar outra música");
            startMusicIdentification();
            dialog.dismiss();
        });
        builder.setCancelable(true);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean checkAudioPermission() { // Método checkAudioPermission verifica a permissão de áudio.
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() { // Método requestAudioPermission solicita a permissão de áudio.
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                AUDIO_PERMISSION_REQUEST_CODE);
    }

    private ActivityResultLauncher<Intent> playlistActivityLauncher; // Variável playlistActivityLauncher gerencia o retorno da atividade de playlist.
    private boolean hasAllPermissions() { // Método hasAllPermissions verifica todas as permissões necessárias.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasMediaAudio = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Android 13+ - READ_MEDIA_AUDIO: " + hasMediaAudio);
            return hasMediaAudio;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean hasManageStorage = Environment.isExternalStorageManager();
            Log.d(TAG, "Android 11+ - MANAGE_EXTERNAL_STORAGE: " + hasManageStorage);
            return hasManageStorage;
        } else {
            boolean hasReadStorage = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Android 10- - READ_EXTERNAL_STORAGE: " + hasReadStorage);
            return hasReadStorage;
        }
    }

    private void checkAndRequestPermissions() { // Método checkAndRequestPermissions solicita permissões automaticamente.
        Log.d(TAG, "=== INICIANDO VERIFICAÇÃO DE PERMISSÕES ===");
        Log.d(TAG, "Versão do Android: " + Build.VERSION.SDK_INT);
        new Handler().postDelayed(() -> {
            if (hasAllPermissions()) {
                Log.d(TAG, "Todas as permissões já concedidas, inicializando app...");
                initializePresenterAndLoadSongs();
            } else {
                Log.d(TAG, "Permissões necessárias, solicitando ao usuário...");
                showPermissionExplanationDialog();
            }
        }, 500);
    }

    private void showPermissionExplanationDialog() { // Método showPermissionExplanationDialog exibe diálogo de explicação de permissões.
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Permissão Necessária");
        builder.setMessage("Para reproduzir suas músicas do celular, precisamos de acesso aos arquivos de áudio.\n\nEsta permissão é essencial para:\n• Listar suas músicas\n• Reproduzir arquivos locais\n• Criar playlists");
        builder.setPositiveButton("CONCORDAR E CONTINUAR", (dialog, which) -> {
            requestStoragePermissionAutomatically();
        });
        builder.setNegativeButton("NEGAR E SAIR", (dialog, which) -> {
            Toast.makeText(this, "O aplicativo não funcionará sem esta permissão", Toast.LENGTH_LONG).show();
            finish();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void requestStoragePermissionAutomatically() { // Método requestStoragePermissionAutomatically solicita permissões de armazenamento.
        Log.d(TAG, "Solicitando permissões de forma automática...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Log.d(TAG, "Solicitando MANAGE_EXTERNAL_STORAGE...");
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
                isWaitingForPermission = true;
                Toast.makeText(this, "Procure por 'Acesso a todos os arquivos' e ATIVE a permissão", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "Erro ao solicitar MANAGE_EXTERNAL_STORAGE", e);
                requestTraditionalPermissions();
            }
        } else {
            requestTraditionalPermissions();
        }
    }

    private void requestTraditionalPermissions() { // Método requestTraditionalPermissions solicita permissões tradicionais.
        Log.d(TAG, "Solicitando permissões tradicionais...");
        List<String> permissionsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            Log.d(TAG, "Solicitando READ_EXTERNAL_STORAGE");
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
            isWaitingForPermission = true;
            Toast.makeText(this, "Por favor, permita o acesso ao armazenamento", Toast.LENGTH_LONG).show();
        } else {
            Log.w(TAG, "Nenhuma permissão para solicitar - algo está errado");
            checkAndRequestPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) { // Método onRequestPermissionsResult trata o resultado das permissões.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult - RequestCode: " + requestCode);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int result = grantResults[i];
                boolean granted = result == PackageManager.PERMISSION_GRANTED;
                Log.d(TAG, "Permissão: " + permission + " - Concedida: " + granted);
                if (!granted) {
                    allGranted = false;
                }
            }
            if (allGranted) {
                Log.d(TAG, "Todas as permissões concedidas! Inicializando app...");
                new Handler().postDelayed(() -> {
                    initializePresenterAndLoadSongs();
                }, 500);
            } else {
                Log.w(TAG, "Algumas permissões foram negadas");
                showPermissionDeniedDialog();
            }
            isWaitingForPermission = false;
        } else if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startMusicIdentification();
            } else {
                Toast.makeText(this, "Permissão de áudio necessária para identificação", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { // Método onActivityResult trata o retorno de atividades.
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult - RequestCode: " + requestCode + ", ResultCode: " + resultCode);
        if (requestCode == MANAGE_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Log.d(TAG, "MANAGE_EXTERNAL_STORAGE concedido! Inicializando app...");
                    new Handler().postDelayed(() -> {
                        initializePresenterAndLoadSongs();
                    }, 500);
                } else {
                    Log.w(TAG, "MANAGE_EXTERNAL_STORAGE negado");
                    showPermissionDeniedDialog();
                }
            }
            isWaitingForPermission = false;
        } else if (requestCode == MUSIC_LIST_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                int selectedSongIndex = data.getIntExtra("selected_song_index", -1);
                if (selectedSongIndex != -1 && presenter != null) {
                    Log.d(TAG, "Música selecionada da lista: " + selectedSongIndex);
                    presenter.playSpecificSong(selectedSongIndex);
                }
            }
        }
    }

    private void showPermissionDeniedDialog() { // Método showPermissionDeniedDialog exibe diálogo quando permissões são negadas.
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Permissão Negada");
        builder.setMessage("Sem acesso aos arquivos de música, o aplicativo não pode acessar suas músicas do celular.\n\nO que você quer fazer?");
        builder.setPositiveButton("TENTAR NOVAMENTE", (dialog, which) -> {
            checkAndRequestPermissions();
        });
        builder.setNegativeButton("USAR MÚSICAS DO APP", (dialog, which) -> {
            Toast.makeText(this, "Usando apenas músicas internas do aplicativo", Toast.LENGTH_LONG).show();
            initializePresenterAndLoadSongs();
        });
        builder.setNeutralButton("CONFIGURAR MANUALMENTE", (dialog, which) -> {
            openAppSettings();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void openAppSettings() { // Método openAppSettings abre as configurações do aplicativo.
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Procure por 'Permissões' e ative o acesso aos arquivos", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao abrir configurações", e);
            Toast.makeText(this, "Erro ao abrir configurações", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializePresenterAndLoadSongs() { // Método initializePresenterAndLoadSongs inicializa o presenter e carrega músicas.
        Log.d(TAG, "Inicializando presenter e carregando músicas...");
        if (presenter == null) {
            presenter = new Presenter(this, this);
        }
        presenter.forceReloadSongs();
        Toast.makeText(this, "Aplicativo pronto para uso!", Toast.LENGTH_SHORT).show();
    }

    private void debugSongInfo(int position) { // Método debugSongInfo exibe informações de depuração da música.
        if (songAdapter != null && songAdapter.getSongs() != null && position < songAdapter.getSongs().size()) {
            Musica musica = songAdapter.getSongs().get(position);
            Log.d(TAG, "DEBUG - Música selecionada: " + musica.getTitulo() +
                    " - Artista: " + musica.getArtista() +
                    " - Tipo: " + (musica.isDeviceMusic() ? "Dispositivo" : "App") +
                    " - Path: " + musica.getFilePath());
        }
    }

    private void checkNotificationIntent(Intent intent) { // Método checkNotificationIntent verifica intenções de notificação.
        if (intent != null && intent.getBooleanExtra("from_notification", false)) {
            Log.d(TAG, "Intent recebido da notificação");
            int songIndex = intent.getIntExtra("current_song_index", -1);
            boolean shouldPlay = intent.getBooleanExtra("is_playing", false);
            int currentPosition = intent.getIntExtra("current_position", 0);
            int duration = intent.getIntExtra("current_duration", 0);
            if (songIndex != -1 && presenter != null) {
                presenter.syncPlayerState(songIndex, shouldPlay, currentPosition, duration);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) { // Método onNewIntent trata novas intenções.
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent chamado");
        checkNotificationIntent(intent);
    }

    @Override
    protected void onDestroy() { // Método onDestroy limpa recursos ao destruir a atividade.
        super.onDestroy();
        Log.d(TAG, "MainActivity sendo DESTRUÍDA - Limpando todos os recursos");
        try {
            stopMusicIdentification();
            identificationTimeoutHandler.removeCallbacksAndMessages(null);
            if (uiHandler != null) {
                uiHandler.removeCallbacksAndMessages(null);
            }
            if (acrCloudRecognizer != null) {
                acrCloudRecognizer.release();
                acrCloudRecognizer = null;
            }
            if (isReceiverRegistered && songUpdateReceiver != null) {
                try {
                    unregisterReceiver(songUpdateReceiver);
                    isReceiverRegistered = false;
                    Log.d(TAG, "BroadcastReceiver desregistrado");
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Receiver já não estava registrado");
                    isReceiverRegistered = false;
                }
            }
            stopAlbumRotation();
            Log.d(TAG, "MainActivity completamente limpa");
        } catch (Exception e) {
            Log.e(TAG, "Erro durante destruição", e);
        }
    }

    private void setupSearchButton() { // Método setupSearchButton configura o botão de busca.
        searchButton.setOnClickListener(v -> {
            toggleSearchVisibility();
        });
    }

    private void toggleSearchVisibility() { // Método toggleSearchVisibility alterna a visibilidade da busca.
        try {
            if (!isSearchVisible) {
                showSearchInToolbar();
            } else {
                hideSearchFromToolbar();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao alternar busca", e);
            Toast.makeText(this, "Erro ao ativar busca", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSearchInToolbar() { // Método showSearchInToolbar exibe a busca na toolbar.
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) return;
        View originalHeaderContent = toolbar.findViewById(R.id.original_header_content);
        if (originalHeaderContent != null) {
            originalHeaderContent.setVisibility(View.GONE);
        }
        searchView = new androidx.appcompat.widget.SearchView(this);
        searchView.setQueryHint("Buscar músicas...");
        searchView.setIconifiedByDefault(false);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setBackgroundColor(Color.TRANSPARENT);
        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchEditText != null) {
            searchEditText.setTextColor(Color.WHITE);
            searchEditText.setHintTextColor(Color.GRAY);
        }
        toolbar.addView(searchView);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (presenter != null) {
                    presenter.filterSongs(newText);
                }
                return true;
            }
        });
        searchView.requestFocus();
        isSearchVisible = true;
        searchButton.setImageResource(R.drawable.ic_close);
    }

    private void hideSearchFromToolbar() { // Método hideSearchFromToolbar esconde a busca da toolbar.
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) return;
        if (searchView != null) {
            toolbar.removeView(searchView);
            searchView = null;
        }
        View originalHeaderContent = toolbar.findViewById(R.id.original_header_content);
        if (originalHeaderContent != null) {
            originalHeaderContent.setVisibility(View.VISIBLE);
        }
        if (presenter != null) {
            presenter.filterSongs("");
        }
        isSearchVisible = false;
        searchButton.setImageResource(R.drawable.ic_search);
    }

    private void filterSongs(String query) { // Método filterSongs filtra músicas com base na query.
        Log.d(TAG, "Filtrando músicas com query: " + query);
        if (todasMusicas == null || todasMusicas.isEmpty()) {
            Log.d(TAG, "Lista de músicas vazia");
            return;
        }
        List<Musica> filteredSongs = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filteredSongs.addAll(todasMusicas);
            Log.d(TAG, "Query vazia - mostrando todas as " + todasMusicas.size() + " músicas");
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            Log.d(TAG, "Buscando por: " + lowerCaseQuery);
            for (Musica musica : todasMusicas) {
                String titulo = musica.getTitulo() != null ? musica.getTitulo().toLowerCase() : "";
                String artista = musica.getArtista() != null ? musica.getArtista().toLowerCase() : "";
                if (titulo.contains(lowerCaseQuery) || artista.contains(lowerCaseQuery)) {
                    filteredSongs.add(musica);
                    Log.d(TAG, "Música encontrada: " + musica.getTitulo() + " - " + musica.getArtista());
                }
            }
            Log.d(TAG, "Encontradas " + filteredSongs.size() + " músicas");
        }
        if (songAdapter != null) {
            songAdapter.updateSongs(filteredSongs);
            songAdapter.notifyDataSetChanged();
            updateSearchResultsText(filteredSongs.size());
        } else {
            Log.e(TAG, "SongAdapter é null!");
        }
    }

    private void updateSearchResultsText(int resultCount) { // Método updateSearchResultsText atualiza o texto de resultados da busca.
        TextView songsHeader = findViewById(R.id.songs_header);
        if (songsHeader != null) {
            if (isSearchVisible) {
                songsHeader.setText("Músicas (" + resultCount + " encontradas)");
            } else {
                songsHeader.setText("Músicas");
            }
        }
    }

    @Override
    public void onBackPressed() { // Método onBackPressed trata o botão de voltar.
        if (isSearchVisible) {
            hideSearchFromToolbar();
        } else {
            super.onBackPressed();
        }
    }

    private void showMusicDetailsDialog() { // Método showMusicDetailsDialog exibe detalhes da música.
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_music_details, null);
        builder.setView(dialogView);
        TextView titleTextView = dialogView.findViewById(R.id.dialog_song_title);
        TextView artistTextView = dialogView.findViewById(R.id.dialog_song_artist);
        TextView sourceTextView = dialogView.findViewById(R.id.dialog_song_source);
        TextView pathTextView = dialogView.findViewById(R.id.dialog_song_path);
        TextView durationTextView = dialogView.findViewById(R.id.dialog_song_duration);
        TextView typeTextView = dialogView.findViewById(R.id.dialog_song_type);
        if (musicaAtual != null) {
            Log.d(TAG, "Mostrando detalhes da música: " + musicaAtual.getTitulo());
            titleTextView.setText(musicaAtual.getTitulo());
            artistTextView.setText(musicaAtual.getArtista());
            if (musicaAtual.isDeviceMusic()) {
                sourceTextView.setText("Dispositivo");
                sourceTextView.setTextColor(Color.GREEN);
                typeTextView.setText("Arquivo de Áudio");
            } else {
                sourceTextView.setText("Aplicativo");
                sourceTextView.setTextColor(Color.BLUE);
                typeTextView.setText("Recurso Interno");
            }
            if (musicaAtual.isDeviceMusic()) {
                String filePath = musicaAtual.getFilePath();
                if (filePath != null && filePath.length() > 50) {
                    pathTextView.setText("..." + filePath.substring(filePath.length() - 50));
                } else {
                    pathTextView.setText(filePath != null ? filePath : "Caminho não disponível");
                }
            } else {
                pathTextView.setText("Resource ID: " + musicaAtual.getNumeroFaixa());
            }
            if (seekBar.getMax() > 0) {
                int duration = seekBar.getMax();
                durationTextView.setText(formatTime(duration) + " (" + duration + " ms)");
            } else if (musicaAtual.getDuracao() > 0) {
                durationTextView.setText(formatTime((int) musicaAtual.getDuracao()) +
                        " (" + musicaAtual.getDuracao() + " ms)");
            } else {
                durationTextView.setText("Duração não disponível");
            }
        } else {
            Log.w(TAG, "Nenhuma música atual para mostrar detalhes");
            titleTextView.setText("Nenhuma música em reprodução");
            artistTextView.setText("Toque uma música para ver os detalhes");
            sourceTextView.setText("---");
            sourceTextView.setTextColor(Color.GRAY);
            pathTextView.setText("---");
            durationTextView.setText("---");
            typeTextView.setText("---");
        }
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            Log.d(TAG, "Diálogo de detalhes fechado");
        });
        if (musicaAtual != null) {
            builder.setNeutralButton(isPlaying ? "PAUSAR" : "TOCAR", (dialog, which) -> {
                if (presenter != null) {
                    if (isPlaying) {
                        presenter.onPauseClicked();
                    } else {
                        presenter.onPlayClicked();
                    }
                }
            });
        }
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        Log.d(TAG, "Diálogo de detalhes da música exibido");
    }

    private void setupRotationAnimation() { // Método setupRotationAnimation configura a animação de rotação do álbum.
        rotateAnimation = new RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setRepeatMode(Animation.RESTART);
    }

    private void startAlbumRotation() { // Método startAlbumRotation inicia a animação de rotação.
        if (!isAnimationRunning && albumArt != null) {
            albumArt.startAnimation(rotateAnimation);
            isAnimationRunning = true;
            Log.d(TAG, "Animação de rotação iniciada");
        }
    }

    private void stopAlbumRotation() { // Método stopAlbumRotation para a animação de rotação.
        if (isAnimationRunning && albumArt != null) {
            albumArt.clearAnimation();
            isAnimationRunning = false;
            Log.d(TAG, "Animação de rotação parada");
        }
    }

    private void resetAlbumRotation() { // Método resetAlbumRotation reinicia a animação de rotação.
        stopAlbumRotation();
        albumArt.postDelayed(() -> {
            if (isPlaying) {
                startAlbumRotation();
            }
        }, 100);
    }

    private void openMusicListActivity() { // Método openMusicListActivity abre a atividade da lista de músicas.
        if (songAdapter != null && songAdapter.getSongs() != null) {
            Intent intent = new Intent(this, MusicListActivity.class);
            intent.putExtra("all_musicas", new ArrayList<>(songAdapter.getSongs()));
            startActivityForResult(intent, MUSIC_LIST_REQUEST_CODE);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            Toast.makeText(this, "Car Hildebrand, carregando músicas...", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatTime(int millis) { // Método formatTime formata o tempo em minutos e segundos.
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds %= 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    public void setSongList(List<Musica> songs) { // Método setSongList define a lista de músicas.
        if (songs != null) {
            this.todasMusicas = new ArrayList<>(songs);
            if (musicaAtual == null && !todasMusicas.isEmpty()) {
                musicaAtual = todasMusicas.get(0);
                Log.d(TAG, "Música atual definida como: " +
                        (musicaAtual != null ? musicaAtual.getTitulo() : "NULL"));
            }
        } else {
            this.todasMusicas = new ArrayList<>();
        }
        songAdapter = new MusicListAdapter(songs, position -> {
            if (presenter != null) {
                debugSongInfo(position);
                presenter.playSpecificSong(position);
                if (songs != null && position >= 0 && position < songs.size()) {
                    musicaAtual = songs.get(position);
                    Log.d(TAG, "Música atual atualizada no clique: " + musicaAtual.getTitulo());
                }
            }
        });
        songListRecyclerView.setAdapter(songAdapter);
        if (songs != null) {
            int deviceSongs = 0;
            int appSongs = 0;
            for (Musica song : songs) {
                if (song.isDeviceMusic()) {
                    deviceSongs++;
                } else {
                    appSongs++;
                }
            }
            String message = "Carregadas " + songs.size() + " músicas";
            if (deviceSongs > 0) {
                message += " (" + deviceSongs + " do celular)";
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "setSongList concluído - Total: " + songs.size() +
                    ", Música atual: " + (musicaAtual != null ? musicaAtual.getTitulo() : "NULL"));
        }
    }

    @Override
    public void mostrarPlay() { // Método mostrarPlay atualiza a UI para estado de reprodução.
        uiHandler.post(() -> {
            isPlaying = true;
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            startAlbumRotation();
        });
    }

    @Override
    public void mostrarStop() { // Método mostrarStop atualiza a UI para estado pausado.
        uiHandler.post(() -> {
            isPlaying = false;
            playPauseBtn.setImageResource(R.drawable.ic_play);
            stopAlbumRotation();
        });
    }

    @Override
    public void mensagemErro(String mensagem) { // Método mensagemErro exibe mensagens de erro.
        uiHandler.post(() -> {
            Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void informacaoMusica(String artista, String titulo) { // Método informacaoMusica atualiza informações da música.
        uiHandler.post(() -> {
            artistName.setText(artista);
            songTitle.setText(titulo);
        });
    }

    @Override
    public void updateShuffleIcon(boolean isOn) { // Método updateShuffleIcon atualiza o ícone de embaralhamento.
        uiHandler.post(() -> {
            if (shuffleBtn != null) {
                shuffleBtn.setColorFilter(isOn ? Color.GREEN : Color.WHITE);
            }
        });
    }

    @Override
    public void updateRepeatIcon(boolean isOn) { // Método updateRepeatIcon atualiza o ícone de repetição.
        uiHandler.post(() -> {
            if (repeatBtn != null) {
                repeatBtn.setColorFilter(isOn ? Color.GREEN : Color.WHITE);
            }
        });
    }

    @Override
    public void next() {} // Método next vazio, implementado pelo presenter.
    @Override
    public void previous() {} // Método previous vazio, implementado pelo presenter.
    @Override
    public void toggleShuffle() {} // Método toggleShuffle vazio, implementado pelo presenter.
    @Override
    public void toggleRepeat() {} // Método toggleRepeat vazio, implementado pelo presenter.
    @Override
    public void seekTo(int position) {} // Método seekTo vazio, implementado pelo presenter.

    @Override
    public void updateSeekBar(int progress, int duration) { // Método updateSeekBar atualiza a barra de progresso.
        uiHandler.post(() -> {
            if (seekBar != null) {
                if (duration > 0) {
                    seekBar.setMax(duration);
                    seekBar.setProgress(progress);
                }
                if (currentTime != null) {
                    currentTime.setText(formatTime(progress));
                }
                if (totalTime != null && duration > 0) {
                    totalTime.setText(formatTime(duration));
                }
            }
        });
    }

    private class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> { // Classe SongAdapter gerencia a exibição de músicas.
        private List<Musica> songs;
        private OnSongClickListener listener;

        public SongAdapter(List<Musica> songs, OnSongClickListener listener) {
            this.songs = songs;
            this.listener = listener;
        }

        public void updateSongs(List<Musica> newSongs) { // Método updateSongs atualiza a lista de músicas.
            this.songs = newSongs;
        }

        public List<Musica> getSongs() { // Método getSongs retorna a lista de músicas.
            return songs;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Musica song = songs.get(position);
            holder.titleTextView.setText(song.getTitulo());
            holder.artistTextView.setText(song.getArtista());
            holder.itemView.setOnClickListener(v -> {
                Log.d("DEBUG", "Música clicada na posição: " + position);
                if (listener != null) {
                    listener.onSongClick(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return songs != null ? songs.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView, artistTextView;

            ViewHolder(View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.song_item_title);
                artistTextView = itemView.findViewById(R.id.song_item_artist);
            }
        }
    }

    private interface OnSongClickListener { // Interface OnSongClickListener define o clique em uma música.
        void onSongClick(int position);
    }

    private void handleProgressUpdate(Intent intent) { // Método handleProgressUpdate atualiza a barra de progresso com base em intenções.
        Log.d(TAG, "handleProgressUpdate() CHAMADO");
        int progress = intent.getIntExtra(MusicService.EXTRA_PROGRESS, -1);
        int duration = intent.getIntExtra(MusicService.EXTRA_DURATION, -1);
        boolean isPlaying = intent.getBooleanExtra(MusicService.EXTRA_IS_PLAYING, false);
        boolean isSeekingBroadcast = intent.getBooleanExtra("is_seeking", false);
        boolean isSeekManual = intent.getBooleanExtra("seek_manual", false);
        Log.d(TAG, "SEEKBAR UPDATE - Progress: " + progress +
                " | isSeeking: " + isSeeking +
                " | isSeekingBroadcast: " + isSeekingBroadcast +
                " | isSeekManual: " + isSeekManual);
        if (isSeeking && !isSeekingBroadcast && !isSeekManual) {
            Log.d(TAG, "Ignorando update automático durante seek manual");
            return;
        }
        if (progress >= 0 && duration > 0) {
            runOnUiThread(() -> {
                try {
                    if (seekBar.getMax() != duration) {
                        seekBar.setMax(duration);
                    }
                    seekBar.setProgress(progress);
                    currentTime.setText(formatTime(progress));
                    totalTime.setText(formatTime(duration));
                    Log.d(TAG, "SeekBar ATUALIZADA: " + formatTime(progress) +
                            " | isSeeking: " + isSeeking);
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao atualizar SeekBar", e);
                }
            });
        }
    }

    private void activateSearchManually() { // Método activateSearchManually ativa a busca manualmente.
        Log.d(TAG, "Ativando busca manualmente...");
        if (isSearchVisible) {
            Log.d(TAG, "Busca já está visível");
            return;
        }
        toggleSearchVisibility();
        new Handler().postDelayed(() -> {
            if (searchView != null) {
                searchView.setQuery("", false);
                searchView.requestFocus();
                Log.d(TAG, "Busca ativada manualmente com sucesso");
            } else {
                Log.e(TAG, "searchView ainda é null após ativar busca");
                Toast.makeText(this, "Busca indisponível no momento", Toast.LENGTH_SHORT).show();
            }
        }, 300);
    }

    private void loadAlbumArt(Musica musica) { // Método loadAlbumArt carrega a arte do álbum.
        if (musica == null || albumArt == null) {
            return;
        }
        albumArt.setImageResource(R.drawable.ic_music);
    }
}