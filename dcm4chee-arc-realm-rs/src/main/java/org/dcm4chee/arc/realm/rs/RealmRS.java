/*
 * *** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * J4Care.
 * Portions created by the Initial Developer are Copyright (C) 2015-2017
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * *** END LICENSE BLOCK *****
 */

package org.dcm4chee.arc.realm.rs;

import org.dcm4che3.conf.json.JsonWriter;
import org.jboss.resteasy.annotations.cache.NoCache;

import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.stream.Stream;

import org.dcm4chee.arc.keycloak.KeycloakContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Vrinda Nayak <vrinda.nayak@j4care.com>
 * @since Mar 2016
 */
@RequestScoped
@Path("realm")
public class RealmRS {

    private static final Logger LOG = LoggerFactory.getLogger(RealmRS.class);

    @Context
    private HttpServletRequest request;

    @GET
    @NoCache
    @Produces("application/json")
    public StreamingOutput query() {
        LOG.info("Process GET {} from {}@{}", request.getRequestURI(), request.getRemoteUser(), request.getRemoteHost());
        try {
            return out -> {
                    if (request.getUserPrincipal() != null) {
                        JsonGenerator gen = Json.createGenerator(out);
                        JsonWriter writer = new JsonWriter(gen);
                        gen.writeStartObject();
                        KeycloakContext ctx = KeycloakContext.valueOf(request);
                        writer.writeNotNullOrDef(
                                "auth-server-url",
                                System.getProperty("auth-server-url", "/auth"),
                                null);
                        writer.writeNotNullOrDef("realm", System.getProperty("realm-name"), null);
                        writer.writeNotNullOrDef("token", ctx.getToken(), null);
                        writer.writeNotNullOrDef("user", ctx.getUserName(), null);
                        writer.write("expiration", ctx.getExpiration());
                        writer.write("systemCurrentTime", (int) (System.currentTimeMillis()/1000L));
                        writer.writeNotEmpty("roles", ctx.getUserRoles());
                        writer.writeNotDef("su",
                                Stream.of(ctx.getUserRoles())
                                      .anyMatch(x -> x.equals(System.getProperty("super-user-role"))),
                                false);
                        gen.writeEnd();
                        gen.flush();
                    } else {
                        Writer w = new OutputStreamWriter(out, "UTF-8");
                        w.write("{\"auth-server-url\":null,\"realm\":null,\"token\":null,\"user\":null,\"roles\":[]}");
                        w.flush();
                    }
            };
        } catch (Exception e) {
            throw new WebApplicationException(errResponseAsTextPlain(e));
        }
    }

    private Response errResponseAsTextPlain(Exception e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(exceptionAsString(e))
                .type("text/plain")
                .build();
    }

    private String exceptionAsString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
