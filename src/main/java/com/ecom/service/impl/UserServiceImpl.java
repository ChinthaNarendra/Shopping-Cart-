package com.ecom.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.UserDtls;
import com.ecom.repository.UserRepository;
import com.ecom.service.CloudinaryService;
import com.ecom.service.UserService;
import com.ecom.util.AppConstant;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private CloudinaryService cloudinaryService;

    /**
     * Save a new user with safe defaults.
     */
    @Override
    public UserDtls saveUser(UserDtls user) {
        // defaults
        if (user.getRole() == null) {
            user.setRole("ROLE_USER");
        }
        if (user.getIsEnable() == null) {
            user.setIsEnable(Boolean.TRUE);
        }
        if (user.getAccountNonLocked() == null) {
            user.setAccountNonLocked(Boolean.TRUE);
        }
        if (user.getFailedAttempt() == null) {
            user.setFailedAttempt(0);
        }
        user.setLockTime(null);

        // encode password (assume non-null password)
        if (user.getPassword() != null) {
            String encodePassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodePassword);
        }

        return userRepository.save(user);
    }

    @Override
    public UserDtls getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * If role is null, return all users; otherwise return by role.
     */
    @Override
    public List<UserDtls> getUsers(String role) {
        if (role == null) {
            return userRepository.findAll();
        }
        return userRepository.findByRole(role);
    }

    @Override
    public Boolean updateAccountStatus(Integer id, Boolean status) {
        Optional<UserDtls> findByUser = userRepository.findById(id);

        if (findByUser.isPresent()) {
            UserDtls userDtls = findByUser.get();
            userDtls.setIsEnable(status);
            userRepository.save(userDtls);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void increasedFailedAttempt(UserDtls user) {
        Integer current = user.getFailedAttempt();
        if (current == null)
            current = 0;
        int attempt = current + 1;
        user.setFailedAttempt(attempt);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void userAccountLock(UserDtls user) {
        user.setAccountNonLocked(false);
        user.setLockTime(new Date());
        userRepository.save(user);
    }

    /**
     * Returns true if account was unlocked by this call (i.e. lock expired). Safely
     * handles null lockTime.
     */
    @Override
    public boolean unlockAccountTimeExpired(UserDtls user) {
        if (user == null || user.getLockTime() == null) {
            return false;
        }

        long lockTime = user.getLockTime().getTime();
        long unlockTime = lockTime + AppConstant.UNLOCK_DURATION_TIME;
        long currentTime = System.currentTimeMillis();

        if (unlockTime < currentTime) {
            user.setAccountNonLocked(true);
            user.setFailedAttempt(0);
            user.setLockTime(null);
            userRepository.save(user);
            return true;
        }

        return false;
    }

    /**
     * Reset attempts for given user id (used after successful login).
     */
    @Override
    public void resetAttempt(int userId) {
        Optional<UserDtls> opt = userRepository.findById(userId);
        if (opt.isPresent()) {
            UserDtls u = opt.get();
            u.setFailedAttempt(0);
            // do not change lockTime/accountNonLocked here unless desired
            userRepository.save(u);
        }
    }

    @Override
    public void updateUserResetToken(String email, String resetToken) {
        UserDtls findByEmail = userRepository.findByEmail(email);
        if (findByEmail != null) {
            findByEmail.setResetToken(resetToken);
            userRepository.save(findByEmail);
        }
    }

    @Override
    public UserDtls getUserByToken(String token) {
        return userRepository.findByResetToken(token);
    }

    @Override
    public UserDtls updateUser(UserDtls user) {
        return userRepository.save(user);
    }

 // import additions:
 // import java.nio.file.StandardCopyOption;
 // import java.util.UUID;
 // import org.springframework.util.StringUtils;

    @Override
    public UserDtls updateUserProfile(UserDtls user, MultipartFile img) {

        if (user.getId() == null) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        Optional<UserDtls> optDb = userRepository.findById(user.getId());

        if (optDb.isEmpty()) {
            return null;
        }

        UserDtls dbUser = optDb.get();

        // Update editable profile fields
        dbUser.setName(user.getName());
        dbUser.setMobileNumber(user.getMobileNumber());
        dbUser.setAddress(user.getAddress());
        dbUser.setCity(user.getCity());
        dbUser.setPinCode(user.getPinCode());
        dbUser.setState(user.getState());

        // Upload profile image to Cloudinary only when a new image is selected
        if (img != null && !img.isEmpty()) {

            try {
                String imageUrl = cloudinaryService.uploadImage(
                        img,
                        "shopping-cart/profiles"
                );

                // Store the Cloudinary secure URL in the database
                dbUser.setProfileImage(imageUrl);

            } catch (IOException ex) {
                throw new RuntimeException(
                        "Failed to upload profile image to Cloudinary.", ex
                );
            }
        }

        // Save profile changes
        return userRepository.save(dbUser);
    }

@Override
public UserDtls saveAdmin(UserDtls user) {
	 if (user.getRole() == null) {
         user.setRole("ROLE_ADMIN");
     }
     if (user.getIsEnable() == null) {
         user.setIsEnable(Boolean.TRUE);
     }
     if (user.getAccountNonLocked() == null) {
         user.setAccountNonLocked(Boolean.TRUE);
     }
     if (user.getFailedAttempt() == null) {
         user.setFailedAttempt(0);
     }
     user.setLockTime(null);

     // encode password (assume non-null password)
     if (user.getPassword() != null) {
         String encodePassword = passwordEncoder.encode(user.getPassword());
         user.setPassword(encodePassword);
     }

     return userRepository.save(user);
}

@Override
public Boolean existsEmail(String email) {
	
	return userRepository.existsByEmail(email);
}

}
