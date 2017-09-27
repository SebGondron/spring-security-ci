/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.oauth2.client.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.client.user.nimbus.NimbusUserInfoRetriever;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.oidc.client.authentication.OidcClientAuthenticationToken;
import org.springframework.util.Assert;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of an {@link OAuth2UserService} that supports standard <i>OAuth 2.0 Provider's</i>.
 * <p>
 * For standard <i>OAuth 2.0 Provider's</i>, the attribute name (from the <i>UserInfo Response</i>)
 * for the <i>&quot;user's name&quot;</i> is required. This is supplied via the constructor,
 * mapped by <code>URI</code>, which represents the <i>UserInfo Endpoint</i> address.
 * <p>
 * <b>NOTE:</b> Attribute names are <b><i>not</i></b> standardized between providers and therefore will vary.
 * Please consult the provider's API documentation for the set of supported user attribute names.
 * <p>
 * This implementation uses a {@link UserInfoRetriever} to obtain the user attributes
 * of the <i>End-User</i> (resource owner) from the <i>UserInfo Endpoint</i>.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see OAuth2UserService
 * @see DefaultOAuth2User
 * @see UserInfoRetriever
 */
public class DefaultOAuth2UserService implements OAuth2UserService {
	private final Map<URI, String> userNameAttributeNames;
	private UserInfoRetriever userInfoRetriever = new NimbusUserInfoRetriever();

	public DefaultOAuth2UserService(Map<URI, String> userNameAttributeNames) {
		Assert.notEmpty(userNameAttributeNames, "userNameAttributeNames cannot be empty");
		this.userNameAttributeNames = Collections.unmodifiableMap(new LinkedHashMap<>(userNameAttributeNames));
	}

	@Override
	public OAuth2User loadUser(OAuth2ClientAuthenticationToken clientAuthentication) throws OAuth2AuthenticationException {
		if (OidcClientAuthenticationToken.class.isAssignableFrom(clientAuthentication.getClass())) {
			return null;
		}

		URI userInfoUri = URI.create(clientAuthentication.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
		if (!this.getUserNameAttributeNames().containsKey(userInfoUri)) {
			throw new IllegalArgumentException(
				"Missing required \"user name\" attribute name for UserInfo Endpoint: " + userInfoUri.toString());
		}
		String userNameAttributeName = this.getUserNameAttributeNames().get(userInfoUri);

		Map<String, Object> userAttributes = this.getUserInfoRetriever().retrieve(clientAuthentication);
		GrantedAuthority authority = new OAuth2UserAuthority(userAttributes);
		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(authority);

		return new DefaultOAuth2User(authorities, userAttributes, userNameAttributeName);
	}

	protected Map<URI, String> getUserNameAttributeNames() {
		return this.userNameAttributeNames;
	}

	protected UserInfoRetriever getUserInfoRetriever() {
		return this.userInfoRetriever;
	}

	public final void setUserInfoRetriever(UserInfoRetriever userInfoRetriever) {
		Assert.notNull(userInfoRetriever, "userInfoRetriever cannot be null");
		this.userInfoRetriever = userInfoRetriever;
	}
}
