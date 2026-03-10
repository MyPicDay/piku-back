package com.pikume.back.character.application.dto;

public record CharacterImageContent(String fileName, String contentType, byte[] bytes) {
}
