package com.saas.libms.reservation.dto;

import com.saas.libms.reservation.Reservation;
import com.saas.libms.reservation.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponseDTO(
        String publicId,
        String bookPublicId,
        String bookTitle,
        String memberPublicId,
        String memberName,
        String institutionId,
        LocalDate reservationDate,
        ReservationStatus status,
        Integer queuePosition,
        LocalDateTime fulfilledAt,
        LocalDateTime expiresAt,
        LocalDateTime reservedUntil,
        String cancelReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean archived
) {
    public static ReservationResponseDTO from(Reservation r) {
        return new ReservationResponseDTO(
                r.getPublicId(),
                r.getBook().getPublicId(),
                r.getBook().getTitle(),
                r.getMember().getPublicId(),
                r.getMember().getName(),
                r.getInstitution().getPublicId(),
                r.getReservationDate(),
                r.getStatus(),
                r.getQueuePosition(),
                r.getFulfilledAt(),
                r.getExpiresAt(),
                r.getReservedUntil(),
                r.getCancelReason(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.isArchived()
        );
}
}
