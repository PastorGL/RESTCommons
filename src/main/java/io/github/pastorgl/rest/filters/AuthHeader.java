package io.github.pastorgl.rest.filters;

import org.apache.commons.lang3.StringUtils;

public class AuthHeader {
    public static final AuthHeader EMPTY_AUTH_HEADER = new AuthHeader(null, null);

    public final AuthScheme scheme;
    public final String param;

    private AuthHeader(AuthScheme scheme, String param) {
        this.scheme = scheme;
        this.param = param;
    }

    public static AuthHeader parse(String authorization) {
        if (StringUtils.isBlank(authorization)) {
            return EMPTY_AUTH_HEADER;
        }

        int delimiterIndex = authorization.indexOf(' ');

        if (delimiterIndex < 0) {
            return EMPTY_AUTH_HEADER;
        }

        String authScheme = authorization.substring(0, delimiterIndex);
        String params = authorization.substring(delimiterIndex + 1);

        return new AuthHeader(AuthScheme.valueOf(authScheme.toUpperCase()), params);
    }
}
