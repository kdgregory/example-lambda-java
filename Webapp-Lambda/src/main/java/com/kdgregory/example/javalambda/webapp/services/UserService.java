// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.services;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.*;

import net.sf.kdgcommons.lang.StringUtil;
import net.sf.kdgcommons.lang.ThreadUtil;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.InvalidJwtSignatureException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kdgregory.example.javalambda.config.Environment;
import com.kdgregory.example.javalambda.webapp.util.Request;
import com.kdgregory.example.javalambda.webapp.util.Response;
import com.kdgregory.example.javalambda.webapp.util.ResponseCodes;
import com.kdgregory.example.javalambda.webapp.util.Tokens;


/**
 *  Manages users: sign-up (with confirmation), sign-in, and token-based
 *  authentication.
 */
public class UserService
{
    /**
     *  All expected parameter names, to avoid typos.
     */
    public static class ParamNames
    {
        public final static String  EMAIL               = "email";
        public final static String  PASSWORD            = "password";
        public final static String  TEMPORARY_PASSWORD  = "temporaryPassword";
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    private Logger logger = LoggerFactory.getLogger(getClass());

    private AWSCognitoIdentityProvider cognitoClient;
    private JwtConsumer jwtConsumer;

    private String cognitoPoolId;
    private String cognitoClientId;


    public UserService()
    {
        cognitoClient = AWSCognitoIdentityProviderClientBuilder.defaultClient();
        cognitoPoolId = Environment.getOrThrow(Environment.COGNITO_POOL_ID);
        cognitoClientId = Environment.getOrThrow(Environment.COGNITO_CLIENT_ID);
    }


    /**
     *  Extracts the tokens that we care about from an authentication result.
     *  This will be used both for initial signin, which supplies both tokens,
     *  and for refresh, which only supplies a new access token. In the latter
     *  case, we need to retain the refresh token from an earlier request, so
     *  give the option of passing in existing tokens.
     */
    private Tokens createAuthTokens(Tokens existing, AuthenticationResultType authResult)
    {
        if (existing == null)
        {
            return new Tokens(authResult.getAccessToken(), authResult.getRefreshToken());
        }
        else
        {
            return new Tokens(authResult.getAccessToken(), existing.getRefreshToken());
        }
    }


    /**
     *  Returns a singleton instance of the key validator. According to http://stackoverflow.com/a/37322865
     *  it's thread-safe, and according to https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples#markdown-header-using-an-https-jwks-endpoint
     *  it will cache the keyspecs so we only contact Amazon the first time through.
     *  <p>
     *  Note: I make no attempt to ensure that only one consumer gets produced; if we have
     *  concurrent requests, last one is retained.
     */
    private JwtConsumer getJwtConsumer()
    {
        if (jwtConsumer == null)
        {
            try
            {
                // if we're not on Lambda then the environment variable won't be set and nothing will work
                String endpoint = String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json",
                                                System.getenv("AWS_REGION"),
                                                cognitoPoolId);

                String issuer = String.format("https://cognito-idp.%s.amazonaws.com/%s",
                                              System.getenv("AWS_REGION"),
                                              cognitoPoolId);

                jwtConsumer = new JwtConsumerBuilder()
                              .setVerificationKeyResolver(new HttpsJwksVerificationKeyResolver(new HttpsJwks(endpoint)))
                              .setJweAlgorithmConstraints(AlgorithmConstraints.DISALLOW_NONE)
                              .setRequireExpirationTime()
                              .setExpectedIssuer(issuer)
                              .build();
            }
            catch (Exception ex)
            {
                logger.error("unable to create JwtConsumer", ex);
                throw new UnhandledServiceException();
            }
        }

        return jwtConsumer;
    }


