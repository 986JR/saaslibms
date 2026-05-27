package com.saas.libms.member;

import com.saas.libms.audit.AuditAction;
import com.saas.libms.audit.AuditEntityType;
import com.saas.libms.audit.AuditLogService;
import com.saas.libms.audit.AuditMetadata;
import com.saas.libms.common.PublicIdGenerator;
import com.saas.libms.exception.ConflictException;
import com.saas.libms.exception.ResourceNotFoundException;
import com.saas.libms.member.dto.MemberCreateDTO;
import com.saas.libms.member.dto.MemberResponseDTO;
import com.saas.libms.member.dto.MemberStatusUpdateDTO;
import com.saas.libms.member.dto.MemberUpdateDTO;
import com.saas.libms.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;

    //create
    @Transactional
    public MemberResponseDTO createMember(MemberCreateDTO dto, CustomUserDetails currentUser) {

        UUID institutionId = currentUser.getUser().getInstitution().getId();

        //email unique in institution level
        if(dto.email() != null && !dto.email().isBlank()) {
            if (memberRepository.existsByEmailAndInstitutionId(dto.email(),institutionId)) {
                throw new ConflictException("A member with email '"+dto.email() + "' already exists in your institution");
            }
        }

        Member member = Member.builder()
                .publicId(PublicIdGenerator.generate("MEMBER"))
                .institution(currentUser.getUser().getInstitution())
                .name(dto.fullName())
                .email(dto.email())
                .phone(dto.phone())
                .status(MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Member saved = memberRepository.save(member);

        auditLogService.log(
                currentUser,
                AuditAction.MEMBER_CREATED,
                AuditEntityType.MEMBER,
                saved.getPublicId(),
                AuditMetadata.builder()
                        .put("name",  saved.getName())
                        .put("email", saved.getEmail())
                        .build()
        );

        return MemberResponseDTO.from(saved);
    }

    //Get All
    public Page<MemberResponseDTO> getAllMembers(int page, int size, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return memberRepository
                .findAllByInstitutionId(institutionId, pageable)
                .map(MemberResponseDTO::from);
    }

    //Get by publicId
    public  MemberResponseDTO getMemberByPublicId(String publicId, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();
        Member member =  memberRepository
                .findByPublicIdAndInstitutionId(publicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Member not Found with ID: "+publicId));
        return MemberResponseDTO.from(member);

    }

    //Update
    @Transactional
    public MemberResponseDTO updateMember(String publicId, MemberUpdateDTO dto, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        Member member = memberRepository
                .findByPublicIdAndInstitutionId(publicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Member not  found with ID: "+publicId));

        //Apply if available
        if(dto.fullName() != null && !dto.fullName().isBlank())  {
            member.setName(dto.fullName());
        }

        if (dto.email() != null && !dto.email().isBlank()) {
            boolean emailChanged = !dto.email().equalsIgnoreCase(member.getEmail());
            if(emailChanged) {

                if(memberRepository.existsByEmailAndInstitutionIdExcluding(dto.email(), institutionId,publicId)) {
                    throw new ConflictException("Member with email '" + dto.email() + "' already exists in this institution");

                }
            }
            member.setEmail(dto.email());
        }

        //update phone
        if (dto.phone() != null && !dto.phone().isBlank()) {
            member.setPhone(dto.phone());
        }

        auditLogService.log(
                currentUser,
                AuditAction.MEMBER_UPDATED,
                AuditEntityType.MEMBER,
                member.getPublicId(),
                AuditMetadata.builder()
                        .put("name",  member.getName())
                        .put("email", member.getEmail())
                        .build()
        );

        return MemberResponseDTO.from(member);

    }

    //delete

    @Transactional
    public void deleteMember(String publicId, CustomUserDetails currentUser) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        Member member = memberRepository.findByPublicIdAndInstitutionId(publicId, institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Member not found with ID: "+publicId));

        member.setStatus(MemberStatus.BLOCKED);

        auditLogService.log(
                currentUser,
                AuditAction.MEMBER_DELETED,
                AuditEntityType.MEMBER,
                publicId,
                AuditMetadata.builder()
                        .put("name",  member.getName())
                        .put("email", member.getEmail())
                        .build()
        );
    }

    //block user(admin)
    @Transactional
    public MemberResponseDTO updateMemberStatus(
            String publicId, CustomUserDetails currentUser,
            MemberStatusUpdateDTO dto
    ) {
        UUID institutionId = currentUser.getUser().getInstitution().getId();

        Member member = memberRepository.findByPublicIdAndInstitutionId(publicId,institutionId)
                .orElseThrow(()-> new ResourceNotFoundException("Member no found with Id: "+publicId));

        member.setStatus(dto.status());

        // Choose the most meaningful action depending on new status.
        AuditAction action = (dto.status() == MemberStatus.BLOCKED)
                ? AuditAction.MEMBER_BLOCKED
                : AuditAction.MEMBER_UPDATED;

        auditLogService.log(
                currentUser,
                action,
                AuditEntityType.MEMBER,
                member.getPublicId(),
                AuditMetadata.builder()
                        .put("name",      member.getName())
                        //emind to put the old member status
                        .put("newStatus", member.getStatus().name())
                        .build()
        );
        return MemberResponseDTO.from(member);
    }
}
