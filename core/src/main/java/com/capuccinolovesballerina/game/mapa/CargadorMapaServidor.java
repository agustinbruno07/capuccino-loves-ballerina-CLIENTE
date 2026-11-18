package com.capuccinolovesballerina.game.mapa;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;

/** Usa el lector TMX de LibGDX y sus mismas colisiones, sin cargar PNG ni OpenGL. */
public class CargadorMapaServidor extends TmxMapLoader {
    public CargadorMapaServidor() {
        super(ruta -> {
            FileHandle archivo = new FileHandle(ruta);
            if (archivo.exists()) return archivo;
            archivo = new FileHandle("assets/" + ruta);
            if (archivo.exists()) return archivo;
            // El servidor tambien debe arrancar desde el JAR, sin depender de la carpeta actual.
            return new FileHandle(ruta, FileType.Classpath) {};
        });
    }

    @Override
    public TiledMap load(String ruta) {
        FileHandle archivo = resolve(ruta);
        root = xml.parse(archivo);
        return loadTiledMap(archivo, new Parameters(), nombre -> new TextureRegion());
    }

    @Override
    protected void addStaticTiles(FileHandle archivo, ImageResolver imagenes, TiledMapTileSet tiles,
        Element elemento, Array<Element> elementos, String nombre, int primerId, int ancho, int alto,
        int separacion, int margen, int offsetX, int offsetY, String fuente, int anchoImagen,
        int altoImagen, FileHandle imagen) {
        int cantidad = elemento.getIntAttribute("tilecount", 0);
        if (cantidad == 0 && imagen != null) {
            cantidad = ((anchoImagen - 2 * margen + separacion) / (ancho + separacion))
                * ((altoImagen - 2 * margen + separacion) / (alto + separacion));
        }
        for (int i = 0; i < cantidad; i++) {
            // Solo importan las medidas para interpretar los objetos del tileset.
            TextureRegion region = new TextureRegion() {
                @Override public int getRegionWidth() { return ancho; }
                @Override public int getRegionHeight() { return alto; }
            };
            addStaticTiledMapTile(tiles, region, primerId + i, offsetX, offsetY);
        }
    }
}
