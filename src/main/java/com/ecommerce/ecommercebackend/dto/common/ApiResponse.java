package com.ecommerce.ecommercebackend.dto.common;

import lombok.Data;

@Data
// @JsonInclude(JsonInclude.Include.NON_NULL) // 🌟 Magic: Hides null fields from the final JSON
public class ApiResponse<T> {

  // Present only on success, completely disappears on error
  private T data;

  // Present only on error, completely disappears on success
  private ErrorDetails error;

  /**
   * Generates a successful response containing the payload. The 'error' field remains null and will
   * be omitted from the JSON.
   */
  public static <T> ApiResponse<T> success(T data) {
    ApiResponse<T> response = new ApiResponse<>();
    response.setData(data);
    return response;
  }

  /**
   * Generates a failed response with a code and message. The 'data' field remains null and will be
   * omitted from the JSON.
   */
  public static <T> ApiResponse<T> error(String code, String message) {
    ApiResponse<T> response = new ApiResponse<>();
    response.setError(new ErrorDetails(code, message));
    return response;
  }

  /** Generates a failed response with additional details (e.g., field validation errors). */
  public static <T> ApiResponse<T> error(String code, String message, Object details) {
    ApiResponse<T> response = new ApiResponse<>();
    response.setError(new ErrorDetails(code, message, details));
    return response;
  }
}
