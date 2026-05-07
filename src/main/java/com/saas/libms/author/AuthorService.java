package com.saas.libms.author;

import com.saas.libms.author.dto.AuthorCreateDTO;
import com.saas.libms.author.dto.AuthorResponseDTO;
import com.saas.libms.author.dto.AuthorUpdateDTO;
import com.saas.libms.common.PublicIdGenerator;
import com.saas.libms.exception.ConflictException;
import com.saas.libms.exception.ResourceNotFoundException;
import com.saas.libms.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;

    //create
    @Transactional
    public AuthorResponseDTO createAuthor(AuthorCreateDTO dto, CustomUserDetails currentUser) {
        var institution = currentUser.getUser().getInstitution();

        if(authorRepository.existsByNameAndInstitutionId(dto.name(), institution.getId())) {
            throw  new ConflictException("An author with this name already exists in your Institution");
        }

        var author = Author.builder()
                .publicId(PublicIdGenerator.generate("AUTHOR"))
                .name(dto.name())
                .status(AuthorStatus.ACTIVE)
                .institution(institution)
                .build();

        return AuthorResponseDTO.from(authorRepository.save(author));
    }

    //getAll
    public Page<AuthorResponseDTO> getAllAuthors(int page, int size, CustomUserDetails currentUser){
        var institutionId = currentUser.getUser().getInstitution().getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        return authorRepository.findAllByInstitutionId(institutionId, pageable)
                .map(AuthorResponseDTO::from);
    }

    //get Author ById
    public AuthorResponseDTO getAuthorByPublicId(String publicId, CustomUserDetails currentUser) {

        var institutionId = currentUser.getUser().getInstitution().getId();
        var author = authorRepository.findByPublicIdAndInstitutionId(publicId,institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Author not found"));

        return   AuthorResponseDTO.from(author);
    }

    //Update
    @Transactional
    public  AuthorResponseDTO updateAuthor(String publicId, AuthorUpdateDTO dto, CustomUserDetails currentUser) {
        var institutionId = currentUser.getUser().getInstitution().getId();
        var author = authorRepository.findByPublicIdAndInstitutionId(publicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Author Not Found "));

        if(dto.name() != null) {
            if(!dto.name().equals(author.getName()) &&
            authorRepository.existsByNameAndInstitutionId(dto.name(), institutionId)){
                throw  new ConflictException("An author with this name already exists");
            }
            author.setName(dto.name());
        }
        return AuthorResponseDTO.from(authorRepository.save(author));

    }

    //Delete
    @Transactional
    public void deleteAuthor(String publicId, CustomUserDetails currentUser) {
        var institutionId = currentUser.getUser().getInstitution().getId();
        var author = authorRepository.findByPublicIdAndInstitutionId(publicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Author To delete not found"));

        author.setStatus(AuthorStatus.DISABLED);
        authorRepository.save(author);
    }

}
