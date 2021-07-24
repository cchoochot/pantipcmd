package net.osx.pantipcmd;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@ShellComponent
@Slf4j
public class PantipShell {

  @Getter
  @Setter
  private List<String> cookies = Collections.emptyList();

  private RestTemplate restTemplate = new RestTemplateBuilder().build();

  @ShellMethod("Head")
  public String head() {
    log.info("Head: {}", getCookies());

    HttpHeaders httpHeaders = restTemplate.headForHeaders("https://pantip.com");

    // return httpHeaders.getAccessControlAllowOrigin();
    return httpHeaders.toSingleValueMap().toString();
  }

  @ShellMethod("Login")
  public String login(String user, String pass) {

    log.info("Login: {}", getCookies());

    // headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    // form data
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("member[email]", user);
    map.add("member[crypted_password]", pass);
    map.add("persistent[remember]", "1");
    map.add("action", "login");

    HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(map, headers);

    ResponseEntity<String> responseEntity = restTemplate
        .postForEntity("https://pantip.com/login/authentication", httpEntity, String.class);

    final List<String> responseCookies = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);
    setCookies(responseCookies);

    String token = Optional.ofNullable(responseCookies).orElse(Collections.emptyList()).stream()
        .filter(item -> item.startsWith("token="))
        .findFirst()
        .orElse(null);

    return StringUtils.hasText(token) ? "Logged in successfully" : "Login fails";
  }

  @ShellMethod("Notifications")
  public String notif() {

    log.info("Notif: {}", getCookies());

    HttpHeaders headers = new HttpHeaders();
    headers.put(HttpHeaders.COOKIE, getCookies());

    ResponseEntity<String> responseEntity = restTemplate.exchange(
        "https://pantip.com/message/private_message/ajax_inbox?p=1",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class);

    return responseEntity.getBody();
  }

  @ShellMethod("Logout")
  public String logout() {

    log.info("Logout: {}", getCookies());

    HttpHeaders headers = new HttpHeaders();
    headers.put(HttpHeaders.COOKIE, getCookies());

    ResponseEntity<String> responseEntity = restTemplate.exchange(
        "https://pantip.com/logout",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class);

    // release cookies
    this.cookies = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);

    return responseEntity.getStatusCode() == HttpStatus.OK ? "Logged out" : "Unknown error";
  }

  // TODO
  //

}
