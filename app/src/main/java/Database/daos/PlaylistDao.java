package com.mussaldynerhey.meuleitorrhey.database.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Transaction;

import com.mussaldynerhey.meuleitorrhey.database.entities.Playlist;
import com.mussaldynerhey.meuleitorrhey.database.relations.PlaylistWithSongs;

import java.util.List;

/**
 * Interface DAO para operações com a entidade Playlist no banco de dados.
 */
@Dao
public interface PlaylistDao {

    /**
     * Insere uma nova playlist no banco de dados.
     * @param playlist Playlist a ser inserida.
     * @return ID da playlist inserida.
     */
    @Insert
    long insert(Playlist playlist);

    /**
     * Atualiza uma playlist existente no banco de dados.
     * @param playlist Playlist a ser atualizada.
     */
    @Update
    void update(Playlist playlist);

    /**
     * Deleta uma playlist do banco de dados.
     * @param playlist Playlist a ser deletada.
     */
    @Delete
    void delete(Playlist playlist);

    /**
     * Obtém todas as playlists ordenadas por nome em ordem ascendente.
     * @return Lista de todas as playlists.
     */
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    List<Playlist> getAllPlaylists();

    /**
     * Obtém uma playlist específica pelo seu ID.
     * @param playlistId ID da playlist.
     * @return Playlist correspondente ao ID.
     */
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    Playlist getPlaylistById(int playlistId);

    /**
     * Conta o número de playlists com um determinado nome.
     * @param name Nome da playlist a ser verificado.
     * @return Número de playlists com o nome especificado.
     */
    @Query("SELECT COUNT(*) FROM playlists WHERE name = :name")
    int countPlaylistsWithName(String name);

    /**
     * Obtém uma playlist com suas músicas associadas em uma única transação.
     * @param playlistId ID da playlist.
     * @return Objeto PlaylistWithSongs contendo a playlist e suas músicas.
     */
    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    PlaylistWithSongs getPlaylistWithSongs(int playlistId);
}