package com.project.school_management.dto.classroom;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallSignalMessage {

    private String roomCode;
    private String type;
    private String fromPeerId;
    private String toPeerId;
    private String displayName;
    private String sdp;
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;
}
