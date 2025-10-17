package com.mussaldynerhey.meuleitorrhey.database.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.mussaldynerhey.meuleitorrhey.database.entities.Song;

import java.util.List;

/**
 * Interface DAO para operações com a entidade Song no banco de dados.
 */
@Dao
public interface SongDao {

    /**
     * Insere uma nova música no banco de dados, ignorando conflitos.
     * @param song Música a ser inserida.
     * @return ID da música inserida.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Song song);

    /**
     * Atualiza uma música existente no banco de dados.
     * @param song Música a ser atualizada.
     */
    @Update
    void update(Song song);

    /**
     * Deleta uma música do banco de dados.
     * @param song Música a ser deletada.
     */
    @Delete
    void delete(Song song);

    /**
     * Obtém todas as músicas ordenadas por título em ordem ascendente.
     * @return Lista de todas as músicas.
     */
    @Query("SELECT * FROM songs ORDER BY title ASC")
    List<Song> getAllSongs();

    /**
     * Obtém uma música específica pelo seu ID.
     * @param songId ID da música.
     * @return Música correspondente ao ID.
     */
    @Query("SELECT * FROM songs WHERE id = :songId")
    Song getSongById(int songId);

    /**
     * Busca músicas pelo título ou artista.
     * @param query Texto de busca.
     * @return Lista de músicas que correspondem à consulta.
     */
    @Query("SELECT * FROM songs WHERE title LIKE :query OR artist LIKE :query")
    List<Song> searchSongs(String query);

    /**
     * Obtém todas as músicas de uma playlist específica, ordenadas por posição.
     * @param playlistId ID da playlist.
     * @return Lista de músicas associadas à playlist.
     */
    @Query("SELECT s.* FROM songs s " +
            "INNER JOIN playlist_songs_ref psr ON s.id = psr.songId " +
            "WHERE psr.playlistId = :playlistId " +
            "ORDER BY psr.position ASC")
    List<Song> getSongsByPlaylist(int playlistId);

    /**
     * Obtém todas as músicas disponíveis para seleção, ordenadas por título.
     * @return Lista de todas as músicas para seleção.
     */
    @Query("SELECT * FROM songs ORDER BY title ASC")
    List<Song> getAllSongsForSelection();
}