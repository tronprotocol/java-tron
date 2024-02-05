# How to generate the keys correctly

Step 1: You must encrypt your private key in PKCS8 format and set '123456' password.

```bash
$ openssl pkcs8 -topk8 -in client.key.pem -out encrypted_private_key.p9 -v1 PBE-SHA1-3DES -passout pass:123456
```

Step 2: Copy the content of your encrypted private key and put into
the `client.cer.pem` file in this exact order:
1. your private key
2. your signed certificate
3. any intermediate CA and root CA

The output is something like this

```
-----BEGIN ENCRYPTED PRIVATE KEY-----
xxxx
-----END ENCRYPTED PRIVATE KEY-----

-----BEGIN CERTIFICATE-----
xxxx
-----END CERTIFICATE-----

-----BEGIN CERTIFICATE-----
xxxx
-----END CERTIFICATE-----
```

References:
https://stackoverflow.com/questions/68742912/kafka-returns-no-matching-private-key-entries-in-pem-file-when-attempting-to-s
https://dttung2905.medium.com/kafka-internals-kip-651-using-pem-certificate-for-ssl-listener-679b56cc9c59
