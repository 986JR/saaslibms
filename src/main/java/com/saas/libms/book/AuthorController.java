package com.saas.libms.book;

import com.saas.libms.book.dto.AuthorCreateDTO;
import com.saas.libms.book.dto.AuthorResponseDTO;
import com.saas.libms.book.dto.AuthorUpdateDTO;
import com.saas.libms.common.ApiResponse;
import com.saas.libms.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    //Post Author
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<AuthorResponseDTO>> createAuthor(
            @Valid @RequestBody AuthorCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
            ) {
        AuthorResponseDTO responseDTO = authorService.createAuthor(dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Author created successfully", responseDTO));
    }

    //get All
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<Page<AuthorResponseDTO>>> getAllAuthors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Page<AuthorResponseDTO> authors = authorService.getAllAuthors(page, size,currentUser);
        return ResponseEntity.ok(ApiResponse.success("Authors fetched successfully", authors));
    }

    //Get ById
    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<AuthorResponseDTO>> getAuthorByPublicId(
            @PathVariable String publicId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        AuthorResponseDTO authorResponseDTO = authorService.getAuthorByPublicId(publicId,currentUser);
        return ResponseEntity.ok(ApiResponse.success("Author fetched successfully", authorResponseDTO));
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<AuthorResponseDTO>> updateAuthor(
            @PathVariable String publicId,
            @RequestBody AuthorUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
            ) {
        AuthorResponseDTO updated = authorService.updateAuthor(publicId, dto, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Author updated successfully", updated));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<Void>> deleteAuthor(
            @PathVariable String publicId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        authorService.deleteAuthor(publicId,currentUser);
        return ResponseEntity.ok(ApiResponse.success("Author disabled successfully",null ));
    }

}
