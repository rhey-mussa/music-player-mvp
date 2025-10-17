Music Player - Meu Leitor de Música
Aplicativo de música para Android desenvolvido como projeto acadêmico, com funcionalidades completas de reprodução, playlists e identificação de músicas.

O que o app faz
Tocar músicas do celular e músicas que vêm com o app

Criar e organizar playlists

Identificar músicas que estão tocando ao redor

Continuar tocando quando o app está fechado

Buscar músicas rapidamente

Embaralhar e repetir músicas

Interface fácil de usar

Como foi feito
Model: Cuida dos dados e das músicas

View: Mostra a tela para o usuário

Presenter: Controla o que acontece quando o usuário toca na tela

Tecnologias usadas
Java

Android 8.0 ou superior

Banco de dados SQLite

Sistema de reconhecimento ACRCloud

Serviço em segundo plano

Para usar
Baixe o código

Abra no Android Studio

Execute no celular ou emulador

Para desenvolvedores
Estrutura das pastas
text
app/src/main/java/
├── Model/              # Arquivos dos dados
├── presenter/          # Arquivos de controle
├── view/               # Arquivos de tela
└── database/           # Arquivos do banco de dados
Configurar reconhecimento de músicas
No arquivo ACRCloudRecognizer.java, coloque suas chaves:

java
private static final String ACCESS_KEY = "a0f469820c7fc88dd1e4a7e7fbd0e95f";
private static final String ACCESS_SECRET = "CbzGnOsWhVc8qJsq02e49n9DSEtXmBZWTS1pdvXX";
Permissões necessárias
Gravar áudio (para identificar músicas)

Ler músicas do celular

Rodar em segundo plano

Como funciona
Telas principais
Tela do player: Onde se controla a música

Lista de músicas: Todas as músicas disponíveis

Playlists: Onde se criam listas de músicas

Identificar música: Tela que descobre que música está tocando

Funcionalidades
Playlists: Crie listas, adicione músicas, organize como quiser

Identificação: O app escuta o som e descobre qual é a música

Busca: Encontre músicas pelo nome ou artista

Segundo plano: A música não para quando fecha o app

Desenvolvido por
Mussaldyne Rhey Mussa
Estudante de Tecnologias de Informação

Observações
Projeto desenvolvido para fins acadêmicos usando tecnologias modernas de desenvolvimento Android.

Versão: 1.0
Data: Outubro 2024
