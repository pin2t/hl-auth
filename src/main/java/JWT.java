import com.auth0.jwt.algorithms.*;
import com.auth0.jwt.exceptions.*;

import java.util.*;

public class JWT {
    static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    static final Algorithm hs256 = Algorithm.HMAC256(Base64.getDecoder().decode("CGWpjarkRIXzCIIw5vXKc+uESy5ebrbOyVMZvftj19k="));
    final String token;

    JWT(String login, String nonce) {
        this.token = com.auth0.jwt.JWT.create()
            .withHeader(HEADER)
            .withPayload("{\"login\":\"" + login + "\",\"nonce\":\"" + nonce + "\"}")
            .sign(hs256);
    }

    JWT(String token) {
        this.token = token;
    }

    String toJSON() {
        return "\"" + token + "\"";
    }

    boolean isValid() {
        try {
            com.auth0.jwt.JWT.require(hs256).build().verify(token);
            return true;
        } catch (SignatureVerificationException e) {
            return false;
        }
    }
}
