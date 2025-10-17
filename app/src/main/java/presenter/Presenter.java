package presenter;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;

import com.mussaldynerhey.meuleitorrhey.MusicService;
import com.mussaldynerhey.meuleitorrhey.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Model.Model;
import Model.Musica;
import view.Contrato;

/**
 * Classe Presenter que atua como intermediária entre o Model e a View no padrão MVP.
 */
public class Presenter implements Contrato.Presenter {
    private final Contrato.View view; // Referência à interface da View
    private final Model model; // Referência ao Model
    private final Context context; // Contexto da aplicação
    private List<Musica> musicas; // Lista de músicas atuais
    private final List<Musica> todasAsMusicas = new ArrayList<>(); // Lista completa de todas as músicas
    private static final String TAG = "Presenter"; // Tag para logs

    /**
     * Construtor do Presenter.
     * @param view Interface da View.
     * @param context Contexto da aplicação.
     */
    public Presenter(@NonNull Contrato.View view, @NonNull Context context) {
        this.view = view;
        this.context = context;
        this.model = new Model(new ArrayList<>(), context);
        loadSongs(); // Carrega as músicas ao inicializar
    }

    /**
     * Atualiza o estado de reprodução na interface.
     * @param isPlaying Indica se a música está tocando.
     */
    @Override
    public void updatePlayingStatus(boolean isPlaying) {
        Log.d(TAG, "Estado de reprodução atualizado para: " + isPlaying);
    }

    /**
     * Força o recarregamento de todas as músicas do dispositivo.
     */
    @Override
    public void forceReloadSongs() {
        Log.d(TAG, "FORÇANDO RECARREGAMENTO DE TODAS AS MÚSICAS DO DISPOSITIVO");

        if (model != null) {
            model.scanAndSyncDeviceMusic(); // Inicia o escaneamento de músicas
            Log.d(TAG, "Scan do dispositivo iniciado");
        }

        // Aguarda 1.5 segundos para garantir que o scan esteja concluído antes de recarregar
        new Handler().postDelayed(() -> {
            loadSongs();
            Log.d(TAG, "Recarregamento de músicas concluído");
        }, 1500);
    }

    /**
     * Carrega as músicas do banco de dados ou usa músicas padrão se necessário.
     */
    public void loadSongs() {
        Log.d(TAG, "Iniciando carregamento de músicas...");
        if (model == null) return;

        model.scanAndSyncDeviceMusic(); // Sincroniza músicas do dispositivo
        List<Musica> allSongs = model.loadAllSongsFromDatabase(); // Carrega do banco de dados

        if (allSongs != null && !allSongs.isEmpty()) {
            this.musicas = allSongs; // Usa músicas do banco
        } else {
            Log.d(TAG, "Banco de dados vazio. Carregando músicas padrão...");
            this.musicas = getDefaultSongs(); // Carrega músicas padrão
        }

        model.updateListaMusica(this.musicas); // Atualiza o Model
        this.todasAsMusicas.clear();
        this.todasAsMusicas.addAll(this.musicas); // Atualiza lista completa

        if (view != null) {
            view.setSongList(this.musicas); // Atualiza a UI com a lista de músicas
            view.updateShuffleIcon(model.isShuffleOn()); // Atualiza ícone de shuffle
            view.updateRepeatIcon(model.isRepeatOn()); // Atualiza ícone de repeat
        }
        if (this.musicas != null && !this.musicas.isEmpty()) {
            syncPlayerState(); // Sincroniza estado com o serviço
        }

        Log.d(TAG, "Carregamento de músicas concluído. Total: " + (this.musicas != null ? this.musicas.size() : 0));

        debugMusicList(); // Log de depuração da lista
        debugMusicState(); // Log de depuração do estado
    }

    /**
     * Inicia a reprodução de uma música ao clicar no botão de play.
     */
    @Override
    public void onPlayClicked() {
        Log.d(TAG, "Enviando ACTION_PLAY para o serviço");
        Intent serviceIntent = new Intent(context, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_PLAY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent); // Inicia serviço em foreground
        } else {
            context.startService(serviceIntent); // Inicia serviço normalmente
        }

