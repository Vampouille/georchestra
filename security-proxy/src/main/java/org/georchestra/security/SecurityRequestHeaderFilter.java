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
import org.apache.http.client.methods.HttpRequestBase;
import org.georchestra.commons.configuration.GeorchestraConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Filters out sec-username when not from trusted hosts
 * 
 * @author jeichar
 */
public class SecurityRequestHeaderFilter implements HeaderFilter {
	protected static transient Log log = LogFactory.getLog(SecurityRequestHeaderFilter.class);

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
					log.info("Add " + address + " as trusted proxy");
				}
				log.info("Final trusted proxy list : " + this.trustedProxy);
			}
		}
	}

    @Override
    public boolean filter(String headerName, HttpServletRequest originalRequest, HttpRequestBase proxyRequest) {

		String username = originalRequest.getHeader(HeaderNames.SEC_USERNAME);

		if (username != null) {
			Authentication auth = new UsernamePasswordAuthenticationToken(username, null);
			SecurityContextHolder.getContext().setAuthentication(auth);
			log.info("Logged user " + username + " from trusted request from " + originalRequest.getRemoteAddr());
		}


		return headerName.equalsIgnoreCase(HeaderNames.SEC_USERNAME) ||
				headerName.equalsIgnoreCase(HeaderNames.SEC_ROLES) ||
				headerName.equalsIgnoreCase(HeaderNames.IMP_USERNAME) ||
				headerName.equalsIgnoreCase(HeaderNames.IMP_ROLES);
    }
}
