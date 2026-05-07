package com.saas.libms.bookauthor;
import com.saas.libms.bookauthor.dto.BookAuthorCreateDTO;
import com.saas.libms.bookauthor.dto.BookAuthorResponseDTO;
import com.saas.libms.common.ApiResponse;
import com.saas.libms.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/book-authors")
@RequiredArgsConstructor
public class BookAuthorController {

    private final BookAuthorService bookAuthorService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<BookAuthorResponseDTO>> create(
            @Valid @RequestBody BookAuthorCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        BookAuthorResponseDTO result = bookAuthorService.createBookAuthor(dto, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Book-author link created successfully", result));
    }


    @GetMapping("/by-author/{authorPublicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<List<BookAuthorResponseDTO>>> getByAuthor(
            @PathVariable String authorPublicId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<BookAuthorResponseDTO> results =
                bookAuthorService.getByAuthorPublicId(authorPublicId, currentUser);

        return ResponseEntity.ok(
                ApiResponse.success("Book-author entries fetched successfully", results));
    }

    @GetMapping("/by-book/{bookPublicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<List<BookAuthorResponseDTO>>> getByBook(
            @PathVariable String bookPublicId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<BookAuthorResponseDTO> results =
                bookAuthorService.getByBookPublicId(bookPublicId, currentUser);

        return ResponseEntity.ok(
                ApiResponse.success("Book-author entries fetched successfully", results));
    }


    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<BookAuthorResponseDTO>> patch(
            @PathVariable UUID id,
            @Valid @RequestBody BookAuthorCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        BookAuthorResponseDTO result = bookAuthorService.patchBookAuthor(id, dto, currentUser);

        return ResponseEntity.ok(
                ApiResponse.success("Book-author link updated successfully", result));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        bookAuthorService.deleteBookAuthor(id, currentUser);

        return ResponseEntity.ok(
                ApiResponse.success("Book-author link deleted successfully", null));
    }
}

