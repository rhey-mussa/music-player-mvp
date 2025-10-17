package com.mussaldynerhey.meuleitorrhey.database.relations;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.mussaldynerhey.meuleitorrhey.database.entities.Playlist;
import com.mussaldynerhey.meuleitorrhey.database.entities.PlaylistSong;
import com.mussaldynerhey.meuleitorrhey.database.entities.Song;

import java.util.List;

/**
 * Classe que representa a relação muitos-para-muitos entre Playlist e Song.
 */
public class PlaylistWithSongs {
    @Embedded
    public Playlist playlist; // Playlist incorporada

    /**
     * Define a relação muitos-para-muitos entre Playlist e Song usando a tabela de junção PlaylistSong.
     */
    @Relation(
            parentColumn = "id", // Chave primária da tabela Playlist
            entityColumn = "id", // Chave primária da tabela Song
            associateBy = @Junction(
                    value = PlaylistSong.class, // Tabela de junção
                    parentColumn = "playlistId", // Coluna que referencia a Playlist
                    entityColumn = "songId"      // Coluna que referencia a Song
            )
    )
    public List<Song> songs; // Lista de músicas associadas à playlist
}