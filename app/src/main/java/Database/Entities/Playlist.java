package com.mussaldynerhey.meuleitorrhey.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidade que representa uma playlist no banco de dados.
 */
@Entity(tableName = "playlists")
public class Playlist {
    @PrimaryKey(autoGenerate = true)
    public int id; // ID único da playlist, gerado automaticamente

    public String name; // Nome da playlist
    public long createdAt; // Data de criação da playlist

    /**
     * Construtor para criar uma nova playlist.
     * @param name Nome da playlist.
     */
    public Playlist(String name) {
        this.name = name;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Obtém o ID da playlist.
     * @return ID da playlist.
     */
    public int getId() {
        return id;
    }

    /**
     * Obtém o nome da playlist.
     * @return Nome da playlist.
     */
    public String getName() {
        return name;
    }

    /**
     * Obtém a data de criação da playlist.
     * @return Data de criação em milissegundos.
     */
    public long getCreatedAt() {
        return createdAt;
    }
}