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
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.UserDtls;
import com.ecom.repository.UserRepository;
import com.ecom.service.UserService;
import com.ecom.util.AppConstant;
import org.springframework.util.StringUtils;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
     Optional<UserDtls> optDb = userRepository.findById(user.getId());
     if (!optDb.isPresent()) {
         return null; // or throw an exception
     }

     UserDtls dbUser = optDb.get();

     // --- update editable fields ---
     dbUser.setName(user.getName());
     dbUser.setMobileNumber(user.getMobileNumber());
     dbUser.setAddress(user.getAddress());
     dbUser.setCity(user.getCity());
     dbUser.setPinCode(user.getPinCode());
     dbUser.setState(user.getState());

     // Handle image upload (if provided)
     if (img != null && !img.isEmpty()) {
         // Basic validation: size and content type (tweak as needed)
         long maxSizeBytes = 5L * 1024L * 1024L; // 5 MB
         String contentType = img.getContentType() == null ? "" : img.getContentType().toLowerCase();
         if (img.getSize() > maxSizeBytes) {
             throw new IllegalArgumentException("File too large. Max 5MB allowed.");
         }
         if (!(contentType.contains("image"))) {
             throw new IllegalArgumentException("Only image files are allowed.");
         }

         try {
             // ensure uploads/profile_img exists
             Path uploadsDir = Paths.get("uploads", "profile_img").toAbsolutePath();
             if (!Files.exists(uploadsDir)) {
                 Files.createDirectories(uploadsDir);
             }

             // generate unique filename
             String original = StringUtils.cleanPath(img.getOriginalFilename());
             String ext = "";
             int dot = original.lastIndexOf('.');
             if (dot >= 0) ext = original.substring(dot);

             String filename = UUID.randomUUID().toString() + ext;

             // target path
             Path target = uploadsDir.resolve(filename);

             // copy file (replace if exists)
             Files.copy(img.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

             // delete old file if present (and not default placeholder)
             if (dbUser.getProfileImage() != null && !dbUser.getProfileImage().isBlank()) {
                 // dbUser.profileImage is stored as e.g. "profile_img/<file>"
                 String existing = dbUser.getProfileImage();
                 // only delete if stored in uploads/profile_img
                 if (existing.startsWith("profile_img/")) {
                     Path old = Paths.get("uploads").resolve(existing).toAbsolutePath();
                     try {
                         Files.deleteIfExists(old);
                     } catch (Exception ex) {
                         // log and continue - do not fail the update because old file couldn't be deleted
                         ex.printStackTrace();
                     }
                 }
             }

             // store relative path in DB so Thymeleaf resolves to /uploads/<relative>
             dbUser.setProfileImage("profile_img/" + filename);

         } catch (IOException ioe) {
             // log error and rethrow or return null based on your error strategy
             ioe.printStackTrace();
             throw new RuntimeException("Failed to store profile image.", ioe);
         }
     }

     // save DB changes
     dbUser = userRepository.save(dbUser);
     return dbUser;
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
