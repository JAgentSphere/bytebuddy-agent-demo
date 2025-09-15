package com.burpsuite;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

public class Spy {

    private static final byte[] ENCRYPTION_KEY = "burpr0x!".getBytes();

    public static void rewriteBytes(Object[] objectArray) {
        byte[] data = (byte[]) objectArray[0];
        byte[] decodedData = Base64.getDecoder().decode(data);
        byte[] decryptedData = decrypt(decodedData);
        String decryptedString = new String(decryptedData);
        String[] parts = decryptedString.split("\u0000");
        objectArray[0] = Arrays.copyOf(parts, parts.length - 2);
    }

    private static byte[] decrypt(byte[] data) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(ENCRYPTION_KEY, "DES");
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(2, secretKeySpec);
            return cipher.doFinal(data);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
    }

    // ==========================================================================================
    // POST https://api.licensespring.com/api/v4/activate_license HTTP/1.1
    //Content-Length: 272
    //
    //{"app_ver":"3.0.0","app_name":"Burp Bounty Pro","sdk_ver":"Java SDK null","is_vm":false,"product":"burpbountypro","hardware_id":"B018-99DE-419F-7545-46D8-AB54-F72D-C370-C933-D854-B6D9-10CB-F891-1910-7A72-08ED","license_key":"Puthereyourlicensekeyandclickonactivatebutton"}
    public static byte[] testFilter(String url, byte[] data) throws Exception {
        try {
            if (url.equals("https://api.licensespring.com/api/v4/activate_license")) {
                String req = new String(data);
                String doSign = getText(req, "hardware_id") + "#" + getText(req, "license_key") + "#2099-12-31t14:58:33.213z";
                String sign = getSign(doSign.toLowerCase());
                String json = "{\"license_signature\":\"" + sign + "\",\"license_type\":\"perpetual\",\"is_trial\":false,\"validity_period\":\"2099-12-31T20:28:33.213+05:30\",\"max_activations\":99,\"times_activated\":99,\"transfer_count\":99,\"prevent_vm\":false,\"customer\":{\"email\":\"taiwan@china.cn\",\"first_name\":\"taiwan\",\"last_name\":\"china\",\"company_name\":\"alibaba\",\"phone\":\"+86\",\"reference\":\"love\"},\"product_details\":{\"product_name\":\"Burp Bounty Pro\",\"short_code\":\"burpbountyprostripe\",\"allow_trial\":false,\"trial_days\":0,\"authorization_method\":\"license-key\"},\"allow_overages\":false,\"max_overages\":0,\"is_floating_cloud\":false,\"floating_users\":0,\"floating_timeout\":0}\n";
                return json.getBytes();
            }
            if (url.startsWith("https://api.licensespring.com/api/v4/product_details")) {
                String json = "{\"product_name\":\"Burp Bounty Pro\",\"short_code\":\"burpbountyprostripe\",\"allow_trial\":false,\"trial_days\":0,\"authorization_method\":\"license-key\"}";
                return json.getBytes();
            }
            if (url.startsWith("https://api.licensespring.com/api/v4/check_license")) {
                String doSign2 = getParam(url, "hardware_id") + "#" + getParam(url, "license_key") + "#2099-12-31t14:58:33.213z";
                String sign2 = getSign(doSign2.toLowerCase());
                String json2 = "{\"license_signature\":\"" + sign2 + "\",\"license_type\":\"perpetual\",\"is_trial\":false,\"validity_period\":\"2099-12-31T20:28:33.213+05:30\",\"max_activations\":0,\"times_activated\":0,\"transfer_count\":0,\"prevent_vm\":false,\"customer\":{\"email\":\"taiwan@china.cn\",\"first_name\":\"taiwan\",\"last_name\":\"china\",\"company_name\":\"alibaba\",\"phone\":\"+86\",\"reference\":\"love\"},\"product_details\":{\"product_name\":\"Burp Bounty Pro\",\"short_code\":\"burpbountyprostripe\",\"allow_trial\":false,\"trial_days\":0,\"authorization_method\":\"license-key\"},\"allow_overages\":false,\"max_overages\":0,\"is_floating_cloud\":false,\"floating_users\":0,\"floating_timeout\":0,\"license_active\":true,\"license_enabled\":true,\"is_expired\":false}";
                return json2.getBytes();
            }
            if (url.startsWith("https://api.licensespring.com/api/v4/deactivate_license")) {
                return "license_deactivated".getBytes();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map testMap(String url, byte[] data, Map<String, List<String>> map) throws Exception {
        try {
            if (url.startsWith("https://api.licensespring.com/api/v4/deactivate_license")) {
                String req = new String(data);
                String sign = getSign("\"" + req + "\"");
                map.put("licensesignature", Collections.singletonList(sign));
            } else if (url.startsWith("https://api.licensespring.com/api/v4/")) {
                String req2 = new String(data);
                String sign2 = getSign(req2);
                map.put("licensesignature", Collections.singletonList(sign2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public static String getText(String json, String label) {
        String before = "\"" + label + "\":\"";
        int start = json.indexOf(before);
        if (start != -1) {
            int start2 = start + before.length();
            int end = json.indexOf("\"", start2);
            return json.substring(start2, end);
        }
        return null;
    }

    public static String getParam(String url, String label) {
        String before = label + "=";
        String[] strs = url.substring(url.indexOf(63) + 1).split("&");
        for (String str : strs) {
            if (str.startsWith(before)) {
                return str.substring(before.length());
            }
        }
        return null;
    }


    public static final String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsUxk7dzeAxsBovoq2echnwozKOHJjDtyFds1r8xN9oJCg9ed4pQ1fuLwEyKfzn9rckTIguvgh9Cw7HKhR7fA4Muj4K4XVLTW3xnLpknCFqrRhQgz3lJDsQf8OWgbDqKiOUI3G3lx5X95a8Pr496PbylBTmxSZDD/qN/xqyTtw0azkz9rPFuiwIce2H6xbwZcRkWGbW21siC5ekOj9M0AGFqt5WBwLdcKysWrqzthVr/MJAgdkI7zB2/shgmX/FlqPhkB1PjdRAGjR5meriWCP/Vty2Zi4KTsS6Pq7up9ULO+4ce00xSwwEnLVXVxiotmqd45Lx2MGPc1/5WoZMIVCQIDAQAB";
    public static final String privateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCxTGTt3N4DGwGi+irZ5yGfCjMo4cmMO3IV2zWvzE32gkKD153ilDV+4vATIp/Of2tyRMiC6+CH0LDscqFHt8Dgy6PgrhdUtNbfGcumScIWqtGFCDPeUkOxB/w5aBsOoqI5QjcbeXHlf3lrw+vj3o9vKUFObFJkMP+o3/GrJO3DRrOTP2s8W6LAhx7YfrFvBlxGRYZtbbWyILl6Q6P0zQAYWq3lYHAt1wrKxaurO2FWv8wkCB2QjvMHb+yGCZf8WWo+GQHU+N1EAaNHmZ6uJYI/9W3LZmLgpOxLo+ru6n1Qs77hx7TTFLDASctVdXGKi2ap3jkvHYwY9zX/lahkwhUJAgMBAAECggEADkVC3m5npEJZOGAAcPeMmjt88K5zxYjHXwD86kB8ifnkFq6VM7aQM71aa3/e8wUIhfMJXJhVwzjF9NIpLxeYO7/IWf5JPHUt0llGLgVDzQVExftqCVv/vNESuSArBVuLySYOP3Tf+QPwpv7nDrlMPDtK2WAYpZ3YBiS4U/kt9gipacjSnThx+AhCKGeZ9sVRnj8Rn3rmCJWdsNmoSoCE9q4jJScFbIzOrjSTfPXibrWzs1WvgQCNCyimsMhX4nJXssgt6TXWM1goOkDUCNg2JknMEKDRJpaFcMJXSvcNGtinzFgK0BX2bSaugnEqjuECQEwy4nuDWxpiidLheSeLnQKBgQDlYj69spl/C+oBlmqoK0q78AScZHwtuzozndIHyQyZ/AV7kq7xFMi891WtNaiGFd7GR6oj9TzuESb2skCMpo1MK1RxMVo26Mcr0DkCSHmTo4uMlx/irS0P1GJoWSICADVcRmgZ12JwiPAICpWALDaqmm3RgsAoH2VALFqQq7NuBwKBgQDF3vlhAMDFe+sw7fACKTir3B3hnMqiDPNSWOiNXHxWCYvmNnC6eMvB0F4Z2mcQBq8dAhr2OYc0+R1WoAzCyrJwCSonpem1AP4En10zJlO0Dv9WYq0z5BZ1X3AY3sG7hpGSdVVCv3r/eHlGnF+tCPATTceCSJ1cqlT5dG4JHQegbwKBgQCM1CCI/pnWsk3c46hfzxR3BgkOq3LB8Ozuu7ozJXAjKeOD1q6pPIVx3rgvO35XtB2txlni8bGSx90QIKgYsjiVxxR02kP06j093PzjNfPOfN40VqQw2vmLem1gezix2cbo/CD1nJLHXIthpH3cz0hQvbcmpgurlnrnR1Pi9keXCQKBgEffZxvqBxt0mIhsVPqj+HbMfHof1qaoJ5Xov4fhaTRjQVK8wZOqHvDme9fOMhNrKh5STnLTkJ4YQqTde+UhdVEmsw41wL9DfgE11ceni03jCLJbI2iu182IfhI6j4pLJgNZ5T9aiBXVr8+LK2GR1opcfTSApdAr+rOlq/ZZehS3AoGBALCdpRCyXu8e7iJhOA6j5TLlLJS6ZAwnEFzEjEI2lRkXQKKceNi+HEBNaDHXiVSmTODKXFrRGpSLq2Ea13QjEF1WZXT/QGveBHRkqY3g4DRINA3BMsLPVHkQUokLmbKSNCYQIrRoWGzpJvirPjpdqRbNKVhx9soASAl90IbCq2cZ";

    public static PrivateKey p;

    private static PrivateKey getPrivateKey() throws Exception {
        if (p == null) {
            p = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey)));
        }
        return p;
    }

    public static String getSign(String dover) throws Exception {
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(getPrivateKey());
        byte[] data = dover.getBytes(StandardCharsets.UTF_8);
        sign.update(data);
        byte[] signature = sign.sign();
        return Base64.getEncoder().encodeToString(signature);
    }

    private static void initKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        System.out.println(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        System.out.println(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
    }

    public static void main(String[] args) throws Exception {
        initKeys();
    }
}
