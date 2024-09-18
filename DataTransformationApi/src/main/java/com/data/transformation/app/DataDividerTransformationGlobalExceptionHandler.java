package com.data.transformation.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.data.transformation.app.exception.MyException;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class DataDividerTransformationGlobalExceptionHandler implements RequestHandler<Map<String, String>, Map<String, Integer>> {

  public Map<String, Integer> handleRequest(final Map<String, String> input, final Context context) {
	Map<String, Integer> response = new HashMap<>();
	try {
	  int dividend = Integer.parseInt(input.get("dividend"));
	  int divisor = Integer.parseInt(input.get("divisor"));
	  int result = dividend / divisor;

	  response.put("dividend", dividend);
	  response.put("divisor", divisor);
	  response.put("result", result);
	} catch (Exception ex) {
	  throw new MyException("Exception: " + ex.getMessage(), ex.getCause());
	}
	return response;
  }

}

