package net.osx.pantipcmd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Console;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.osx.pantipcmd.entity.Inbox;
import net.osx.pantipcmd.entity.InboxItem;
import net.osx.pantipcmd.entity.InboxMsg;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.TerminalBuilder;
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
  public String login(/*@ShellOption(defaultValue = "xxx")*/ String user,
      /*@ShellOption(defaultValue = "xxx")*/ String pass) {

    // TODO enter password input
//    String user;
//    String pass;
//    Console console = System.console();
//    if(flag && console != null) {
//      user = console.readLine("[%s]", "Username: ");
//      pass = String.valueOf(console.readPassword("[%s]", "Password: "));
//    } else {
//      LineReader lineReader = LineReaderBuilder.builder().build();
//      user = lineReader.readLine("Username: ");
//      pass = lineReader.readLine("Password: ");
//    }



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

    return split[split.length - 1];
  }

  @ShellMethod("Inbox")
  @ShellMethodAvailability("availabilityCheck")
  public String inbox(@ShellOption(defaultValue = "1") int page) {
    HttpHeaders headers = new HttpHeaders();
    headers.put(HttpHeaders.COOKIE, cookies);

    ResponseEntity<String> responseEntity = restTemplate.exchange(
        config.getInboxUrl() + "?p=" + page,
        HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Inbox inbox;
    try {
      inbox = mapper.readValue(responseEntity.getBody(), Inbox.class);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      inbox = null;
    }

    InboxItem item = inbox.getItem();
    List<InboxMsg> msgList = item.getMsg();

    String subjectList = msgList.stream().map(InboxMsg::getSubject)
        .collect(Collectors.joining(System.lineSeparator()));
    return "Inbox messages: " + msgList.size() + "  [ new messages: " + item.getNew_message()
        .getInbox() + " ]" + System.lineSeparator() + subjectList;
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