        Log.d(TAG, "ACTION_PLAY enviada para Service");
    }

    /**
     * Pausa a reprodução de uma música ao clicar no botão de pausa.
     */
    @Override
    public void onPauseClicked() {
        Log.d(TAG, "Enviando ACTION_PAUSE para o serviço");
        Intent serviceIntent = new Intent(context, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_PAUSE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        Log.d(TAG, "ACTION_PAUSE enviada para Service");
    }

    /**
     * Atualiza a UI para mostrar o estado de reprodução.
     */
    @Override
    public void mostrarPlay() {
        Log.d(TAG, "Presenter: atualizando UI para estado de reprodução");
    }

    /**
     * Atualiza a UI para mostrar o estado de pausa.
     */
    @Override
    public void mostrarStop() {
        Log.d(TAG, "Presenter: atualizando UI para estado de pausa");
    }

    /**
     * Toca uma nova lista de músicas a partir de uma posição específica.
     * @param newList Nova lista de músicas.
     * @param position Posição inicial para reprodução.
     */
    @Override
    public void playNewList(List<Musica> newList, int position) {
        if (newList == null || newList.isEmpty() || position < 0 || position >= newList.size()) {
            Log.e(TAG, "Não é possível tocar a nova lista. Dados inválidos.");
            if (view != null) view.mensagemErro("Playlist vazia ou erro ao carregar.");
            return;
        }
        Log.d(TAG, "Trocando para nova lista com " + newList.size() + " músicas.");

        this.musicas = new ArrayList<>(newList);
        model.updateListaMusica(this.musicas); // Atualiza o Model

        if (view != null) {
            view.setSongList(this.musicas); // Atualiza a UI
        }

        startPlayback(position); // Inicia a reprodução
    }

    /**
     * Inicia a reprodução de uma música em um índice específico.
     * @param index Índice da música na lista.
     */
    private void startPlayback(int index) {
        if (musicas == null || index < 0 || index >= musicas.size()) {
            Log.e(TAG, "Índice de música inválido");
            return;
        }

        model.setIndiceActual(index); // Define o índice no Model
        Musica musicaParaTocar = musicas.get(index);

        Log.d(TAG, "ENVIANDO PARA SERVICE - Música: " + musicaParaTocar.getTitulo());

        Intent serviceIntent = new Intent(context, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_PLAY);

        if (musicas != null && !musicas.isEmpty()) {
            ArrayList<Musica> musicasList = new ArrayList<>(musicas);
            serviceIntent.putExtra("SONG_LIST", musicasList); // Envia a lista completa
            serviceIntent.putExtra("SONG_INDEX", index); // Envia o índice
            Log.d(TAG, "Enviando lista com " + musicasList.size() + " músicas para Service");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
            Log.d(TAG, "startForegroundService() chamado");
        } else {
            context.startService(serviceIntent);
            Log.d(TAG, "startService() chamado");
        }

        Log.d(TAG, "Comando PLAY enviado para índice: " + index);

        if (view != null) {
            view.informacaoMusica(musicaParaTocar.getArtista(), musicaParaTocar.getTitulo()); // Atualiza UI
        }
    }

    /**
     * Toca uma música específica com base no índice.
     * @param index Índice da música na lista.
     */
    @Override
    public void playSpecificSong(int index) {
        if (this.musicas != null && !this.musicas.isEmpty() && index >= 0 && index < this.musicas.size()) {
            Log.d(TAG, "Tocando música específica no índice: " + index);
            startPlayback(index);
        } else {
            Log.e(TAG, "Falha ao tocar música específica: lista vazia ou índice inválido.");
            if (view != null) {
                view.mensagemErro("Erro ao tentar tocar a música selecionada.");
            }
        }
    }

    /**
     * Avança para a próxima música.
     */
    @Override
    public void next() {
        model.next();
        startPlayback(model.getIndiceActual());
    }

    /**
     * Volta para a música anterior.
     */
    @Override
    public void previous() {
        model.previous();
        startPlayback(model.getIndiceActual());
    }

    /**
     * Move a reprodução para uma posição específica.
     * @param position Posição em milissegundos.
     */
    @Override
    public void seekTo(int position) {
        Log.d(TAG, "PRESENTER.seekTo() CHAMADO - Posição: " + position +
                "ms (" + formatTime(position) + ")");

        if (position < 0) {
            position = 0;
            Log.w(TAG, "Posição negativa, corrigindo para 0");
        }

        Intent serviceIntent = new Intent(context, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_SEEK_TO);
        serviceIntent.putExtra("SEEK_POSITION", position);

        Log.d(TAG, "Intent criada - Action: " + serviceIntent.getAction() +
                ", Extra: " + serviceIntent.getIntExtra("SEEK_POSITION", -1));

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
                Log.d(TAG, "startForegroundService() chamado");
            } else {
                context.startService(serviceIntent);
                Log.d(TAG, "startService() chamado");
            }
            Log.d(TAG, "Comando SEEK enviado com SUCESSO para Service");
        } catch (Exception e) {
            Log.e(TAG, "ERRO ao enviar seek para Service", e);
        }
    }

    /**
     * Formata o tempo em milissegundos para minutos e segundos.
     * @param millis Tempo em milissegundos.
     * @return String formatada no formato MM:SS.
     */
    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds %= 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Alterna o modo shuffle (embaralhamento).
     */
    @Override
    public void toggleShuffle() {
        Log.d(TAG, "Toggle Shuffle - Estado atual: " + model.isShuffleOn());

        model.toggleShuffle(); // Alterna o shuffle no Model

        Musica musicaAtual = model.getMusicaAtual();

        if (view != null) {
            view.updateShuffleIcon(model.isShuffleOn()); // Atualiza ícone de shuffle
            view.updateRepeatIcon(model.isRepeatOn()); // Atualiza ícone de repeat

            if (musicaAtual != null) {
                view.informacaoMusica(musicaAtual.getArtista(), musicaAtual.getTitulo()); // Atualiza UI
                Log.d(TAG, "UI atualizada com música correta: " + musicaAtual.getTitulo());
            }
        }

        syncPlayerState(); // Sincroniza estado com o serviço

        Log.d(TAG, "Shuffle " + (model.isShuffleOn() ? "ATIVADO" : "DESATIVADO"));
    }

    /**
     * Alterna o modo repeat (repetição).
     */
    @Override
    public void toggleRepeat() {
        model.toggleRepeat();
        if (view != null) {
            view.updateRepeatIcon(model.isRepeatOn());
            view.updateShuffleIcon(model.isShuffleOn());
        }
        syncPlayerState();
    }

    /**
     * Sincroniza o estado do player com o serviço.
     */
    private void syncPlayerState() {
        if (musicas == null || musicas.isEmpty()) {
            Log.w(TAG, "Não é possível sincronizar: lista de músicas vazia");
            return;
        }

        Intent serviceIntent = new Intent(context, MusicService.class);
        serviceIntent.setAction("SYNC_STATE");
        serviceIntent.putExtra("SHUFFLE_STATE", model.isShuffleOn());
        serviceIntent.putExtra("REPEAT_STATE", model.isRepeatOn());
        serviceIntent.putExtra("CURRENT_INDEX", model.getIndiceActual());

        try {
            context.startService(serviceIntent);
            Log.d(TAG, "Estados sincronizados - Shuffle: " + model.isShuffleOn() +
                    ", Repeat: " + model.isRepeatOn() + ", Índice: " + model.getIndiceActual());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao sincronizar estados", e);
        }
    }

    /**
     * Sincroniza o estado do player com base em informações específicas.
     * @param songIndex Índice da música atual.
     * @param shouldPlay Indica se deve tocar.
     * @param currentPosition Posição atual da reprodução.
     * @param duration Duração total da música.
     */
    @Override
    public void syncPlayerState(int songIndex, boolean shouldPlay, int currentPosition, int duration) {
        if (model != null && musicas != null && songIndex >= 0 && songIndex < musicas.size()) {
            model.setIndiceActual(songIndex);
            Musica musica = musicas.get(songIndex);
            if (view != null) {
                view.informacaoMusica(musica.getArtista(), musica.getTitulo());
                view.updateSeekBar(currentPosition, duration);
            }
        }
    }

    /**
     * Filtra músicas com base em uma consulta.
     * @param query Texto de busca.
     */
    @Override
    public void filterSongs(String query) {
        List<Musica> filteredSongs = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filteredSongs.addAll(todasAsMusicas);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (Musica musica : todasAsMusicas) {
                if (musica.getTitulo().toLowerCase().contains(lowerCaseQuery) ||
                        musica.getArtista().toLowerCase().contains(lowerCaseQuery)) {
                    filteredSongs.add(musica);
                }
            }
        }
        if (view != null) {
            view.showFilteredSongs(filteredSongs);
        }
    }

    /**
     * Obtém a lista de músicas padrão.
     * @return Lista de músicas padrão.
     */
    private List<Musica> getDefaultSongs() {
        return Arrays.asList(
                new Musica("Imagine", "John Lennon", R.raw.musica1, getDurationFromRaw(context, R.raw.musica1)),
                new Musica("Bohemian Rhapsody", "Queen", R.raw.musica2, getDurationFromRaw(context, R.raw.musica2)),
                new Musica("Blowin' in the Wind", "Bob Dylan", R.raw.musica3, getDurationFromRaw(context, R.raw.musica3)),
                new Musica("Hotel California", "Eagles", R.raw.musica4, getDurationFromRaw(context, R.raw.musica4))
        );
    }

    /**
     * Obtém a duração de um recurso de áudio bruto.
     * @param context Contexto da aplicação.
     * @param rawResId ID do recurso bruto.
     * @return Duração em milissegundos.
     */
    private long getDurationFromRaw(Context context, int rawResId) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(rawResId);
            retriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            afd.close();
            if (durationStr != null) {
                return Long.parseLong(durationStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao obter duração: " + rawResId, e);
        } finally {
            try {
                retriever.release();
            } catch (Exception e) { /* ignorar */ }
        }
        return 0;
    }

    /**
     * Exibe informações de depuração da lista de músicas.
     */
    public void debugMusicList() {
        Log.d(TAG, "=== DEBUG MUSIC LIST ===");
        Log.d(TAG, "Musicas carregadas: " + (musicas != null ? musicas.size() : 0));
        if (musicas != null) {
            for (int i = 0; i < Math.min(musicas.size(), 5); i++) {
                Musica m = musicas.get(i);
                Log.d(TAG, i + ": " + m.getTitulo() + " - " + m.getArtista() +
                        " | Device: " + m.isDeviceMusic() + " | Path: " + m.getFilePath());
            }
        }
        Log.d(TAG, "=== FIM DEBUG ===");
    }

    /**
     * Exibe informações de depuração do estado do player.
     */
    public void debugMusicState() {
        Log.d(TAG, "=== DEBUG MUSIC STATE ===");
        Log.d(TAG, "Musicas no Presenter: " + (musicas != null ? musicas.size() : "NULL"));
        Log.d(TAG, "Model indice: " + model.getIndiceActual());
        Log.d(TAG, "Shuffle: " + model.isShuffleOn() + ", Repeat: " + model.isRepeatOn());

        if (musicas != null && !musicas.isEmpty()) {
            for (int i = 0; i < Math.min(musicas.size(), 3); i++) {
                Musica m = musicas.get(i);
                Log.d(TAG, i + ": " + m.getTitulo() +
                        " | Device: " + m.isDeviceMusic() +
                        " | Path: " + m.getFilePath() +
                        " | URI: " + m.getContentUri());
            }
        }
        Log.d(TAG, "=== FIM DEBUG ===");
    }

    /**
     * Lida com a remoção da tarefa para manter o serviço vivo.
     * @param rootIntent Intent raiz.
     */
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved chamado - Mantendo serviço vivo");
    }

    /**
     * Mantém o serviço de música em execução em primeiro plano.
     */
    private void keepServiceAlive() {
        Log.d(TAG, "Mantendo serviço vivo em primeiro plano");
    }

    /**
     * Força o recarregamento das capas dos álbuns.
     */
    public void forceReloadAlbumArts() {
        Log.d(TAG, "FORÇANDO RECARREGAMENTO DE CAPAS");

        if (model != null) {
            model.scanAndSyncDeviceMusic(); // Força novo scan para capas

            new Handler().postDelayed(() -> {
                List<Musica> updatedSongs = model.loadAllSongsFromDatabase();
                if (view != null && updatedSongs != null) {
                    view.setSongList(updatedSongs);
                    Log.d(TAG, "Capas recarregadas - " + updatedSongs.size() + " músicas");

                    int withCover = 0;
                    for (Musica musica : updatedSongs) {
                        if (musica.getContentUri() != null) withCover++;
                    }
                    Log.d(TAG, withCover + " músicas com capa de " + updatedSongs.size());
                }
            }, 2000);
        }
    }
}