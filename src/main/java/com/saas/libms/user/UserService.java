package com.saas.libms.user;

import com.saas.libms.common.PublicIdGenerator;
import com.saas.libms.exception.ConflictException;
import com.saas.libms.exception.ForbiddenExecption;
import com.saas.libms.institution.Institution;
import com.saas.libms.security.CustomUserDetails;
import com.saas.libms.user.dto.UserCreateDTO;
import com.saas.libms.user.dto.UserResponseDTO;
import com.saas.libms.user.dto.UserUpdateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //Create user Only Admins
    @Transactional
    public UserResponseDTO createUser(UserCreateDTO dto, CustomUserDetails currentUser) {

        //check role
        if (currentUser.getUser().getRole() != UserRole.ADMIN) {
            throw new ForbiddenExecption("Only Admins Can create Librians");
        }

        //Unique email
        if(userRepository.existsByEmail(dto.email())) {
            throw new ConflictException("A user with this email already exists");
        }

        Institution institution = currentUser.getUser().getInstitution();

        User user = new User();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.setPublicId(PublicIdGenerator.generate("USER"));
        user.setRole(UserRole.LIBRARIAN);
        user.setInstitution(institution);

        User saved = userRepository.save(user);
        log.info("[UserService] Admin {} createde user {} in institution {}",
                currentUser.getUsername(), saved.getPublicId(), institution.getPublicId());

        return UserResponseDTO.from(saved);
    }

    //Get All Users, Admins only
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers(CustomUserDetails currentUser) {
        if(currentUser.getUser().getRole() != UserRole.ADMIN) {
            throw new ForbiddenExecption("Only admins can list Users");

        }

        return userRepository
                .findAllByInstitutionId(currentUser.getInstitutionId())
                .stream()
                .map(UserResponseDTO::from)
                .toList();
    }

    //Get /me
    @Transactional(readOnly = true)
    public UserResponseDTO getCurrentUser(CustomUserDetails currentUser) {

        return UserResponseDTO.from(currentUser.getUser());
    }

    //User Patch
    @Transactional
    public UserResponseDTO updateUser(String targetPublicId,
                                      UserUpdateDTO dto,
                                      CustomUserDetails currentUser) {

        User currentUserEntity = currentUser.getUser();
        boolean isSelf = currentUserEntity.getPublicId().equals(targetPublicId);
        boolean isAdmin = currentUserEntity.getRole() == UserRole.ADMIN;

        //must be self or admin
        if(!isSelf && !isAdmin) {
            throw new ForbiddenExecption("You are not Allowed To update this user");

        } else {

        }

        //Uswer must be in the same isnititution
        User target = userRepository.findByPublicIdAndInstitutionId(targetPublicId,
                currentUserEntity.getInstitution().getId());

        if (isSelf) {
            applySelfUpdate(target,dto);
        }
        else {
            //preventing admin to chnage other admin
            if(target.getRole() == UserRole.ADMIN) {
                throw   new ForbiddenExecption("You can not update another Admin");
            }
            applyAdminUpdate(target, dto);
        }
User saved = userRepository.save(target);
        return  UserResponseDTO.from(saved);

    }

    //Delete user, only Admins
    @Transactional
    public void deleteUser(String targetPublicId,
                           CustomUserDetails currentUser) {

        User currentUserEntity = currentUser.getUser();

        if(currentUserEntity.getRole() != UserRole.ADMIN) {
            System.out.println("here 1");
            throw new ForbiddenExecption("Only Admins Can Delete Users");
        }

        //should not delete self
        if (currentUserEntity.getPublicId().equals(targetPublicId)) {
            System.out.println("here 2");
            throw new ForbiddenExecption("You can not Delete Your own Account");
        }

        //Must be in same institution
        if(!userRepository.existsByPublicIdAndInstitutionId(targetPublicId,currentUserEntity.getInstitution().getId())){
            System.out.println("here 3");
            throw new ForbiddenExecption("You must be in the same institution");
        }

        User target = userRepository.findByPublicIdAndInstitutionId(targetPublicId,currentUserEntity.getInstitution().getId());

        //Cannot delete another admin
        if (target.getRole() == UserRole.ADMIN) {
            throw new ForbiddenExecption("You can not delete an Admin Account");
        }
        //Soft delete
        target.setStatus(UserStatus.DISABLED);
        userRepository.save(target);

        log.info("[UserService] Admin {} disabled user {} in institution {}",
                currentUserEntity.getPassword(),targetPublicId, currentUserEntity.getInstitution().getPublicId());


    }

    //helpers
    private void applySelfUpdate(User target, UserUpdateDTO dto) {
        if(dto.username() != null && !dto.username().isBlank()) {
            target.setUsername(dto.username());
        }

        if(dto.email() != null && !dto.email().isBlank()) {
            if(userRepository.existsByEmail(dto.email()) && !dto.email().equalsIgnoreCase(target.getEmail()))  {
                throw new ConflictException("This email is already in use");

            }
            target.setEmail(dto.email());
        }

        if (dto.password() != null && !dto.password().isBlank()) {
            target.setPassword(passwordEncoder.encode(dto.password()));
        }

    }

    // helpers 2
    private void applyAdminUpdate(User target, UserUpdateDTO dto) {
        if (dto.status() != null) {
            target.setStatus(dto.status());
        }

    }
}
