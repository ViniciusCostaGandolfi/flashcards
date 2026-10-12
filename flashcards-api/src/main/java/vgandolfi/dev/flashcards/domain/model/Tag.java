package vgandolfi.dev.flashcards.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tag reutilizavel aplicavel a um ou mais cards ({@code Card.tags}).
 *
 * <p>O nome e exibido na forma informada ({@code name}), mas a unicidade e a
 * busca sao case-insensitive: a coluna {@code name_normalized} guarda a versao
 * minuscula/trim e tem constraint UNIQUE. A normalizacao e feita pelo
 * {@code TagService} (unico ponto de criacao de tags).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_tags_name_normalized", columnNames = "name_normalized"))
public class Tag extends BaseEntity {

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 60)
    private String nameNormalized;
}
