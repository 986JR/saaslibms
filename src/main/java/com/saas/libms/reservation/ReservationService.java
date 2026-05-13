package com.saas.libms.reservation;

import com.saas.libms.book.Book;
import com.saas.libms.book.BookRepository;
import com.saas.libms.common.PublicIdGenerator;
import com.saas.libms.exception.BadRequestException;
import com.saas.libms.exception.ConflictException;
import com.saas.libms.exception.ResourceNotFoundException;
import com.saas.libms.institution.InstitutionRepository;
import com.saas.libms.loan.LoanRepository;
import com.saas.libms.loan.LoanStatus;
import com.saas.libms.member.Member;
import com.saas.libms.member.MemberRepository;
import com.saas.libms.member.MemberStatus;
import com.saas.libms.member.dto.MemberResponseDTO;
import com.saas.libms.reservation.dto.ReservationCreateDTO;
import com.saas.libms.reservation.dto.ReservationResponseDTO;
import com.saas.libms.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {
    private static final int RESERVATION_LIMIT = 3;

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final InstitutionRepository institutionRepository;
    private final LoanRepository loanRepository;

    //create reservation
    @Transactional
    public ReservationResponseDTO createReservation(
            ReservationCreateDTO dto, CustomUserDetails currentUser
    ) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        //find member
        Member member = memberRepository.findByPublicIdAndInstitutionId(dto.memberPublicId(), institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not Found. Ensure the member is registered in your institution"));

        //check member status, Active
        if (member.getStatus() == MemberStatus.BLOCKED) {
            throw new BadRequestException(
                    "Member is currently Blocked and Can not make a reservation. " +
                            "Contact the Institution Administrator to unblock member " + member.getName()
            );
        }

        //validate book that is found in the institution
        Book book = bookRepository.findByPublicIdAndInstitutionId(dto.bookPublicId(), institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found in your institution"));

        //ensure books are out of copies
        if (book.getCopiesAvailable() > 0) {
            throw new BadRequestException("This Book current has " + book.getCopiesAvailable() + " copies" +
                    " Available Please issue a loan, if copies are Not enough...:( report this bug to Joshua,the dev"
            );

        }

        //check member does not already have an active reservation
        //pending and fulfilled counts
        if (reservationRepository.existsActiveReservation(member.getId(), book.getId())) {
            throw new ConflictException(
                    "Member already has an active reservation for this book. " +
                            "Duplicate reservations for the same book are not allowed."
            ); }

            //Members with LATE loans can not reserve books

            long lateLoans = loanRepository.countByMemberIdAndStatusAndArchivedFalse(member.getId(), LoanStatus.LATE);
            if (lateLoans > 0) {
                throw new BadRequestException(
                        "Member " + member.getName() + "has " + lateLoans + " overdue loan(s). " +
                                "All overdue books must be returned before a new reservation can be made."
                );

            }

            //Enforce reservation limit per member
            int activeReservations = reservationRepository.countActiveReservationsByMember(member.getId());
            if (activeReservations >= RESERVATION_LIMIT) {
                throw new BadRequestException(
                        "Member has reached the maximum of " + RESERVATION_LIMIT + " active reservations. " +
                                "Cancel or wait for an existing reservation to be fulfilled before adding a new one."
                );
            }

            //calc queue position
            int queuePosition = reservationRepository.countPendingReservationsForBook(book.getId()) + 1;

            //build and save the reservation
            Reservation reservation = Reservation.builder()
                    .publicId(PublicIdGenerator.generate("RESERV"))
                    .institution(currentUser.getUser().getInstitution())
                    .book(book)
                    .member(member)
                    .reservationDate(LocalDate.now())
                    .status(ReservationStatus.PENDING)
                    .queuePosition(queuePosition)
                    .build();
            reservation = reservationRepository.save(reservation);
            log.info("Reservation {} created for member {} on book {} at queue position {}",
                    reservation.getPublicId(), member.getPublicId(), book.getPublicId(), queuePosition);

            return ReservationResponseDTO.from(reservation);

        }


        //cancel reservation
    @Transactional
    public ReservationResponseDTO cancelReservation(String publicId, String reason, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        //find the reservation
        Reservation reservation = reservationRepository.findByPublicIdAndInstitutionId(publicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Reservation not found."));

        //only Pending and fullfiled reservations can be canceld
        if(reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Reservation is already canceled.");
        }

        if(reservation.getStatus() ==ReservationStatus.EXPIRED) {
            throw new BadRequestException("Reservation has already expired and cannot be canceled");
        }

        //if we cancel the fulfilled reservation, the copy goes back availble
        if(reservation.getStatus() == ReservationStatus.FULFILLED) {
            Book book = reservation.getBook();
            int restored = Math.min(book.getCopiesAvailable()+1, book.getCopiesTotal());

            book.setCopiesAvailable(restored);
            bookRepository.save(book);
            log.info("REservation {} was FULFILLED , restored 1 copy to book {}", publicId, book.getPublicId());

        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelReason(reason);
        reservation.setArchived(true);
        reservation.setUpdatedAt(LocalDateTime.now());

        reservation = reservationRepository.save(reservation);
        log.info("Reservation {} cancelled. Reason: {}", publicId, reason);

        return ReservationResponseDTO.from(reservation);
    }

//Get all reservations Pagenated
@Transactional(readOnly = true)
public Page<ReservationResponseDTO> getAllReservations(
        UUID institutionId,
        String statusStr,
        String memberPublicId,
        String bookPublicId,
        int page,
        int size
) {
    ReservationStatus status = null;
    if (statusStr != null && !statusStr.isBlank()) {
        try {
            status = ReservationStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid reservation status: '" + statusStr + "'. " +
                            "Valid values are: PENDING, FULFILLED, CANCELLED, EXPIRED");
        }
    }

    Pageable pageable = PageRequest.of(page, size);
    Page<Reservation> reservationPage = reservationRepository.findAllByInstitutionId(
            institutionId,
            status,
            memberPublicId != null && !memberPublicId.isBlank() ? memberPublicId : null,
            bookPublicId != null && !bookPublicId.isBlank() ? bookPublicId : null,
            pageable
    );

    return reservationPage.map(ReservationResponseDTO::from);
}

//get by publicId
@Transactional(readOnly = true)
public ReservationResponseDTO getReservation(String publicId, UUID institutionId) {
    Reservation reservation = reservationRepository
            .findByPublicIdAndInstitutionId(publicId, institutionId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Reservation not found: " + publicId));
    return ReservationResponseDTO.from(reservation);
}

    }


