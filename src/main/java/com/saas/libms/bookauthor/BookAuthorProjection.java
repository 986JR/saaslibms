package com.saas.libms.bookauthor;

import java.util.UUID;

public interface BookAuthorProjection {
    UUID getId();

    String getBookPublicId();

    String getBookTitle();

    String getBookIsbn();

    String getBookPublisher();

    Integer getBookPublishedYear();

    String getAuthorPublicId();

    String getAuthorName();

    String getAuthorStatus();
}
