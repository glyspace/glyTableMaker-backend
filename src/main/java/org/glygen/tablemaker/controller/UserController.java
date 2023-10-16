package org.glygen.tablemaker.controller;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityExistsException;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/account")
public class UserController {
    //public static Logger logger=(Logger) LoggerFactory.getLogger(UserController.class);
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
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
        return new ResponseEntity<>(new SuccessResponse(users, MessageFormat.format("{0} result found", users.size())), HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse> register(@Valid @RequestBody User user) {
        
        UserEntity newUser = new UserEntity();
        newUser.setUsername(user.getUserName());        
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setEnabled(true);
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
        logger.info("New user {} is added to the system", newUser.getUsername());
        user.setPassword(null);   // Do not send the password
        return ResponseEntity.ok(new SuccessResponse(user, "Registered successfully"));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<SuccessResponse> authenticateUser(@Valid @RequestBody LoginRequest login) {

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken((GlygenUser) authentication.getPrincipal());

        return ResponseEntity.ok(new SuccessResponse(jwt, "Login Successfully"));
    }

}
