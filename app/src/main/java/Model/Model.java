package Model;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.mussaldynerhey.meuleitorrhey.database.entities.Song;
import com.mussaldynerhey.meuleitorrhey.database.repository.MusicRepository;
import com.mussaldynerhey.meuleitorrhey.utils.MusicScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Classe que gerencia a lógica de negócios para músicas e playlists.
 */
public class Model {
    private List<Musica> listaMusica; // Lista de músicas atual
    private List<Musica> listaOriginal; // Lista original de músicas (para restauração após shuffle)
    private int indiceActual = 0; // Índice da música atual
    private boolean isShuffleOn = false; // Estado do modo shuffle
    private boolean isRepeatOn = false; // Estado do modo repeat
    private Random random = new Random(); // Gerador de números aleatórios
    private Musica musicaAtual; // Música atualmente selecionada
    private Musica musicaAtualShuffle; // Música atual no modo shuffle
    private MusicRepository musicRepository; // Repositório para acesso ao banco de dados
    private Context context; // Contexto da aplicação
    private int currentPlaylistId = -1; // ID da playlist atual
    private static final String TAG = "Model";

    /**
     * Construtor do Model.
     * @param listaMusica Lista inicial de músicas.
     * @param context Contexto da aplicação.
     */
    public Model(List<Musica> listaMusica, Context context) {
        this.listaMusica = new ArrayList<>(listaMusica);
        this.listaOriginal = new ArrayList<>(listaMusica);
        this.context = context;
        if (context != null) {
            musicRepository = new MusicRepository((Application) context.getApplicationContext());
            initializeDatabase();
        }
        Log.d(TAG, "Model inicializado com " + listaMusica.size() + " músicas");
    }

    /**
     * Inicializa o banco de dados com músicas iniciais, se necessário.
     */
    private void initializeDatabase() {
        new Thread(() -> {
            List<Song> existingSongs = musicRepository.getAllSongs();
            if (existingSongs == null || existingSongs.isEmpty()) {
                for (Musica musica : listaMusica) {
                    Song song = new Song(
                            musica.getTitulo(),
                            musica.getArtista(),
                            "", // Path inicial vazio
                            musica.getNumeroFaixa(), // resourceId
                            musica.getDuracao(),
                            null // Caminho da capa do álbum (null para músicas internas)
                    );
                    musicRepository.insertSong(song);
                }

                long playlistId = musicRepository.insertPlaylist(
                        new com.mussaldynerhey.meuleitorrhey.database.entities.Playlist("Minha Playlist")
                );
                currentPlaylistId = (int) playlistId;

                List<Song> songs = musicRepository.getAllSongs();
                for (Song song : songs) {
                    musicRepository.addSongToPlaylist(currentPlaylistId, song.getId());
                }
            }
        }).start();
    }

    /**
     * Escaneia e sincroniza músicas do dispositivo com o banco de dados.
     */
    public void scanAndSyncDeviceMusic() {
        if (musicRepository != null && context != null) {
            Log.d(TAG, "📱 Iniciando scan de músicas do dispositivo...");
            MusicScanner musicScanner = new MusicScanner(context, musicRepository);
            musicScanner.syncDeviceMusicWithDatabase();
            musicScanner.debugMusicScan();
        } else {
            Log.e(TAG, "❌ Não é possível fazer scan - Repository ou Context null");
        }
    }

    /**
     * Carrega todas as músicas do banco de dados e atualiza as listas internas.
     * @return Lista de músicas carregada.
     */
    public List<Musica> loadAllSongsFromDatabase() {
        if (this.listaMusica == null) {
            this.listaMusica = new ArrayList<>();
        } else {
            this.listaMusica.clear();
        }
        if (this.listaOriginal == null) {
            this.listaOriginal = new ArrayList<>();
        } else {
            this.listaOriginal.clear();
        }

        if (musicRepository != null) {
            List<Song> databaseSongs = musicRepository.getAllSongs();
            if (databaseSongs != null) {
                for (Song dbSong : databaseSongs) {
                    Musica musica;
                    if (dbSong.getResourceId() == 0) {
                        musica = new Musica(
                                dbSong.getTitle(),
                                dbSong.getArtist(),
                                dbSong.getPath(),
                                dbSong.getDuration(),
                                dbSong.getAlbumArtPath()
                        );
                    } else {
                        musica = new Musica(
                                dbSong.getTitle(),
                                dbSong.getArtist(),
                                dbSong.getPath(),
                                dbSong.getDuration(),
                                dbSong.getAlbumArtPath()
                        );
                    }
                    this.listaMusica.add(musica);
                    this.listaOriginal.add(musica);
                }
            }
        }

        Log.d(TAG, "Listas internas atualizadas do banco. Total: " + this.listaMusica.size());
        return this.listaMusica;
    }

    /**
     * Obtém a lista de músicas atual.
     * @return Lista de músicas.
     */
    public List<Musica> getListaMusica() {
        return listaMusica;
    }

    /**
     * Obtém o índice da música atual.
     * @return Índice atual.
     */
    public int getIndiceActual() {
        return indiceActual;
    }

