package com.mussaldynerhey.meuleitorrhey;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import Model.Musica;

/**
 * Adaptador para exibir uma lista de músicas em um RecyclerView.
 */
public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.ViewHolder> {
    private List<Musica> songs; // Lista de músicas a serem exibidas
    private OnSongClickListener listener; // Listener para cliques em itens da lista

    /**
     * Construtor do adaptador.
     * @param songs Lista inicial de músicas.
     * @param listener Listener para cliques em músicas.
     */
    public MusicListAdapter(List<Musica> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    /**
     * Atualiza a lista de músicas exibidas no adaptador.
     * @param newSongs Nova lista de músicas.
     */
    public void updateSongs(List<Musica> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged(); // Notifica o RecyclerView para atualizar a exibição
    }

    /**
     * Cria um novo ViewHolder para um item da lista.
     * @param parent ViewGroup pai.
     * @param viewType Tipo de visualização.
     * @return Novo ViewHolder.
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_simple, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Vincula os dados de uma música a um ViewHolder.
     * @param holder ViewHolder a ser preenchido.
     * @param position Posição da música na lista.
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Musica song = songs.get(position);
        holder.titleTextView.setText(song.getTitulo()); // Define o título da música
        holder.artistTextView.setText(song.getArtista()); // Define o nome do artista

        // Define ícone padrão para todas as músicas, ignorando contentUri
        holder.iconImageView.setImageResource(R.drawable.ic_music);

        // Configura o clique no item para acionar o listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(position);
            }
        });
    }

    /**
     * Retorna o número total de músicas na lista.
     * @return Tamanho da lista de músicas.
     */
    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    /**
     * Obtém a lista atual de músicas.
     * @return Lista de músicas.
     */
    public List<Musica> getSongs() {
        return songs;
    }

    /**
     * Classe interna para representar o ViewHolder de cada item da lista.
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, artistTextView;
        ImageView iconImageView;

        /**
         * Construtor do ViewHolder.
         * @param itemView View do item da lista.
         */
        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.song_item_title); // TextView para o título
            artistTextView = itemView.findViewById(R.id.song_item_artist); // TextView para o artista
            iconImageView = itemView.findViewById(R.id.song_icon); // ImageView para o ícone da música
        }
    }

    /**
     * Interface para lidar com cliques em itens da lista.
     */
    public interface OnSongClickListener {
        void onSongClick(int position);
    }
}