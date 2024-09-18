package com.data.transformation.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.data.transformation.app.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class DataTransformationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final CognitoUserService cognitoUserService;
  private final String appClientId;
  private final String appClientSecret;

  public DataTransformationHandler() {
	this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
	this.appClientId = System.getenv("MY_COGNITO_POOL_APP_CLIENT_ID");
	this.appClientSecret = System.getenv("MY_COGNITO_POOL_APP_CLIENT_SECRET");
  }

  public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
	Map<String, String> headers = new HashMap<>();
	headers.put("Content-Type", "application/json");

	var response = new APIGatewayProxyResponseEvent().withHeaders(headers);

	String requestBody = input.getBody();
	LambdaLogger logger = context.getLogger();
	logger.log("Original json body : " + requestBody);

	JsonObject userDetails = JsonParser.parseString(requestBody).getAsJsonObject();

	try {
	  JsonObject createdUserResult = cognitoUserService.createUser(userDetails, appClientId, appClientSecret);
	  response.withStatusCode(200);
	  response.withBody(new Gson().toJson(createdUserResult, JsonObject.class));
	} catch (AwsServiceException ex) {
	  logger.log(ex.awsErrorDetails().errorMessage());
	  response.withStatusCode(500);
	}
	return response;
  }

}