    /**
     *  Checks an access token for validity. If valid, we return the username so it can
     *  be logged. If invalid (for whatever reason) we return null. For now, this is
     *  sufficient as an auth check.
     */
    private String checkAccessToken(String accessToken)
    throws InvalidJwtSignatureException
    {
        try
        {
            JwtClaims claims = getJwtConsumer().process(accessToken).getJwtClaims();
            return (String)claims.getClaimValue("username");
        }
        catch (InvalidJwtException ignored)
        {
            // in practice, if we get a correctly signed token, the only reason for
            // it to be invalid is if it's expired
            return null;
        }
    }


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    /**
     *  Attempts to sign-in an existing user.
     *  <p>
     *  Expected parameters:
     *  <dl>
     *  <dt> email
     *  <dd> The user's email address, which serves as the account identifier.
     *  <dt> password
     *  <dd> The user's password.
     *  </dl>
     */
    public Response signIn(Request request)
    {
        String emailAddress = (String)request.getBody().get(ParamNames.EMAIL);
        String password = (String)request.getBody().get(ParamNames.PASSWORD);
        if (StringUtil.isBlank(emailAddress) || StringUtil.isBlank(password))
        {
            // log as warning because it ain't coming from our client
            logger.warn("signIn: missing parameters");
            return new Response(ResponseCodes.INVALID_REQUEST, null);
        }

        logger.debug("signIn: {}", emailAddress);

        try
        {
            Map<String,String> authParams = new HashMap<>();
            authParams.put("USERNAME", emailAddress);
            authParams.put("PASSWORD", password);

            AdminInitiateAuthRequest authRequest = new AdminInitiateAuthRequest()
                    .withAuthFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                    .withAuthParameters(authParams)
                    .withUserPoolId(cognitoPoolId)
                    .withClientId(cognitoClientId);

            AdminInitiateAuthResult authResponse = cognitoClient.adminInitiateAuth(authRequest);
            if (StringUtil.isBlank(authResponse.getChallengeName()))
            {
                logger.debug("signIn: success: {}", emailAddress);
                return new Response(createAuthTokens(null, authResponse.getAuthenticationResult()));
            }
            else if (ChallengeNameType.NEW_PASSWORD_REQUIRED.name().equals(authResponse.getChallengeName()))
            {
                logger.debug("signIn: attempt to sign in with temporary password: {}", emailAddress);
                return new Response(ResponseCodes.TEMPORARY_PASSWORD, null);
            }
            else
            {
                logger.error("signIn: unexpected challenge: {}", authResponse.getChallengeName());
                throw new UnhandledServiceException();
            }
        }
        catch (UserNotFoundException ex)
        {
            logger.debug("signIn: user not found: {}", emailAddress);
            return new Response(ResponseCodes.INVALID_USER, null);

        }
        catch (NotAuthorizedException ex)
        {
            logger.debug("signIn: invalid password: {}", emailAddress);
            return new Response(ResponseCodes.INVALID_USER, null);
        }
        catch (TooManyRequestsException ex)
        {
            logger.warn("signIn: caught TooManyRequestsException, delaying then retrying");
            ThreadUtil.sleepQuietly(250);
            return signIn(request);
        }
    }


    /**
     *  Initiates signup for a new user.
     *  <p>
     *  Expected parameters:
     *  <dl>
     *  <dt> email
     *  <dd> The user's email address, which serves as the account identifier.
     *  </dl>
     */
    public Response signUp(Request request)
    {
        String emailAddress = (String)request.getBody().get(ParamNames.EMAIL);
        if (StringUtil.isBlank(emailAddress))
        {
            // log as warning because it ain't coming from our client
            logger.warn("signUp: missing parameters");
            return new Response(ResponseCodes.INVALID_REQUEST, null);
        }

        logger.debug("signUp: {}", emailAddress);

        try
        {
            AdminCreateUserRequest cognitoRequest = new AdminCreateUserRequest()
                    .withUserPoolId(cognitoPoolId)
                    .withUsername(emailAddress)
                    .withUserAttributes(
                            new AttributeType()
                                .withName("email")
                                .withValue(emailAddress),
                            new AttributeType()
                                .withName("email_verified")
                                .withValue("true"))
                    .withDesiredDeliveryMediums(DeliveryMediumType.EMAIL)
                    .withForceAliasCreation(Boolean.FALSE);

            cognitoClient.adminCreateUser(cognitoRequest);
            return new Response(ResponseCodes.USER_CREATED, null);
        }
        catch (UsernameExistsException ex)
        {
            logger.debug("signUp: user already exists: {}", emailAddress);
            return new Response(ResponseCodes.USER_ALREADY_EXISTS, null);
        }
        catch (TooManyRequestsException ex)
        {
            logger.warn("signUp: caught TooManyRequestsException, delaying then retrying");
            ThreadUtil.sleepQuietly(250);
            return signUp(request);
        }
    }


