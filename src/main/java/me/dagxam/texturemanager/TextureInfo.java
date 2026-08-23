package me.dagxam.texturemanager;

import java.nio.file.Path;

/**
 * Информация о найденной пользовательской текстуре.
 */
public record TextureInfo(Path source, String relativePath, int width, int height) {
}
