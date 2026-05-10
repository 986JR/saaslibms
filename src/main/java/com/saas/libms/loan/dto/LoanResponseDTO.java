package com.saas.libms.loan.dto;

import com.saas.libms.loan.Loan;
import com.saas.libms.loan.LoanStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoanResponseDTO(
       String publicId,
       //Book
       String bookPublicId,
       String bookTitle,

       //Member summary
       String memberPublicId,
       String memberName,

       //Institution
       String institutionId,

       //Loan DEtails
       int quantity,
       LocalDateTime borrowDate,
       LocalDate dueDate,
       LocalDate returnDate,
       LoanStatus status,
       boolean archived
) {
    public static LoanResponseDTO from(Loan loan) {
        return new LoanResponseDTO(
                loan.getPublicId(),
                loan.getBook().getPublicId(),
                loan.getBook().getTitle(),
                loan.getMember().getPublicId(),
                loan.getMember().getName(),
                loan.getInstitution().getPublicId(),
                loan.getQuantity(),
                loan.getBorrowDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.getStatus(),
                loan.isArchived()
        );
    }

}
