package com.saas.libms.loan;

import com.saas.libms.common.ApiResponse;
import com.saas.libms.loan.dto.LoanCreateDTO;
import com.saas.libms.loan.dto.LoanResponseDTO;
import com.saas.libms.loan.dto.LoanReturnDTO;
import com.saas.libms.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    //Post a loan
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> createLoan(
            @Valid @RequestBody LoanCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
            ) {
        LoanResponseDTO responseDTO = loanService.createLoan(dto, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Loan creaatede successfully", responseDTO));
    }

    //Patch to return
    @PatchMapping("/{publicId}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> returnLoan(
            @PathVariable String publicId,
            @Valid @RequestBody(required = false) LoanReturnDTO loanReturnDTO,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        LoanResponseDTO responseDTO = loanService.returnLoan(publicId,loanReturnDTO, currentUser);

        String message = responseDTO.status() == LoanStatus.LATE ? "Book returned successfully. Note: this return was overdue." : "Book returned successfully";
        return ResponseEntity.ok(ApiResponse.success(message, responseDTO));
    }

    //Get All Loans
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<Page<LoanResponseDTO>>> getAllLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String memberPublicId,
            @RequestParam(required = false) String bookPublicId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Page<LoanResponseDTO> loans = loanService.getAllLoans(page,size,status,memberPublicId,bookPublicId,currentUser);

        return ResponseEntity.ok(ApiResponse.success("Loans fetched successfully",loans));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> getLoan(
            @PathVariable String publicId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        LoanResponseDTO responseDTO = loanService.getLoanByPublicId(publicId,currentUser);
        return ResponseEntity.ok(ApiResponse.success("Loan fetched successfully",responseDTO));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public  ResponseEntity<ApiResponse<Void>> archiveLoan(
            @PathVariable String publicId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        loanService.archiveLoan(publicId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Loan archived successfully",null));
    }

    @GetMapping("/{memberPublicId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<List<LoanResponseDTO>>> getActivateLoansByMember(
            @PathVariable String memberPublicId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        List<LoanResponseDTO> loans = loanService.getActiveLoansByMember(memberPublicId,currentUser);
        return ResponseEntity.ok(ApiResponse.success("Active loans fetched successfully", loans));
    }

}
