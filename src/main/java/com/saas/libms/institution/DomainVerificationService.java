package com.saas.libms.institution;

import com.saas.libms.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import java.net.http.HttpRequest;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.springframework.stereotype.Service;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class DomainVerificationService {

    //Blocked disposable or personl emails
    private static final Set<String> BLOCKED_DOMAINS = Set.of(
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
            "tempmail.com", "mailinator.com", "guerrillamail.com", "yopmail.com"
    );

    //Extract domain from email
    public String extractEmailDomain(String email) {
        return email.substring(email.indexOf('@') +1).toLowerCase();
    }

    //Extract domain from URL
    public String extractWebsiteDomain(String websiteUrl) {
        try {
            URI uri = new URI(websiteUrl.toLowerCase());
            String host = uri.getHost();
            if (host == null) throw new BadRequestException("Invalid website URL");
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (URISyntaxException e) {
            throw new BadRequestException("Invalid website URL format");
        }
    }

    //Check email domain is not gmail/yahoo
    public void assertNotDisposableOrPersonal(String emailDomain) {
        if (BLOCKED_DOMAINS.contains(emailDomain)) {
            throw new BadRequestException(
                    "Personal or disposable email providers are not allowed. Use your institution email."
            );
        }
    }

    //check email domain matches website domain, sub domains allowed
    public void assertDomainsMatch(String emailDomain, String websiteDomain) {
        boolean exactMatch = emailDomain.equals(websiteDomain);
        boolean subdomainMatch = emailDomain.endsWith("." + websiteDomain);
        if (!exactMatch && !subdomainMatch) {
            throw new BadRequestException(
                    "Email domain (" + emailDomain + ") must match website domain (" + websiteDomain + ")"
            );
        }
    }

    //Check domain exists
    public void assertDomainExists(String domain) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(domain);
            if (addresses == null || addresses.length == 0) {
                throw new BadRequestException("Institution domain could not be resolved. Check your website URL.");
            }
        } catch (UnknownHostException e) {
            throw new BadRequestException("Institution domain not found: " + domain);
        }
    }

    //Check website reachability
    //Check DNS TXT verification record
    public boolean checkDnsTxtRecord(String domain, String expectedValue) {
        try {
            Record[] records = new Lookup(domain, Type.TXT).run();

            if (records == null) {
                return false;
            }

            for (Record record : records) {

                if (record instanceof TXTRecord txt) {

                    for (String value : txt.getStrings()) {

                        if (value.contains(expectedValue)) {
                            return true;
                        }
                    }
                }
            }

            return false;

        } catch (Exception e) {
            log.warn("TXT record check failed for {}: {}", domain, e.getMessage());
            return false;
        }
    }

    public void assertHasMxRecords(String emailDomain) {
        try {
            Record[] records = new Lookup(emailDomain, Type.MX).run();
            if (records == null || records.length == 0) {
                throw new BadRequestException(
                        "No email (MX) records found for domain " + emailDomain + ". Use a valid institution email domain."
                );
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("MX record check failed for {}: {}", emailDomain, e.getMessage());
            throw new BadRequestException("Could not verify email capability for domain: " + emailDomain);
        }
    }


    /**
     * HTTP GET — is the website actually reachable?
     */
    public void assertWebsiteReachable(String websiteUrl) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(websiteUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();

            if (status < 200 || status >= 400) {
                throw new BadRequestException("Institution website is not reachable (HTTP " + status + ")");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Website reachability check failed for {}: {}", websiteUrl, e.getMessage());
            throw new BadRequestException("Could not reach institution website. Please check the URL.");
        }
    }

    /**
     * Generate the DNS TXT record value they need to add.
     * Example: "saaslib-verify=a1b2c3d4"
     */
    public String generateDnsTxtValue() {
        return "saaslib-verify=" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

}
