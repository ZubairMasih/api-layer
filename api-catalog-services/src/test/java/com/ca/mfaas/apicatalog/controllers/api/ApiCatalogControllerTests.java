/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.controllers.api;

import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.model.APIService;
import com.ca.mfaas.apicatalog.services.cached.CachedProductFamilyService;
import com.ca.mfaas.apicatalog.services.cached.CachedServicesService;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApiCatalogControllerTests {

    @Mock
    private CachedServicesService cachedServicesService;

    @Mock
    private CachedProductFamilyService cachedProductFamilyService;

    @InjectMocks
    private ApiCatalogController apiCatalogController;

    @Test
    public void whenGetAllContainers_givenNothing_thenReturnContainersWithState() {
        Application service1 = new Application("service-1");
        service1.addInstance(getStandardInstance("service1", InstanceInfo.InstanceStatus.UP));

        Application service2 = new Application("service-2");
        service1.addInstance(getStandardInstance("service2", InstanceInfo.InstanceStatus.DOWN));

        given(this.cachedServicesService.getService("service1")).willReturn(service1);
        given(this.cachedServicesService.getService("service2")).willReturn(service2);
        given(this.cachedProductFamilyService.getAllContainers()).willReturn(createContainers());

        RestAssuredMockMvc.standaloneSetup(apiCatalogController);
        RestAssuredMockMvc.given().
            when().
            get("/containers").
            then().
            statusCode(200);
    }

    // =========================================== Helper Methods ===========================================

    private List<APIContainer> createContainers() {
        Set<APIService> services = new HashSet<>();

        APIService service = new APIService("service1", "service-1", "service-1", false, "home");
        services.add(service);

        service = new APIService("service2", "service-2", "service-2", true, "home");
        services.add(service);

        APIContainer container = new APIContainer("api-one", "API One", "This is API One", services);

        APIContainer container1 = new APIContainer("api-two", "API Two", "This is API Two", services);

        return Arrays.asList(container, container1);
    }

    private InstanceInfo getStandardInstance(String serviceId, InstanceInfo.InstanceStatus status) {
        return new InstanceInfo(serviceId, null, null, "192.168.0.1", null, new InstanceInfo.PortWrapper(true, 9090),
            null, null, null, null, null, null, null, 0, null, "hostname", status, null, null, null, null, null,
            null, null, null, null);
    }
}
