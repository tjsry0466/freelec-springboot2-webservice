package config.auth;

import com.kyojs.book.springboot.domain.user.User;
import com.kyojs.book.springboot.domain.user.UserRepository;
import config.auth.dto.OAuthAttributes;
import config.auth.dto.SessionUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Collections;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository ;
    private final HttpSession httpSession ;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        //현재 로그인 진행중인 서비스를 구분하는 코드, 지금은 구글만 사용하는 불필요한 기능이지만 이후 네이버 로그인 연동시 필요
        String registrationId = userRequest.getClientRegistration().getRegistrationId() ;

        //OAuth2 로그인 진행 시 키가되는 필드값을 이야기 합니다. PK와 같은 의미, 구글만 기본 코드 sub를 지원
        String userNameAttributedName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName() ;

        //OAuth2UserService를 통해 가져온 OAuth2User의 attribute를 담을 클래스
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributedName, oAuth2User.getAttributes()) ;

        User user = saveOrUpdate(attributes) ;
        //SessionUser : 세션에 사용자 정보를 담기위한 Dto, User 클래스보다 좋음
        httpSession.setAttribute("user", new SessionUser(user));


        return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey());
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByEmail(attributes.getEmail())
                .map(entity -> entity.update(attributes.getName(), attributes.getPicture()))
                .orElse(attributes.toEntity()) ;

        return userRepository.save(user) ;
    }
}