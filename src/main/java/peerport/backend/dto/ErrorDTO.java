package peerport.backend.dto;

import java.util.Date;

public class ErrorDTO {
    public int status;
    public String message;
    public String timestamp;
    public String path;

    public ErrorDTO(int status, String message, String path) {
        this.status = status;
        this.message = message;
        this.timestamp = new Date().toString();
        this.path = path;
    }
}
