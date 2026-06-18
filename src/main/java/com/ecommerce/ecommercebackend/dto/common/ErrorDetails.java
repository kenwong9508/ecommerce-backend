package com.ecommerce.ecommercebackend.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ErrorDetails {

  private String code;
  private String message;

  // Can hold a String, a Map (for field errors), or be null
  private Object details;

  // Formats the timestamp nicely for frontend consumption
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime timestamp;

  // Constructor for standard errors without additional details
  public ErrorDetails(String code, String message) {
    this.code = code;
    this.message = message;
    this.timestamp = LocalDateTime.now();
  }

  // Constructor for complex errors (e.g., validation failures with field maps)
  public ErrorDetails(String code, String message, Object details) {
    this.code = code;
    this.message = message;
    this.details = details;
    this.timestamp = LocalDateTime.now();
  }
}
