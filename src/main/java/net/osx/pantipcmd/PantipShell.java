package net.osx.pantipcmd;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@ShellComponent
@Slf4j
public class PantipShell {

  @Autowired
  private PantipCmdConfiguration config;

  private boolean isLogin = false;

  private String userId = null;

  private List<String> cookies = Collections.emptyList();

  private RestTemplate restTemplate = new RestTemplateBuilder().build();

  public Availability availabilityCheck() {
    return isLogin
        ? Availability.available()
        : Availability.unavailable("you have not logged in yet.");
  }

  @ShellMethod("Head")
  public String head() {
    HttpHeaders headers = new HttpHeaders();
    headers.put(HttpHeaders.COOKIE, cookies);

    HttpHeaders httpHeaders = restTemplate
        .headForHeaders(config.getHeadUrl(), new HttpEntity<>(headers));
    log.info("head {}", httpHeaders);

    return httpHeaders.toSingleValueMap().toString();
  }

  @ShellMethod("Login")
  public String login(String user, String pass) {
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
        .postForEntity(config.getLoginUrl(), httpEntity, String.class);

    final List<String> responseCookies = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);
    cookies = responseCookies;

    final String token = Optional.ofNullable(responseCookies).orElse(Collections.emptyList())
        .stream()
        .filter(item -> item.startsWith("token="))
        .findFirst()
        .orElse(null);

    isLogin = StringUtils.hasText(token);
    if (isLogin) {
      userId = retrieveUserId(responseEntity);
    }

    return isLogin ? "UserID " + userId + " Logged in successfully" : "Login fails";
  }

  private String retrieveUserId(ResponseEntity<String> responseEntity) {
    String path = responseEntity.getHeaders().getLocation().getPath();
    String[] split = path.split("/");
    String userId = split[split.length - 1];
    return userId;
  }

  @ShellMethod("Notifications")
  @ShellMethodAvailability("availabilityCheck")
  public String notif(@ShellOption(defaultValue = "1") int page) {
    HttpHeaders headers = new HttpHeaders();
    headers.put(HttpHeaders.COOKIE, cookies);

    ResponseEntity<String> responseEntity = restTemplate.exchange(
        config.getNotificationUrl() + "?p=" + page,
        HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class);

    return responseEntity.getBody();
  }

  @ShellMethod("Logout")
  @ShellMethodAvailability("availabilityCheck")
  public String logout() {
    HttpHeaders headers = new HttpHeaders();
    headers.put(HttpHeaders.COOKIE, cookies);

    ResponseEntity<String> responseEntity = restTemplate.exchange(
        config.getLogoutUrl(),
        HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class);

    // release cookies
    cookies = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);

    if (responseEntity.getStatusCode() == HttpStatus.OK) {
      isLogin = false;
    }

    return isLogin ? "Unknown error" : "Logged out";
  }

  // TODO
  //

}
