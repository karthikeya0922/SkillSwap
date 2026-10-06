package com.skillswap.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "Full name is required") @Size(min = 2, max = 100, message = "Full name must be 2-100 characters") String fullName,
		@NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 150) String email,
		@NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one number") String password,
		@NotBlank(message = "College or university is required") @Size(max = 150) String college,
		@NotBlank(message = "Department is required") @Size(max = 100) String department,
		@NotNull(message = "Year of study is required") @Min(value = 1, message = "Year must be between 1 and 6") @Max(value = 6, message = "Year must be between 1 and 6") Integer yearOfStudy) {
}
