package com.mussaldynerhey.meuleitorrhey;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mussaldynerhey.meuleitorrhey.database.entities.Playlist;
import com.mussaldynerhey.meuleitorrhey.database.relations.PlaylistWithSongs;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador para exibir playlists em um RecyclerView com suporte a filtragem.
 */
public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> implements Filterable {

    private List<PlaylistWithSongs> playlistsWithSongs; // Lista para exibição (pode ser filtrada)
    private final List<PlaylistWithSongs> playlistsWithSongsFull; // Lista mestre com todos os itens
    private final OnPlaylistClickListener clickListener; // Listener para cliques normais
    private final OnPlaylistLongClickListener longClickListener; // Listener para cliques longos

    /**
     * Interface para lidar com cliques normais em uma playlist.
     */
    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    /**
     * Interface para lidar com cliques longos em uma playlist.
     */
    public interface OnPlaylistLongClickListener {
        boolean onPlaylistLongClick(Playlist playlist);
    }

    /**
     * Construtor do adaptador.
     * @param playlistsWithSongs Lista inicial de playlists com suas músicas.
     * @param clickListener Listener para cliques normais.
     * @param longClickListener Listener para cliques longos.
     */
    public PlaylistAdapter(List<PlaylistWithSongs> playlistsWithSongs, OnPlaylistClickListener clickListener, OnPlaylistLongClickListener longClickListener) {
        this.playlistsWithSongs = playlistsWithSongs;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.playlistsWithSongsFull = new ArrayList<>(playlistsWithSongs); // Cria cópia da lista original
    }

    /**
     * Cria um novo ViewHolder para um item da lista.
     * @param parent Grupo de visualização pai.
     * @param viewType Tipo de visualização.
     * @return Novo ViewHolder.
     */
    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    /**
     * Vincula os dados de uma playlist ao ViewHolder.
     * @param holder ViewHolder a ser atualizado.
     * @param position Posição do item na lista.
     */
    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        PlaylistWithSongs currentData = playlistsWithSongs.get(position);
        holder.bind(currentData, clickListener, longClickListener);
    }

    /**
     * Retorna o número total de itens na lista.
     * @return Tamanho da lista de exibição.
     */
    @Override
    public int getItemCount() {
        return playlistsWithSongs.size();
    }

    /**
     * Atualiza os dados do adaptador com uma nova lista de playlists.
     * @param newData Nova lista de playlists com suas músicas.
     */
    public void updateData(List<PlaylistWithSongs> newData) {
        this.playlistsWithSongs.clear();
        this.playlistsWithSongs.addAll(newData);
        this.playlistsWithSongsFull.clear();
        this.playlistsWithSongsFull.addAll(newData);
        notifyDataSetChanged();
    }

    /**
     * ViewHolder para um item de playlist.
     */
    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private final TextView playlistName; // TextView para o nome da playlist
        private final TextView playlistInfo; // TextView para informações da playlist

        /**
         * Construtor do ViewHolder.
         * @param itemView Visualização do item.
         */
        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistName = itemView.findViewById(R.id.playlist_name);
            playlistInfo = itemView.findViewById(R.id.playlist_info);
        }

        /**
         * Vincula os dados da playlist ao ViewHolder.
         * @param data Dados da playlist com suas músicas.
         * @param clickListener Listener para cliques normais.
         * @param longClickListener Listener para cliques longos.
         */
        public void bind(final PlaylistWithSongs data, final OnPlaylistClickListener clickListener, final OnPlaylistLongClickListener longClickListener) {
            Playlist playlist = data.playlist;
            playlistName.setText(playlist.getName());

            int songCount = data.songs != null ? data.songs.size() : 0;
            String songCountText = songCount == 1 ? "1 música" : songCount + " músicas";
            playlistInfo.setText(songCountText);

            itemView.setOnClickListener(v -> clickListener.onPlaylistClick(playlist));
            itemView.setOnLongClickListener(v -> longClickListener.onPlaylistLongClick(playlist));
        }
    }

    /**
     * Retorna o filtro para busca de playlists.
     * @return Objeto Filter para filtragem.
     */
    @Override
    public Filter getFilter() {
        return playlistFilter;
    }

    private final Filter playlistFilter = new Filter() {
        /**
         * Realiza a filtragem em uma thread de fundo.
         * @param constraint Texto de busca.
         * @return Resultados da filtragem.
         */
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<PlaylistWithSongs> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(playlistsWithSongsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (PlaylistWithSongs item : playlistsWithSongsFull) {
                    if (item.playlist.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        /**
         * Publica os resultados da filtragem na UI thread.
         * @param constraint Texto de busca.
         * @param results Resultados da filtragem.
         */
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            playlistsWithSongs.clear();
            playlistsWithSongs.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };
}