package com.saas.libms.member;

import com.saas.libms.common.ApiResponse;
import com.saas.libms.member.dto.MemberCreateDTO;
import com.saas.libms.member.dto.MemberResponseDTO;
import com.saas.libms.member.dto.MemberStatusUpdateDTO;
import com.saas.libms.member.dto.MemberUpdateDTO;
import com.saas.libms.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.awt.print.Pageable;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    //Post new Member
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<MemberResponseDTO>> createMember(
            @Valid @RequestBody MemberCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
            ) {
        MemberResponseDTO responseDTO = memberService.createMember(dto,currentUser);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Member registered successfully", responseDTO));
    }

    //Get All
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<Page<MemberResponseDTO>>> getAllMembers(
           @RequestParam(defaultValue = "0") int page,
           @RequestParam(defaultValue = "10") int size,
           @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Page<MemberResponseDTO> members = memberService.getAllMembers(page,size,currentUser);
        return ResponseEntity.ok(ApiResponse.success("Member fetched successfully" , members));
    }

    //Get byId
    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<MemberResponseDTO>> getMemberByPublicId(
            @PathVariable String publicId,
            @Valid @RequestBody MemberCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        MemberResponseDTO responseDTO = memberService.getMemberByPublicId(publicId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Member fetched successfully", responseDTO));
    }

    //Patch Member details
    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<MemberResponseDTO>> updateMember(
            @PathVariable String publicId,
            @Valid @RequestBody MemberUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        MemberResponseDTO responseDTO = memberService.updateMember(publicId,dto,currentUser);
        return ResponseEntity.ok(ApiResponse.success("Member updated successfully", responseDTO));
    }

    @PatchMapping("/{publicId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MemberResponseDTO>> updateMemberStatus(
            @PathVariable String publicId,
            @Valid @RequestBody MemberStatusUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        MemberResponseDTO response = memberService.updateMemberStatus(publicId, currentUser,dto);
        return ResponseEntity.ok(ApiResponse.success("Member status updated successfully", response));
    }


    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            @PathVariable String publicId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        memberService.deleteMember(publicId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Member blocked successfully", null));
    }
}
