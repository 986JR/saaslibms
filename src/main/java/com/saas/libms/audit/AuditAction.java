package com.saas.libms.audit;

public enum AuditAction {

    // Book actions
    BOOK_CREATED,
    BOOK_UPDATED,
    BOOK_DELETED,

    // Author actions
    AUTHOR_CREATED,
    AUTHOR_UPDATED,
    AUTHOR_DELETED,

    // BookAuthor (link) actions
    BOOK_AUTHOR_LINKED,
    BOOK_AUTHOR_RELINKED,
    BOOK_AUTHOR_UNLINKED,

    // Category actions
    CATEGORY_CREATED,
    CATEGORY_UPDATED,
    CATEGORY_DELETED,

    // Member actions
    MEMBER_CREATED,
    MEMBER_UPDATED,
    MEMBER_BLOCKED,
    MEMBER_DELETED,

    // Loan actions
    LOAN_ISSUED,
    LOAN_RETURNED,
    LOAN_MARKED_OVERDUE,

    // Reservation actions
    RESERVATION_CREATED,
    RESERVATION_CANCELLED,

    // User (staff) actions
    USER_CREATED,
    USER_UPDATED,
    USER_DISABLED,
    USER_DELETED,

    // Auth actions
    PASSWORD_RESET_REQUESTED
}
