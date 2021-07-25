package net.osx.pantipcmd.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InboxId {

  @JsonProperty("$oid")
  private String oid;
}
