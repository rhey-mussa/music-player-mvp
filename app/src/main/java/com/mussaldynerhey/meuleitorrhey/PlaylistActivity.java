package com.mussaldynerhey.meuleitorrhey;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mussaldynerhey.meuleitorrhey.database.MusicDatabase;
import com.mussaldynerhey.meuleitorrhey.database.daos.PlaylistDao;
import com.mussaldynerhey.meuleitorrhey.database.entities.Playlist;
import com.mussaldynerhey.meuleitorrhey.database.relations.PlaylistWithSongs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Activity para gerenciar e exibir playlists.
 */
public class PlaylistActivity extends AppCompatActivity {

    private static final String TAG = "PlaylistActivity";

    // Variáveis para o banco de dados e a UI
    private PlaylistDao playlistDao; // DAO para operações com playlists
    private Executor databaseExecutor; // Executor para operações do banco em background
    private RecyclerView playlistsRecyclerView; // RecyclerView para exibir playlists
    private PlaylistAdapter playlistAdapter; // Adaptador para o RecyclerView
    private List<PlaylistWithSongs> playlistWithSongsList = new ArrayList<>(); // Lista de playlists com suas músicas

    /**
     * Método chamado quando a Activity é criada.
     * @param savedInstanceState Estado salvo da Activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        // Configuração da Toolbar
        Toolbar toolbar = findViewById(R.id.playlist_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Ativa o botão de voltar

        // Inicialização do Banco de Dados e Executor
        MusicDatabase db = MusicDatabase.getDatabase(this);
        playlistDao = db.playlistDao();
        databaseExecutor = Executors.newSingleThreadExecutor(); // Thread para operações do banco

        // Configuração do RecyclerView
        playlistsRecyclerView = findViewById(R.id.playlists_recycler_view);
        playlistsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        playlistAdapter = new PlaylistAdapter(playlistWithSongsList,
                playlist -> { // Manipulador de clique normal
                    Log.d(TAG, "Playlist clicada: " + playlist.getName() + " (ID: " + playlist.getId() + ")");
                    Intent intent = new Intent(PlaylistActivity.this, PlaylistDetailsActivity.class);
                    intent.putExtra(PlaylistDetailsActivity.EXTRA_PLAYLIST_ID, playlist.getId());
                    intent.putExtra(PlaylistDetailsActivity.EXTRA_PLAYLIST_NAME, playlist.getName());
                    startActivityForResult(intent, 1);
                },
                playlist -> { // Manipulador de clique longo
                    showPlaylistOptionsDialog(playlist);
                    return true; // Evento consumido
                }
        );
        playlistsRecyclerView.setAdapter(playlistAdapter);

        // Configuração do Botão de Adicionar (FAB)
        FloatingActionButton fab = findViewById(R.id.fab_add_playlist);
        fab.setOnClickListener(view -> showCreatePlaylistDialog());

        // Carrega as playlists existentes ao iniciar a tela
        loadPlaylists();
    }

    /**
     * Recarrega as playlists quando a Activity é retomada.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadPlaylists();
    }

    /**
     * Captura o resultado retornado pela PlaylistDetailsActivity.
     * @param requestCode Código da requisição.
     * @param resultCode Código do resultado.
     * @param data Dados retornados.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
        }
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

    /**
     * Carrega todas as playlists e suas músicas do banco e atualiza o RecyclerView.
     */
    private void loadPlaylists() {
        databaseExecutor.execute(() -> {
            List<Playlist> allPlaylists = playlistDao.getAllPlaylists();
            List<PlaylistWithSongs> resultData = new ArrayList<>();

            for (Playlist p : allPlaylists) {
                PlaylistWithSongs data = playlistDao.getPlaylistWithSongs(p.id);
                if (data != null) {
                    resultData.add(data);
                }
            }

            runOnUiThread(() -> {
                playlistWithSongsList.clear();
                playlistWithSongsList.addAll(resultData);
                playlistAdapter.notifyDataSetChanged();
                Log.d(TAG, "Playlists (com contagem de músicas) carregadas: " + playlistWithSongsList.size());
            });
        });
    }

    /**
     * Exibe um diálogo para criar uma nova playlist.
     */
    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Criar Nova Playlist");

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint("Nome da Playlist");
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("CRIAR", (dialog, which) -> {
            String playlistName = input.getText().toString().trim();
            if (!playlistName.isEmpty()) {
                createNewPlaylist(playlistName);
            } else {
                Toast.makeText(this, "O nome da playlist não pode ser vazio", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * Salva uma nova playlist no banco de dados em uma thread de fundo.
     * @param name Nome da nova playlist.
     */
    private void createNewPlaylist(final String name) {
        databaseExecutor.execute(() -> {
            if (playlistDao.countPlaylistsWithName(name) > 0) {
                runOnUiThread(() -> Toast.makeText(PlaylistActivity.this, "Uma playlist com este nome já existe", Toast.LENGTH_SHORT).show());
                return;
            }

            Playlist newPlaylist = new Playlist(name);
            long newId = playlistDao.insert(newPlaylist);

            if (newId != -1) {
                runOnUiThread(() -> {
                    Toast.makeText(PlaylistActivity.this, "Playlist '" + name + "' criada!", Toast.LENGTH_SHORT).show();
                    loadPlaylists();
                });
            } else {
                runOnUiThread(() -> Toast.makeText(PlaylistActivity.this, "Erro ao criar playlist", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Exibe um diálogo com opções para a playlist (editar, deletar).
     * @param playlist Playlist selecionada.
     */
    private void showPlaylistOptionsDialog(final Playlist playlist) {
        final CharSequence[] options = {"Editar nome", "Deletar playlist", "Cancelar"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Opções para '" + playlist.getName() + "'");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Editar nome")) {
                showEditPlaylistDialog(playlist);
            } else if (options[item].equals("Deletar playlist")) {
                showDeletePlaylistDialog(playlist);
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /**
     * Exibe um diálogo para editar o nome da playlist.
     * @param playlist Playlist a ser editada.
     */
    private void showEditPlaylistDialog(final Playlist playlist) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Nome");

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(playlist.getName());
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("SALVAR", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(playlist.getName())) {
                updatePlaylistName(playlist, newName);
            }
        });
        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }

    /**
     * Exibe um diálogo de confirmação para deletar a playlist.
     * @param playlist Playlist a ser deletada.
     */
    private void showDeletePlaylistDialog(final Playlist playlist) {
        new AlertDialog.Builder(this)
                .setTitle("Deletar Playlist")
                .setMessage("Tem certeza que deseja deletar a playlist '" + playlist.getName() + "'? Esta ação não pode ser desfeita.")
                .setPositiveButton("DELETAR", (dialog, which) -> deletePlaylist(playlist))
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    /**
     * Atualiza o nome da playlist no banco de dados.
     * @param playlist Playlist a ser atualizada.
     * @param newName Novo nome da playlist.
     */
    private void updatePlaylistName(final Playlist playlist, final String newName) {
        databaseExecutor.execute(() -> {
            playlist.name = newName;
            playlistDao.update(playlist);
            runOnUiThread(this::loadPlaylists);
        });
    }

    /**
     * Deleta a playlist do banco de dados.
     * @param playlist Playlist a ser deletada.
     */
    private void deletePlaylist(final Playlist playlist) {
        databaseExecutor.execute(() -> {
            playlistDao.delete(playlist);
            runOnUiThread(this::loadPlaylists);
        });
    }
}