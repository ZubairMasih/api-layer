/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.service.ZaasClient;
import org.zowe.apiml.zaasclient.service.ZaasToken;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CachingControllerTest {
    private static final String SERVICE_ID = "test-service";
    private static final String KEY = "key";
    private static final String VALUE = "value";

    private static final KeyValue KEY_VALUE = new KeyValue(KEY, VALUE);
    private static final ZaasToken TOKEN = new ZaasToken();

    private MockHttpServletRequest mockRequest;

    private Storage mockStorage;
    private ZaasClient mockZaasClient;
    private final MessageService messageService = new YamlMessageService("/caching-log-messages.yml");
    private CachingController underTest;

    @BeforeEach
    void setUp() throws ZaasClientException {
        mockRequest = new MockHttpServletRequest();
        mockStorage = mock(Storage.class);
        mockZaasClient = mock(ZaasClient.class);
        when(mockZaasClient.query(any(HttpServletRequest.class))).thenReturn(TOKEN);

        TOKEN.setUserId(SERVICE_ID);
        underTest = new CachingController(mockStorage, mockZaasClient, messageService);
    }

    @Test
    void givenStorageReturnsValidValue_whenGetByKey_thenReturnProperValue() {
        when(mockStorage.read(SERVICE_ID, KEY)).thenReturn(KEY_VALUE);

        ResponseEntity<?> response = underTest.getValue(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));

        KeyValue body = (KeyValue) response.getBody();
        assertThat(body.getValue(), is(VALUE));
    }

    @Test
    void givenNoKey_whenGetByKey_thenResponseBadRequest() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotProvided", SERVICE_ID).mapToView();

        ResponseEntity<?> response = underTest.getValue(null, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStoreWithNoKey_whenGetByKey_thenResponseNotFound() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", KEY, SERVICE_ID).mapToView();
        when(mockStorage.read(any(), any())).thenReturn(null);

        ResponseEntity<?> response = underTest.getValue(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageReturnsValidValues_whenGetByService_thenReturnProperValues() {
        Map<String, KeyValue> values = new HashMap<>();
        values.put(KEY, new KeyValue("key2", VALUE));
        when(mockStorage.readForService(SERVICE_ID)).thenReturn(values);

        ResponseEntity<?> response = underTest.getAllValues(mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));

        Map<String, KeyValue> result = (Map<String, KeyValue>) response.getBody();
        assertThat(result, is(values));
    }

    @Test
    void givenStorage_whenCreateKey_thenResponseCreated() {
        when(mockStorage.create(SERVICE_ID, KEY_VALUE)).thenReturn(KEY_VALUE);

        ResponseEntity<?> response = underTest.createKey(KEY_VALUE, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenStorageWithExistingKey_whenCreateKey_thenResponseConflict() {
        when(mockStorage.create(SERVICE_ID, KEY_VALUE)).thenReturn(null);
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyCollision", KEY).mapToView();

        ResponseEntity<?> response = underTest.createKey(KEY_VALUE, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.CONFLICT));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageWithKey_whenUpdateKey_thenResponseNoContent() {
        when(mockStorage.update(SERVICE_ID, KEY_VALUE)).thenReturn(KEY_VALUE);

        ResponseEntity<?> response = underTest.update(KEY_VALUE, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenStorageWithNoKey_whenUpdateKey_thenResponseNotFound() {
        when(mockStorage.update(SERVICE_ID, KEY_VALUE)).thenReturn(null);
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", KEY, SERVICE_ID).mapToView();

        ResponseEntity<?> response = underTest.update(KEY_VALUE, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageWithKey_whenDeleteKey_thenResponseNoContent() {
        when(mockStorage.delete(any(), any())).thenReturn(KEY_VALUE);

        ResponseEntity<?> response = underTest.delete(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenNoKey_whenDeleteKey_thenResponseBadRequest() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotProvided").mapToView();

        ResponseEntity<?> response = underTest.delete(null, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageWithNoKey_whenDeleteKey_thenResponseNotFound() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", KEY, SERVICE_ID).mapToView();
        when(mockStorage.delete(any(), any())).thenReturn(null);

        ResponseEntity<?> response = underTest.delete(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenNoPayload_whenValidatePayload_thenResponseBadRequest() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.invalidPayload",
            null, "No KeyValue provided in the payload").mapToView();

        ResponseEntity<?> response = underTest.createKey(null, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @ParameterizedTest
    @MethodSource("provideStringsForGivenBadKeyValue")
    void givenBadKeyValue_whenValidatePayload_thenResponseBadRequest(String key, String value, String errMessage) {
        KeyValue keyValue = new KeyValue(key, value);
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.invalidPayload",
            keyValue, errMessage).mapToView();

        ResponseEntity<?> response = underTest.createKey(keyValue, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    private static Stream<Arguments> provideStringsForGivenBadKeyValue() {
        return Stream.of(
            Arguments.of("key", null, "No value provided in the payload"),
            Arguments.of(null, "value", "No key provided in the payload"),
            Arguments.of("key ", "value", "Key is not alphanumeric")
        );
    }

    @Test
    void givenNoToken_whenQueryToken_thenResponseBadRequest() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.query.tokenNotProvided",
            mockRequest.getRequestURL().toString()).mapToView();
        when(mockZaasClient.query(any(HttpServletRequest.class))).thenThrow(new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED));

        ResponseEntity<?> response = underTest.getValue(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenInvalidToken_whenQueryToken_thenResponseUnauthorized() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.query.invalidToken",
            mockRequest.getRequestURL().toString()).mapToView();
        when(mockZaasClient.query(any(HttpServletRequest.class))).thenThrow(new ZaasClientException(ZaasClientErrorCodes.INVALID_JWT_TOKEN));

        ResponseEntity<?> response = underTest.getValue(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenExpiredToken_whenQueryToken_thenResponseUnauthorized() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.expiredToken",
            mockRequest.getRequestURL().toString()).mapToView();
        when(mockZaasClient.query(any(HttpServletRequest.class))).thenThrow(new ZaasClientException(ZaasClientErrorCodes.EXPIRED_JWT_EXCEPTION));

        ResponseEntity<?> response = underTest.getValue(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenNoGateway_whenQueryToken_thenResponseUnauthorized() throws ZaasClientException {
        ZaasClientException zaasException = new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE, "This is an error");
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.gatewayUnavailable",
            mockRequest.getRequestURL().toString(), zaasException.getMessage()).mapToView();
        when(mockZaasClient.query(any(HttpServletRequest.class))).thenThrow(zaasException);

        ResponseEntity<?> response = underTest.getValue(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenRandomError_whenQueryToken_thenResponseInternalError() throws ZaasClientException {
        Throwable errCause = new Exception("This is an error");
        ZaasClientException zaasException = new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION, errCause);
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.common.internalRequestError",
            mockRequest.getRequestURL().toString(), zaasException.getMessage(), zaasException.getCause()).mapToView();
        when(mockZaasClient.query(any(HttpServletRequest.class))).thenThrow(zaasException);

        ResponseEntity<?> response = underTest.getValue(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(response.getBody(), is(expectedBody));
    }
}
