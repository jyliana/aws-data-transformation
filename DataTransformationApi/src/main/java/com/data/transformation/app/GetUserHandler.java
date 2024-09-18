package com.data.transformation.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.data.transformation.app.service.CognitoUserService;
import com.data.transformation.app.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.HashMap;
import java.util.Map;

public class GetUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final CognitoUserService cognitoUserService;

  public GetUserHandler() {
	this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
	Map<String, String> headers = new HashMap<>();
	headers.put("Content-Type", "application/json");

	var response = new APIGatewayProxyResponseEvent().withHeaders(headers);
	LambdaLogger logger = context.getLogger();
	try {
	  Map<String, String> requestHeaders = input.getHeaders();
	  JsonObject userDetails = cognitoUserService.getUser(requestHeaders.get("AccessToken"));
	  response.withBody(new Gson().toJson(userDetails, JsonObject.class));
	  response.withStatusCode(200);
	} catch (AwsServiceException ex) {
	  logger.log(ex.awsErrorDetails().errorMessage());
	  response.withBody(ex.awsErrorDetails().errorMessage());
	  response.withStatusCode(ex.awsErrorDetails().sdkHttpResponse().statusCode());
	} catch (Exception ex) {
	  logger.log(ex.getMessage());
	  response.withBody(ex.getMessage());
	  response.withStatusCode(500);
	}

	return response;
  }

}
