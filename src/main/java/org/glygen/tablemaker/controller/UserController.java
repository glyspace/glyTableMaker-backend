package org.glygen.tablemaker.controller;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.glygen.tablemaker.exception.BadRequestException;
import org.glygen.tablemaker.exception.DataNotFoundException;
import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.persistence.GlygenUser;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.UserLoginType;
import org.glygen.tablemaker.persistence.dao.RoleRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.security.TokenProvider;
import org.glygen.tablemaker.service.EmailManager;
import org.glygen.tablemaker.service.UserManager;
import org.glygen.tablemaker.service.UserManagerImpl;
import org.glygen.tablemaker.view.ChangePassword;
import org.glygen.tablemaker.view.LoginRequest;
import org.glygen.tablemaker.view.LoginResponse;
import org.glygen.tablemaker.view.SuccessResponse;
import org.glygen.tablemaker.view.User;
import org.glygen.tablemaker.view.validation.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/account")
public class UserController {
    //public static Logger logger=(Logger) LoggerFactory.getLogger(UserController.class);
    final static Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserManager userManager;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final EmailManager emailManager;
    
    
    public UserController(UserManager userManager,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            TokenProvider tokenProvider, UserRepository userRepo, RoleRepository roleRepo, EmailManager emailManager) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userManager = userManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepo;
        this.roleRepository = roleRepo;
        this.emailManager = emailManager;
    }
    
    
    @GetMapping("/user/{userName}")
    @Operation(summary="Retrieve the information for the given user", security = { @SecurityRequirement(name = "bearer-key") })
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="User retrieved successfully", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))}), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="404", description="User with given login name does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> getUser (
            @Parameter(required=true, description="login name of the user")
            @PathVariable("userName")
            String userName) {
        UserEntity user = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) { 
            // username of the authenticated user should match the username parameter
            // a user can only see his/her own user information
            // but admin can access all the users' information
            user = userRepository.findByUsernameIgnoreCase(userName.trim());
            if (user == null) {
                // try with email
                user = userRepository.findByEmailIgnoreCase(userName.trim());
                if (user == null) {
                    throw new DataNotFoundException("A user with loginId " + userName + " does not exist");
                }
            }
            if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                // no issues, the admin can access any profile
            } else if (auth.getName().equalsIgnoreCase(userName.trim())) {
                // the user can display his/her own details
            } else if (user.getEmail().equalsIgnoreCase(userName.trim())) {
                // the user can retrieve his/her own details
            } else {
                logger.info("The user: " + auth.getName() + " is not authorized to access " + userName + "'s information");
                throw new AccessDeniedException("The user: " + auth.getName() + " is not authorized to access " + userName + "'s information");
            }
        }
        else { // should not reach here at all
            throw new BadCredentialsException ("The user has not been authenticated");
        }
        
        User userView = new User();
        userView.setAffiliation(user.getAffiliation());
        userView.setAffiliationWebsite(user.getAffiliationWebsite());
        userView.setEmail(user.getEmail());
        userView.setFirstName(user.getFirstName());
        userView.setLastName(user.getLastName());
        userView.setTempPassword(user.getTempPassword());
        userView.setUserName(user.getUsername());
        userView.setUserType(user.getLoginType().name());
        userView.setGroupName(user.getGroupName());
        userView.setDepartment(user.getDepartment());
        return ResponseEntity.ok(new SuccessResponse(userView, "User Information retrieved successfully"));
    }
    
    @PostMapping("/update/{userName}")
    @Operation(summary="Updates the information for the given user. Only the non-empty fields will be updated. "
            + "\"username\" cannot be changed", security = { @SecurityRequirement(name = "bearer-key") })
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="User updated successfully" , content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = SuccessResponse.class))}), 
                @ApiResponse(responseCode="400", description="Illegal arguments, username should match the submitted user info"),
                @ApiResponse(responseCode="401", description="Unauthorized"),
                @ApiResponse(responseCode="403", description="Not enough privileges to update users"),
                @ApiResponse(responseCode="404", description="User with given login name does not exist"),
                @ApiResponse(responseCode="415", description="Media type is not supported"),
                @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> updateUser (@RequestBody(required=true) User user, @PathVariable("userName") String loginId) {
        UserEntity userEntity = userRepository.findByUsernameIgnoreCase(loginId.trim());
        if (userEntity == null) {
            // find it with email
            userEntity = userRepository.findByEmailIgnoreCase(loginId.trim());
            if (userEntity == null) {
                throw new DataNotFoundException("A user with loginId " + loginId + " does not exist");
            }
            
        }
        if ((user.getUserName() == null || user.getUserName().isEmpty()) || (!loginId.equalsIgnoreCase(user.getUserName()) && !loginId.equalsIgnoreCase(user.getEmail()))) {
            throw new IllegalArgumentException("userName (path variable) and the submitted user information do not match");
        } 
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) { 
            // a user can only update his/her own user information
            // username of the authenticated user should match the username of the user retrieved from the db
            
            if (auth.getName().equalsIgnoreCase(loginId) || auth.getName().equalsIgnoreCase(userEntity.getUsername())) {
               /* if (user.getEmail() != null && !user.getEmail().isEmpty() && !user.getEmail().trim().equals(userEntity.getEmail())) {
                    // send email confirmation
                    try {
                        // make sure the new email is not assigned to a different user
                        // check if the email is already in the system
                        UserEntity existing = userRepository.findByEmailIgnoreCase(user.getEmail().trim());
                        if (existing != null && !existing.getUserId().equals(userEntity.getUserId())) {
                            logger.info("There is already an account with this email: " + user.getEmail());
                            throw new DuplicateException("There is already an account with this email: " + user.getEmail(), errorMessage);
                        } 
                        
                        emailManager.sendEmailChangeNotification(userEntity);
                        UserEntity userWithNewEmail = new UserEntity();
                        userWithNewEmail.setEmail(user.getEmail().trim());
                        userWithNewEmail.setUserId(userEntity.getUserId());
                        userWithNewEmail.setUsername(userEntity.getUsername());
                        userWithNewEmail.setRoles(userEntity.getRoles());
                        emailManager.sendVerificationToken(userWithNewEmail);
                        userManager.changeEmail(userEntity, userEntity.getEmail(), user.getEmail().trim());
                    } catch (MailSendException e) {
                        // email cannot be sent, do not update the user
                        logger.error("Mail cannot be sent: ", e);
                        throw e;
                    }
                }*/   //TODO handle email change
                
                if (user.getAffiliation() != null) userEntity.setAffiliation(user.getAffiliation().trim());
                if (user.getGroupName() != null) userEntity.setGroupName(user.getGroupName().trim());
                if (user.getDepartment() != null) userEntity.setDepartment(user.getDepartment().trim());
                if (user.getAffiliationWebsite() != null) userEntity.setAffiliationWebsite(user.getAffiliationWebsite().trim());
                if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) userEntity.setFirstName(user.getFirstName().trim());
                if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) userEntity.setLastName(user.getLastName().trim());
                if (user.getTempPassword() != null) userEntity.setTempPassword(user.getTempPassword());
                userRepository.save(userEntity);
            }
            else {
                logger.info("The user: " + auth.getName() + " is not authorized to update user " + loginId);
                throw new AccessDeniedException("The user: " + auth.getName() + " is not authorized to update user with id " + loginId);
            }
        }
        else { // should not reach here at all
            throw new BadCredentialsException ("The user has not been authenticated");
        }
        
        return ResponseEntity.ok(new SuccessResponse(user, "User updated successfully"));
    }
    
    @GetMapping("/availableUsername")
    @Operation(summary="Checks whether the given username is available to be used (returns true if available, false if alredy in use")
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Check performed successfully", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))}), 
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Boolean checkUserName(@RequestParam("username") final String username) {
        userManager.cleanUpExpiredSignup(); // to make sure we are not holding onto any user name which is not verified and expired
        UserEntity user = userRepository.findByUsernameIgnoreCase(username.trim());
        if(user!=null) {
            throw new DuplicateException("This user " + username + " already exists!");
        }
        return user == null;
    }

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse> register(@Valid @RequestBody User user) {
        
        UserEntity newUser = new UserEntity();
        newUser.setUsername(user.getUserName());        
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setEnabled(false);  
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setEmail(user.getEmail());
        newUser.setAffiliation(user.getAffiliation());
        newUser.setGroupName(user.getGroupName());
        newUser.setDepartment(user.getDepartment());
        newUser.setAffiliationWebsite(user.getAffiliationWebsite()); 
        newUser.setTempPassword(false);
        newUser.setRoles(Arrays.asList(roleRepository.findByRoleName("ROLE_USER")));
        newUser.setLoginType(UserLoginType.LOCAL); 
        
        // clean up expired tokens if any
        userManager.cleanUpExpiredSignup();
        
        // check if the user already exists
        UserEntity existing = userRepository.findByUsernameIgnoreCase(user.getUserName());
        if (existing != null) {
            throw new DuplicateException("This user " + user.getUserName() + " already exists!");
        }
        
        // check if the email is already in the system
        existing = userRepository.findByEmailIgnoreCase(user.getEmail());
        if (existing != null) {
            throw new DuplicateException ("There is already an account with this email: " + user.getEmail());
        }
            
        userManager.createUser(newUser);  
        
        // send email confirmation
        try {
            emailManager.sendVerificationToken(newUser);
        } catch (Exception e) {
            // email cannot be sent, remove the user
            logger.error("Mail cannot be sent: ", e);
            userManager.deleteUser(newUser);
            throw e;
        }
        logger.info("New user {} is added to the system", newUser.getUsername());
        user.setPassword(null);   // Do not send the password
        return ResponseEntity.ok(new SuccessResponse(user, "Registered successfully"));
    }
    
    @GetMapping(value = "/registrationConfirm")
    @Operation(summary="Enables the user by checking the confirmation token, removes the user if token is expired already")
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="User is confirmed successfully" , content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = SuccessResponse.class))}), 
            @ApiResponse(responseCode="400", description="Link already expired (ErrorCode=4050 Expired)"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> confirmRegistration(@RequestParam("token") final String token) throws UnsupportedEncodingException {
        final String result = userManager.validateVerificationToken(token.trim());
        if (result.equals(UserManagerImpl.TOKEN_VALID)) {
            final UserEntity user = userManager.getUserByToken(token.trim());
            // we don't need the token after confirmation
            userManager.deleteVerificationToken(token.trim());
            return ResponseEntity.ok(new SuccessResponse(null, "Verified Successfully"));
        } else if (result.equals(UserManagerImpl.TOKEN_INVALID)) {
            logger.error("Token entered is not valid!");
            throw new IllegalArgumentException("Token entered is not valid");
        } else if (result.equals(UserManagerImpl.TOKEN_EXPIRED)) {
            logger.error("Token is expired, please signup again!");
            throw new IllegalArgumentException("Token is expired, please signup again!");
        }
        throw new BadRequestException("User verification link is expired");
    }
    
    @GetMapping("/recover")
    @Operation(summary="Recovers the user's username. Sends an email to the email provided by the user if it has valid account")
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Username recovered successfully", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = SuccessResponse.class))}), 
            @ApiResponse(responseCode="400", description="Illegal argument - valid email has to be provided"),
            @ApiResponse(responseCode="404", description="User with given email does not exist"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public @ResponseBody ResponseEntity<SuccessResponse> recoverUsername (@RequestParam(value="email", required=true) String email) {
        UserEntity user = userManager.recoverLogin(email.trim());
        
        if (user == null) {
            throw new DataNotFoundException("A user with email " + email + " does not exist");
        }
        
        String userEmail = user.getEmail();
        emailManager.sendUserName(user);
        
        logger.info("UserName Recovery email is sent to {}", userEmail);
        return ResponseEntity.ok(new SuccessResponse(null, "Email containing the username was sent"));
    }
    
    @GetMapping("/{userName}/password")
    @Operation(summary="Recovers the user's password. Sends an email to the registered email of the user",
            security = { @SecurityRequirement(name = "bearer-key") })
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Password recovered successfully", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = SuccessResponse.class))}), 
            @ApiResponse(responseCode="404", description="User with given login name does not exist"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public @ResponseBody ResponseEntity<SuccessResponse> recoverPassword (
            @PathVariable("userName") String loginId) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(loginId.trim());
        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(loginId.trim());
            if (user == null) {
                throw new DataNotFoundException ("A user with loginId " + loginId + " does not exist");
            }
        }
        emailManager.sendPasswordReminder(user);
        logger.info("Password recovery email is sent to {}", user.getEmail());
        return ResponseEntity.ok(new SuccessResponse(null, "Password recovery email is sent"));        
    }
    
    @PutMapping("/{userName}/password")
    @Operation(summary="Changes the password for the given user", 
        description="Only authenticated user can change his/her password", security = { @SecurityRequirement(name = "bearer-key") })
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Password changed successfully", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = SuccessResponse.class))}), 
            @ApiResponse(responseCode="400", description="Illegal argument - new password should be valid"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update password"),
            @ApiResponse(responseCode="404", description="User with given login name does not exist"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public @ResponseBody ResponseEntity<SuccessResponse> changePassword (
            Principal p,
            @Parameter(description = "your password", schema = @Schema(type = "string", format = "password"))
            @RequestBody(required=true) 
            ChangePassword changePassword, 
            @PathVariable("userName") String userName) {
        if (p == null) {
            // not authenticated
            throw new BadCredentialsException("Unauthorized to change the password");
        }
        UserEntity user = userManager.getUserByUsername(userName.trim());
        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(userName.trim());
            if (user == null) {
                throw new DataNotFoundException("A user with loginId " + userName + " does not exist");
            }
        }
        
        if (!p.getName().equalsIgnoreCase(userName.trim()) && !p.getName().equalsIgnoreCase(user.getUsername())) {
            logger.warn("The user: " + p.getName() + " is not authorized to change " + userName + "'s password");
            throw new AccessDeniedException("The user: " + p.getName() + " is not authorized to change " + userName + "'s password");
        }
        
        // using @NotEmpty for newPassword didn't work, so have to handle it here
        if (null == changePassword.getNewPassword() || changePassword.getNewPassword().isEmpty()) {
            throw new IllegalArgumentException("Invalid Input: new password cannot be empty");
        }
        
        if (null == changePassword.getCurrentPassword() || changePassword.getCurrentPassword().isEmpty()) {
            throw new IllegalArgumentException("Invalid Input: current password cannot be empty");
        }
        
        //password validation 
        Pattern pattern = Pattern.compile(PasswordValidator.PASSWORD_PATTERN);
        
        if (!pattern.matcher(changePassword.getNewPassword().trim()).matches()) {
            throw new IllegalArgumentException("new password is not valid. The password length must be greater than or equal to 5, must contain one or more uppercase characters, \n" + 
                   "must contain one or more lowercase characters, must contain one or more numeric values and must contain one or more special characters");
        }
        
        if(passwordEncoder.matches(changePassword.getCurrentPassword().trim(), user.getPassword())) {
            // encrypt the password
            String hashedPassword = passwordEncoder.encode(changePassword.getNewPassword().trim());
            userManager.changePassword(user, hashedPassword);
        } else {
            logger.error("Current Password is not valid!");
            throw new IllegalArgumentException("Current password is invalid");
        }
        return ResponseEntity.ok(new SuccessResponse(null, "Password changed successfully"));  
    }

    @PostMapping("/authenticate")
    public ResponseEntity<SuccessResponse> authenticateUser(@Valid @RequestBody LoginRequest login) {

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt;
        try {
            GlygenUser user = (GlygenUser) authentication.getPrincipal();
            jwt = tokenProvider.generateToken(user);
            User userView = new User();
            userView.setAffiliation(user.getAffiliation());
            userView.setAffiliationWebsite(user.getAffiliationWebsite());
            userView.setEmail(user.getEmail());
            userView.setFirstName(user.getFirstName());
            userView.setLastName(user.getLastName());
            userView.setTempPassword(user.getTempPassword());
            userView.setUserName(user.getUsername());
            
            LoginResponse resp = new LoginResponse(jwt, userView);
            return ResponseEntity.ok(new SuccessResponse(resp, "Login Successfully"));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to generate an authentication token!", e);
        }
        
    }

}
