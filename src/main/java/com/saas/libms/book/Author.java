package com.saas.libms.book;

import com.saas.libms.institution.Institution;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "authors",indexes = {
@Index(name = "idx_authors_institution", columnList = "institution_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_id", unique = true, nullable = false)
    private String publicId;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_status")
    private AuthorStatus status = AuthorStatus.ACTIVE;

}
