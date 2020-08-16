// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.*;

import net.sf.kdgcommons.lang.StringUtil;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kdgregory.example.javalambda.shared.config.Environment;
import com.kdgregory.example.javalambda.webapp.util.Request;
import com.kdgregory.example.javalambda.webapp.util.Response;


/**
 *  Handles request authorization, validating the provided access token and
 *  (if valid) retrieving the username from Cognito.
 *  <p>
 *  Note: I completely ignore the provided user token (which also contains
 *  the username), because the load balancer team seems to have gone out of
 *  their way to make it difficult to use. Not only does it use the ES256
 *  signature algorithm (which is not available on some versions of OpenJDK),
 *  but the public key is stored as an OpenSSL keyfile rather than a JWKS.
 *  <p>
 *  Note 2: since we're running in Lambda, I make no attempt at thread safety.
 */
public class AuthService
{

//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    private Logger logger = LoggerFactory.getLogger(getClass());

    private AWSCognitoIdentityProvider cognitoClient;
    private JwtConsumer jwtConsumer;

    private String cognitoPoolId;

    // caches sub->email so that we're not constantly hitting Cognito; no need for
    // synchronization because we're running in Lambda; LRU behavior is overkill
    // for a demo app, but would be important in the real world
    private Map<String,String> usernameLookup = new LinkedHashMap<String,String>(100, .75f, true)
    {
        private static final long serialVersionUID = -5369643365198636769L;

        @Override
        protected boolean removeEldestEntry(Entry<String,String> eldest)
        {
            return size() > 1000;
        }
    };


    public AuthService()
    {
        cognitoClient = AWSCognitoIdentityProviderClientBuilder.defaultClient();
        cognitoPoolId = Environment.getOrThrow(Environment.COGNITO_POOL_ID);
    }


    /**
     *  Returns a singleton instance of the key validator. According to
     *  https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples#markdown-header-using-an-https-jwks-endpoint
     *  it will cache the keyspecs so we only contact Amazon the first time through.
     */
    private JwtConsumer getJwtConsumer()
    {
        if (jwtConsumer == null)
        {
            try
            {
                String issuer = String.format("https://cognito-idp.%s.amazonaws.com/%s",
                                              System.getenv("AWS_REGION"),
                                              cognitoPoolId);
                String publicKeyEndpoint = issuer + "/.well-known/jwks.json";

                jwtConsumer = new JwtConsumerBuilder()
                              .setVerificationKeyResolver(new HttpsJwksVerificationKeyResolver(new HttpsJwks(publicKeyEndpoint)))
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
     *  Validates the access token. If successful, retrieves the user's email
     *  address the user's email address from Cognito (or the cache). If
     *  unsuccessful, returns <code>null</code>.
     */
    private String checkAccessToken(String accessToken)
    {
        // this should never happen, but if it does we'll short-circuit
        if (StringUtil.isEmpty(accessToken))
            return null;

        try
        {
            JwtClaims claims = getJwtConsumer().process(accessToken).getJwtClaims();
            return retrieveEmailAddress((String)claims.getClaimValue("sub"));
        }
        catch (InvalidJwtException ignored)
        {
            // in practice, if we get a correctly signed token, the only reason for
            // it to be invalid is if it's expired
            return null;
        }
    }


    private String retrieveEmailAddress(String cognitoUserId)
    {
        String emailAddress = usernameLookup.get(cognitoUserId);
        if (emailAddress != null)
            return emailAddress;

        logger.debug("retrieving email address for {}", cognitoUserId);
        AdminGetUserRequest request = new AdminGetUserRequest()
                                      .withUserPoolId(cognitoPoolId)
                                      .withUsername(cognitoUserId);
        AdminGetUserResult response = cognitoClient.adminGetUser(request);
        for (AttributeType attr : response.getUserAttributes())
        {
            if ("email".equals(attr.getName()))
            {
                emailAddress = attr.getValue();
                usernameLookup.put(cognitoUserId, emailAddress);
                break;
            }
        }

        return emailAddress;
    }


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    /**
     *  Checks the tokens, refreshing them if necessary. Returns an appropriate error if
     *  the user isn't authorized, otherwise appends the user info to the request and
     *  invokes the passed operation.
     */
    public Response invokeCheckedOperation(Request request, Function<Request,Response> op)
    {
        String username = checkAccessToken(request.getAccessToken());
        if (username != null)
        {
            logger.debug("checkAuthorization: {}", username);
            request.setUser(username);
            return op.apply(request);
        }
        else
        {
            logger.debug("checkAuthorization: invalid user");
            return new Response(403);
        }
    }
}
