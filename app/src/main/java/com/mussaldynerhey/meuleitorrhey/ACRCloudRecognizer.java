package com.mussaldynerhey.meuleitorrhey;

import android.content.Context;
import android.util.Log;

import com.acrcloud.rec.ACRCloudClient;
import com.acrcloud.rec.ACRCloudConfig;
import com.acrcloud.rec.IACRCloudListener;
import com.acrcloud.rec.ACRCloudResult;

import java.util.List;
import java.util.Map;

public class ACRCloudRecognizer {
    private static final String TAG = "ACRCloudRecognizer"; // Variável TAG armazena o nome para logs.

    private static final String HOST = "identify-eu-west-1.acrcloud.com"; // Variável HOST contém o endereço do servidor.
    private static final String ACCESS_KEY = "a0f469820c7fc88dd1e4a7e7fbd0e95f"; // Variável ACCESS_KEY guarda a chave de acesso.
    private static final String ACCESS_SECRET = "CbzGnOsWhVc8qJsq02e49n9DSEtXmBZWTS1pdvXX"; // Variável ACCESS_SECRET guarda o segredo de acesso.

    private ACRCloudClient mClient; // Variável mClient representa o cliente ACRCloud.
    private RecognitionListener mListener; // Variável mListener lida com os resultados do reconhecimento.
    private boolean initState = false; // Variável initState indica se o cliente foi inicializado.

    public interface RecognitionListener {
        void onRecognitionResult(String title, String artist, String album); // Método onRecognitionResult retorna título, artista e álbum.
        void onRecognitionError(String error); // Método onRecognitionError lida com erros do reconhecimento.

        void onBackToSearch(); // Método onBackToSearch volta para a tela de busca.
    }

    public ACRCloudRecognizer(Context context, RecognitionListener listener) {
        this.mListener = listener; // Atribui o listener recebido à variável mListener.
        initACRCloud(context); // Método initACRCloud configura o cliente ACRCloud.
    }

