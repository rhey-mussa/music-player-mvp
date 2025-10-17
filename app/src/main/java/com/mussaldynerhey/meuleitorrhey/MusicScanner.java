package com.mussaldynerhey.meuleitorrhey.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.mussaldynerhey.meuleitorrhey.database.entities.Song;
import com.mussaldynerhey.meuleitorrhey.database.repository.MusicRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Classe responsável por escanear músicas no dispositivo e sincronizá-las com o banco de dados.
 */
public class MusicScanner {
    private static final String TAG = "MusicScanner";
    private Context context; // Contexto da aplicação
    private MusicRepository musicRepository; // Repositório para acesso ao banco de dados

    /**
     * Construtor da classe MusicScanner.
     * @param context Contexto da aplicação.
     * @param musicRepository Repositório para gerenciar músicas no banco de dados.
     */
    public MusicScanner(Context context, MusicRepository musicRepository) {
        this.context = context;
        this.musicRepository = musicRepository;
    }

    /**
     * Escaneia o dispositivo em busca de arquivos de música.
     * @return Lista de objetos Song representando as músicas encontradas.
     */
    public List<Song> scanDeviceForMusic() {
        List<Song> deviceSongs = new ArrayList<>(); // Lista para armazenar músicas encontradas
        Set<String> uniquePaths = new HashSet<>(); // Conjunto para evitar duplicatas por caminho do arquivo

        ContentResolver contentResolver = context.getContentResolver(); // Resolver para acessar dados do dispositivo
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; // URI para arquivos de áudio

        // Colunas a serem recuperadas do MediaStore
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM
        };

        // Filtro para incluir apenas arquivos de música
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC"; // Ordenar por título em ordem ascendente

        Cursor cursor = null;
        try {
            // Executar consulta ao MediaStore
            cursor = contentResolver.query(musicUri, projection, selection, null, sortOrder);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Extrair dados do cursor
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));

                    // Definir "Artista Desconhecido" se o campo de artista estiver vazio
                    if (artist == null || artist.trim().isEmpty()) {
                        artist = "Artista Desconhecido";
                    }

                    // Pular música se o título estiver vazio
                    if (title == null || title.trim().isEmpty()) {
                        continue;
                    }

                    // Verificar duplicatas pelo caminho do arquivo
                    if (uniquePaths.contains(path)) {
                        Log.d(TAG, "Música duplicada ignorada: " + title + " - " + path);
                        continue;
                    }
                    uniquePaths.add(path);

                    // Criar URI para a música
                    Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                    // Criar objeto Song com os dados extraídos
                    Song song = new Song(
                            title,
                            artist,
                            path,
                            0, // resourceId (0 para músicas do dispositivo)
                            duration,
                            songUri.toString() // Usar URI como albumArtPath para consistência
                    );
                    deviceSongs.add(song);

                    Log.d(TAG, "Música encontrada: " + title + " - " + artist);

                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar músicas do dispositivo", e);
        } finally {
            if (cursor != null) {
                cursor.close(); // Fechar o cursor para evitar vazamentos de memória
            }
        }

        Log.d(TAG, "Total de músicas encontradas no dispositivo: " + deviceSongs.size() +
                " (Duplicadas removidas: " + (uniquePaths.size() - deviceSongs.size()) + ")");
        return deviceSongs;
    }

    /**
     * Sincroniza as músicas do dispositivo com o banco de dados, adicionando apenas novas músicas.
     */
    public void syncDeviceMusicWithDatabase() {
        new Thread(() -> {
            // Escanear músicas do dispositivo
            List<Song> deviceSongs = scanDeviceForMusic();

            // Obter músicas já existentes no banco de dados
            List<Song> existingSongs = musicRepository.getAllSongs();

            // Adicionar apenas músicas que não estão no banco
            for (Song deviceSong : deviceSongs) {
                boolean alreadyExists = false;

                if (existingSongs != null) {
                    for (Song existingSong : existingSongs) {
                        if (existingSong.getPath() != null &&
                                existingSong.getPath().equals(deviceSong.getPath())) {
                            alreadyExists = true;
                            break;
                        }
                    }
                }

                if (!alreadyExists) {
                    musicRepository.insertSong(deviceSong); // Inserir nova música no banco
                    Log.d(TAG, "Nova música adicionada ao banco: " + deviceSong.getTitle());
                }
            }
        }).start(); // Executar em thread separada para evitar bloqueio da UI
    }

    /**
     * Realiza uma varredura de músicas para depuração, exibindo informações detalhadas no log.
     */
    public void debugMusicScan() {
        new Thread(() -> {
            List<Song> deviceSongs = scanDeviceForMusic();
            Log.d("DEBUG_SCAN", "=== Início do Debug de Escaneamento ===");
            Log.d("DEBUG_SCAN", "Total de músicas encontradas: " + deviceSongs.size());

            for (Song song : deviceSongs) {
                Log.d("DEBUG_SCAN", "Música: " + song.getTitle() + " - " + song.getArtist() + " | Caminho: " + song.getPath());
            }
            Log.d("DEBUG_SCAN", "=== Fim do Debug de Escaneamento ===");
        }).start(); // Executar em thread separada para evitar bloqueio da UI
    }
}