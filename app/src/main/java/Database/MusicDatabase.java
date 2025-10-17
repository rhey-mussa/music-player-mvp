package com.mussaldynerhey.meuleitorrhey.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.mussaldynerhey.meuleitorrhey.database.daos.SongDao;
import com.mussaldynerhey.meuleitorrhey.database.daos.PlaylistDao;
import com.mussaldynerhey.meuleitorrhey.database.daos.PlaylistSongDao;
import com.mussaldynerhey.meuleitorrhey.database.entities.Song;
import com.mussaldynerhey.meuleitorrhey.database.entities.Playlist;
import com.mussaldynerhey.meuleitorrhey.database.entities.PlaylistSong;

/**
 * Classe de banco de dados Room que define as entidades e fornece acesso aos DAOs.
 */
@Database(
        entities = {Song.class, Playlist.class, PlaylistSong.class},
        version = 9,
        exportSchema = false
)
public abstract class MusicDatabase extends RoomDatabase {

    /**
     * Obtém o DAO para operações com músicas.
     * @return Instância de SongDao.
     */
    public abstract SongDao songDao();

    /**
     * Obtém o DAO para operações com playlists.
     * @return Instância de PlaylistDao.
     */
    public abstract PlaylistDao playlistDao();

    /**
     * Obtém o DAO para operações com a relação playlist-música.
     * @return Instância de PlaylistSongDao.
     */
    public abstract PlaylistSongDao playlistSongDao();

    private static volatile MusicDatabase INSTANCE; // Instância singleton do banco de dados

    /**
     * Obtém a instância singleton do banco de dados.
     * @param context Contexto da aplicação.
     * @return Instância do banco de dados.
     */
    public static MusicDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MusicDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    MusicDatabase.class, "music_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}