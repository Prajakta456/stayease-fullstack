package com.stayease.dto;

/**
 * Bean Validation group for constraints that apply only when creating a resource: fields that are
 * required on create may be omitted from an update, which keeps their current value.
 */
public interface OnCreate {
}
