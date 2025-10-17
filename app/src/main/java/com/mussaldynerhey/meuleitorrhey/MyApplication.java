package com.mussaldynerhey.meuleitorrhey;

import android.app.Application;

/**
 * Classe de aplicação personalizada para gerenciar a instância global da aplicação.
 */
public class MyApplication extends Application {

    private static MyApplication instance; // Instância singleton da aplicação

    /**
     * Método chamado quando a aplicação é criada.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this; // Define a instância singleton
    }

    /**
     * Obtém a instância singleton da aplicação.
     * @return Instância da aplicação.
     */
    public static MyApplication getInstance() {
        return instance;
    }
}