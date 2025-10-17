package com.mussaldynerhey.meuleitorrhey.database.repository;

import android.app.Application;
import android.os.AsyncTask;

import com.mussaldynerhey.meuleitorrhey.database.MusicDatabase;
import com.mussaldynerhey.meuleitorrhey.database.daos.SongDao;
import com.mussaldynerhey.meuleitorrhey.database.daos.PlaylistDao;
import com.mussaldynerhey.meuleitorrhey.database.daos.PlaylistSongDao;
import com.mussaldynerhey.meuleitorrhey.database.entities.Song;
import com.mussaldynerhey.meuleitorrhey.database.entities.Playlist;
import com.mussaldynerhey.meuleitorrhey.database.entities.PlaylistSong;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repositório para gerenciar operações com músicas e playlists no banco de dados.
 */
public class MusicRepository {
    private SongDao songDao; // DAO para operações com músicas
    private PlaylistDao playlistDao; // DAO para operações com playlists
    private PlaylistSongDao playlistSongDao; // DAO para operações com a relação playlist-música
    private ExecutorService executorService; // Executor para operações em background

    /**
     * Construtor do repositório.
     * @param application Aplicação Android para obter o banco de dados.
     */
    public MusicRepository(Application application) {
        MusicDatabase database = MusicDatabase.getDatabase(application);
        songDao = database.songDao();
        playlistDao = database.playlistDao();
        playlistSongDao = database.playlistSongDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Insere uma nova música no banco de dados.
     * @param song Música a ser inserida.
     */
    public void insertSong(Song song) {
        executorService.execute(() -> songDao.insert(song));
    }

    /**
     * Atualiza uma música existente no banco de dados.
     * @param song Música a ser atualizada.
     */
    public void updateSong(Song song) {
        executorService.execute(() -> songDao.update(song));
    }

    /**
     * Deleta uma música do banco de dados.
     * @param song Música a ser deletada.
     */
    public void deleteSong(Song song) {
        executorService.execute(() -> songDao.delete(song));
    }

    /**
     * Obtém todas as músicas do banco de dados.
     * @return Lista de todas as músicas.
     */
    public List<Song> getAllSongs() {
        try {
            return new GetAllSongsAsync(songDao).execute().get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtém todas as músicas de uma playlist específica.
     * @param playlistId ID da playlist.
     * @return Lista de músicas da playlist.
     */
    public List<Song> getSongsByPlaylist(int playlistId) {
        try {
            return new GetSongsByPlaylistAsync(songDao).execute(playlistId).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Insere uma nova playlist no banco de dados.
     * @param playlist Playlist a ser inserida.
     * @return ID da playlist inserida.
     */
    public long insertPlaylist(Playlist playlist) {
        try {
            return new InsertPlaylistAsync(playlistDao).execute(playlist).get();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Atualiza uma playlist existente no banco de dados.
     * @param playlist Playlist a ser atualizada.
     */
    public void updatePlaylist(Playlist playlist) {
        executorService.execute(() -> playlistDao.update(playlist));
    }

    /**
     * Deleta uma playlist do banco de dados.
     * @param playlist Playlist a ser deletada.
     */
    public void deletePlaylist(Playlist playlist) {
        executorService.execute(() -> playlistDao.delete(playlist));
    }

    /**
     * Obtém todas as playlists do banco de dados.
     * @return Lista de todas as playlists.
     */
    public List<Playlist> getAllPlaylists() {
        try {
            return new GetAllPlaylistsAsync(playlistDao).execute().get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Adiciona uma música a uma playlist.
     * @param playlistId ID da playlist.
     * @param songId ID da música.
     */
    public void addSongToPlaylist(int playlistId, int songId) {
        executorService.execute(() -> {
            int position = playlistSongDao.getSongCountInPlaylist(playlistId);
            PlaylistSong playlistSong = new PlaylistSong(playlistId, songId, position);
            playlistSongDao.addSongToPlaylist(playlistSong);
        });
    }

    /**
     * Remove uma música de uma playlist.
     * @param playlistId ID da playlist.
     * @param songId ID da música.
     */
    public void removeSongFromPlaylist(int playlistId, int songId) {
        executorService.execute(() -> {
            playlistSongDao.removeSongFromPlaylist(playlistId, songId);
        });
    }

    /**
     * Remove todas as músicas de uma playlist.
     * @param playlistId ID da playlist.
     */
    public void clearPlaylist(int playlistId) {
        executorService.execute(() -> {
            playlistSongDao.deleteAllSongsFromPlaylist(playlistId);
        });
    }

    /**
     * AsyncTask para obter todas as músicas do banco de dados.
     */
    private static class GetAllSongsAsync extends AsyncTask<Void, Void, List<Song>> {
        private SongDao songDao;

        GetAllSongsAsync(SongDao songDao) {
            this.songDao = songDao;
        }

        @Override
        protected List<Song> doInBackground(Void... voids) {
            return songDao.getAllSongs();
        }
    }

    /**
     * AsyncTask para obter todas as músicas de uma playlist específica.
     */
    private static class GetSongsByPlaylistAsync extends AsyncTask<Integer, Void, List<Song>> {
        private SongDao songDao;

        GetSongsByPlaylistAsync(SongDao songDao) {
            this.songDao = songDao;
        }

        @Override
        protected List<Song> doInBackground(Integer... playlistIds) {
            return songDao.getSongsByPlaylist(playlistIds[0]);
        }
    }

    /**
     * AsyncTask para obter todas as playlists do banco de dados.
     */
    private static class GetAllPlaylistsAsync extends AsyncTask<Void, Void, List<Playlist>> {
        private PlaylistDao playlistDao;

        GetAllPlaylistsAsync(PlaylistDao playlistDao) {
            this.playlistDao = playlistDao;
        }

        @Override
        protected List<Playlist> doInBackground(Void... voids) {
            return playlistDao.getAllPlaylists();
        }
    }

    /**
     * AsyncTask para inserir uma nova playlist no banco de dados.
     */
    private static class InsertPlaylistAsync extends AsyncTask<Playlist, Void, Long> {
        private PlaylistDao playlistDao;

        InsertPlaylistAsync(PlaylistDao playlistDao) {
            this.playlistDao = playlistDao;
        }

        @Override
        protected Long doInBackground(Playlist... playlists) {
            return playlistDao.insert(playlists[0]);
        }
    }
}