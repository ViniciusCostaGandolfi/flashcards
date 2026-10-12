package vgandolfi.dev.flashcards.infrastructure.s3;

import java.time.Duration;

/**
 * Porta de armazenamento de objetos (MinIO/S3). Interface fina para permitir
 * mock facil nos testes unitarios.
 */
public interface S3Storage {

    /**
     * Faz upload de bytes.
     *
     * @param key         chave do objeto (ex.: {@code uploads/{userId}/{uuid}-{filename}})
     * @param bytes       conteudo binario
     * @param contentType MIME type do conteudo
     * @return a propria key gravada
     */
    String uploadBytes(String key, byte[] bytes, String contentType);

    /**
     * Baixa o conteudo binario de um objeto.
     *
     * @param key chave do objeto
     * @return conteudo binario
     */
    byte[] downloadBytes(String key);

    /**
     * Gera uma URL pre-assinada (GET) para download temporario.
     *
     * @param key      chave do objeto
     * @param duration validade do link
     * @return URL pre-assinada
     */
    String presignedGetUrl(String key, Duration duration);
}
