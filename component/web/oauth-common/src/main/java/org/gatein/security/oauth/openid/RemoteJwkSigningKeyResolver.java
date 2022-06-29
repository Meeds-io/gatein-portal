package org.gatein.security.oauth.openid;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolver;
import io.jsonwebtoken.io.Decoders;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import javax.json.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoteJwkSigningKeyResolver implements SigningKeyResolver {

  private final    String              issuer;


  private final    Object           lock = new Object();

  private volatile       Map<String, Key> keyMap = new HashMap<>();
  private static final Log              LOG    = ExoLogger.getLogger(RemoteJwkSigningKeyResolver.class);


  public RemoteJwkSigningKeyResolver(String issuer) {
    this.issuer = issuer;
  }

  @Override
  public Key resolveSigningKey(JwsHeader header, Claims claims) {
    return getKey(header.getKeyId());
  }

  @Override
  public Key resolveSigningKey(JwsHeader header, String plaintext) {
    return getKey(header.getKeyId());
  }

  private Key getKey(String keyId) {

    // check non synchronized to avoid a lock
    Key result = keyMap.get(keyId);
    if (result != null) {
      return result;
    }

    synchronized (lock) {
      // once synchronized, check the map once again the a previously
      // synchronized thread could have already updated they keys
      result = keyMap.get(keyId);
      if (result != null) {
        return result;
      }

      // finally, fallback to updating the keys, an return a value (or null)
      updateKeys();
      return keyMap.get(keyId);
    }
  }

  private void updateKeys() {

    JsonObject configuration = getJson(issuer + "/.well-known/openid-configuration");
    String jwksUrl = configuration.getString("jwks_uri");

    JsonObject keys = getJson(jwksUrl);

    Map<String, Key> newKeys = keys.getJsonArray("keys").stream()
                                   .map(JsonValue::asJsonObject)
                                   .filter(json -> "sig".equals(json.getString("use")))
                                   .filter(json -> "RSA".equals(json.getString("kty")))
                                   .collect(Collectors.toMap(jsonObject -> jsonObject.getString("kid"), jsonObject -> {
                                     BigInteger modulus = base64ToBigInteger(jsonObject.getString("n"));
                                     BigInteger exponent = base64ToBigInteger(jsonObject.getString("e"));
                                     RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
                                     try {
                                       KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                                       return keyFactory.generatePublic(rsaPublicKeySpec);
                                     } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                                       throw new IllegalStateException("Failed to parse public key");
                                     }
                                   }));

    keyMap = Collections.unmodifiableMap(newKeys);

  }

  private JsonObject getJson(String url) {
    StringBuilder content = new StringBuilder();
    try {
      URL wellKnownUrl = new URL(url); // creating a url object
      URLConnection urlConnection = wellKnownUrl.openConnection(); // creating a urlconnection object
      // wrapping the urlconnection in a bufferedreader
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
      String line;
      // reading from the urlconnection using the bufferedreader
      while ((line = bufferedReader.readLine()) != null) {
        content.append(line + "\n");
      }
      bufferedReader.close();

    } catch (Exception e) {
      LOG.error("Error when reading url {}",url);
    }

    JsonReader reader = Json.createReader(new StringReader(content.toString()));
    return reader.readObject();
  }

  private BigInteger base64ToBigInteger(String value) {
    return new BigInteger(1, Decoders.BASE64URL.decode(value));
  }
}
