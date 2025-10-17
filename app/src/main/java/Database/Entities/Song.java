package com.mussaldynerhey.meuleitorrhey.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

/**
 * Entidade que representa uma música no banco de dados.
 */
@Entity(tableName = "songs")
public class Song implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id; // ID único da música, gerado automaticamente

    private String title; // Título da música
    private String artist; // Artista da música
    private String path; // Caminho do arquivo da música
    private int resourceId; // ID do recurso da música
    private long duration; // Duração da música em milissegundos

    @ColumnInfo(name = "album_art_path")
    private String albumArtPath; // Caminho da capa do álbum

    /**
     * Construtor para criar uma nova música.
     * @param title Título da música.
     * @param artist Artista da música.
     * @param path Caminho do arquivo da música.
     * @param resourceId ID do recurso da música.
     * @param duration Duração da música em milissegundos.
     * @param albumArtPath Caminho da capa do álbum.
     */
    public Song(String title, String artist, String path, int resourceId, long duration, String albumArtPath) {
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.resourceId = resourceId;
        this.duration = duration;
        this.albumArtPath = albumArtPath;
    }

    /**
     * Obtém o ID da música.
     * @return ID da música.
     */
    public int getId() {
        return id;
    }

    /**
     * Define o ID da música.
     * @param id ID da música.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtém o título da música.
     * @return Título da música.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Define o título da música.
     * @param title Título da música.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Obtém o artista da música.
     * @return Artista da música.
     */
    public String getArtist() {
        return artist;
    }

    /**
     * Define o artista da música.
     * @param artist Artista da música.
     */
    public void setArtist(String artist) {
        this.artist = artist;
    }

    /**
     * Obtém o caminho do arquivo da música.
     * @return Caminho do arquivo.
     */
    public String getPath() {
        return path;
    }

    /**
     * Define o caminho do arquivo da música.
     * @param path Caminho do arquivo.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Obtém o ID do recurso da música.
     * @return ID do recurso.
     */
    public int getResourceId() {
        return resourceId;
    }

    /**
     * Define o ID do recurso da música.
     * @param resourceId ID do recurso.
     */
    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * Obtém a duração da música.
     * @return Duração em milissegundos.
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Define a duração da música.
     * @param duration Duração em milissegundos.
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * Obtém o caminho da capa do álbum.
     * @return Caminho da capa do álbum.
     */
    public String getAlbumArtPath() {
        return albumArtPath;
    }

    /**
     * Define o caminho da capa do álbum.
     * @param albumArtPath Caminho da capa do álbum.
     */
    public void setAlbumArtPath(String albumArtPath) {
        this.albumArtPath = albumArtPath;
    }
}