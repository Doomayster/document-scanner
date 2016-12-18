/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.document.scanner.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.document.scanner.ifaces.ImageWrapper;

/**
 * Quite bad implementation of {@link ImageWrapper} which loads every request
 * from disk and reexecutes every transformation.
 *
 * Stores files inside directory {@code storageDir} naming them with an
 * increased counter. Storage fails if {@code storageDir} contains a file with
 * a name equals to the counter. That means that the directory should be emptied
 * at application shutdown.
 *
 * @author richter
 */
/*
internal implementation notes:
- It's not worth storing images on disk using their MD5 hash as filename since
that only prevents waste of a small amount of disk space until shutdown of the
application in case more than one ImageWrapper is created for the same
BufferedImage. Currently instances are only created when needed and references
passed to data consumers.
*/
public class DefaultImageWrapper implements ImageWrapper {
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultImageWrapper.class);
    private static int storageFileNameCounter = 0;
    static {
        new JFXPanel();
            //initialize JavaFX once
        ImageIO.setUseCache(true);
            //use ImageIO parallel to the cache implemented in
            //DefaultImageWrapper because it can't hurt
        try {
            File imageIOCacheDir = File.createTempFile("document-scanner-image-io-cache", null);
            if(!imageIOCacheDir.delete()) {
                throw new IOException(String.format("deletion of file '%s' failed", imageIOCacheDir.getAbsolutePath()));
            }
            if(!imageIOCacheDir.mkdirs()) {
                throw new IOException(String.format("creation of directory '%s' failed", imageIOCacheDir.getAbsolutePath()));
            }
            ImageIO.setCacheDirectory(imageIOCacheDir);
            LOGGER.debug(String.format("set '%s' as cache directory for ImageIO", imageIOCacheDir.getAbsolutePath()));
        } catch (IOException ex) {
            LOGGER.error("creation of ImageIO cache directory failed, skipping setting of cache");
        }
    }
    private final File storageFile;
    private final int initialWidth;
    private final int initialHeight;
    private double rotationDegrees;

    public DefaultImageWrapper(File storageDir,
            BufferedImage image) throws IOException {
        assert storageDir.exists();
        assert storageDir.isDirectory();
        synchronized(this) {
            this.storageFile = new File(storageDir, String.valueOf(storageFileNameCounter));
            storageFileNameCounter += 1;
        }
        assert !storageFile.exists();
        ImageIO.write(image, "png", this.storageFile);
        this.initialWidth = image.getWidth();
        this.initialHeight = image.getHeight();
    }

    @Override
    public double getRotationDegrees() {
        return this.rotationDegrees;
    }

    @Override
    public void setRotationDegrees(double rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
    }

    /**
     * Gets an {@link InputStream} which allows passing data to an application
     * without loading it into memory.
     * @return an {@link FileInputStream} which allows reading image data
     */
    /*
    internal implementation notes:
    - The only source for the stream which makes sense is a file.
    - Rotation has to occur in memory.
    - Since an update of the UI is usually requested right after the rotation
    because all rotation is requested by GUI components there's no sense in
    rotating asynchronously.
    */
    @Override
    public FileInputStream getOriginalImageStream() throws IOException {
        ImageView imageView = new ImageView(this.storageFile.toURI().toURL().toString());
        imageView.setRotate(rotationDegrees);
        File tmpFile = File.createTempFile("image-wrapper", null);
        Image rotatedImage = imageView.getImage();
        //There's no way to write a JavaFX image into a file without converting
        //it into a Swing/AWT image
        ImageIO.write(SwingFXUtils.fromFXImage(rotatedImage, null), "png", tmpFile);
        FileInputStream retValue = new FileInputStream(tmpFile);
        return retValue;
    }

    /**
     * Since it's overly hard to handle already rotated images in JavaFX (need
     * to figure out whether to set {@link ImageView#setFitWidth(double) } or
     * {@link ImageView#setFitHeight(double) }, etc.) always return the
     * unrotated image and rotate in Swing and JavaFX separately since that's
     * easier to maintain and no computation overhead.
     *
     * @return
     * @throws IOException
     */
    @Override
    public BufferedImage getOriginalImage() throws IOException {
        BufferedImage original = ImageIO.read(this.storageFile);
        return original;
    }

    @Override
    public BufferedImage getImagePreview(int width) throws IOException {
        BufferedImage original = getOriginalImage();
        if(width == initialWidth && rotationDegrees/360 == 0) {
            return original;
        }
        //Implementations using Java AWT are quite complicated (because and don't work,
        //so using JavaFX is fine as a workaround
        WritableImage originalImage = SwingFXUtils.toFXImage(original,
                null //wimg (specifying an existing empty image with desired
                    //width and height doesn't cause the created image to be
                    //scaled)
        );
        ImageView imageView = getImagePreviewFXImageView(originalImage,
                width);
        BufferedImage[] imageValue = new BufferedImage[1];
        //this will be called form Swing EDT
        Platform.runLater(() -> {
            BufferedImage image = SwingFXUtils.fromFXImage(imageView.snapshot(null, null), null);
            imageValue[0] = image;
        });
        while(imageValue[0] == null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        BufferedImage image = imageValue[0];
        return image;

        //Implementation using Graphics2D
//        int width0, height0;
//        if(this.rotationDegrees/90 % 2 == 0) {
//            width0 = width;
//            height0 = getImageHeightScaled(width);
//        }else {
//            width0 = getImageHeightScaled(width);
//            height0 = width;
//        }
//        BufferedImage retValue = new BufferedImage(width0,
//                height0,
//                BufferedImage.TYPE_INT_ARGB);
//        Graphics2D bGr = retValue.createGraphics();
//        bGr.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            //prevents bad quality after scaling
//        bGr.rotate(Math.toRadians(rotationDegrees),
//                original.getWidth()/2,
//                original.getHeight()/2);
//        boolean drawingCompleted = bGr.drawImage(original,
//                0, //x
//                0, //y
//                width0, //width (specifying width and height is as good as specifying the scale factory
//                height0, //height
//                null //imageObserver
//        );
//        assert drawingCompleted;
//        bGr.dispose();

        //Implementation using AffineTransform (causing empty image (except for
        //small white strip in the lower left corner
//        AffineTransform at = new AffineTransform();
//        at.rotate(Math.toRadians(rotationDegrees),
//                original.getWidth()/(double)2, //center for rotating
//                original.getHeight()/(double)2);
//        double scale = width/(double)original.getWidth();
//        at.scale(scale,
//                scale);
//        AffineTransformOp affineTransformOp = new AffineTransformOp(at,
//                new RenderingHints(RenderingHints.KEY_INTERPOLATION,
//                        RenderingHints.VALUE_INTERPOLATION_BILINEAR));
//        retValue = affineTransformOp.filter(original,
//                null);
//        return retValue;
    }

    /**
     * Allows invoking {@link ImageView#snapshot(javafx.scene.SnapshotParameters, javafx.scene.image.WritableImage) }
     * on return value from JavaFX thread and from Swing EDT (inside a
     * {@link Platform#runLater(java.lang.Runnable) } lambda).
     *
     * @param width
     * @return
     */
    private ImageView getImagePreviewFXImageView(WritableImage originalImage,
            int width) throws IOException {
        //Since there's no way to scale a WritableImage without an ImageView
        //<ref>http://stackoverflow.com/questions/35611176/how-can-i-resize-a-javafx-image</ref>
        //use the following:
        ImageView imageView = new ImageView(originalImage);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(width);
            //omitting fitHeight cause the ratio to be preserved
            //setting fitWidth and then rotating avoid to figure out whether
            //fitWidth or fitHeight has to be set depending on rotation
        imageView.setRotate(this.rotationDegrees);
        imageView.setSmooth(false //allows faster scaling
        );
        return imageView;
    }

    @Override
    public WritableImage getImagePreviewFX(int width) throws IOException {
        BufferedImage image = getOriginalImage();
        WritableImage originalImage = SwingFXUtils.toFXImage(image,
                null //wimg (specifying an existing empty image with desired
                    //width and height doesn't cause the created image to be
                    //scaled)
        );
        if(width == initialWidth && rotationDegrees/360 == 0) {
            return originalImage;
        }
        ImageView imageView = getImagePreviewFXImageView(originalImage,
                width);
        WritableImage retValue = imageView.snapshot(null, null);
        return retValue;
    }

    @Override
    public int getImageHeightScaled(int width) {
        int retValue = this.initialHeight*width/this.initialWidth;
        return retValue;
    }

    @Override
    public int getInitialWidth() {
        return initialWidth;
    }

    @Override
    public int getInitialHeight() {
        return initialHeight;
    }
}
