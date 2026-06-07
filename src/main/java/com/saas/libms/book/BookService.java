package com.saas.libms.book;

import com.saas.libms.audit.AuditAction;
import com.saas.libms.audit.AuditEntityType;
import com.saas.libms.audit.AuditLogService;
import com.saas.libms.audit.AuditMetadata;
import com.saas.libms.book.dto.BookCreateDTO;
import com.saas.libms.book.dto.BookResponseDTO;
import com.saas.libms.book.dto.BookUpdateDTO;
import com.saas.libms.category.Category;
import com.saas.libms.category.CategoryRepository;
import com.saas.libms.common.PublicIdGenerator;
import com.saas.libms.exception.ConflictException;
import com.saas.libms.exception.ResourceNotFoundException;
import com.saas.libms.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;
    public static final String BOOKS_CACHE = "books";
   // private final PublicIdGenerator publicIdGenerator;

    //Create Book
    @Transactional
//    @CachePut(
//            value = BOOKS_CACHE,
//            key = "#currentUser.user.institution.id + ':' + #result.publicId()"
//    )
    public BookResponseDTO createBook(BookCreateDTO dto, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        if (dto.isbn() != null && !dto.isbn().isBlank()
        && bookRepository.existsByIsbnAndInstitutionId(dto.isbn(), institutionId)) {
            throw new ConflictException("A book with this ISBN already exists in your institution");


        }

        String publicId = PublicIdGenerator.generate("BOOK");


        Category category = findCategoryByNameOrThrow(dto.categoryName());
        Book book = Book.builder()
                .publicId(publicId)
                .institution(currentUser.getUser().getInstitution())
                .title(dto.title())
                .isbn(dto.isbn())
                .publisher(dto.publisher())
                .publishedYear(dto.publishedYear())
                .copiesTotal(dto.copiesTotal())
                .copiesAvailable(dto.copiesTotal())
                .category(category)
                .build();

        auditLogService.log(
                currentUser,
                AuditAction.BOOK_CREATED,
                AuditEntityType.BOOK,
                book.getPublicId(),
                AuditMetadata.builder()
                        .put("title",     book.getTitle())
                        .put("isbn",      book.getIsbn())
                        .put("publisher", book.getPublisher())
                        .put("copies",    book.getCopiesTotal())
                        .build()
        );

        return BookResponseDTO.from(bookRepository.save(book));
    }

       //Pagination get all
    public Page<BookResponseDTO> getAllBooks(CustomUserDetails currentUSer, int page, int size) {
        UUID institutionId = currentUSer.getUser().getInstitution().getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return bookRepository.findAllByInstitutionId(institutionId,pageable)
                .map(BookResponseDTO::from);
    }

    //Get Books By Id
    @Cacheable(
            value = BOOKS_CACHE,
            key = "#currentUser.user.institution.id + ':' + #publicId"
    )
    public BookResponseDTO getBookByPubliId(String publicId, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        Book book = findBookOrThrow(publicId,institutionId);
        return BookResponseDTO.from(book);
    }

    //PAthcing, update
    @Transactional
    @CacheEvict(
            value = BOOKS_CACHE,
            key = "#currentUser.user.institution.id + ':' + #result.publicId()"
    )
    public BookResponseDTO updateBook(String publicId, BookUpdateDTO dto, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();
        Book book = findBookOrThrow(publicId, institutionId);
        Category category = findCategoryByNameOrThrow(dto.categoryName());

        if (dto.isbn() != null && !dto.isbn().isBlank()
        && !dto.isbn().equals(book.getIsbn())
        && bookRepository.existsByIsbnAndInstitutionId(dto.isbn(), institutionId)) {
            throw new ConflictException("A book with this ISBN already exists in your institution");
        }

        if (dto.title() != null && !dto.title().isBlank()) {
            book.setTitle(dto.title());
        }

        if (dto.isbn() != null) {
            book.setIsbn(dto.isbn());
        }

        if (dto.publisher() != null ) {
            book.setPublisher(dto.publisher());
        }

        if (dto.publishedYear() != null) {
            book.setPublishedYear(dto.publishedYear());
        }

        if(dto.categoryName() != null) {
            book.setCategory(category);
        }
        if(dto.copiesTotal() != null) {
           // int diff = dto.copiesTotal() - book.getCopiesTotal();
            book.setCopiesTotal(Math.max(0,dto.copiesTotal()));

        }

        auditLogService.log(
                currentUser,
                AuditAction.BOOK_UPDATED,
                AuditEntityType.BOOK,
                book.getPublicId(),
                AuditMetadata.builder()
                        .put("title", book.getTitle())
                        .build()
        );

        return BookResponseDTO.from(bookRepository.save(book));
    }

    @Transactional
    @CacheEvict(
            value = BOOKS_CACHE,
            key = "#currentUser.user.institution.id + ':' + #publicId"
    )
    public void deleteBook(String publicId, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();
        Book book = findBookOrThrow(publicId, institutionId);
        bookRepository.delete(book);

        auditLogService.log(
                currentUser,
                AuditAction.BOOK_DELETED,
                AuditEntityType.BOOK,
                publicId,
                AuditMetadata.builder()
                        .put("title", book.getTitle())
                        .build()
        );
    }

    //helpers
    private Book findBookOrThrow(String publicId, UUID institutionId) {
        return bookRepository.findByPublicIdAndInstitutionId(publicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Book not Found"));
    }

    private Category findCategoryOrThrow(String categoryPublicId) {
        return categoryRepository.findByPublicId(categoryPublicId)
                .orElseThrow(()-> new ResourceNotFoundException("Category Does Not Exist"));
    }

    private Category findCategoryByNameOrThrow(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseThrow(()-> new ResourceNotFoundException("Category Does Not Exist!"));
    }

}