    /**
     * Define o índice da música atual.
     * @param indice Novo índice.
     */
    public void setIndiceActual(int indice) {
        if (indice >= 0 && indice < (listaMusica != null ? listaMusica.size() : 0)) {
            this.indiceActual = indice;
            Log.d(TAG, "Índice definido para: " + indice);
        } else {
            Log.e(TAG, "Índice inválido: " + indice);
        }
    }

    /**
     * Alterna o modo shuffle (embaralhamento).
     */
    public void toggleShuffle() {
        if (!isShuffleOn) {
            isShuffleOn = true;
            isRepeatOn = false;
            Log.d(TAG, "Shuffle ATIVADO - Repeat DESATIVADO");
        } else {
            isShuffleOn = false;
            Musica musicaTocandoAgora = getMusicaAtual();
            Log.d(TAG, "🔀 Desativando shuffle - Música tocando: " +
                    (musicaTocandoAgora != null ? musicaTocandoAgora.getTitulo() : "null"));

            if (musicaTocandoAgora != null) {
                int posicaoOriginal = -1;
                for (int i = 0; i < listaOriginal.size(); i++) {
                    Musica m = listaOriginal.get(i);
                    if (m.getTitulo().equals(musicaTocandoAgora.getTitulo()) &&
                            m.getArtista().equals(musicaTocandoAgora.getArtista())) {
                        posicaoOriginal = i;
                        break;
                    }
                }

                if (posicaoOriginal != -1) {
                    indiceActual = posicaoOriginal;
                    listaMusica = new ArrayList<>(listaOriginal);
                    Log.d(TAG, "✅ Shuffle desativado - Posição original: " + posicaoOriginal);
                } else {
                    listaMusica = new ArrayList<>(listaOriginal);
                    indiceActual = 0;
                    Log.w(TAG, "⚠️ Música não encontrada na lista original - usando índice 0");
                }
            }
        }
    }

    /**
     * Alterna o modo repeat (repetição).
     */
    public void toggleRepeat() {
        if (!isRepeatOn) {
            isRepeatOn = true;
            isShuffleOn = false;
            Log.d(TAG, "Repeat ATIVADO - Shuffle DESATIVADO");
        } else {
            isRepeatOn = false;
            Log.d(TAG, "Repeat DESATIVADO");
        }
    }

    /**
     * Verifica se o modo shuffle está ativado.
     * @return True se o shuffle está ativado.
     */
    public boolean isShuffleOn() {
        return isShuffleOn;
    }

    /**
     * Verifica se o modo repeat está ativado.
     * @return True se o repeat está ativado.
     */
    public boolean isRepeatOn() {
        return isRepeatOn;
    }

    /**
     * Avança para a próxima música.
     */
    public void next() {
        if (listaMusica.isEmpty()) {
            Log.e(TAG, "Lista de músicas vazia em next()");
            return;
        }

        Log.d(TAG, "Next - Shuffle: " + isShuffleOn + ", Repeat: " + isRepeatOn);

        if (isRepeatOn) {
            Log.d(TAG, "Repeat ativado - mantendo música atual");
            return;
        } else if (isShuffleOn) {
            int newIndex;
            if (listaMusica.size() > 1) {
                do {
                    newIndex = random.nextInt(listaMusica.size());
                } while (newIndex == indiceActual);
            } else {
                newIndex = 0;
            }
            indiceActual = newIndex;
            Log.d(TAG, "🎲 Shuffle - Novo índice: " + indiceActual);
        } else {
            indiceActual = (indiceActual + 1) % listaMusica.size();
            Log.d(TAG, "Ordem normal - próximo índice: " + indiceActual);
        }

        getMusicaAtual();
    }

    /**
     * Volta para a música anterior.
     */
    public void previous() {
        if (listaMusica.isEmpty()) {
            Log.e(TAG, "Lista de músicas vazia em previous()");
            return;
        }

        Log.d(TAG, "Previous - Shuffle: " + isShuffleOn + ", Repeat: " + isRepeatOn);

        if (isRepeatOn) {
            Log.d(TAG, "Repeat ativado - mantendo música atual");
            return;
        } else if (isShuffleOn) {
            int newIndex;
            if (listaMusica.size() > 1) {
                do {
                    newIndex = random.nextInt(listaMusica.size());
                } while (newIndex == indiceActual);
            } else {
                newIndex = 0;
            }
            indiceActual = newIndex;
            musicaAtualShuffle = getMusicaAtual();
            Log.d(TAG, "🎲 Shuffle - Música anterior: " +
                    (musicaAtualShuffle != null ? musicaAtualShuffle.getTitulo() : "null") +
                    " (índice " + indiceActual + ")");
        } else {
            indiceActual = (indiceActual - 1 + listaMusica.size()) % listaMusica.size();
            Log.d(TAG, "Ordem normal - índice anterior: " + indiceActual);
        }
    }

