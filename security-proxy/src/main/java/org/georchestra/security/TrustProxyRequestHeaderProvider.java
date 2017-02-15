/*
 * Copyright (C) 2009-2016 by the geOrchestra PSC
 *
 * This file is part of geOrchestra.
 *
 * geOrchestra is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * geOrchestra is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * geOrchestra.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.georchestra.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.georchestra.commons.configuration.GeorchestraConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

/**
 * Allows to bypass auth for request coming from trusted proxy
 */
public class TrustProxyRequestHeaderProvider extends HeaderProvider {

    protected static final Log logger = LogFactory.getLog(TrustProxyRequestHeaderProvider.class.getPackage().getName());

    private Set<InetAddress> trustedProxy = new HashSet<InetAddress>();

    @Autowired
    private GeorchestraConfiguration georchestraConfiguration;

    public void init() throws IOException {
        if ((this.georchestraConfiguration != null) && (this.georchestraConfiguration.activated())) {
            trustedProxy.clear();
            String rawValue = this.georchestraConfiguration.getProperty("TrustedProxy");
            if(rawValue != null && rawValue.trim().length() != 0){
                String[] values = rawValue.trim().split("\\s*,\\s*");
                for (String value: values) {
                    InetAddress address = InetAddress.getByName(value);
                    this.trustedProxy.add(address);
                    logger.info("Add " + address + " as trusted proxy");
                }
                logger.info("Final trusted proxy list : " + this.trustedProxy);
            }
        }
    }

    @Override
    protected Collection<Header> getCustomRequestHeaders(HttpSession session, HttpServletRequest originalRequest) {

        String username = originalRequest.getHeader(HeaderNames.SEC_USERNAME);

        if (username != null) {
            Authentication auth = new UsernamePasswordAuthenticationToken(username, null);
            SecurityContextHolder.getContext().setAuthentication(auth);
            logger.info("Logged user " + username + " from trusted request from " + originalRequest.getRemoteAddr());
        }
        return Collections.emptyList();
///            headers.add(new BasicHeader(HeaderNames.SEC_USERNAME, originalRequest.getHeader(HeaderNames.IMP_USERNAME)));
//            headers.add(new BasicHeader(HeaderNames.SEC_ROLES, originalRequest.getHeader(HeaderNames.IMP_ROLES)));

//              public static final String PROTECTED_HEADER_PREFIX = "sec-";
//    public static final String SEC_USERNAME = "sec-username";
//    public static final String SEC_ROLES = "sec-roles";
//    public static final String REFERER_HEADER_NAME = "referer";
//    public static final String IMP_ROLES = "imp-roles";
//    public static final String IMP_USERNAME = "imp-username";
//    public static final String JSESSION_ID = "JSESSIONID";
//    public static final String SET_COOKIE_ID ="Set-Cookie";
//    public static final String COOKIE_ID ="Cookie";
//    public static final String CONTENT_LENGTH = "content-length";
//    public static final String ACCEPT_ENCODING = "Accept-Encoding";
//    public static final String HOST = "host";
//    public static final String SEC_PROXY = "sec-proxy";
//    public static final String LOCATION = "location";
//    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
//    public static final String CHUNKED = "chunked";


    }

}
