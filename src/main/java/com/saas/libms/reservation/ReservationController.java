package com.saas.libms.reservation;

import com.saas.libms.common.ApiResponse;
import com.saas.libms.reservation.dto.ReservationCancelDTO;
import com.saas.libms.reservation.dto.ReservationCreateDTO;
import com.saas.libms.reservation.dto.ReservationResponseDTO;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> createReservation(
            @Valid @RequestBody ReservationCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUSer
            ) {
        ReservationResponseDTO responseDTO = reservationService.createReservation(dto,currentUSer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("REservation created successfully. Member placed at queue position " +
                        " "+ responseDTO.queuePosition() +".", responseDTO));
    }

    @PatchMapping("/{publicId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> cancelReservation(
            @PathVariable String publicId,
            @Valid @RequestBody ReservationCancelDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        ReservationResponseDTO responseDTO = reservationService.cancelReservation(publicId,dto.reason(),currentUser);
        return ResponseEntity.ok(ApiResponse.success("Reservation cancelled successfully.",responseDTO));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<Page<ReservationResponseDTO>>> getAllReservations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String memberPublicId,
            @RequestParam(required = false) String bookPublicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID institutionId = userDetails.getUser().getInstitution().getId();
        Page<ReservationResponseDTO> result = reservationService.getAllReservations(
                institutionId, status, memberPublicId, bookPublicId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Reservations fetched successfully.", result));
    }

    // ─── GET /api/v1/reservations/{publicId} ─────────────────────────────────
    // Get a single reservation by public ID

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> getReservation(
            @PathVariable String publicId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID institutionId = userDetails.getUser().getInstitution().getId();
        ReservationResponseDTO response = reservationService.getReservation(publicId, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Reservation fetched successfully.", response));
    }
}
