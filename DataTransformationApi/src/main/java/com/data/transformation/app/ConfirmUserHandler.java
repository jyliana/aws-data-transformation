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
import com.google.gson.JsonParser;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

public class ConfirmUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final CognitoUserService cognitoUserService;
  private final String appClientId;
  private final String appClientSecret;

  public ConfirmUserHandler() {
	this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
	this.appClientId = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID");
	this.appClientSecret = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET");
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
	APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

	LambdaLogger logger = context.getLogger();

	try {
	  String requestJsonString = input.getBody();
	  JsonObject requestBody = JsonParser.parseString(requestJsonString).getAsJsonObject();
	  String email = requestBody.get("email").getAsString();
	  String confirmationCode = requestBody.get("code").getAsString();

	  JsonObject confirmUserResult = cognitoUserService.confirmSignup(appClientId, appClientSecret, email, confirmationCode);
	  response.withStatusCode(200);
	  response.withBody(new Gson().toJson(confirmUserResult, JsonObject.class));
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
