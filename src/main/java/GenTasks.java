public class GenTasks {
    public static void main(String[] args) {
        for (int i = 0; i < 500000; i++) {
            System.out.println("{\"id\":" + (i + 1) + ",\"method\":\"POST\",\"location\":\"/echo\",\"path\":\"/echo\",\"headers\":{\"X-FORWARDED-FOR\":\"140.248.59.51\"},\"body\":\"{\\\"login\\\":\\\"eoZTBuHoDdX\\\",\\\"name\\\":\\\"name\\\",\\\"phone\\\":\\\"+1234567890\\\",\\\"country\\\":\\\"Russia\\\"}\",\"checks\":{\"code\":200,\"jsonBody\":{\"login\":\"eoZTBuHoDdX\",\"name\":\"name\",\"phone\":\"+1234567890\",\"country\":\"Russia\"}}}");
        }
    }
}
