package cl.pedidos360.rabbitadmin.admin;

import jakarta.validation.constraints.*;

public final class AdminRequests {

    public static final String NAME_PATTERN =
            "^(?!amq\\.)[A-Za-z0-9][A-Za-z0-9._-]{0,99}$";

    private AdminRequests() {
    }

    public record QueueRequest(
            @NotBlank @Pattern(regexp = NAME_PATTERN) String nombre,
            @Min(1) Long ttlMs,
            @Min(1) Integer maxLength
    ) {
    }

    public enum ExchangeType {
        DIRECT, TOPIC
    }

    public record ExchangeRequest(
            @NotBlank @Pattern(regexp = NAME_PATTERN) String nombre,
            @NotNull ExchangeType tipo
    ) {
    }

    public record BindingRequest(
            @NotBlank @Pattern(regexp = NAME_PATTERN) String cola,
            @NotBlank @Pattern(regexp = NAME_PATTERN) String exchange,
            @NotBlank @Size(max = 255)
            @Pattern(regexp = "[A-Za-z0-9._*#-]+") String routingKey
    ) {
    }
}