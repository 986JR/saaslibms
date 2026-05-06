package com.saas.libms.book;

import com.saas.libms.book.dto.BookCreateDTO;
import com.saas.libms.book.dto.BookResponseDTO;
import com.saas.libms.book.dto.BookUpdateDTO;
import com.saas.libms.common.PublicIdGenerator;
import com.saas.libms.exception.ConflictException;
import com.saas.libms.exception.ResourceNotFoundException;
import com.saas.libms.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
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
   // private final PublicIdGenerator publicIdGenerator;

    //Create Book
    @Transactional
    public BookResponseDTO createBook(BookCreateDTO dto, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        if (dto.isbn() != null && !dto.isbn().isBlank()
        && bookRepository.existsByIsbnAndInstitutionId(dto.isbn(), institutionId)) {
            throw new ConflictException("A book with this ISBN already exists in your institution");


        }

        String publicId = PublicIdGenerator.generate("BOOK");

        Book book = Book.builder()
                .publicId(publicId)
                .institution(currentUser.getUser().getInstitution())
                .title(dto.title())
                .isbn(dto.isbn())
                .publisher(dto.publisher())
                .publishedYear(dto.publishedYear())
                .copiesTotal(dto.copiesTotal())
                .copiesAvailable(dto.copiesTotal())
                .build();

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
    public BookResponseDTO getBookByPubliId(String publicId, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        Book book = findBookOrThrow(publicId,institutionId);
        return BookResponseDTO.from(book);
    }

    //PAthcing, update
    @Transactional
    public BookResponseDTO updateBook(String publicId, BookUpdateDTO dto, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();
        Book book = findBookOrThrow(publicId, institutionId);

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
        if(dto.copiesTotal() != null) {
            int diff = dto.copiesTotal() - book.getCopiesTotal();
            book.setCopiesAvailable(Math.max(0,book.getCopiesAvailable() + diff));

        }

        return BookResponseDTO.from(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(String publicId, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();
        Book book = findBookOrThrow(publicId, institutionId);
        bookRepository.delete(book);
    }

    //helpers
    private Book findBookOrThrow(String publicId, UUID institutionId) {
        return bookRepository.findByPublicIdAndInstitutionId(publicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Book not Found"));
    }

}