    /**
     * Obtém a música atual.
     * @return Música atual ou null se a lista estiver vazia.
     */
    public Musica getMusicaAtual() {
        if (listaMusica.isEmpty()) {
            Log.e(TAG, "Lista de músicas vazia em getMusicaAtual()");
            return null;
        }

        if (indiceActual < 0 || indiceActual >= listaMusica.size()) {
            Log.e(TAG, "Índice inválido: " + indiceActual + ", corrigindo para 0");
            indiceActual = 0;
        }

        Musica musica = listaMusica.get(indiceActual);
        if (musicaAtual == null ||
                !musica.getTitulo().equals(musicaAtual.getTitulo()) ||
                !musica.getArtista().equals(musicaAtual.getArtista())) {
            musicaAtual = musica;
            Log.d(TAG, "🔄 Música atual atualizada: " + musicaAtual.getTitulo());
        }

        return musica;
    }

    /**
     * Verifica se a música atual deve ser repetida.
     * @return True se o modo repeat está ativado.
     */
    public boolean shouldRepeatCurrentSong() {
        return isRepeatOn;
    }

    /**
     * Atualiza a lista de músicas e reinicia o índice.
     * @param novasMusicas Nova lista de músicas.
     */
    public void updateListaMusica(List<Musica> novasMusicas) {
        this.listaMusica = new ArrayList<>(novasMusicas);
        this.listaOriginal = new ArrayList<>(novasMusicas);
        if (indiceActual >= listaMusica.size()) {
            indiceActual = 0;
        }
        musicaAtual = null;
        getMusicaAtual();
        Log.d(TAG, "Lista de músicas atualizada. Total: " + listaMusica.size());
    }

    /**
     * Obtém o ID da playlist atual.
     * @return ID da playlist atual.
     */
    public int getCurrentPlaylistId() {
        return currentPlaylistId;
    }

    /**
     * Define o ID da playlist atual e carrega suas músicas.
     * @param playlistId ID da playlist.
     */
    public void setCurrentPlaylistId(int playlistId) {
        this.currentPlaylistId = playlistId;
        loadPlaylistSongs(playlistId);
    }

    /**
     * Carrega as músicas de uma playlist específica do banco de dados.
     * @param playlistId ID da playlist.
     */
    private void loadPlaylistSongs(int playlistId) {
        new Thread(() -> {
            List<Song> databaseSongs = musicRepository.getSongsByPlaylist(playlistId);
            if (databaseSongs != null) {
                listaMusica.clear();
                for (Song dbSong : databaseSongs) {
                    Musica musica;
                    if (dbSong.getResourceId() == 0) {
                        musica = new Musica(
                                dbSong.getTitle(),
                                dbSong.getArtist(),
                                dbSong.getPath(),
                                dbSong.getDuration(),
                                dbSong.getAlbumArtPath()
                        );
                    } else {
                        musica = new Musica(
                                dbSong.getTitle(),
                                dbSong.getArtist(),
                                dbSong.getPath(),
                                dbSong.getDuration(),
                                dbSong.getAlbumArtPath()
                        );
                    }
                    listaMusica.add(musica);
                }
                listaOriginal = new ArrayList<>(listaMusica);
                indiceActual = 0;
                Log.d(TAG, "Playlist carregada: " + listaMusica.size() + " músicas");
            }
        }).start();
    }

    /**
     * Obtém o repositório de música.
     * @return Instância do MusicRepository.
     */
    public MusicRepository getMusicRepository() {
        return musicRepository;
    }

    /**
     * Obtém informações de depuração do modelo.
     * @return String com informações de depuração.
     */
    public String getDebugInfo() {
        return "Model Debug:\n" +
                "Total músicas: " + listaMusica.size() + "\n" +
                "Índice atual: " + indiceActual + "\n" +
                "Shuffle: " + (isShuffleOn ? "ON" : "OFF") + "\n" +
                "Repeat: " + (isRepeatOn ? "ON" : "OFF") + "\n" +
                "Música atual: " + (getMusicaAtual() != null ?
                getMusicaAtual().getTitulo() + " - " + getMusicaAtual().getArtista() : "Nenhuma");
    }

    /**
     * Toca uma música específica com base no índice.
     * @param index Índice da música na lista.
     */
    public void playSpecificSong(int index) {
        if (listaMusica == null || index < 0 || index >= listaMusica.size()) {
            Log.e(TAG, "Índice inválido para playSpecificSong: " + index);
            return;
        }
        Log.d(TAG, "Preparando para tocar música específica no índice: " + index);
        setIndiceActual(index);
        Log.d(TAG, "Índice atual definido para: " + index + ". Notificando Presenter...");
    }

    /**
     * Define uma nova lista de músicas.
     * @param newList Nova lista de músicas.
     */
    public void setMusicList(List<Musica> newList) {
        if (this.listaMusica != null) {
            this.listaMusica.clear();
            this.listaMusica.addAll(newList);
            this.listaOriginal.clear();
            this.listaOriginal.addAll(newList);
            Log.d(TAG, "Nova lista de músicas definida. Total: " + this.listaMusica.size());
        }
    }
}