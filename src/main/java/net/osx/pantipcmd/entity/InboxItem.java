package net.osx.pantipcmd.entity;

import java.util.List;
import lombok.Data;

@Data
public class InboxItem {

  private List<InboxMsg> msg;
  private InboxNewMsg new_message;
}
