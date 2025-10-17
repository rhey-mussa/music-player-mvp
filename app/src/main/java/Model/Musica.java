package Model;

import java.io.Serializable;

/**
 * Classe que representa uma música, com suporte para músicas internas e externas.
 */
public class Musica implements Serializable {
    private String titulo; // Título da música
    private String artista; // Artista da música
    private int numeroFaixa; // Número da faixa para músicas internas (raw)
    private String filePath; // Caminho do arquivo para músicas do dispositivo
    private boolean isDeviceMusic; // Indica se a música é do dispositivo
    private long duracao; // Duração da música em milissegundos
    private String contentUri; // URI no formato "content://" para músicas externas

    /**
     * Construtor para músicas internas (armazenadas no diretório 'raw').
     * @param titulo Título da música.
     * @param artista Artista da música.
     * @param numeroFaixa Número da faixa.
     * @param duracao Duração da música em milissegundos.
     */
    public Musica(String titulo, String artista, int numeroFaixa, long duracao) {
        this.titulo = titulo;
        this.artista = artista;
        this.numeroFaixa = numeroFaixa;
        this.filePath = null;
        this.isDeviceMusic = false;
        this.duracao = duracao;
        this.contentUri = null; // Músicas internas não têm content URI
    }

    /**
     * Construtor para músicas externas (do dispositivo).
     * @param titulo Título da música.
     * @param artista Artista da música.
     * @param filePath Caminho do arquivo da música.
     * @param duracao Duração da música em milissegundos.
     * @param contentUri URI no formato "content://" da música.
     */
    public Musica(String titulo, String artista, String filePath, long duracao, String contentUri) {
        this.titulo = titulo;
        this.artista = artista;
        this.numeroFaixa = 0;
        this.filePath = filePath;
        this.isDeviceMusic = true;
        this.duracao = duracao;
        this.contentUri = contentUri;
    }

    /**
     * Obtém o título da música.
     * @return Título da música.
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * Obtém o artista da música.
     * @return Artista da música.
     */
    public String getArtista() {
        return artista;
    }

    /**
     * Obtém o número da faixa (para músicas internas).
     * @return Número da faixa.
     */
    public int getNumeroFaixa() {
        return numeroFaixa;
    }

    /**
     * Obtém o caminho do arquivo da música (para músicas externas).
     * @return Caminho do arquivo.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Verifica se a música é do dispositivo.
     * @return True se a música é do dispositivo, false caso contrário.
     */
    public boolean isDeviceMusic() {
        return isDeviceMusic;
    }

    /**
     * Obtém a duração da música.
     * @return Duração em milissegundos.
     */
    public long getDuracao() {
        return duracao;
    }

    /**
     * Obtém a URI no formato "content://" da música (para músicas externas).
     * @return URI da música.
     */
    public String getContentUri() {
        return contentUri;
    }
}