    private void initACRCloud(Context context) {
        try {
            if (!isNetworkAvailable(context)) {
                Log.e(TAG, "Sem conexão com a internet");
                if (mListener != null) {
                    mListener.onRecognitionError("Sem conexão com a internet. Verifique WiFi/dados móveis.");
                }
                return;
            }

            ACRCloudConfig config = new ACRCloudConfig(); // Variável config define as configurações do ACRCloud.

            config.context = context; // Atribui o contexto à configuração.
            config.host = HOST; // Define o host na configuração.
            config.accessKey = ACCESS_KEY; // Define a chave de acesso na configuração.
            config.accessSecret = ACCESS_SECRET; // Define o segredo de acesso na configuração.
            config.acrcloudListener = new IACRCloudListener() {
                @Override
                public void onResult(ACRCloudResult result) {
                    Log.d(TAG, "Resultado do reconhecimento recebido");
                    processRecognitionResult(result); // Método processRecognitionResult processa o resultado do reconhecimento.
                }

                @Override
                public void onVolumeChanged(double volume) {
                    Log.d(TAG, "Volume: " + volume);
                }
            };

            config.recorderConfig.isVolumeCallback = true; // Ativa o callback de volume na configuração.

            mClient = new ACRCloudClient(); // Cria uma nova instância do cliente ACRCloud.
            initState = mClient.initWithConfig(config); // Inicializa o cliente com as configurações.

            if (initState) {
                Log.d(TAG, "ACRCloud inicializado com sucesso");
            } else {
                Log.e(TAG, "Erro na inicialização do ACRCloud");
                if (mListener != null) {
                    mListener.onRecognitionError("Erro de conexão com o serviço de identificação");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao inicializar ACRCloud", e);
            if (mListener != null) {
                mListener.onRecognitionError("Erro na inicialização: " + e.getMessage());
            }
        }
    }

    private boolean isNetworkAvailable(Context context) { // Método isNetworkAvailable verifica se há conexão com a internet.
        try {
            android.net.ConnectivityManager connectivityManager =
                    (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao verificar conexão", e);
            return false;
        }
    }

    public void startRecognizing() { // Método startRecognizing inicia o reconhecimento de música.
        if (mClient != null && initState) {
            Log.d(TAG, "Iniciando reconhecimento de música...");
            if (!mClient.startRecognize()) {
                Log.e(TAG, "Erro ao iniciar reconhecimento");
                if (mListener != null) {
                    mListener.onRecognitionError("Erro ao iniciar reconhecimento");
                }
            }
        } else {
            Log.e(TAG, "Cliente ACRCloud não inicializado");
            if (mListener != null) {
                mListener.onRecognitionError("Serviço não disponível");
            }
        }
    }

    public void stopRecognizing() { // Método stopRecognizing para o reconhecimento de música.
        if (mClient != null && initState) {
            mClient.cancel();
            Log.d(TAG, "Reconhecimento parado");
        }
    }

    public void release() { // Método release libera os recursos do cliente ACRCloud.
        if (mClient != null) {
            mClient.release();
            mClient = null;
            initState = false;
            Log.d(TAG, "ACRCloud liberado");
        }
    }

    @SuppressWarnings("unchecked")
    private void processRecognitionResult(ACRCloudResult result) { // Método processRecognitionResult processa os resultados do reconhecimento.
        try {
            String resultString = result.getResult();
            Log.d(TAG, "Resultado bruto: " + resultString);

            if (resultString == null || resultString.isEmpty()) {
                Log.e(TAG, "Resultado está vazio ou nulo");
                if (mListener != null) {
                    mListener.onRecognitionError("Nenhum resultado recebido");
                }
                return;
            }

            Map<String, Object> resultMap = new com.google.gson.Gson().fromJson(resultString, Map.class);
            Map<String, Object> status = (Map<String, Object>) resultMap.get("status");

            Log.d(TAG, "Status: " + status);

            if (status != null) {
                Object codeObj = status.get("code");
                int code;
                if (codeObj instanceof Double) {
                    code = ((Double) codeObj).intValue();
                } else {
                    code = Integer.parseInt(codeObj.toString());
                }

                Log.d(TAG, "Código do status: " + code);

                if (code == 0) {
                    Map<String, Object> metadata = (Map<String, Object>) resultMap.get("metadata");
                    Log.d(TAG, "Metadata: " + metadata);

                    if (metadata != null) {
                        java.util.List<Map<String, Object>> musicList =
                                (java.util.List<Map<String, Object>>) metadata.get("music");

                        Log.d(TAG, "Lista de músicas: " + musicList);

                        if (musicList != null && !musicList.isEmpty()) {
                            Map<String, Object> music = musicList.get(0);

                            String title = extractStringFromMap(music, "title"); // Variável title extrai o título da música.
                            String artist = extractArtistFromMap(music); // Variável artist extrai o nome do artista.
                            String album = extractAlbumFromMap(music); // Variável album extrai o nome do álbum.

                            Log.d(TAG, "MÚSICA RECONHECIDA: " + title + " - " + artist);

                            if (mListener != null) {
                                mListener.onRecognitionResult(title, artist, album);
                            }
                            return;
                        } else {
                            Log.e(TAG, "Lista de músicas vazia");
                        }
                    } else {
                        Log.e(TAG, "Metadata é nulo");
                    }
                } else {
                    String errorMsg = (String) status.get("msg");
                    Log.e(TAG, "Erro do ACRCloud: " + errorMsg + " (código: " + code + ")");
                    if (mListener != null) {
                        mListener.onRecognitionError("Erro do serviço: " + errorMsg);
                    }
                    return;
                }
            } else {
                Log.e(TAG, "Status é nulo");
            }

            if (mListener != null) {
                mListener.onRecognitionError("Música não reconhecida ou formato inválido");
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar resultado", e);
            e.printStackTrace();
            if (mListener != null) {
                mListener.onRecognitionError("Erro no processamento: " + e.getMessage());
            }
        }
    }

    private String extractStringFromMap(Map<String, Object> map, String key) { // Método extractStringFromMap extrai uma string de um mapa.
        try {
            Object value = map.get(key);
            if (value instanceof String) {
                return (String) value;
            }
            return value != null ? value.toString() : "Desconhecido";
        } catch (Exception e) {
            Log.e(TAG, "Erro ao extrair " + key + " do mapa", e);
            return "Desconhecido";
        }
    }

    private String extractArtistFromMap(Map<String, Object> music) { // Método extractArtistFromMap extrai o nome do artista.
        try {
            Object artistsObj = music.get("artists");
            if (artistsObj instanceof List) {
                List<Object> artistsList = (List<Object>) artistsObj;
                if (!artistsList.isEmpty()) {
                    Object firstArtist = artistsList.get(0);
                    if (firstArtist instanceof Map) {
                        Map<String, Object> artistMap = (Map<String, Object>) firstArtist;
                        return extractStringFromMap(artistMap, "name");
                    } else if (firstArtist instanceof String) {
                        return (String) firstArtist;
                    }
                }
            }
            return "Artista Desconhecido";
        } catch (Exception e) {
            Log.e(TAG, "Erro ao extrair artista", e);
            return "Artista Desconhecido";
        }
    }

    private String extractAlbumFromMap(Map<String, Object> music) { // Método extractAlbumFromMap extrai o nome do álbum.
        try {
            Object albumObj = music.get("album");
            if (albumObj instanceof Map) {
                Map<String, Object> albumMap = (Map<String, Object>) albumObj;
                return extractStringFromMap(albumMap, "name");
            } else if (albumObj instanceof String) {
                return (String) albumObj;
            }
            return "Álbum Desconhecido";
        } catch (Exception e) {
            Log.e(TAG, "Erro ao extrair álbum", e);
            return "Álbum Desconhecido";
        }
    }
}