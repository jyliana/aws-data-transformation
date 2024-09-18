package com.data.transformation.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class DataDividerTransformationMultiplyExceptionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
	Map<String, String> headers = new HashMap<>();
	headers.put("Content-Type", "application/json");

	var response = new APIGatewayProxyResponseEvent().withHeaders(headers);

	Map<String, String> queryStringParameters = input.getQueryStringParameters();
	try {
	  int dividend = Integer.parseInt(queryStringParameters.get("dividend"));
	  int divisor = Integer.parseInt(queryStringParameters.get("divisor"));

	  int result = dividend / divisor;

	  response.withStatusCode(200).withBody("""
		{
		   "dividend": %d,
		   "divisor": %d,
		   "result": %d
		}
		""".formatted(dividend, divisor, result));
	} catch (NumberFormatException | ArithmeticException ex) {
	  response.withStatusCode(500);
	  response.withBody("""
		{
		   "error": %s
		}
		""".formatted(ex.getMessage()));
	  return response;
	}

	return response;
  }

}
