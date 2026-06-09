package com.saas.libms.analytics;

import com.saas.libms.book.Book;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookViewEventService {

    private final BookViewEventRepository bookViewEventRepository;

    /**
     * Record that a book was viewed.
     * Called from BookService — fires and forgets on a background thread.
     *
     * @param book          the book entity that was fetched
     */
    @Async
    @Transactional
    public void record(Book book) {
        try {
            BookViewEvent event = BookViewEvent.builder()
                    .bookId(book.getId())
                    .bookPublicId(book.getPublicId())
                    .bookTitle(book.getTitle())
                    .institutionId(book.getInstitution().getId())
                    .build();

            bookViewEventRepository.save(event);
        } catch (Exception e) {
            // Never let analytics failure affect the main request
            log.warn("Failed to record book view event for book {}: {}",
                    book.getPublicId(), e.getMessage());
        }
    }
}
