package view;

import java.util.ArrayList;
import java.util.List;

import Model.Musica;

/**
 * Interface que define os contratos para a View e o Presenter no padrão MVP.
 */
public interface Contrato {

    /**
     * Interface que define os métodos que a View deve implementar.
     */
    interface View {
        /**
         * Define a lista de músicas a ser exibida na interface.
         * @param songs Lista de músicas.
         */
        void setSongList(List<Musica> songs);

        /**
         * Atualiza a UI para exibir o estado de reprodução.
         */
        void mostrarPlay();

        /**
         * Atualiza a UI para exibir o estado de pausa.
         */
        void mostrarStop();

        /**
         * Exibe uma mensagem de erro na interface.
         * @param mensagem Mensagem de erro a ser exibida.
         */
        void mensagemErro(String mensagem);

        /**
         * Exibe informações da música atual (artista e título).
         * @param artista Nome do artista.
         * @param titulo Título da música.
         */
        void informacaoMusica(String artista, String titulo);

        /**
         * Avança para a próxima música.
         */
        void next();

        /**
         * Volta para a música anterior.
         */
        void previous();

        /**
         * Alterna o modo shuffle (embaralhamento).
         */
        void toggleShuffle();

        /**
         * Alterna o modo repeat (repetição).
         */
        void toggleRepeat();

        /**
         * Move a reprodução para uma posição específica.
         * @param position Posição em milissegundos.
         */
        void seekTo(int position);

        /**
         * Atualiza a barra de progresso da reprodução.
         * @param progress Posição atual da reprodução.
         * @param duration Duração total da música.
         */
        void updateSeekBar(int progress, int duration);

        /**
         * Atualiza o ícone do modo shuffle.
         * @param isOn Indica se o shuffle está ativado.
         */
        void updateShuffleIcon(boolean isOn);

        /**
         * Atualiza o ícone do modo repeat.
         * @param isOn Indica se o repeat está ativado.
         */
        void updateRepeatIcon(boolean isOn);

        /**
         * Exibe a lista de músicas filtrada por uma busca.
         * @param filteredSongs Lista de músicas filtradas.
         */
        void showFilteredSongs(List<Musica> filteredSongs);

        /**
         * Toca uma nova lista de músicas a partir de um índice específico.
         * @param songs Lista de músicas.
         * @param index Índice da música inicial.
         */
        void playNewList(ArrayList<Musica> songs, int index);
    }

    /**
     * Interface que define os métodos que o Presenter deve implementar.
     */
    interface Presenter {
        /**
         * Notifica a View para exibir o estado de reprodução.
         */
        void mostrarPlay();

        /**
         * Notifica a View para exibir o estado de pausa.
         */
        void mostrarStop();

        /**
         * Avança para a próxima música.
         */
        void next();

        /**
         * Volta para a música anterior.
         */
        void previous();

        /**
         * Alterna o modo shuffle (embaralhamento).
         */
        void toggleShuffle();

        /**
         * Alterna o modo repeat (repetição).
         */
        void toggleRepeat();

        /**
         * Atualiza o estado de reprodução.
         * @param isPlaying Indica se a música está tocando.
         */
        void updatePlayingStatus(boolean isPlaying);

        /**
         * Move a reprodução para uma posição específica.
         * @param position Posição em milissegundos.
         */
        void seekTo(int position);

        /**
         * Toca uma música específica com base no índice.
         * @param position Índice da música.
         */
        void playSpecificSong(int position);

        /**
         * Força o recarregamento de todas as músicas do dispositivo.
         */
        void forceReloadSongs();

        /**
         * Sincroniza o estado do player com o serviço.
         * @param songIndex Índice da música atual.
         * @param shouldPlay Indica se deve tocar.
         * @param currentPosition Posição atual da reprodução.
         * @param duration Duração total da música.
         */
        void syncPlayerState(int songIndex, boolean shouldPlay, int currentPosition, int duration);

        /**
         * Lida com o clique no botão de play.
         */
        void onPlayClicked();

        /**
         * Lida com o clique no botão de pausa.
         */
        void onPauseClicked();

        /**
         * Filtra músicas com base em uma consulta.
         * @param query Texto de busca.
         */
        void filterSongs(String query);

        /**
         * Toca uma nova lista de músicas a partir de um índice específico.
         * @param newList Nova lista de músicas.
         * @param position Índice da música inicial.
         */
        void playNewList(List<Musica> newList, int position);
    }
}