package nl.captcha.text.renderer;

import java.awt.image.BufferedImage;

/**
 * @author <a href="mailto:james.childers@gmail.com">James Childers</a>
 * 
 */
public interface WordRenderer {
    /**
     * Render a word to a BufferedImage.
     * 
     * @param word The word to be rendered.
     * @param image The BufferedImage created from the word.
     */
    public void render(String word, BufferedImage image);

}
