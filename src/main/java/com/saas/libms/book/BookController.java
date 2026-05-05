package com.saas.libms.book;

import com.saas.libms.book.dto.BookCreateDTO;
import com.saas.libms.book.dto.BookResponseDTO;
import com.saas.libms.book.dto.BookUpdateDTO;
import com.saas.libms.common.ApiResponse;
import com.saas.libms.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN,LBRARIAN')")
    public ResponseEntity<ApiResponse<BookResponseDTO>> createBook(
            @Valid @RequestBody BookCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails curretUser
            ) {
        BookResponseDTO responseDTO = bookService.createBook(dto,curretUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Book added successfully", responseDTO));

    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN,LIBRARIAN')")
    public ResponseEntity<ApiResponse<Page<BookResponseDTO>>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails currentUSer

    ) {
        Page<BookResponseDTO> books = bookService.getAllBooks(currentUSer, page, size);
        return ResponseEntity.ok(ApiResponse.success("Books fetched successfully", books));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN', LIBRARIAN)")
    public ResponseEntity<ApiResponse<BookResponseDTO>> getBook(
            @PathVariable String publicId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        BookResponseDTO bookResponseDTO = bookService.getBookByPubliId(publicId,currentUser);
        return ResponseEntity.ok(ApiResponse.success("Book fetched successfully",bookResponseDTO));

    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN,LIBRARIAN')")
    public ResponseEntity<ApiResponse<BookResponseDTO>> updateBook(
            @PathVariable String publicId,
            @Valid @RequestBody BookUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
            ) {
        BookResponseDTO updated = bookService.updateBook(publicId,dto,currentUser);
        return ResponseEntity.ok(ApiResponse.success("Book updated Successfully", updated));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN,LIBRARIAN)")
    public ResponseEntity<ApiResponse<Void>> deleteBook(
            @PathVariable String publicId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        bookService.deleteBook(publicId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Book deleted Successfully", null));
    }

}
