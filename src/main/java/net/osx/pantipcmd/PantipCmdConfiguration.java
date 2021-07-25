package net.osx.pantipcmd;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "config.properties")
public class PantipCmdConfiguration {

  @Getter
  @Setter
  private String headUrl;

  @Getter
  @Setter
  private String loginUrl;

  @Getter
  @Setter
  private String logoutUrl;

  @Getter
  @Setter
  private String inboxUrl;
}
