package vgandolfi.dev.flashcards.infrastructure.s3;

import java.time.Duration;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import vgandolfi.dev.flashcards.infrastructure.config.properties.S3Properties;

/**
 * Implementacao de {@link S3Storage} sobre o cliente AWS SDK v2 (compativel
 * com MinIO via path-style addressing).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageImpl implements S3Storage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public String uploadBytes(String key, byte[] bytes, String contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Properties.bucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (S3Exception ex) {
            log.error("s3_upload_failed key={} bucket={} status={}",
                    key, s3Properties.bucket(), ex.statusCode());
            throw ex;
        }
        log.info("s3_uploaded key={} bucket={} size={}", key, s3Properties.bucket(), bytes.length);
        return key;
    }

    @Override
    public byte[] downloadBytes(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .build();
            try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(request)) {
                return stream.readAllBytes();
            } catch (java.io.IOException ex) {
                throw new IllegalStateException("Falha ao ler objeto do S3: " + key, ex);
            }
        } catch (S3Exception ex) {
            log.error("s3_download_failed key={} bucket={} status={}",
                    key, s3Properties.bucket(), ex.statusCode());
            throw ex;
        }
    }

    @Override
    public String presignedGetUrl(String key, Duration duration) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(s3Properties.bucket())
                        .key(key)
                        .build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
