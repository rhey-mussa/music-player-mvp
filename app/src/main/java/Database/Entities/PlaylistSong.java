package com.mussaldynerhey.meuleitorrhey.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * Entidade que representa a relação entre uma playlist e uma música no banco de dados.
 */
@Entity(
        tableName = "playlist_songs_ref",
        primaryKeys = {"playlistId", "songId"},
        foreignKeys = {
                @ForeignKey(
                        entity = com.mussaldynerhey.meuleitorrhey.database.entities.Playlist.class,
                        parentColumns = "id",
                        childColumns = "playlistId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = com.mussaldynerhey.meuleitorrhey.database.entities.Song.class,
                        parentColumns = "id",
                        childColumns = "songId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("playlistId"),
                @Index("songId")
        }
)
public class PlaylistSong {
    public int playlistId; // ID da playlist
    public int songId; // ID da música
    public int position; // Posição da música na playlist

    /**
     * Construtor para criar uma relação entre playlist e música.
     * @param playlistId ID da playlist.
     * @param songId ID da música.
     * @param position Posição da música na playlist.
     */
    public PlaylistSong(int playlistId, int songId, int position) {
        this.playlistId = playlistId;
        this.songId = songId;
        this.position = position;
    }

    /**
     * Obtém o ID da playlist.
     * @return ID da playlist.
     */
    public int getPlaylistId() {
        return playlistId;
    }

    /**
     * Obtém o ID da música.
     * @return ID da música.
     */
    public int getSongId() {
        return songId;
    }

    /**
     * Obtém a posição da música na playlist.
     * @return Posição da música.
     */
    public int getPosition() {
        return position;
    }
}