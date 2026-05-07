package com.saas.libms.bookauthor;

import com.saas.libms.author.Author;
import com.saas.libms.author.AuthorRepository;
import com.saas.libms.book.Book;
import com.saas.libms.book.BookRepository;
import com.saas.libms.bookauthor.dto.BookAuthorCreateDTO;
import com.saas.libms.bookauthor.dto.BookAuthorResponseDTO;
import com.saas.libms.exception.ConflictException;
import com.saas.libms.exception.ResourceNotFoundException;
import com.saas.libms.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookAuthorService {

    private final BookAuthorRepository bookAuthorRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

//Create
    @Transactional
    public BookAuthorResponseDTO createBookAuthor(BookAuthorCreateDTO dto,
                                                  CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        Book book = bookRepository.findByPublicIdAndInstitutionId(dto.bookPublicId(), institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Book Not Found :" + dto.bookPublicId()));

        Author author = authorRepository.findByPublicIdAndInstitutionId(dto.authorPublicId(), institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Author Not Found: " + dto.authorPublicId()));

        if(bookAuthorRepository.existsByBookIdAndAuthorId(book.getId(), author.getId())) {
            throw  new ConflictException("This author is already linked to this book");
        }

        BookAuthor bookAuthor = BookAuthor.builder()
                .book(book)
                .author(author)
                .build();

        BookAuthor saved = bookAuthorRepository.save(bookAuthor);

        List<BookAuthorProjection> projections = bookAuthorRepository.findByBookPublicId(dto.bookPublicId(), institutionId);

        return projections.stream()
                .filter(p-> p.getId().equals(saved.getId()))
                .map(BookAuthorResponseDTO::from)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Book Not Found After Save"));
        }

   //Get by author PublicId
    public List<BookAuthorResponseDTO> getByAuthorPublicId(String authorPublicId, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        authorRepository.findByPublicIdAndInstitutionId(authorPublicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Author not found: "+ authorPublicId));

        return bookAuthorRepository.findByAuthorPublicId(authorPublicId, institutionId)
                .stream()
                .map(BookAuthorResponseDTO::from)
                .toList();


    }

    //get by Book Public id
    public List<BookAuthorResponseDTO> getByBookPublicId(String bookPublicId,
                                                         CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        bookRepository.findByPublicIdAndInstitutionId(bookPublicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Book not Found: " + bookPublicId));

        return bookAuthorRepository
                .findByBookPublicId(bookPublicId, institutionId)
                .stream()
                .map(BookAuthorResponseDTO::from)
                .toList();

    }

    //Patch
    @Transactional
    public BookAuthorResponseDTO patchBookAuthor(UUID bookAuthorId,
                                                 BookAuthorCreateDTO dto,
                                                 CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        BookAuthor existing = bookAuthorRepository.findById(bookAuthorId)
                .orElseThrow(()-> new ResourceNotFoundException("BookAuthor record not found: " + bookAuthorId));

        Book newBook = bookRepository.findByPublicIdAndInstitutionId(dto.bookPublicId(), institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Book not found: " + dto.bookPublicId() ));

        Author newAuthor = authorRepository.findByPublicIdAndInstitutionId(dto.authorPublicId(),institutionId)
                .orElseThrow(()->new ResourceNotFoundException("Author not found: " + dto.authorPublicId()));

        boolean linkExists = bookAuthorRepository.existsByBookIdAndAuthorId(newBook.getId(), newAuthor.getId());

        boolean isSameRow = existing.getBook().getId().equals(newBook.getId()) && existing.getAuthor().getId().equals(newAuthor.getId());


        if (linkExists && !isSameRow) {
            throw  new ConflictException("This author is already linked to this book");
        }

        existing.setBook(newBook);
        existing.setAuthor(newAuthor);

        BookAuthor updated = bookAuthorRepository.save(existing);

        List<BookAuthorProjection> projections =
                bookAuthorRepository.findByBookPublicId(dto.bookPublicId(), institutionId);

        return projections.stream()
                .filter(p->p.getId().equals(updated.getId()))
                .map(BookAuthorResponseDTO::from)
                .findFirst()
                .orElseThrow(()-> new ResourceNotFoundException("BookAuthor not found after patch"));
    }

    @Transactional
    public void deleteBookAuthor(UUID bookAuthorId, CustomUserDetails currentUser) {

        UUID institutionId = currentUser.getUser().getInstitution().getId();

        BookAuthor existing = bookAuthorRepository.findById(bookAuthorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BookAuthor record not found: " + bookAuthorId));

        if (!existing.getBook().getInstitution().getId().equals(institutionId)) {
            throw new ResourceNotFoundException(
                    "BookAuthor record not found: " + bookAuthorId);
        }

        UUID authorInternalId = existing.getAuthor().getId();

        bookAuthorRepository.deleteByAuthorId(authorInternalId);
    }


}
