package com.training.orderservice.client.dto;

/**
 * Wire contract for the Notification Service's email endpoint
 * (POST /api/v1/notifications/email). Deliberately flat — the rich
 * {@link OrderConfirmationRequest} is mapped down onto these fields.
 * templateId is intentionally omitted: the notification send path does not
 * render templates, so we send a fully-composed subject/message instead.
 */
public record EmailNotificationRequest(String recipient, String subject, String message) {
}
