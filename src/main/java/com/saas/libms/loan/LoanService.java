package com.saas.libms.loan;

import com.saas.libms.book.Book;
import com.saas.libms.book.BookRepository;
import com.saas.libms.common.PublicIdGenerator;
import com.saas.libms.exception.BadRequestException;
import com.saas.libms.exception.ResourceNotFoundException;
import com.saas.libms.loan.dto.LoanCreateDTO;
import com.saas.libms.loan.dto.LoanResponseDTO;
import com.saas.libms.loan.dto.LoanReturnDTO;
import com.saas.libms.member.Member;
import com.saas.libms.member.MemberRepository;
import com.saas.libms.member.MemberStatus;
import com.saas.libms.reservation.Reservation;
import com.saas.libms.reservation.ReservationRepository;
import com.saas.libms.reservation.ReservationStatus;
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
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {
    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;

    //Standard loan overdue days
    private static final int LOAN_PERIOD_DAYS = 14;

    //maximum copies a member can borrow in one transaction
    private static final int MAX_QUANTITY_PER_LOAN = 5;

    //max active loans
    private static final int MAX_ACTIVE_LOANS_PER_MEMBER = 10;

    //Create a loan
    @Transactional
    public LoanResponseDTO createLoan(LoanCreateDTO dto, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        int quantity = dto.quantity() != null ? dto.quantity() : 1;

        //member should belong to this institution
        Member member = memberRepository.findByPublicIdAndInstitutionId(
                dto.memberPublicId(), institutionId
        ).orElseThrow(()-> new ResourceNotFoundException("Member not found."));

        //Blocked Member should not be allowed
        if(member.getStatus() == MemberStatus.BLOCKED) {
            throw new BadRequestException(
                    "Member is Blocked and cannot Borrow books. " +
                            "Contact Institution Administrator to unblock " +
                            "Member "+member.getPublicId()
            );
        }

        //loan limit check
        long activeLoanCount = loanRepository.countByMemberIdAndStatusAndArchivedFalse(member.getId(),LoanStatus.BORROWED);

        //count late loans
        long lateLoanCount = loanRepository.countByMemberIdAndStatusAndArchivedFalse(member.getId(),LoanStatus.LATE);

        if (activeLoanCount + lateLoanCount >= MAX_ACTIVE_LOANS_PER_MEMBER) {
            throw new BadRequestException(
                    "Member has reached the maximum  active loan limit of " +
                            " "+ MAX_ACTIVE_LOANS_PER_MEMBER + ", They must turn some books before borrowing more."
            );
        }

        Book book = bookRepository.findByPublicIdAndInstitutionId(dto.bookPublicId(), institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Book not Found. Make sure the book belongs to your institution"));

        //Quantity per single transaction
        if(quantity > MAX_QUANTITY_PER_LOAN) {
            throw new BadRequestException(
                    "Can not Borrow more than "+ MAX_QUANTITY_PER_LOAN + " copies in a single loan"
            );
        }

        boolean memberHasActiveReservation = reservationRepository.hasPendingOrFulfilledReservation(member.getId(), book.getId());

        //Track whether we are fulfilling a reservation so we can skip the
        // copiesAvailable check (the scheduler already decremented it) and
        // close the reservation record after the loan is saved.
        boolean isFulfillingReservation = false;
        Reservation reservationToClose = null;

        if(memberHasActiveReservation) {
            Reservation activeReservation = reservationRepository.findActiveReservationForMemberAndBook(member.getId(), book.getId())
                    .orElseThrow(()-> new ResourceNotFoundException("Reservation record not found. This is an unexpected state please retry"));
            if (activeReservation.getStatus() == ReservationStatus.FULFILLED) {
                log.info("Loan creation: member '{}' is collectiing their FULFILLED  reservation '{}' for book '{}'.", member.getPublicId(),activeReservation.getPublicId(), book.getTitle());
                isFulfillingReservation=true;
                reservationToClose =activeReservation;
            }
            else {
                throw new BadRequestException(
                        "Member "+ member.getName() + " already has a PENDING reservation " +
                                "(Queue Position " + activeReservation.getQueuePosition() + ") " +
                                "for this book (reservation: " + activeReservation.getPublicId() + "). " +
                                "They must wait for the system to fulfill their reservation before a loan can be issued."
                );
            }
        } else {
            long pendingReservationCount = reservationRepository.countPendingReservationsForBook(book.getId());
            if (pendingReservationCount > 0) {
                throw new BadRequestException(
                        "This book has " + pendingReservationCount +
                                "member(s) waiting in the reservation queue. " +
                                "The available copy(s) is reserved for the next member in line. " +
                                "Please ask this member to make a reservation, or wait until " +
                                "the reservation queue is cleared."
                );
            }

            if(book.getCopiesAvailable() < quantity) {
                throw new BadRequestException(
                        "not enough copies available. Requested: " + quantity + ", " +
                                "Available: " + book.getCopiesAvailable() + "."
                );
            }
        }

        //issue the loan
        if (!isFulfillingReservation) {
            book.setCopiesAvailable(book.getCopiesAvailable()-quantity);
            bookRepository.save(book);
        }

        Loan loan = Loan.builder()
                .publicId(PublicIdGenerator.generate("LOAN"))
                .institution(currentUser.getUser().getInstitution())
                .book(book)
                .member(member)
                .borrowDate(LocalDateTime.now())
                .dueDate(LocalDate.now().plusWeeks(2)) // standard 2-week due date
                .status(LoanStatus.BORROWED)
                .quantity(quantity)
                .archived(false)
                .build();

        loanRepository.save(loan);

        if (isFulfillingReservation && reservationToClose != null) {
            reservationToClose.setStatus(ReservationStatus.CANCELLED);
            reservationToClose.setCancelReason("Collected — loan issued (" + loan.getPublicId() + ")");
            reservationToClose.setArchived(true);
            reservationToClose.setUpdatedAt(LocalDateTime.now());
            reservationRepository.save(reservationToClose);

            log.info("Reservation '{}' closed as COLLECTED — linked to loan '{}'.",
                    reservationToClose.getPublicId(), loan.getPublicId());
        }

        return LoanResponseDTO.from(loan);


    }

    //Return a loan
    @Transactional
    public LoanResponseDTO returnLoan(String publicId, LoanReturnDTO dto, CustomUserDetails currentUser)
    {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        //Find a loan
        Loan loan = loanRepository.findByPublicIdInstitutionId(publicId, institutionId)
                .orElseThrow(()-> new BadRequestException("Loan not found in your institution"));

        //Can not return returned loan
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BadRequestException("This loan has already been returned");
        }

        //determine number of copies being retuned
        int returnQty = dto != null && dto.quantity() != null ? dto.quantity() : loan.getQuantity();

        if (returnQty > loan.getQuantity()) {
            throw new BadRequestException(
                    "Return quantity (" + returnQty + ") cannot exceed the borrowed quantity" +
                            "(" + loan.getQuantity() + ")."
            );
        }

        //Restore Copies to the book
        Book book = loan.getBook();
        int newAvailable = Math.min(book.getCopiesAvailable() + returnQty, book.getCopiesTotal());

        book.setCopiesAvailable(newAvailable);
        bookRepository.save(book);

        //determine if return is late
        LocalDate today = LocalDate.now();
        LoanStatus newStatus = today.isAfter(loan.getDueDate()) ? LoanStatus.LATE : LoanStatus.RETURNED;

        //upadate loan fields
        loan.setReturnDate(today);
        loan.setStatus(newStatus);
        loanRepository.save(loan);

        //Warn if books did not complete
        return  LoanResponseDTO.from(loan);
    }

    //Get one loan
    public LoanResponseDTO getLoanByPublicId(String publicId, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        Loan loan = loanRepository.findByPublicIdInstitutionId(publicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Loan no found in your institution"));

        return  LoanResponseDTO.from(loan);
    }

    //Get all loans paginated
    public Page<LoanResponseDTO> getAllLoans(
            int page, int size,
            String statusStr,
            String memberPublicId,
            String bookPublicId,
            CustomUserDetails currentUser
    ) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        LoanStatus status = null;
        if(statusStr != null && !statusStr.isBlank()) {
            try {
                status = LoanStatus.valueOf(statusStr.toUpperCase());

            } catch (IllegalArgumentException e) {
                throw new BadRequestException(
                        "Invalid status value: '" + statusStr
                );
            }
        }

        Pageable pageable = PageRequest.of(page, size);

        return loanRepository
                .findAllByInstitutionId(institutionId, status,
                        memberPublicId != null && memberPublicId.isBlank() ? null : memberPublicId,
                        bookPublicId != null && bookPublicId.isBlank() ? null :bookPublicId,
        pageable).map(LoanResponseDTO:: from);
    }

    //deleting softly
    @Transactional
    public void archiveLoan(String publicId, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        Loan loan = loanRepository.findByPublicIdInstitutionId(publicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Loan not Found in your institution"));
        //not archive a loan that still has unreturned books
        if(loan.getStatus() == LoanStatus.BORROWED || loan.getStatus() == LoanStatus.LATE) {
            throw new BadRequestException(
                    "Can not archive a loan that not been returned yet. " +
                            "Precess the return first "
            );
        }

        loan.setArchived(true);
        loanRepository.save(loan);

    }
    //get members with active loans in the institution
    public List<LoanResponseDTO> getActiveLoansByMember(String memberPublicId, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        Member member = memberRepository.findByPublicIdAndInstitutionId(memberPublicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Member not found in your institution"));

        return loanRepository.findActiveByMemberAndInstitution(member.getId(), institutionId)
                .stream()
                .map(LoanResponseDTO::from)
                .toList();
    }


}
