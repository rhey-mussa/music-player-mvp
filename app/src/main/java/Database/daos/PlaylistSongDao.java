package com.mussaldynerhey.meuleitorrhey.database.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mussaldynerhey.meuleitorrhey.database.entities.PlaylistSong;

@Dao
public interface PlaylistSongDao {

    /**
     * Adiciona uma música a uma playlist. Se a música já estiver na playlist, ignora a inserção.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addSongToPlaylist(PlaylistSong playlistSong);

    /**
     * Remove uma música específica de uma playlist específica.
     * Usado quando o usuário deleta uma música de dentro da tela de detalhes da playlist.
     */
    @Query("DELETE FROM playlist_songs_ref WHERE playlistId = :playlistId AND songId = :songId")
    void removeSongFromPlaylist(int playlistId, int songId);

    /**
     * Remove TODAS as músicas de uma playlist.
     * Usado antes de deletar a playlist inteira para limpar as referências.
     */
    @Query("DELETE FROM playlist_songs_ref WHERE playlistId = :playlistId")
    void deleteAllSongsFromPlaylist(int playlistId);

    /**
     * Conta quantas músicas uma playlist específica possui.
     * Útil para exibir a contagem de músicas na lista de playlists.
     */
    @Query("SELECT COUNT(songId) FROM playlist_songs_ref WHERE playlistId = :playlistId")
    int getSongCountInPlaylist(int playlistId);
}
