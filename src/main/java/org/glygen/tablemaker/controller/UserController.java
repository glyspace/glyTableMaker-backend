package org.glygen.tablemaker.controller;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import org.glygen.tablemaker.exception.DataNotFoundException;
import org.glygen.tablemaker.persistence.GlygenUser;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.UserLoginType;
import org.glygen.tablemaker.persistence.dao.RoleRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.security.TokenProvider;
import org.glygen.tablemaker.service.UserManager;
import org.glygen.tablemaker.view.LoginRequest;
import org.glygen.tablemaker.view.SuccessResponse;
import org.glygen.tablemaker.view.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityExistsException;
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
    
    public UserController(UserManager userManager,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            TokenProvider tokenProvider, UserRepository userRepo, RoleRepository roleRepo) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userManager = userManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepo;
        this.roleRepository = roleRepo;
    }
    
    @Operation(summary = "Get all users", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/user")
    public ResponseEntity<SuccessResponse> getAllUser() {
        List<UserEntity> users = userRepository.findAll();
        
        //TODO do not return passwords
        return new ResponseEntity<>(new SuccessResponse(users, MessageFormat.format("{0} result found", users.size())), HttpStatus.OK);
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
        userView.setPublicFlag(user.getPublicFlag());
        userView.setUserName(user.getUsername());
        userView.setUserType(user.getLoginType().name());
        userView.setGroupName(user.getGroupName());
        userView.setDepartment(user.getDepartment());
        return ResponseEntity.ok(new SuccessResponse(user, "User Information retrieved successfully"));
    }

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse> register(@Valid @RequestBody User user) {
        
        UserEntity newUser = new UserEntity();
        newUser.setUsername(user.getUserName());        
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setEnabled(true);   //TODO set to false initially
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setEmail(user.getEmail());
        newUser.setAffiliation(user.getAffiliation());
        newUser.setGroupName(user.getGroupName());
        newUser.setDepartment(user.getDepartment());
        newUser.setAffiliationWebsite(user.getAffiliationWebsite()); 
        newUser.setPublicFlag(user.getPublicFlag());
        newUser.setRoles(Arrays.asList(roleRepository.findByRoleName("ROLE_USER")));
        newUser.setLoginType(UserLoginType.LOCAL); 
        
        
        // check if the user already exists
        UserEntity existing = userRepository.findByUsernameIgnoreCase(user.getUserName());
        if (existing != null) {
            throw new EntityExistsException("This user " + user.getUserName() + " already exists!");
        }
        
        // check if the email is already in the system
        existing = userRepository.findByEmailIgnoreCase(user.getEmail());
        if (existing != null) {
            throw new EntityExistsException ("There is already an account with this email: " + user.getEmail());
        }
            
        userManager.createUser(newUser);  
        
        // TODO add email verification
        // set user.enabled to false
        logger.info("New user {} is added to the system", newUser.getUsername());
        user.setPassword(null);   // Do not send the password
        return ResponseEntity.ok(new SuccessResponse(user, "Registered successfully"));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<SuccessResponse> authenticateUser(@Valid @RequestBody LoginRequest login) {

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt;
        try {
            jwt = tokenProvider.generateToken((GlygenUser) authentication.getPrincipal());
            return ResponseEntity.ok(new SuccessResponse(jwt, "Login Successfully"));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to generate an authentication token!", e);
        }
        
    }

}
