package com.example.ebank.dto;

import java.util.List;

/**
 * @param passwordChangeRequired true while the client has not replaced the one-time password
 */
public record ClientDetailsResponse(ClientResponse client, boolean passwordChangeRequired,
                                    List<AccountResponse> accounts) {
}
