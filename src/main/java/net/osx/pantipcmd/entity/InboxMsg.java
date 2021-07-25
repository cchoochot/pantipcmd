package net.osx.pantipcmd.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class InboxMsg {

  private InboxId id;
  private int leave;
  private int shows;
  private String subject;
  private int cnt_msg;

  private String first_sender;
  private String first_sender_link;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy HH:mm:ss")
  private Date iso_time;
  private String display_time;

  private String unread;

  private List<InboxParticipate> participate;
}

