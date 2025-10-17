package com.mussaldynerhey.meuleitorrhey;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mussaldynerhey.meuleitorrhey.database.MusicDatabase;
import com.mussaldynerhey.meuleitorrhey.database.daos.PlaylistSongDao;
import com.mussaldynerhey.meuleitorrhey.database.daos.SongDao;
import com.mussaldynerhey.meuleitorrhey.database.entities.PlaylistSong;
import com.mussaldynerhey.meuleitorrhey.database.entities.Song;
import com.mussaldynerhey.meuleitorrhey.database.relations.PlaylistWithSongs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Activity para exibir e gerenciar os detalhes de uma playlist, incluindo suas músicas.
 */
public class PlaylistDetailsActivity extends AppCompatActivity {

    private static final String TAG = "PlaylistDetailsActivity";
    public static final String EXTRA_PLAYLIST_ID = "playlist_id";
    public static final String EXTRA_PLAYLIST_NAME = "playlist_name";

    private int playlistId = -1; // ID da playlist atual
    private MusicDatabase db; // Instância do banco de dados
    private Executor databaseExecutor; // Executor para operações do banco em background
    private SongAdapter songAdapter; // Adaptador para o RecyclerView de músicas
    private final ArrayList<Song> songsInPlaylist = new ArrayList<>(); // Lista de músicas na playlist

    /**
     * Método chamado quando a Activity é criada.
     * @param savedInstanceState Estado salvo da Activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_details);

        playlistId = getIntent().getIntExtra(EXTRA_PLAYLIST_ID, -1);
        String playlistName = getIntent().getStringExtra(EXTRA_PLAYLIST_NAME);

        if (playlistId == -1) {
            finish();
            return;
        }

        db = MusicDatabase.getDatabase(this);
        databaseExecutor = Executors.newSingleThreadExecutor();

        // Configuração da Toolbar
        Toolbar toolbar = findViewById(R.id.details_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(playlistName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Configuração do RecyclerView
        RecyclerView songsRecyclerView = findViewById(R.id.playlist_songs_recycler_view);
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        songAdapter = new SongAdapter(songsInPlaylist,
                song -> {
                    int clickedIndex = songsInPlaylist.indexOf(song);
                    if (clickedIndex != -1) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("SONG_LIST_RESULT", songsInPlaylist);
                        resultIntent.putExtra("SONG_INDEX_RESULT", clickedIndex);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                },
                song -> { // Clique longo para remover
                    showRemoveSongDialog(song);
                    return true;
                }
        );
        songsRecyclerView.setAdapter(songAdapter);

        // Configuração do Botão de Adicionar (FAB)
        FloatingActionButton fab = findViewById(R.id.fab_add_songs);
        fab.setOnClickListener(view -> showAddSongsDialog());
    }

    /**
     * Recarrega as músicas da playlist quando a Activity é retomada.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadSongsInPlaylist();
    }

    /**
     * Carrega as músicas da playlist do banco e atualiza o RecyclerView.
     */
    private void loadSongsInPlaylist() {
        databaseExecutor.execute(() -> {
            PlaylistWithSongs playlistWithSongs = db.playlistDao().getPlaylistWithSongs(playlistId);

            songsInPlaylist.clear();
            if (playlistWithSongs != null && playlistWithSongs.songs != null) {
                songsInPlaylist.addAll(playlistWithSongs.songs);
            }

            runOnUiThread(() -> {
                songAdapter.notifyDataSetChanged();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle(songsInPlaylist.size() == 1 ? "1 música" : songsInPlaylist.size() + " músicas");
                }
            });
        });
    }

    /**
     * Exibe um diálogo para adicionar músicas à playlist.
     */
    private void showAddSongsDialog() {
        databaseExecutor.execute(() -> {
            SongDao songDao = db.songDao();
            List<Song> allSongs = songDao.getAllSongsForSelection();
            String[] songTitles = allSongs.stream().map(Song::getTitle).toArray(String[]::new);

            boolean[] checkedItems = new boolean[allSongs.size()];
            ArrayList<Song> selectedSongs = new ArrayList<>();

            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Adicionar Músicas");
                builder.setMultiChoiceItems(songTitles, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedSongs.add(allSongs.get(which));
                    } else {
                        selectedSongs.remove(allSongs.get(which));
                    }
                });

                builder.setPositiveButton("ADICIONAR", (dialog, which) -> {
                    if (!selectedSongs.isEmpty()) {
                        addSongsToPlaylist(selectedSongs);
                    }
                });
                builder.setNegativeButton("CANCELAR", null);
                builder.create().show();
            });
        });
    }

    /**
     * Adiciona músicas selecionadas à playlist no banco de dados.
     * @param songsToAdd Lista de músicas a serem adicionadas.
     */
    private void addSongsToPlaylist(List<Song> songsToAdd) {
        databaseExecutor.execute(() -> {
            PlaylistSongDao playlistSongDao = db.playlistSongDao();
            for (Song song : songsToAdd) {
                int position = playlistSongDao.getSongCountInPlaylist(playlistId);
                PlaylistSong playlistSong = new PlaylistSong(playlistId, song.getId(), position);
                playlistSongDao.addSongToPlaylist(playlistSong);
            }
            loadSongsInPlaylist();
        });
    }

    /**
     * Exibe um diálogo de confirmação para remover uma música da playlist.
     * @param song Música a ser removida.
     */
    private void showRemoveSongDialog(Song song) {
        new AlertDialog.Builder(this)
                .setTitle("Remover Música")
                .setMessage("Tem certeza que deseja remover '" + song.getTitle() + "' desta playlist?")
                .setPositiveButton("REMOVER", (dialog, which) -> removeSongFromPlaylist(song))
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    /**
     * Remove uma música da playlist no banco de dados.
     * @param song Música a ser removida.
     */
    private void removeSongFromPlaylist(Song song) {
        databaseExecutor.execute(() -> {
            PlaylistSongDao playlistSongDao = db.playlistSongDao();
            playlistSongDao.removeSongFromPlaylist(playlistId, song.getId());
            loadSongsInPlaylist();
        });
    }

    /**
     * Trata o clique no botão de voltar da toolbar.
     * @return True se a navegação foi tratada.
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}