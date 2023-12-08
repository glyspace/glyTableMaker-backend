package org.glygen.tablemaker.security.oauth2;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.RoleRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.security.CustomUserDetails;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userService;
    private final RoleRepository roleRepository;
    private final List<OAuth2UserInfoExtractor> oAuth2UserInfoExtractors;

    public CustomOAuth2UserService(UserRepository userService, List<OAuth2UserInfoExtractor> oAuth2UserInfoExtractors,
            RoleRepository roleRepository) {
        this.userService = userService;
        this.oAuth2UserInfoExtractors = oAuth2UserInfoExtractors;
        this.roleRepository = roleRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Optional<OAuth2UserInfoExtractor> oAuth2UserInfoExtractorOptional = oAuth2UserInfoExtractors.stream()
                .filter(oAuth2UserInfoExtractor -> oAuth2UserInfoExtractor.accepts(userRequest))
                .findFirst();
        if (oAuth2UserInfoExtractorOptional.isEmpty()) {
            throw new InternalAuthenticationServiceException("The OAuth2 provider is not supported yet");
        }

        CustomUserDetails customUserDetails = oAuth2UserInfoExtractorOptional.get().extractUserInfo(oAuth2User);
        UserEntity user = upsertUser(customUserDetails);
        customUserDetails.setId(user.getUserId());
        return customUserDetails;
    }

    private UserEntity upsertUser(CustomUserDetails customUserDetails) {
        UserEntity user = userService.findByEmailIgnoreCase(customUserDetails.getUsername());
        if (user == null) {
            user = new UserEntity();
            user.setUsername(customUserDetails.getUsername());
            user.setFirstName(customUserDetails.getName());
            user.setEmail(customUserDetails.getEmail());
            //user.setImageUrl(customUserDetails.getAvatarUrl());
            user.setLoginType(customUserDetails.getProvider());
            user.setRoles(Arrays.asList(roleRepository.findByRoleName("ROLE_USER")));
        } else {
            user.setEmail(customUserDetails.getEmail());
            //user.setImageUrl(customUserDetails.getAvatarUrl());
        }
        return userService.save(user);
    }
}
