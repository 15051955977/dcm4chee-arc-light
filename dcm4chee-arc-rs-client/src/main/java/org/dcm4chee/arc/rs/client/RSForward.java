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
 * Portions created by the Initial Developer are Copyright (C) 2017-2019
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

package org.dcm4chee.arc.rs.client;

import org.dcm4che3.conf.api.IWebApplicationCache;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.IDWithIssuer;
import org.dcm4che3.json.JSONWriter;
import org.dcm4che3.net.WebApplication;
import org.dcm4che3.util.ByteUtils;
import org.dcm4chee.arc.conf.ArchiveAEExtension;
import org.dcm4chee.arc.conf.RSOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;

/**
 * @author Vrinda Nayak <vrinda.nayak@j4care.com>
 * @since Aug 2017
 */

@ApplicationScoped
public class RSForward {

    private static final Logger LOG = LoggerFactory.getLogger(RSForward.class);

    @Inject
    private RSClient rsClient;

    @Inject
    private IWebApplicationCache iWebAppCache;

    public void forward(RSOperation rsOp, ArchiveAEExtension arcAE, Attributes attrs, HttpServletRequest request) {
        forward(rsOp, arcAE, toContent(attrs),
                rsOp == RSOperation.CreatePatient ? IDWithIssuer.pidOf(attrs) : null, request);
    }

    public void forward(
            RSOperation rsOp, ArchiveAEExtension arcAE, byte[] in, IDWithIssuer patientID, HttpServletRequest request) {
        LOG.info("Restful Service Forward invoked for {} {}?{} from {}@{}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                request.getRemoteUser(),
                request.getRemoteHost());
        String requestURI = request.getRequestURI();
        String appendURI = requestURI.substring(requestURI.indexOf("/rs/") + 4);

        arcAE.findRSForwardRules(rsOp, request).forEach(rule -> {
            try {
                WebApplication webApplication = iWebAppCache.findWebApplication(rule.getWebAppName());
                if (webApplication.containsServiceClass(WebApplication.ServiceClass.DCM4CHEE_ARC_AET)) {
                    String serviceURL = webApplication.getServiceURL().toString();
                    String targetURI = serviceURL + (serviceURL.endsWith("rs/") ? "" : "/") + appendURI;
                    if (!targetURI.equals(request.getRequestURL().toString())) {
                        if (rsOp == RSOperation.CreatePatient)
                            targetURI += patientID;
                        rsClient.scheduleRequest(
                                getMethod(rsOp),
                                targetURI,
                                in,
                                webApplication.getKeycloakClientID(),
                                rule.isTlsAllowAnyHostname(),
                                rule.isTlsDisableTrustManager());
                        LOG.info("Forwarded RSOperation {} {} {}?{} from {}@{} using RSForward rule {} to device {}. Target URL is {}",
                                rsOp,
                                request.getMethod(),
                                request.getRequestURI(),
                                request.getQueryString(),
                                request.getRemoteUser(),
                                request.getRemoteHost(),
                                rule,
                                webApplication.getDevice().getDeviceName(),
                                targetURI);
                    }
                }
            } catch (Exception e) {
                LOG.warn("Failed to apply RSForwardRule {} to {} {}?{} from {}@{} for RSOperation {} :\n",
                        rule,
                        request.getMethod(),
                        request.getRequestURI(),
                        request.getQueryString(),
                        request.getRemoteUser(),
                        request.getRemoteHost(),
                        rsOp, e);
            }
        });
    }

    private static byte[] toContent(Attributes attrs) {
        if (attrs == null)
            return ByteUtils.EMPTY_BYTES;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator gen = Json.createGenerator(out)) {
            new JSONWriter(gen).write(attrs);
        }
        return out.toByteArray();
    }

    private String getMethod(RSOperation rsOp) {
        String method = null;
        switch (rsOp) {
            case CreatePatient:
            case UpdatePatient:
            case UpdateStudyExpirationDate:
            case UpdateSeriesExpirationDate:
            case UpdateStudyAccessControlID:
                method = "PUT";
                break;
            case ChangePatientID:
            case MergePatient:
            case MergePatients:
            case UpdateStudy:
            case RejectStudy:
            case RejectSeries:
            case RejectInstance:
            case CreateMWL:
            case UpdateMWL:
            case ApplyRetentionPolicy:
                method = "POST";
                break;
            case DeletePatient:
            case DeleteStudy:
            case DeleteMWL:
                method = "DELETE";
                break;
        }
        return method;
    }
}
