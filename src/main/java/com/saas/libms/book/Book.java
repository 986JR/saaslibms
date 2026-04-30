package com.saas.libms.book;

import com.saas.libms.institution.Institution;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.net.Inet4Address;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "books",
        indexes = {
                @Index(name = "idx_books_institution", columnList = "institution_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_id", unique = true, nullable = false, length = 20)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false)
    private String title;

    private String isbn;
    private String publisher;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(name = "copies_total", nullable = false)
    private int copiesTotal;

    @Column(name = "copies_available", nullable = false)
    private int copiesAvailable;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;



}
