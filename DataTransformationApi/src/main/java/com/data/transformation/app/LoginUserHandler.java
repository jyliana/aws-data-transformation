package com.data.transformation.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.data.transformation.app.response.ErrorResponse;
import com.data.transformation.app.service.CognitoUserService;
import com.data.transformation.app.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.HashMap;
import java.util.Map;

public class LoginUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final CognitoUserService cognitoUserService;
  private final String appClientId;
  private final String appClientSecret;

  public LoginUserHandler() {
	this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
	this.appClientId = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID");
	this.appClientSecret = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET");
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
	Map<String, String> headers = new HashMap<>();
	headers.put("Content-Type", "application/json");

	var response = new APIGatewayProxyResponseEvent().withHeaders(headers);
	LambdaLogger logger = context.getLogger();

	try {
	  JsonObject loginDetails = JsonParser.parseString(input.getBody()).getAsJsonObject();
	  JsonObject loginResult = cognitoUserService.userLogin(loginDetails, appClientId, appClientSecret);
	  response.withBody(new Gson().toJson(loginResult, JsonObject.class));
	  response.withStatusCode(200);
	} catch (AwsServiceException ex) {
	  logger.log(ex.awsErrorDetails().errorMessage());
	  var errorResponse = new ErrorResponse(ex.awsErrorDetails().errorMessage());
	  String errorResponseJsonString = new Gson().toJson(errorResponse, ErrorResponse.class);

	  response.withBody(errorResponseJsonString);
	  response.withStatusCode(ex.awsErrorDetails().sdkHttpResponse().statusCode());
	} catch (Exception ex) {
	  logger.log(ex.getMessage());

	  var errorResponse = new ErrorResponse(ex.getMessage());
	  String errorResponseJsonString = new Gson().toJson(errorResponse, ErrorResponse.class);

	  response.withBody(errorResponseJsonString);
	  response.withStatusCode(500);
	}

	return response;
  }

}
