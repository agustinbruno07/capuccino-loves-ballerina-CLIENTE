package com.capuccinolovesballerina.game.Utilidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import java.util.HashMap;

public class Recursos {


    private static final HashMap<String, Texture> imagenes = new HashMap<String, Texture>();
    private static Skin skin;

    public static Texture getImagen(String ruta) {
        if (!imagenes.containsKey(ruta)) {
            imagenes.put(ruta, new Texture(Gdx.files.internal(ruta)));
        }
        return imagenes.get(ruta);
    }

    public static Skin getSkin() {
        if (skin == null) {
            skin = new Skin(Gdx.files.internal(Constantes.RUTA_SKIN));
            agregarOConAcento();
        }
        return skin;
    }

    /** La fuente original solo incluye ASCII; completa la Ó del aviso de busqueda. */
    private static void agregarOConAcento() {
        BitmapFont fuente = skin.getFont("default-font");
        if (fuente.getData().hasGlyph('Ó')) return;
        BitmapFont.Glyph original = new BitmapFont.BitmapFontData(
            Gdx.files.internal("skin/default.fnt"), false).getGlyph('O');
        Pixmap pagina = new Pixmap(Gdx.files.internal("skin/default.png"));
        Pixmap imagen = new Pixmap(original.width, original.height + 3, Pixmap.Format.RGBA8888);
        imagen.drawPixmap(pagina, 0, 3, original.srcX, original.srcY, original.width, original.height);
        imagen.setColor(1, 1, 1, 1);
        imagen.drawLine(original.width / 2, 2, original.width / 2 + 2, 0);
        Texture textura = new Texture(imagen);
        pagina.dispose();
        imagen.dispose();
        skin.add("letra-o-acentuada", textura, Texture.class); // Skin libera esta textura.
        TextureRegion region = new TextureRegion(textura);
        BitmapFont.Glyph glifo = new BitmapFont.Glyph();
        glifo.id = 'Ó';
        glifo.width = original.width;
        glifo.height = original.height + 3;
        glifo.xoffset = original.xoffset;
        glifo.yoffset = original.yoffset;
        glifo.xadvance = original.xadvance;
        glifo.page = fuente.getRegions().size;
        fuente.getRegions().add(region);
        fuente.getData().setGlyph('Ó', glifo);
        fuente.getData().setGlyphRegion(glifo, region);
    }

    public static void dispose() {
        for (Texture textura : imagenes.values()) {
            textura.dispose();
        }
        imagenes.clear();
        if (skin != null) {
            skin.dispose();
            skin = null;
        }
    }
}
