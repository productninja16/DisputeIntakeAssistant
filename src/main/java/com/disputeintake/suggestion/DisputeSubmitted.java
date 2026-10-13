package com.disputeintake.suggestion;

import java.util.UUID;

/** Published after a dispute is saved; triggers the asynchronous suggestion. */
public record DisputeSubmitted(UUID disputeId) {
}
