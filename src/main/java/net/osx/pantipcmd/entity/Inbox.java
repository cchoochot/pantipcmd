package net.osx.pantipcmd.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Inbox {

  private String error;
  private int page;
  private String first_id;
  private String last_id;
  private int max_page;

  private InboxItem item;
}