    /**
     *  Confirms signup for a new user.
     *  <p>
     *  Expected parameters:
     *  <dl>
     *  <dt> email
     *  <dd> The user's email address, which serves as the account identifier.
     *  <dt> temporaryPassword
     *  <dd> The temporary password that Cognito sent to the user.
     *  <dt> password
     *  <dd> The permanent password selected by the user.
     *  </dl>
     */
    public Response confirmSignUp(Request request)
    {
        String emailAddress = (String)request.getBody().get(ParamNames.EMAIL);
        String tempPassword = (String)request.getBody().get(ParamNames.TEMPORARY_PASSWORD);
        String finalPassword = (String)request.getBody().get(ParamNames.PASSWORD);
        if (StringUtil.isBlank(emailAddress) || StringUtil.isBlank(tempPassword) || StringUtil.isBlank(finalPassword))
        {
            // log as warning because it ain't coming from our client
            logger.warn("confirmSignup: missing parameters");
            return new Response(ResponseCodes.INVALID_REQUEST, null);
        }

        logger.debug("confirmSignup: {}", emailAddress);

        try
        {
            // must attempt signin with temporary password in order to establish session for password change
            // (even though it's documented as not required)

            Map<String,String> initialParams = new HashMap<String,String>();
            initialParams.put("USERNAME", emailAddress);
            initialParams.put("PASSWORD", tempPassword);

            AdminInitiateAuthRequest initialRequest = new AdminInitiateAuthRequest()
                    .withAuthFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                    .withAuthParameters(initialParams)
                    .withUserPoolId(cognitoPoolId)
                    .withClientId(cognitoClientId);

            AdminInitiateAuthResult initialResponse = cognitoClient.adminInitiateAuth(initialRequest);
            if (! ChallengeNameType.NEW_PASSWORD_REQUIRED.name().equals(initialResponse.getChallengeName()))
            {
                throw new RuntimeException(
                    String.format("confirmSignup: unexpected challenge: {} for {}",
                                  initialResponse.getChallengeName(), emailAddress));
            }

            Map<String,String> challengeResponses = new HashMap<String,String>();
            challengeResponses.put("USERNAME", emailAddress);
            challengeResponses.put("PASSWORD", tempPassword);
            challengeResponses.put("NEW_PASSWORD", finalPassword);

            AdminRespondToAuthChallengeRequest finalRequest = new AdminRespondToAuthChallengeRequest()
                    .withChallengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
                    .withChallengeResponses(challengeResponses)
                    .withUserPoolId(cognitoPoolId)
                    .withClientId(cognitoClientId)
                    .withSession(initialResponse.getSession());

            AdminRespondToAuthChallengeResult challengeResponse = cognitoClient.adminRespondToAuthChallenge(finalRequest);
            if (StringUtil.isBlank(challengeResponse.getChallengeName()))
            {
                logger.debug("confirmSignUp: success: {}", emailAddress);
                return new Response(createAuthTokens(null, challengeResponse.getAuthenticationResult()));
            }
            else
            {
                logger.error("confirmSignUp: unexpected challenge: {}", challengeResponse.getChallengeName());
                throw new UnhandledServiceException();
            }
        }
        catch (InvalidPasswordException ex)
        {
            logger.debug("confirmSignup: invalid password for {}", emailAddress);
            return new Response(ResponseCodes.INVALID_PASSWORD);
        }
        catch (UserNotFoundException ex)
        {
            logger.debug("confirmSignup: user not found: {}", emailAddress);
            return new Response(ResponseCodes.INVALID_USER, null);
        }
        catch (NotAuthorizedException ex)
        {
            logger.debug("confirmSignup: invalid password: {}", emailAddress);
            return new Response(ResponseCodes.INVALID_USER, null);
        }
        catch (TooManyRequestsException ex)
        {
            logger.warn("confirmSignup: caught TooManyRequestsException, delaying then retrying");
            ThreadUtil.sleepQuietly(250);
            return confirmSignUp(request);
        }
    }


