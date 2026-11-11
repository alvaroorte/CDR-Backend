package bo.cdr.app.common.custom.validator;

import bo.cdr.app.util.ImageBase64Util;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Base64;

/** Copiado de la referencia (detección por firma de bytes) + límite de tamaño y prefijo data URL. */
public class ImageBase64Validator implements ConstraintValidator<IsImageBase64, String> {

    private long maxBytes;

    @Override
    public void initialize(IsImageBase64 constraintAnnotation) {
        this.maxBytes = constraintAnnotation.maxBytes();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        try {
            byte[] imageBytes = Base64.getDecoder().decode(ImageBase64Util.stripDataUrlPrefix(value));
            return imageBytes.length <= maxBytes && isImage(imageBytes);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isImage(byte[] bytes) {
        if (bytes.length < 12) { // tamaño mínimo para incluir AVIF
            return false;
        }
        return (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) || // JPEG
                (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) || // PNG
                (bytes[0] == (byte) 0x47 && bytes[1] == (byte) 0x49 && bytes[2] == (byte) 0x46) || // GIF
                (bytes[0] == (byte) 0x42 && bytes[1] == (byte) 0x4D) || // BMP
                (bytes[0] == (byte) 0x52 && bytes[1] == (byte) 0x49 && bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x46 &&
                bytes[8] == (byte) 0x57 && bytes[9] == (byte) 0x45 && bytes[10] == (byte) 0x42 && bytes[11] == (byte) 0x50) || // WebP
                ((bytes[4] == (byte) 0x66 && bytes[5] == (byte) 0x74 && bytes[6] == (byte) 0x79 && bytes[7] == (byte) 0x70 &&
                bytes[8] == (byte) 0x61 && bytes[9] == (byte) 0x76 && bytes[10] == (byte) 0x69 && bytes[11] == (byte) 0x66) ||
                (bytes[4] == (byte) 0x66 && bytes[5] == (byte) 0x74 && bytes[6] == (byte) 0x79 && bytes[7] == (byte) 0x70 &&
                bytes[8] == (byte) 0x6D && bytes[9] == (byte) 0x69 && bytes[10] == (byte) 0x66 && bytes[11] == (byte) 0x31)); // AVIF (MIF1)
    }
}
