package com.data.transformation.app;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.data.transformation.app.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.data.transformation.app.shared.Constants.COGNITO_USER_ID;
import static com.data.transformation.app.shared.Constants.IS_CONFIRMED;
import static com.data.transformation.app.shared.Constants.IS_SUCCESSFUL;
import static com.data.transformation.app.shared.Constants.STATUS_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataTransformationHandlerTest {

  @Mock
  private CognitoUserService cognitoUserService;

  @Mock
  private APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;

  @Mock
  private Context context;

  @Mock
  private LambdaLogger logger;

  @InjectMocks
  private CreateUserHandler handler;

  @BeforeEach
  public void init() {
	when(context.getLogger()).thenReturn(logger);
  }

  @Test
  public void testHandlerRequest_whenValidDetailsProvided_returnsSuccessfulResponse() {
	// given
	var userDetails = new JsonObject();
	userDetails.addProperty("firstName", "Inna");
	userDetails.addProperty("lastName", "La.");
	userDetails.addProperty("email", "user@gmail.com");
	userDetails.addProperty("password", "12345678");

	String userDetailsJsonString = new Gson().toJson(userDetails, JsonObject.class);

	when(apiGatewayProxyRequestEvent.getBody()).thenReturn(userDetailsJsonString);

	JsonObject createUserResult = new JsonObject();
	createUserResult.addProperty(IS_SUCCESSFUL, true);
	createUserResult.addProperty(STATUS_CODE, 200);
	createUserResult.addProperty(COGNITO_USER_ID, UUID.randomUUID().toString());
	createUserResult.addProperty(IS_CONFIRMED, false);
	when(cognitoUserService.createUser(any(), any(), any())).thenReturn(createUserResult);

	// when
	APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(apiGatewayProxyRequestEvent, context);
	String responseBody = responseEvent.getBody();
	JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

	// then
	verify(logger, times(1)).log(anyString());
	assertTrue(responseBodyJson.get(IS_SUCCESSFUL).getAsBoolean());
	assertEquals(200, responseBodyJson.get(STATUS_CODE).getAsInt());
	assertNotNull(responseBodyJson.get(COGNITO_USER_ID).getAsString());
	assertEquals(200, responseEvent.getStatusCode());
	assertFalse(createUserResult.get(IS_CONFIRMED).getAsBoolean());
	verify(cognitoUserService, times(1)).createUser(any(), any(), any());
  }

  @Test
  public void testHandlerRequest_whenEmptyRequestBodyProvided_returnedErrorMessage() {
	// given
	when(apiGatewayProxyRequestEvent.getBody()).thenReturn("");

	// when
	APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(apiGatewayProxyRequestEvent, context);
	String responseBody = responseEvent.getBody();

	// then
	assertEquals(500, responseEvent.getStatusCode());
  }

}
