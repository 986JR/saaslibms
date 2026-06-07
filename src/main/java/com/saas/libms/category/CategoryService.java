package com.saas.libms.category;

import com.saas.libms.audit.AuditAction;
import com.saas.libms.audit.AuditEntityType;
import com.saas.libms.audit.AuditLogService;
import com.saas.libms.audit.AuditMetadata;
import com.saas.libms.category.dto.CategoryCreateDTO;
import com.saas.libms.category.dto.CategoryResponseDTO;
import com.saas.libms.category.dto.CategoryUpdateDTO;
import com.saas.libms.common.PublicIdGenerator;
import com.saas.libms.exception.ConflictException;
import com.saas.libms.exception.ResourceNotFoundException;
import com.saas.libms.institution.Institution;
import com.saas.libms.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;
    public static final String CATEGORIES_CACHE = "categories";

    //Create
    @Transactional
    public CategoryResponseDTO createCategory(CategoryCreateDTO dto, CustomUserDetails currentUser) {
        Institution institution = currentUser.getUser().getInstitution();

        if (categoryRepository.existsByNameAndInstitutionId(dto.name(), institution.getId())) {
            throw new ConflictException("A category with this name already exists in your ");
        }

        String publicId = PublicIdGenerator.generate("CATEGORY");

        Category category = Category.builder()
                .institution(institution)
                .name(dto.name())
                .publicId(publicId)
                .build();

        auditLogService.log(
                currentUser,
                AuditAction.CATEGORY_CREATED,
                AuditEntityType.CATEGORY,
                category.getPublicId(),
                AuditMetadata.builder()
                        .put("name", category.getName())
                        .build()
        );

        return CategoryResponseDTO.from(categoryRepository.save(category));
    }

    //get All Paginated
    public Page<CategoryResponseDTO> getAllCategories(int page, int size, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("name").ascending());

        return categoryRepository
                .findAllByInstitutionId(institutionId, pageRequest)
                .map(CategoryResponseDTO::from);
    }

    //get By Name
    @Cacheable(
            value = CATEGORIES_CACHE,
            key = "#currentUser.user.institution.id + ':' + #name"
    )
    public CategoryResponseDTO getCategoryByName(String name, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        Category category = categoryRepository.findByName(name)
                .filter(c-> c.getInstitution().getId().equals(institutionId))
                .orElseThrow(()-> new ResourceNotFoundException("Category with name '"+name+"' not found in your institution"));

        return CategoryResponseDTO.from(category);
    }

    //update
    @Transactional
    @CacheEvict(
            value = CATEGORIES_CACHE,
            key = "#currentUser.user.institution.id + ':' + #publicId"
    )
    public CategoryResponseDTO updateCategory(String publicId, CategoryUpdateDTO dto, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        Category category = categoryRepository.findByPublicIdAndInstitutionId(publicId,institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Category Not Found"));

        String newName = dto.name().trim();

    if(!newName.equalsIgnoreCase(category.getName())) {
        if(categoryRepository.existsByNameAndInstitutionId(newName, institutionId)) {
            throw  new ConflictException("A category with this name already exists in your institution");

        }

    }
    category.setName(newName);

        auditLogService.log(
                currentUser,
                AuditAction.CATEGORY_UPDATED,
                AuditEntityType.CATEGORY,
                category.getPublicId(),
                AuditMetadata.builder()
                        .put("name", category.getName())
                        .build()
        );

    return CategoryResponseDTO.from(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(
            value = CATEGORIES_CACHE,
            key = "#currentUser.user.institution.id + ':' + #publicId"
    )
    public void deleteCategory(String publicId, CustomUserDetails currentUSer) {

        UUID institutionId = currentUSer.getUser().getInstitution().getId();

        Category category = categoryRepository
                .findByPublicIdAndInstitutionId(publicId,institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        categoryRepository.detachBookBooksFromCategory(category.getId());

        categoryRepository.delete(category);

        auditLogService.log(
                currentUSer,
                AuditAction.CATEGORY_DELETED,
                AuditEntityType.CATEGORY,
                publicId,
                AuditMetadata.builder()
                        .put("name", category.getName())
                        .build()
        );
    }

}
