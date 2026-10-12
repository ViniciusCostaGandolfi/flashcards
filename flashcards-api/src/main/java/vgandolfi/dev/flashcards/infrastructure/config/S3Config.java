package vgandolfi.dev.flashcards.infrastructure.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import vgandolfi.dev.flashcards.infrastructure.config.properties.S3Properties;

/**
 * Cliente S3/MinIO (AWS SDK v2) com path-style addressing (compativel com
 * MinIO) e criacao idempotente do bucket. Tambem expoe o {@link S3Presigner}
 * usado para gerar URLs pre-assinadas de download.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class S3Config {

    private final S3Properties s3Properties;

    @Bean
    public S3Client s3Client() {
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(endpoint()))
                .credentialsProvider(credentials())
                .region(Region.of(region()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
        ensureBucketExists(client);
        return client;
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint()))
                .credentialsProvider(credentials())
                .region(Region.of(region()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    /**
     * Endpoint validado: falha rapido com mensagem clara quando a variavel nao
     * foi definida (o placeholder nao resolvido chega como "${S3_ENDPOINT}" e
     * estouraria um {@code URISyntaxException} críptico em {@code URI.create}).
     */
    private String endpoint() {
        String endpoint = s3Properties.endpoint();
        if (!StringUtils.hasText(endpoint) || endpoint.contains("${")) {
            throw new IllegalStateException(
                    "Bucket S3 nao configurado. Defina a variavel de ambiente "
                    + "S3_ENDPOINT (ex.: https://s3.exemplo.com). Valor atual: '" + endpoint + "'");
        }
        return endpoint;
    }

    private String region() {
        String region = s3Properties.region();
        return (StringUtils.hasText(region) && !region.contains("${")) ? region : "us-east-1";
    }

    private StaticCredentialsProvider credentials() {
        String accessKey = s3Properties.accessKey();
        String secretKey = s3Properties.secretKey();
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)
                || accessKey.contains("${") || secretKey.contains("${")) {
            throw new IllegalStateException(
                    "Credenciais do bucket nao configuradas. Defina S3_ACCESS_KEY e S3_SECRET_KEY.");
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    /**
     * Cria o bucket se nao existir (idempotente). Compatível com MinIO, Garage
     * e AWS S3: usa headBucket e cria apenas quando retorna 404. Falhas de
     * rede nao derrubam o boot — o upload falhara com mensagem clara.
     */
    private void ensureBucketExists(S3Client client) {
        String bucket = s3Properties.bucket();
        if (!StringUtils.hasText(bucket)) {
            return;
        }
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("s3_bucket_exists bucket={}", bucket);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                try {
                    client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                    log.info("s3_bucket_created bucket={}", bucket);
                } catch (S3Exception createEx) {
                    if (createEx.statusCode() == 409 || createEx.statusCode() == 400) {
                        log.info("s3_bucket_already_exists bucket={}", bucket);
                    } else {
                        log.error("s3_bucket_create_failed bucket={} status={}",
                                bucket, createEx.statusCode());
                        throw createEx;
                    }
                }
            } else {
                log.warn("s3_bucket_head_failed bucket={} status={}", bucket, ex.statusCode());
            }
        } catch (SdkClientException ex) {
            log.warn("s3_bucket_head_unreachable bucket={} error={}", bucket, ex.getMessage());
        }
    }
}
