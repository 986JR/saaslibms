package com.saas.libms.category;

import com.saas.libms.category.dto.CategoryCreateDTO;
import com.saas.libms.category.dto.CategoryResponseDTO;
import com.saas.libms.category.dto.CategoryUpdateDTO;
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
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> createCategory(
            @Valid @RequestBody CategoryCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUSer
            ) {
        CategoryResponseDTO data = categoryService.createCategory(dto,currentUSer);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully",data));
    }

    //get
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<Page<CategoryResponseDTO>>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser
    )
    {
        Page<CategoryResponseDTO> data = categoryService.getAllCategories(page,size,currentUser);

        return ResponseEntity.ok(ApiResponse.success("Categories fetched successfully", data));
    }

    @GetMapping("/by-name")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getCategoryByName(
            @RequestParam String name,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        CategoryResponseDTO data = categoryService.getCategoryByName(name,currentUser);

        return ResponseEntity.ok(ApiResponse.success("Category fetched Successfully", data));
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> updateCategory(
            @PathVariable String publicId,
            @Valid @RequestBody CategoryUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
            ) {
        CategoryResponseDTO responseDTO = categoryService.updateCategory(publicId,dto,currentUser);

        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", responseDTO));

    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable String publicId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {

        categoryService.deleteCategory(publicId,currentUser);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully. Associated Books have been uncategorised",null));

    }

}