    /**
     *  Checks the tokens, refreshing them if necessary. Returns an appropriate error if
     *  the user isn't authorized, otherwise appends the user info to the request and
     *  invokes the passed operation.
     */
    public Response invokeCheckedOperation(Request request, Function<Request,Response> op)
    {
        String accessToken = request.getTokens().getAccessToken();
        String refreshToken = request.getTokens().getRefreshToken();

        if (StringUtil.isBlank(accessToken) || StringUtil.isBlank(refreshToken))
        {
            logger.debug("missing tokens -- {}; forcing re-auth", request.getTokens());
            return new Response(ResponseCodes.NOT_AUTHENTICATED);
        }

        // first try to validate the access token
        try
        {
            String username = checkAccessToken(accessToken);
            if (username != null)
            {
                logger.debug("checkAuthorization: success: {}", username);
                request.setUser(username);
                return op.apply(request);
            }
            else
            {
                Tokens tokens = attemptRefresh(request.getTokens());
                if (tokens == null)
                {
                    return new Response(ResponseCodes.NOT_AUTHENTICATED);
                }
                else
                {
                    logger.debug("attempting auth check again with {}", tokens);
                    request.setTokens(tokens);
                    return invokeCheckedOperation(request, op);
                }
            }
        }
        catch (InvalidJwtSignatureException ex)
        {
            // this might fill up the logs on a DOS attack, but I think it's useful for review
            logger.debug("checkAuthorization: invalid access token: {}", accessToken);
            return new Response(ResponseCodes.NOT_AUTHENTICATED);
        }
    }


    /**
     *  This handles the refresh logic. It's separate from authenticate() because otherwise
     *  the indentation would grow too large. I'm keeping them physically close, however.
     */
    private Tokens attemptRefresh(Tokens existingTokens)
    {
        logger.debug("attemptRefresh: starting");
        try
        {
            Map<String,String> authParams = new HashMap<String,String>();
            authParams.put("REFRESH_TOKEN", existingTokens.getRefreshToken());

            AdminInitiateAuthRequest refreshRequest = new AdminInitiateAuthRequest()
                                              .withAuthFlow(AuthFlowType.REFRESH_TOKEN)
                                              .withAuthParameters(authParams)
                                              .withUserPoolId(cognitoPoolId)
                                              .withClientId(cognitoClientId);

            AdminInitiateAuthResult refreshResponse = cognitoClient.adminInitiateAuth(refreshRequest);
            if (StringUtil.isBlank(refreshResponse.getChallengeName()))
            {
                AuthenticationResultType authResult = refreshResponse.getAuthenticationResult();
                String username = checkAccessToken(authResult.getAccessToken());
                if (username != null)
                {
                    logger.debug("attemptRefresh: success: {}", username);
                    return createAuthTokens(existingTokens, authResult);
                }
                else
                {
                    // this should never happen
                    logger.warn("attemptRefresh: unable to get username after successful refresh: {}", authResult.getAccessToken());
                    return null;
                }
            }
            else
            {
                logger.warn("attemptRefresh: unexpected challenge: {}", refreshResponse.getChallengeName());
                throw new UnhandledServiceException();
            }
        }
        catch (TooManyRequestsException ex)
        {
            logger.warn("attemptRefresh: caught TooManyRequestsException, delaying then retrying");
            ThreadUtil.sleepQuietly(250);
            return attemptRefresh(existingTokens);
        }
        catch (Exception ex)
        {
            logger.error("attemptRefresh: unexpected exception", ex);
            throw new UnhandledServiceException();
        }
    }
}
