package com.data.transformation.app.service;

import com.google.gson.JsonObject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.data.transformation.app.shared.Constants.COGNITO_USER_ID;
import static com.data.transformation.app.shared.Constants.IS_CONFIRMED;
import static com.data.transformation.app.shared.Constants.IS_SUCCESSFUL;
import static com.data.transformation.app.shared.Constants.STATUS_CODE;

public class CognitoUserService {

  private final CognitoIdentityProviderClient cognitoIdentityProviderClient;

  public CognitoUserService(String region) {
	this.cognitoIdentityProviderClient = CognitoIdentityProviderClient.builder()
			.region(Region.of(region))
			.build();
  }

  public CognitoUserService(CognitoIdentityProviderClient cognitoIdentityProviderClient) {
	this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
  }

  public JsonObject createUser(JsonObject user, String appClientId, String appClientSecret) {
	String email = user.get("email").getAsString();
	String password = user.get("password").getAsString();
	String userId = UUID.randomUUID().toString();
	String firstName = user.get("firstName").getAsString();
	String lastName = user.get("lastName").getAsString();

	AttributeType emailAttribute = AttributeType.builder()
			.name("email")
			.value(email)
			.build();

	AttributeType nameAttribute = AttributeType.builder()
			.name("name")
			.value(firstName + " " + lastName)
			.build();

	AttributeType userIdAttribute = AttributeType.builder()
			.name("custom:userId")
			.value(userId)
			.build();

	List<AttributeType> attributes = new ArrayList<>();
	attributes.add(emailAttribute);
	attributes.add(nameAttribute);
	attributes.add(userIdAttribute);

	String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, email);

	SignUpRequest signUpRequest = SignUpRequest.builder()
			.username(email)
			.password(password)
			.userAttributes(attributes)
			.clientId(appClientId)
			.secretHash(generatedSecretHash)
			.build();

	SignUpResponse signUpResponse = cognitoIdentityProviderClient.signUp(signUpRequest);

	var createdUserResult = new JsonObject();
	createdUserResult.addProperty(IS_SUCCESSFUL, signUpResponse.sdkHttpResponse().isSuccessful());
	createdUserResult.addProperty(STATUS_CODE, signUpResponse.sdkHttpResponse().statusCode());
	createdUserResult.addProperty(COGNITO_USER_ID, signUpResponse.userSub());
	createdUserResult.addProperty(IS_CONFIRMED, signUpResponse.userConfirmed());
	return createdUserResult;
  }

  public JsonObject confirmSignup(String appClientId, String appClientSecret, String email, String confirmationCode) {
	String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, email);

	var confirmSignUpRequest = ConfirmSignUpRequest.builder()
			.secretHash(generatedSecretHash)
			.username(email)
			.confirmationCode(confirmationCode)
			.clientId(appClientId)
			.build();

	ConfirmSignUpResponse confirmSignUpResponse = cognitoIdentityProviderClient.confirmSignUp(confirmSignUpRequest);

	var confirmUserResponse = new JsonObject();
	confirmUserResponse.addProperty(IS_SUCCESSFUL, confirmSignUpResponse.sdkHttpResponse().isSuccessful());
	confirmUserResponse.addProperty(STATUS_CODE, confirmSignUpResponse.sdkHttpResponse().statusCode());
	return confirmUserResponse;
  }

  public JsonObject userLogin(JsonObject loginDetails, String appClientId, String appClientSecret) {
	String email = loginDetails.get("email").getAsString();
	String password = loginDetails.get("password").getAsString();
	String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, email);

	Map<String, String> authParam = new HashMap<>() {
	  {
		put("USERNAME", email);
		put("PASSWORD", password);
		put("SECRET_HASH", generatedSecretHash);
	  }
	};

	InitiateAuthRequest initiateAuthRequest = InitiateAuthRequest.builder()
			.clientId(appClientId)
			.authFlow(AuthFlowType.USER_PASSWORD_AUTH)
			.authParameters(authParam)
			.build();

	InitiateAuthResponse initiateAuthResponse = cognitoIdentityProviderClient.initiateAuth(initiateAuthRequest);
	AuthenticationResultType authenticationResultType = initiateAuthResponse.authenticationResult();

	var loginUserResult = new JsonObject();
	loginUserResult.addProperty(IS_SUCCESSFUL, initiateAuthResponse.sdkHttpResponse().isSuccessful());
	loginUserResult.addProperty(STATUS_CODE, initiateAuthResponse.sdkHttpResponse().statusCode());
	loginUserResult.addProperty("idToken", authenticationResultType.idToken());
	loginUserResult.addProperty("accessToken", authenticationResultType.accessToken());
	loginUserResult.addProperty("refreshToken", authenticationResultType.refreshToken());
	return loginUserResult;
  }

  public JsonObject addUserToGroup(String groupName, String userName, String userPoolId) {
	var adminAddUserToGroupRequest = AdminAddUserToGroupRequest.builder()
			.groupName(groupName)
			.username(userName)
			.userPoolId(userPoolId)
			.build();

	var adminAddUserToGroupResponse = cognitoIdentityProviderClient.adminAddUserToGroup(adminAddUserToGroupRequest);

	var addUserToGroupResponse = new JsonObject();
	addUserToGroupResponse.addProperty(IS_SUCCESSFUL, adminAddUserToGroupResponse.sdkHttpResponse().isSuccessful());
	addUserToGroupResponse.addProperty(STATUS_CODE, adminAddUserToGroupResponse.sdkHttpResponse().statusCode());
	return addUserToGroupResponse;
  }

  public JsonObject getUser(String accessToken) {
	GetUserRequest getUserRequest = GetUserRequest.builder().accessToken(accessToken).build();
	GetUserResponse getUserResponse = cognitoIdentityProviderClient.getUser(getUserRequest);

	JsonObject getUserResult = new JsonObject();
	getUserResult.addProperty(IS_SUCCESSFUL, getUserResponse.sdkHttpResponse().isSuccessful());
	getUserResult.addProperty(STATUS_CODE, getUserResponse.sdkHttpResponse().statusCode());

	List<AttributeType> userAttributes = getUserResponse.userAttributes();
	JsonObject userDetails = new JsonObject();
	userAttributes.stream().forEach((attribute) -> {
	  userDetails.addProperty(attribute.name(), attribute.value());
	});
	getUserResult.add("user", userDetails);

	return getUserResult;
  }

  public String calculateSecretHash(String userPoolClientId, String userPoolClientSecret, String userName) {
	final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

	SecretKeySpec signingKey = new SecretKeySpec(
			userPoolClientSecret.getBytes(StandardCharsets.UTF_8),
			HMAC_SHA256_ALGORITHM);
	try {
	  Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
	  mac.init(signingKey);
	  mac.update(userName.getBytes(StandardCharsets.UTF_8));
	  byte[] rawHmac = mac.doFinal(userPoolClientId.getBytes(StandardCharsets.UTF_8));
	  return Base64.getEncoder().encodeToString(rawHmac);
	} catch (Exception e) {
	  throw new RuntimeException("Error while calculating ");
	}
  }

}
