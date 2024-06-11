import com.auth0.jwt.algorithms.*;
import com.auth0.jwt.exceptions.*;

import java.util.*;

public class JWT {
    static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    static final Algorithm hs256 = Algorithm.HMAC256(Base64.getDecoder().decode("CGWpjarkRIXzCIIw5vXKc+uESy5ebrbOyVMZvftj19k="));
    final String token, payload;

    JWT(String login, String nonce) {
        this.payload = "{\"login\":\"" + login + "\",\"nonce\":\"" + nonce + "\"}";
        this.token = com.auth0.jwt.JWT.create().withHeader(HEADER).withPayload(payload).sign(hs256);
    }

    JWT(String token) {
        this.token = token;
        String[] items = token.split("\\.");
        if (items.length > 1) {
            this.payload = new String(Base64.getDecoder().decode(items[1]));
        } else {
            this.payload = "";
        }
    }

    String toJSON() {
        return "\"" + token + "\"";
    }

    String payload() {
        return payload;
    }

    boolean isValid() {
        try {
            com.auth0.jwt.JWT.require(hs256).build().verify(token);
            return true;
        } catch (SignatureVerificationException | JWTDecodeException e) {
            return false;
        }
    }
}
