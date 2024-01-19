import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertificateDownloader {

    public static void main(String[] args) {
        String certificateUrl = "https://example.com/certificate.crt";
        String keystorePath = "path/to/keystore.jks";
        String keystorePassword = "your_keystore_password";

        try {
            // Download the certificate from the URL
            URL url = new URL(certificateUrl);
            InputStream in = new BufferedInputStream(url.openStream());
            FileOutputStream out = new FileOutputStream("downloaded_certificate.crt");

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer, 0, 1024)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            in.close();
            out.close();

            // Load the certificate into a Certificate object
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream certInputStream = new FileInputStream("downloaded_certificate.crt");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(certInputStream);

            // Load the keystore
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream ksInputStream = new FileInputStream(keystorePath);
            ks.load(ksInputStream, keystorePassword.toCharArray());

            // Add the certificate to the keystore
            ks.setCertificateEntry("alias_for_certificate", cert);

            // Save the updated keystore
            FileOutputStream ksOutputStream = new FileOutputStream(keystorePath);
            ks.store(ksOutputStream, keystorePassword.toCharArray());

            System.out.println("Certificate added to keystore successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
