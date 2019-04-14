package com.moparisthebest.dns.listen;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.ExecutorService;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.resolve.Resolver;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class DohController {

    // probably a much better way to do this... later...
    static Resolver resolver = null;
    static ExecutorService executor = null;

    private static final HttpHeaders headers = new HttpHeaders();
    private static final Base64.Decoder decoder = Base64.getUrlDecoder();

    static {
        headers.setContentType(MediaType.parseMediaType("application/dns-message"));
    }

    @RequestMapping(value = "/dns-query", method = {RequestMethod.POST, RequestMethod.PUT})
    public HttpEntity<byte[]> dnsQueryPost(final HttpEntity<byte[]> request) throws Exception {
        return dnsQuery(request.getBody());
    }

    @RequestMapping(value = "/dns-query", method = {RequestMethod.GET})
    public HttpEntity<byte[]> dnsQueryGet(@RequestParam("dns") final String dns) throws Exception {
        return dnsQuery(decoder.decode(dns));
    }

    private static HttpEntity<byte[]> dnsQuery(final byte[] request) throws Exception {
        //return request;
        //return new HttpEntity<>(new byte[]{1});
        //final BaseRequestResponse requestResponse = new BaseRequestResponse(new Packet(ByteBuffer.wrap(request)));
        //System.out.println(requestResponse);
        //debugPacket(requestResponse.getRequest().getBuf());

        //System.out.println(resolver);
        //final byte[] response = resolver.resolveAsync(requestResponse).get().getResponse().getBuf().array();
        final byte[] response = resolver.resolve(new Packet(ByteBuffer.wrap(request))).getBuf().array();

        return new HttpEntity<>(response, headers);
    }
}
