package com.mussaldynerhey.meuleitorrhey;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import Model.Musica;
import presenter.Presenter;
import view.Contrato;

public class MusicListActivity extends AppCompatActivity implements Contrato.View {
    private static final String TAG = "MusicListActivity";

    private RecyclerView musicListRecyclerView;
    private MusicListAdapter musicListAdapter;
    private List<Musica> allMusicas;
    private List<Musica> todasMusicas; // Lista completa para busca
    private Presenter presenter;

    // Sistema de busca
    private androidx.appcompat.widget.SearchView searchView;
    private boolean isSearchVisible = false;
    private ImageButton searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.music_list_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Botão voltar com transição
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // Botão do microfone
        ImageButton micButton = findViewById(R.id.mic_button);
        micButton.setOnClickListener(v -> {
            Log.d(TAG, "🎤 Botão do microfone clicado na lista de músicas");
            showMusicRecognitionDialog();
        });

        // Botão de busca
        searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> {
            toggleSearchVisibility();
        });

        // Título
        TextView title = findViewById(R.id.title);
        title.setText("Todas as Músicas");

        // RecyclerView
        musicListRecyclerView = findViewById(R.id.music_list);
        musicListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar presenter
        presenter = new Presenter(this, this);

        // Carregar lista de músicas do Intent ou do Presenter
        if (getIntent().hasExtra("all_musicas")) {
            allMusicas = (List<Musica>) getIntent().getSerializableExtra("all_musicas");
            todasMusicas = new ArrayList<>(allMusicas);
            setMusicList(allMusicas);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_music_recognition_list) {
            showMusicRecognitionDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMusicRecognitionDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("🎵 Reconhecimento de Música");
        builder.setMessage("Identifique uma música e adicione à sua lista!");
        builder.setPositiveButton("🎤 IDENTIFICAR", (dialog, which) -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("start_recognition", true);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            dialog.dismiss();
        });
        builder.setNegativeButton("CANCELAR", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.setNeutralButton("🔍 BUSCAR NA LISTA", (dialog, which) -> {
            if (searchView != null && !isSearchVisible) {
                toggleSearchVisibility();
            }
            if (searchView != null) {
                searchView.requestFocus();
            }
            dialog.dismiss();
        });
        builder.show();
    }

    @Override
    public void setSongList(List<Musica> songs) {
        this.allMusicas = songs;
        this.todasMusicas = new ArrayList<>(songs);
        setMusicList(songs);
    }

    private void setMusicList(List<Musica> songs) {
        musicListAdapter = new MusicListAdapter(songs, position -> {
            Log.d(TAG, "Música selecionada: " + position);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selected_song_index", position);
            setResult(RESULT_OK, resultIntent);
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        musicListRecyclerView.setAdapter(musicListAdapter);
    }

    private void toggleSearchVisibility() {
        if (!isSearchVisible) {
            showSearchInToolbar();
        } else {
            hideSearchFromToolbar();
        }
    }

    private void showSearchInToolbar() {
        TextView title = findViewById(R.id.title);
        if (title != null) {
            title.setVisibility(View.GONE);
        }
        searchView = new androidx.appcompat.widget.SearchView(this);
        searchView.setQueryHint("Buscar músicas...");
        searchView.setIconifiedByDefault(false);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        int searchEditTextId = androidx.appcompat.R.id.search_src_text;
        EditText searchEditText = searchView.findViewById(searchEditTextId);
        if (searchEditText != null) {
            searchEditText.setTextColor(Color.WHITE);
            searchEditText.setHintTextColor(Color.GRAY);
            searchEditText.setBackgroundColor(Color.TRANSPARENT);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        params.setMargins(16, 0, 16, 0);
        searchView.setLayoutParams(params);
        LinearLayout headerLayout = (LinearLayout) findViewById(R.id.back_button).getParent();
        int titleIndex = headerLayout.indexOfChild(findViewById(R.id.title));
        headerLayout.addView(searchView, titleIndex);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText);
                return true;
            }
        });
        searchView.requestFocus();
        isSearchVisible = true;
        searchButton.setImageResource(R.drawable.ic_close);
    }

    private void hideSearchFromToolbar() {
        if (searchView != null) {
            LinearLayout headerLayout = (LinearLayout) findViewById(R.id.back_button).getParent();
            headerLayout.removeView(searchView);
            searchView = null;
        }
        TextView title = findViewById(R.id.title);
        if (title != null) {
            title.setVisibility(View.VISIBLE);
        }
        if (musicListAdapter != null && todasMusicas != null) {
            musicListAdapter.updateSongs(todasMusicas);
            musicListAdapter.notifyDataSetChanged();
        }
        isSearchVisible = false;
        searchButton.setImageResource(R.drawable.ic_search);
    }

    private void filterSongs(String query) {
        Log.d(TAG, "Filtrando músicas com query: " + query);
        if (todasMusicas == null || todasMusicas.isEmpty()) {
            Log.d(TAG, "Lista de músicas vazia");
            return;
        }
        List<Musica> filteredSongs = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filteredSongs.addAll(todasMusicas);
            Log.d(TAG, "Query vazia - mostrando todas as " + todasMusicas.size() + " músicas");
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            Log.d(TAG, "Buscando por: " + lowerCaseQuery);
            for (Musica musica : todasMusicas) {
                String titulo = musica.getTitulo() != null ? musica.getTitulo().toLowerCase() : "";
                String artista = musica.getArtista() != null ? musica.getArtista().toLowerCase() : "";
                if (titulo.contains(lowerCaseQuery) || artista.contains(lowerCaseQuery)) {
                    filteredSongs.add(musica);
                    Log.d(TAG, "Música encontrada: " + musica.getTitulo() + " - " + musica.getArtista());
                }
            }
            Log.d(TAG, "Encontradas " + filteredSongs.size() + " músicas");
        }
        if (musicListAdapter != null) {
            musicListAdapter.updateSongs(filteredSongs);
            musicListAdapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "MusicListAdapter é null!");
        }
    }

    @Override
    public void onBackPressed() {
        if (isSearchVisible) {
            hideSearchFromToolbar();
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    @Override
    public void mostrarPlay() {}
    @Override
    public void mostrarStop() {}
    @Override
    public void mensagemErro(String mensagem) {}
    @Override
    public void informacaoMusica(String artista, String titulo) {}
    @Override
    public void next() {}
    @Override
    public void previous() {}
    @Override
    public void toggleShuffle() {}
    @Override
    public void toggleRepeat() {}
    @Override
    public void seekTo(int position) {}
    @Override
    public void updateSeekBar(int progress, int duration) {}
    @Override
    public void updateShuffleIcon(boolean isOn) {}
    @Override
    public void updateRepeatIcon(boolean isOn) {}
    @Override
    public void showFilteredSongs(List<Musica> filteredSongs) {
        if (musicListAdapter != null) {
            musicListAdapter.updateSongs(filteredSongs);
        }
    }
    @Override
    public void playNewList(ArrayList<Musica> songs, int index) {}
}