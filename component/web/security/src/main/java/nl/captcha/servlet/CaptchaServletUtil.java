package nl.captcha.servlet;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.servlet.http.HttpServletResponse;

public final class CaptchaServletUtil {

  protected static final Log LOG = ExoLogger.getLogger(CaptchaServletUtil.class);

  private CaptchaServletUtil() {
    // Utils Class, no constructor
  }

  public static void writeImage(HttpServletResponse response, BufferedImage bi) {
    response.setHeader("Cache-Control", "private,no-cache,no-store");
    response.setContentType("image/png"); // PNGs allow for transparency. JPGs
                                          // do not.
    try {
      writeImage(response.getOutputStream(), bi);
    } catch (IOException e) {
      LOG.error("Error writing generated captcha image in HTTP response", e);
    }
  }

  public static void writeImage(OutputStream os, BufferedImage bi) {
    try {
      ImageIO.write(bi, "png", os);
      os.close();
    } catch (IOException e) {
      LOG.error("Error writing generated captcha image", e);
    }
  }
}
