package consulo.ui.desktop.internal.image.libraryImage;

import com.intellij.ui.JBColor;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.JBHiDPIScaledImage;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2020-09-30
 */
public class DesktopPngImageImpl extends DesktopInnerImageImpl<DesktopPngImageImpl> {
  private static final Logger LOG = Logger.getInstance(DesktopPngImageImpl.class);

  public static class ImageBytes {
    private volatile byte[] myBytes;
    private BufferedImage myImage;

    public ImageBytes(@Nullable byte[] bytes, @Nullable BufferedImage image) {
      myBytes = bytes;
      myImage = image;
    }

    @Nullable
    public BufferedImage getOrLoad() {
      if(myBytes == null && myImage == null) {
        return null;
      }

      if(myImage != null) {
        return myImage;
      }

      byte[] bytes = myBytes;
      if(bytes != null) {
        try {
          BufferedImage image = ImageIO.read(new UnsyncByteArrayInputStream(bytes));
          myImage = image;
          myBytes = null;
          return image;
        }
        catch (IOException e) {
          LOG.warn(e);
        }
      }

      return myImage;
    }
  }
  
  private final ImageBytes myX1Data;
  private final ImageBytes myX2Data;

  public DesktopPngImageImpl(@Nonnull ImageBytes x1Data, @Nullable ImageBytes x2Data, int width, int height, @Nullable Supplier<ImageFilter> imageFilterSupplier) {
    super(width, height, imageFilterSupplier);

    myX1Data = x1Data;
    myX2Data = x2Data;
  }

  @Nonnull
  @Override
  protected DesktopPngImageImpl withFilter(@Nullable Supplier<ImageFilter> filter) {
    return new DesktopPngImageImpl(myX1Data, myX2Data, myWidth, myHeight, filter);
  }

  @SuppressWarnings("UndesirableClassUsage")
  @Nonnull
  @Override
  protected Image calcImage(@Nonnull Graphics originalGraphics) {
    ImageBytes target = myX1Data;
    float scale = 1f;
    if ((scale = JBUIScale.sysScale((Graphics2D)originalGraphics)) > 1f) {
      target = myX2Data != null ? myX2Data : target;
    }

    BufferedImage bufferedImage = target.getOrLoad();
    if (bufferedImage == null) {
      BufferedImage blueImage = new BufferedImage(myWidth, myHeight, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = blueImage.createGraphics();
      graphics.setColor(JBColor.BLUE);
      graphics.fillRect(0, 0, myWidth, myHeight);
      graphics.dispose();
      return blueImage;
    }

    JBHiDPIScaledImage image = new JBHiDPIScaledImage(bufferedImage, myWidth, myHeight, BufferedImage.TYPE_INT_ARGB);

    image = image.scale(JBUI.scale(1f));

    java.awt.Image toPaintImage = image;
    if (myFilter != null) {
      ImageFilter imageFilter = myFilter.get();
      toPaintImage = ImageUtil.filter(toPaintImage, imageFilter);

      toPaintImage = ImageUtil.ensureHiDPI(toPaintImage, JBUI.ScaleContext.create((Graphics2D)originalGraphics));
    }
    return toPaintImage;
  }
}
