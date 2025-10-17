package com.mussaldynerhey.meuleitorrhey;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mussaldynerhey.meuleitorrhey.database.entities.Song;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador para exibir músicas em um RecyclerView.
 */
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private final List<Song> songs; // Lista de músicas para exibição
    private final OnSongClickListener clickListener; // Listener para cliques normais
    private final OnSongLongClickListener longClickListener; // Listener para cliques longos

    /**
     * Interface para lidar com cliques normais em uma música.
     */
    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    /**
     * Interface para lidar com cliques longos em uma música.
     */
    public interface OnSongLongClickListener {
        boolean onSongLongClick(Song song);
    }

    /**
     * Construtor do adaptador.
     * @param songs Lista de músicas.
     * @param clickListener Listener para cliques normais.
     * @param longClickListener Listener para cliques longos.
     */
    public SongAdapter(List<Song> songs, OnSongClickListener clickListener, OnSongLongClickListener longClickListener) {
        this.songs = songs;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    /**
     * Cria um novo ViewHolder para um item da lista.
     * @param parent Grupo de visualização pai.
     * @param viewType Tipo de visualização.
     * @return Novo ViewHolder.
     */
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    /**
     * Vincula os dados de uma música ao ViewHolder.
     * @param holder ViewHolder a ser atualizado.
     * @param position Posição do item na lista.
     */
    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song currentSong = songs.get(position);
        holder.bind(currentSong, clickListener, longClickListener);
    }

    /**
     * Retorna o número total de itens na lista.
     * @return Tamanho da lista de músicas.
     */
    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    /**
     * ViewHolder para um item de música.
     */
    static class SongViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView; // TextView para o título da música
        private final TextView artistTextView; // TextView para o artista
        private final ImageView albumArtImageView; // ImageView para a capa do álbum

        /**
         * Construtor do ViewHolder.
         * @param itemView Visualização do item.
         */
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.song_title);
            artistTextView = itemView.findViewById(R.id.song_artist);
            albumArtImageView = itemView.findViewById(R.id.album_art);
        }

        /**
         * Vincula os dados da música ao ViewHolder.
         * @param song Música a ser exibida.
         * @param clickListener Listener para cliques normais.
         * @param longClickListener Listener para cliques longos.
         */
        public void bind(final Song song, final OnSongClickListener clickListener, final OnSongLongClickListener longClickListener) {
            titleTextView.setText(song.getTitle());
            artistTextView.setText(song.getArtist());

            // Carregar a capa do álbum (se a lógica estiver implementada)
            // new LoadAlbumArtTask(albumArtImageView).execute(song.getPath());

            itemView.setOnClickListener(v -> clickListener.onSongClick(song));
            itemView.setOnLongClickListener(v -> longClickListener.onSongLongClick(song));
        }
    }
}